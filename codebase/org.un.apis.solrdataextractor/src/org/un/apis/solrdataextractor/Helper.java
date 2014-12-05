package org.un.apis.solrdataextractor;

import java.io.File;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author Kevin T Bradley
 * @dateCreated 03 December 2014
 * @description This is a general helper class used to assist in common activites - THIS CLASS IS A CUT DOWN VERSION OF THE GENERAL HELPER
 * @version 1.0
 * @codeReviewer: Daniel Buenavad
 * @codeReviewChecklist: efficiency, security, performance, exception handling, comments
 * @codeReviewComments: 
 */
public class Helper {
	    
	/**
     * This function which returns a Timestamp is used to obtain a Timestamp object for a particular date
     */
	static Timestamp getTimestamp(String sdate){
		if (sdate == null || sdate.equals(""))
			sdate = "1900-01-01T00:00:00Z";
	    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date date = null;
		try {
			date = dateFormat.parse(sdate);
		} catch (Exception e) {
			recordInfo("Error Parsing Date Time(" + sdate + ") - " + e.getMessage());
		}
		long time = date.getTime();
		return new Timestamp(time);
	}
	
	/**
     * This function is used to display and record an application info within the program
     */
    static void recordInfo(String infoMessage) {
    	System.out.println(infoMessage);
    }
    
	/**
     * This function which returns an String is used to identify a single instance of a regular expression pattern
     */
    static String getSingleValue(String fullText, String pattern, String[] keywords) {
    	Pattern p = Pattern.compile(pattern);
    	Matcher m = p.matcher(fullText.toLowerCase());
    	String vals = "";
    	while (m.find()){
    		vals = m.group();
    	}
    	for(String s : keywords) {
    		vals = vals.replace(s, "");
    	}
    	return vals.trim();
    }

	/**
     * This function which returns an String[] is used to identify multiple instances of a regular expression pattern and store these in a String array
     */
    static String[] getMultiValue(String fullText, String startTextPattern, String endTextPattern, String splitter, String[] keywords) {
    	String lowerFullText = fullText.toLowerCase();
    	int startIndex = getIndexOf(lowerFullText, startTextPattern, -1); 
    	int endIndex = getIndexOf(lowerFullText, endTextPattern, startIndex);
    	int startTextLength = startTextPattern.length();
    	String vals;
    	if (startIndex < 0 || endIndex < 0)
    		return new String[] { "" };
    	else
    		vals = lowerFullText.substring(startIndex+startTextLength, endIndex);
    	for(String s : keywords) {
    		vals = vals.replace(s, "");
    	}
    	String cleaned = vals.trim();
    	return cleaned.split(splitter);
    }
    
	/**
     * This function which returns an String is used to Format a date into a UNIX based format yyyy-MM-dd'T'HH:mm:ss'Z'
     */
    static String getFormattedDate(String unformattedDate, String format) {
    	String formattedDate = "";
    	DateFormat df = null;
    	DateFormat sf = null;
    	Date udate = null;
    	
    	if (unformattedDate == null || unformattedDate.equals("")) {
    		unformattedDate = "01 January 1900";
    	}

	    df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    sf = new SimpleDateFormat(format);
	    
		try {
			udate = sf.parse(unformattedDate);
			formattedDate = df.format(udate);
		} catch (Exception e) {
			Helper.recordInfo("ERROR: Formatting Date Time(" + unformattedDate + ") - " + e.getMessage());
			return "1900-01-01T00:00:01Z";
		}
		
	    return formattedDate;
	}
    
	/**
     * This function which returns an String is used to Camel Case text
     */
    static String toCamelCase(final String init) {
        if (init==null)
            return null;

        final StringBuilder ret = new StringBuilder(init.length());

        for (final String word : init.split(" ")) {
            if (!word.isEmpty()) {
                ret.append(word.substring(0, 1).toUpperCase());
                ret.append(word.substring(1).toLowerCase());
            }
            if (!(ret.length()==init.length()))
                ret.append(" ");
        }
        return ret.toString();
    }
    
	/**
     * This function which returns an Int is used to obtain the index of a particular regular expression pattern
     */
    static int getIndexOf(String text, String regex, int startIndex) {
    	int index = -1;
    	Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        // Check all occurrences
        if (startIndex < 1) {
	        while (matcher.find()) {
	        	index = matcher.start();
	        	break;
	        }
        }
        else {
	        while (matcher.find(startIndex)) {
	        	index = matcher.start();
	        	break;
	        } 
        }
        return index;
    }

	/**
     * This function which returns a Timestamp is used to obtain the current Timestamp
     */
	static Timestamp getCurrentTimestamp() {
		long time = System.currentTimeMillis();
		java.sql.Timestamp timestamp = new java.sql.Timestamp(time);
		return timestamp;
	}
	
	/**
     * This function which returns a String simply obtains the property from the config file
     */
	static String getProperty(String name){
		return AppProp.configFile.getProperty(name);
	}
	
	/**
     * This function is used to initalise the configuration file
     */
	static void initialiseConfigFile(){
		try {
			AppProp.configFile = getProperties(Service.class.getClassLoader());
		} catch (Exception e) {
			Helper.recordInfo("ERROR: Error occured whilst attempting to open config file: " + e.getMessage());
		}
	}

	/**
     * This method which returns a Proporty object is used to obtain the properties file and consequent properties for the application
     */
    static Properties getProperties(ClassLoader loader) throws Exception {
    	Properties configFile = new Properties();
    	try {
    		configFile.load(loader.getResourceAsStream("config.properties"));
		} catch (Exception e) {
			Helper.recordInfo("ERROR: Error obtaining config properties - " + e.getMessage());
		}
    	return configFile;
    }
    
	/**
     * This function simply creates a directory
     */
 	static void createDirectory(String name) {
 		File f = new File(name);
 		if (!f.exists()) {
 			f.mkdir();
 		}
 	}
}

