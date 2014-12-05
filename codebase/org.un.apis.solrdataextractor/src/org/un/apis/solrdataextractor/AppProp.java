package org.un.apis.solrdataextractor;

import java.util.Properties;

/**
 * @author Kevin T Bradley
 * @dateCreated 03 December 2014
 * @description This is a property class which is used to store global variables
 * @version 1.0
 * @codeReviewer: Daniel Buenavad
 * @codeReviewChecklist: efficiency, security, performance, exception handling, comments
 * @codeReviewComments: code review completed on 03 December 2014
 */
public class AppProp {
	
	// Initialisation variables
	static int rowSet;
	static String requestHandler;

	// Collection info & security
	static String collection;
	static String solrUrl;
	static String solrUser;
	static String solrPassword;
	
	// Filter specific variables
	static String dateField;
	static int beginYear;
	static int endYear;
	static String sortField;
	static String sortOrder;
	static String baseOuputDirectory;
	static String systemSeperator;
	static String fl;
	static String wt;
	static String query;
	static String language;
	
	// Database variables
	static String postgresLocation;
	static String postgresUser;
	static String postgresPassword;
	static String tableName;
	
	static Properties configFile;

}
