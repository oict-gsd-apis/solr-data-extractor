-- Database: "SolrDocuments"

-- DROP DATABASE "SolrDocuments";

CREATE DATABASE "SolrDocuments"
  WITH OWNER = postgres
       ENCODING = 'UTF8'
       TABLESPACE = pg_default
       LC_COLLATE = 'en_US.UTF-8'
       LC_CTYPE = 'en_US.UTF-8'
       CONNECTION LIMIT = -1;



-- Table: "DocumentsIDs"

-- DROP TABLE "DocumentsIDs";

CREATE TABLE "DocumentsIDs"
(
  "Id" serial NOT NULL,
  "Collection" text,
  "DocId" text,
  "DocGUID" text,
  "Timestamp" timestamp without time zone default current_timestamp
)
WITH (
  OIDS=FALSE
);
ALTER TABLE "DocumentsIDs"
  OWNER TO postgres;



-- Table: "ErrorDocumentsIDs"

-- DROP TABLE "ErrorDocumentsIDs";

CREATE TABLE "ErrorDocumentsIDs"
(
  "Id" serial NOT NULL,
  "Collection" text,
  "DocId" text,
  "Timestamp" timestamp without time zone default current_timestamp
)
WITH (
  OIDS=FALSE
);
ALTER TABLE "ErrorDocumentsIDs"
  OWNER TO postgres;
