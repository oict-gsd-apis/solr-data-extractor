INTRODUCTION
This application was designed to allow users whether technical or not to extract all Data from a particular collection via the front end of Solr. The reason we required this was that we needed to re-index our data and on some instances we had no original ingestion documents. Having these documents exported allowed us to re-index with ease and ensure DR.

PREREQUISITES
- Java 1.7
- Jar files should be included with solution, which are: commons-codec-1.10.jar & postgresql-9.3-1101.jdbc41.jar. Also available in codebase/jars

SETUP
1. Simply open the solution in your IDE of choice, ensuring there are no errors - potential build path errors for location of Jars
2. Update the config.proporties file to correspond to the information related to your solr instance and query
3. Run the application
4. Optionally you can create a runnable jar file and use this

USES
- Export Solr Documents via the front end easily.