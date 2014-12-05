package org.un.apis.solrdataextractor;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.un.apis.solrdataextractor.AppProp;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.apache.commons.codec.binary.Base64;

/**
 * @author Kevin T Bradley
 * @dateCreated 03 December 2014
 * @description This class is the entry point, and is used to extract Solr Data
 * @version 1.0
 * @codeReviewer: Daniel Buenavad
 * @codeReviewChecklist: efficiency, security, performance, exception handling, comments
 * @codeReviewComments: 
 */
public class Service {

	// Instance variables
	static Date startDate;
	static Date endDate;
	static int fileCount = 0;
	static int errorCount = 0;
	
	static int currentSet = 0; 			// Set to zero to start=0 then increment
	static int numFound = 0; 			// This should match the numFound when you run the full query with start and rows set

	static String outputDirectory = "";
	static OutputPostgres db = null;
	
	public static void main(String[] args) {
		startDate = new Date();
		// Initialise the configuration file to extract the properties
		Helper.initialiseConfigFile();
		
		System.out.println("INFO: Extracting Properties");
		
		// Extract the properties and store these in static properties
		AppProp.collection = Helper.getProperty("collection");
		AppProp.solrUrl = Helper.getProperty("solrUrl");
		AppProp.solrUser = Helper.getProperty("solrUser");
		AppProp.solrPassword = Helper.getProperty("solrPassword");
		AppProp.rowSet = Integer.parseInt(Helper.getProperty("rowSet"));
		AppProp.baseOuputDirectory = Helper.getProperty("baseOuputDirectory");
		AppProp.systemSeperator = Helper.getProperty("systemSeperator");
		
		// Data Variables
		AppProp.postgresLocation = Helper.getProperty("postgresLocation");
		AppProp.postgresUser = Helper.getProperty("postgresUser");
		AppProp.postgresPassword = Helper.getProperty("postgresPassword");
		AppProp.tableName = Helper.getProperty("tableName");
		
		// Filtering Options
		AppProp.language = Helper.getProperty("language");
		AppProp.dateField = Helper.getProperty("dateField");
		AppProp.beginYear = Integer.parseInt(Helper.getProperty("beginYear"));
		AppProp.endYear = Integer.parseInt(Helper.getProperty("endYear"));
		AppProp.sortField = Helper.getProperty("sortField");
		AppProp.sortOrder = Helper.getProperty("sortOrder");
		AppProp.fl = Helper.getProperty("fl");
		AppProp.wt = Helper.getProperty("wt");
		AppProp.query = Helper.getProperty("query");
		AppProp.requestHandler = Helper.getProperty("requestHandler");
		
		// Create a new instance of the Connector to Postgres
		db = new OutputPostgres();
		db.establishConnection();
		// Create the output directory
		Helper.createDirectory(AppProp.baseOuputDirectory);
		
		extractData();
		
		endDate = new Date();
		System.out.println("INFO: Finishing Job at: " + endDate);
		
		long diff = endDate.getTime() - startDate.getTime();
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        
		System.out.println("INFO: Finished Processing");
		System.out.println("==========================");
		System.out.println("Num Found: " + numFound);
		System.out.println("Expected Output Files: " + ((numFound / AppProp.rowSet)+1));
		System.out.println("Actual Output Files: " + fileCount);
		System.out.println("Error Count: " + errorCount);
		if (diffMinutes < 1)
			System.out.println("Time (Seconds): " + diffSeconds);
		else
			System.out.println("Time (Minutes): " + diffMinutes);
	}
		
	/**
    * This function returning a String builds a dynamic query based on the config proporties
    */
	static String buildQuery() {
		String query = AppProp.solrUrl + AppProp.collection + AppProp.systemSeperator;
		query += AppProp.requestHandler + "?q=" + AppProp.query;
		if (!AppProp.dateField.isEmpty() && AppProp.beginYear != 0 && AppProp.endYear != 0 )
			query += "+AND+"+ AppProp.dateField + "%3A[" + AppProp.beginYear + "-01-01T00%3A00%3A01Z+TO+" + AppProp.endYear + "-12-31T00%3A00%3A01Z]+";
		if (!AppProp.language.isEmpty())
			query += "+AND+languageCode%3A%22" + AppProp.language + "%22";
		if (!AppProp.sortField.isEmpty() && !AppProp.sortOrder.isEmpty())
			query += "&sort=" + AppProp.sortField + "+" + AppProp.sortOrder;
		if (!AppProp.fl.isEmpty())
			query += "&fl=" + AppProp.fl;
		return query;
	}
	
