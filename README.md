# simple-webapp
A simple Java WebApp with REST

The idea is to use standard JDK features and not really on any further frameworks. 

This webapp will run on Standard Java 11 and Tomcat 9.x, using PostgreSQL as the local database.

The webapp is built using Maven.

# Prepare

To get it running you will need a database `simple` as is configured in `DbPool`:

    create user simple with password 'simple';
    create database simple;
    GRANT ALL PRIVILEGES ON DATABASE simple to simple;
    GRANT CONNECT ON DATABASE simple TO simple;

Further a little table with some values:

    CREATE TABLE IF NOT EXISTS simple (
      id serial primary key not null,
      description varchar(255) not null,
      created timestamp with time zone not null
    );
    
    INSERT INTO simple 
      (description, created) 
    values(
      'The first value',
      CURRENT_TIMESTAMP
    );
    
    INSERT INTO simple 
      (description, created) 
    values(
      'The second value',
      CURRENT_TIMESTAMP
    );

# Build

Build using maven:

    mvn clean package