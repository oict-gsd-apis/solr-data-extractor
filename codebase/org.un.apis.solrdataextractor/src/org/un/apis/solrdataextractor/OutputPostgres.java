package org.un.apis.solrdataextractor;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kevin T Bradley
 * @dateCreated 03 December 2014
 * @description This class is used as an output to PostGres database
 * @version 1.0
 * @codeReviewer: Daniel Buenavad
 * @codeReviewChecklist: efficiency, security, performance, exception handling, comments
 * @codeReviewComments: 
 */
public class OutputPostgres {

	// Instance variable for the connection
	private Connection connection = null;
	
	/**
    * This function returning a Boolean is used to identify if the application is connected to postgres
    */
	public boolean isConnected() {
		try {
			if (!connection.isClosed())
				return true;
			else
				return false;
		} catch (SQLException e) {
			Helper.recordInfo("ERROR: Checking if connected to Postgres: " + e.getMessage());
			return false;
		}
	}
	
	/**
    * This function is used to establish a connection to the Postgres Database
    */
	public void establishConnection() {
		if (connection == null) {
			try {
				Class.forName("org.postgresql.Driver");
			} catch (ClassNotFoundException e) {
				Helper.recordInfo("ERROR: Attempting to obtain Driver org.postgresql.Driver: " + e.getMessage());
			}
			
			try {
				Helper.recordInfo("INFO: Connecting to Database [Server:" + AppProp.postgresLocation + "] [User: " + AppProp.postgresUser + "] [Password: " + AppProp.postgresPassword + "]");
				connection = DriverManager.getConnection("jdbc:postgresql://" + AppProp.postgresLocation, AppProp.postgresUser, AppProp.postgresPassword);
			} catch (SQLException e) {
				Helper.recordInfo("ERROR: Cannot establish a connection to Postgres " + e.getMessage());
			}
		}
	}
	
	/**
    * This function which returns a Boolean is used to insert a record into the database
    */
	// TODO Make generic using a Map<Integer, Object> params and building a dynamic query
	public boolean insertRecord(String collection, String docid, String docguid) {
		try {			    
		    String qry = "INSERT INTO \""+ AppProp.tableName + "\" (\"Collection\", \"DocId\", \"DocGUID\") VALUES(?, ?, ?)";
		    Map<Integer, Object> params = new HashMap<Integer, Object>();
		    params.put(1, collection);
		    params.put(2, docid);
		    params.put(3, docguid);

		    runQuery(qry, params);
			
			return true;
			
		} catch (Exception e) {
			Helper.recordInfo("ERROR: Inserting record to Postgres table [" + AppProp.tableName + "] " + e.getMessage());
			return false;
		}
	}

	/**
    * This function is used to run a query on the postgres database assigning parameters
    */
	public void runQuery(String query, Map<Integer, Object> params) {
		PreparedStatement pst;
		try {
			pst = this.connection.prepareStatement(query);
			for (Map.Entry<Integer, Object> kv : params.entrySet()) {
				if (kv.getValue().getClass().isAssignableFrom(String.class)) {
					pst.setString(kv.getKey(), (String) kv.getValue());
				}
				if (kv.getValue().getClass().isAssignableFrom(Timestamp.class)) {
					pst.setTimestamp(kv.getKey(), (Timestamp) kv.getValue());
				}	
			}
			pst.executeUpdate();
		} catch (SQLException e) {
			Helper.recordInfo("ERROR: Attempting to run query (" + query + "): " + e.getMessage());
		}
		
	}

	/**
    * This function which returns a Boolean is used to identify if a record exists based on a parameter
    */
	public boolean getExists(String param) {
		Boolean itExists = this.<Boolean>callFunction("recordExists", param, Boolean.class);
		return itExists;
	}
	
	/**
    * This function which returns a Timestamp is used to identify if a record exists based on a parameter
    */
	public Timestamp getLastUpdated(String param) {
		Timestamp ts = this.<Timestamp>callFunction("getLastUpdated", param, Timestamp.class);
		return ts;
	}

	/**
    * This function which returns a T of type Class is used to call a postgres function based on a class type
    */
	@SuppressWarnings("unchecked")
	public <T> T callFunction(String functionName, String param, Class<T> type) {
		
		Object returnParam = null;
		
		if (type.isAssignableFrom(Boolean.class)) {
			returnParam = genericCallFunction(functionName, param, Types.BOOLEAN, new Boolean(false));
		}
		if (type.isAssignableFrom(Timestamp.class)) {
			returnParam = genericCallFunction(functionName, param, Types.TIMESTAMP, new Timestamp(0));
		}

		return (T) returnParam;
	}
	
	/**
    * This function which returns a Object of param type is a generic method to to call a function within Postgres
    */
	public Object genericCallFunction(String functionName, String param, int outputParamType, Object returnParam) {
			
		try {
			CallableStatement funct = this.connection.prepareCall("{ ? = call " + functionName + "( ? ) }");
			funct.registerOutParameter(1, outputParamType);
			funct.setString(2, param);
			funct.execute();
			if (returnParam instanceof Boolean) {
				returnParam = funct.getBoolean(1);
			}
			if (returnParam instanceof Timestamp) {
				returnParam = funct.getTimestamp(1);
			}
			funct.close();
			return returnParam;
		} catch (SQLException e) {
			Helper.recordInfo("ERROR: Attempting to call function(" + functionName + "): " + e.getMessage());
		}
		return null;
	}

}