	/**
    * This function loops through the results from query and outputs each document to its own xml
    */
	static void extractData() {
		String newId = "";
		String docId = "";

		// Authenticate
		String authString = AppProp.solrUser + ":" + AppProp.solrPassword;
		byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
		String authStringEnc = new String(authEncBytes);
		// Obtain the number found for the query so we can loop exactly
		numFound = getNumFound(authStringEnc);
		
		for (int i = 0; i < numFound; i++) {
			try {
				// Build the dynamic query and pull back only <doc> data
				URL url = new URL(buildQuery() + "&rows=" + AppProp.rowSet + "&start=" + currentSet + "&wt=xslt&tr=updateXml.xsl");
				URLConnection conn = url.openConnection();
				conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			    DocumentBuilder builder = factory.newDocumentBuilder();
			    Document doc = builder.parse(conn.getInputStream()); 
			    // Obtain the document Id used for a KV pair match with the GUID stored in the database
			    docId = getDocId(doc);
			    if (!docId.isEmpty()) {
				    newId = java.util.UUID.randomUUID().toString();
				    TransformerFactory tfactory = TransformerFactory.newInstance();
			        Transformer xform = tfactory.newTransformer();
				    File myOutput = new File(AppProp.baseOuputDirectory + newId + ".xml");
				    //add collection name to the document
				    doc= addCollectionName(doc);
				    // Save the file to the output directory
				    xform.transform(new DOMSource(doc), new StreamResult(myOutput));
				    // Record the entry in the database for review and error checking
				    db.insertRecord(AppProp.collection, docId, newId);
			    } else {
			    	// TODO Add the code to insert into the database if there is an error
			    	System.out.println("ERROR: Could not export Document " + docId);
			    }
			// Error Handling for specific Exceptions
			} catch (MalformedURLException e) {
				System.out.println("ERROR URL: With Subset [start=" + currentSet + "] [DocId:"+ docId +"] " + e.getMessage());
				errorCount++;
			} catch (IOException e) {
				System.out.println("ERROR IO: With Subset [start=" + currentSet + "] [DocId:"+ docId +"] " + e.getMessage());
				errorCount++;
			} catch (ParserConfigurationException e) {
				System.out.println("ERROR PARSER: With Subset [start=" + currentSet + "] [DocId:"+ docId +"] " + e.getMessage());
				errorCount++;
			} catch (SAXException e) {
				System.out.println("ERROR SAX: With Subset [start=" + currentSet + "] [DocId:"+ docId +"] " + e.getMessage());
				errorCount++;
			} catch (TransformerConfigurationException e) {
				System.out.println("ERROR TRANSFORMER CONFIG: With Subset [start=" + currentSet + "] [DocId:"+ docId +"] " + e.getMessage());
				errorCount++;
			} catch (TransformerException e) {
				System.out.println("ERROR TRANFORMER: With Subset [start=" + currentSet + "] [DocId:"+ docId +"] " + e.getMessage());
				errorCount++;
			} catch (StackOverflowError e) {
				System.out.println("ERROR STACK OVERFLOW: With Subset [start=" + currentSet + "] [DocId:"+ docId +"] " + e.getMessage());
				errorCount++;
			} catch (Exception e) {
				System.out.println("ERROR GENERAL: With Subset [start=" + currentSet + "] [DocId:"+ docId +"] " + e.getMessage());
				errorCount++;
			}
			
			System.out.println("INFO: Processed Subset [start=" + currentSet + "] successfully creating file for [DocId:"+ docId +"] - " + currentSet + " [" + outputDirectory + "" + newId + ".xml]");
			// Increment the currentSet thus moving onto the next pagination in Solr
			currentSet += AppProp.rowSet;
		}
	}

	/**
    * This function returning a String gets the document Id based on the Solr Schema
    */	
	static String getDocId(Document doc) {
		String docId = "";
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
		try {
			docId = xpath.evaluate("/add/doc/field[@name='id']/text()", doc);
		} catch (XPathExpressionException e) {
			System.out.println("ERROR: Obtaining Doc Id" + e.getMessage());
		} catch (StackOverflowError e) {
			System.out.println("ERROR STACK OVERFLOW: getDocId " + e.getMessage());
		} catch (Exception e) {
			System.out.println("ERROR GENERAL: getDocId " + e.getMessage());
		}
		return docId;
	}

	
	/**
    * This function add a node to the XML fiile with collection's name
    */	
	static Document addCollectionName(Document doc) {

		Node node = doc.getElementsByTagName("doc").item(0); 
		Text a = doc.createTextNode(AppProp.collection); 
		Element p = doc.createElement("field");
		p.setAttribute("name", "collection");	
		
		try {
			p.appendChild(a);
			node.appendChild(p);  
		} catch (Exception e) {
			System.out.println("ERROR GENERAL: addCollectionName " + e.getMessage());
		}
		return doc;
	}

	/**
    * This function returning an Int returns the number found. This is used to loop
    */	
	static int getNumFound(String authStringEnc) {
		
		int numFound = 0;
		try {
			URL url = new URL(buildQuery() + "&wt=xml&rows=0" );
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder = factory.newDocumentBuilder();
		    Document doc = builder.parse(conn.getInputStream());
		    
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			try {
				javax.xml.xpath.XPathExpression expr = xpath.compile("//response/result[@numFound]");
				NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
				for (int i = 0; i < nl.getLength(); i++)
				{
				    Node currentItem = nl.item(i);
				    String key = currentItem.getAttributes().getNamedItem("numFound").getNodeValue();
				    numFound = Integer.parseInt(key);
				}
			} catch (XPathExpressionException e) {
				System.out.println("ERROR: Obtaining NumFound" + e.getMessage());
			}
		} catch (Exception ex) {
			System.out.println("ERROR: Obtaining NumFound - calling URL" + ex.getMessage());
		}
		return numFound;
	}
}