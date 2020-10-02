# simple-webapp
A simple Java WebApp with REST including Privilege Authorization

The idea is to use standard JDK features and not really on any further frameworks. 

This webapp will run on Standard Java 11 and Tomcat 9.x, using PostgreSQL as the local database.

The webapp is built using Maven.

Privilege management is done using Strolch Privilege. See the PrivilegeConfig.xml, PrivilegeRoles.xml and PrivilegeUsers.xml. For questions on Privilege please ask eitch@eitchnet.ch to finally write a documentation =).

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

The current version of Strolch Privilege might not be on Maven Central, so build locally:

    git clone https://github.com/4treesCH/strolch.git
    cd strolch
    mvn clean install -DskipTests

Now change the paths in PrivilegeConfig.xml, so that the persistSessionsPath and basePath are absolute values.

Then build using maven:

    mvn clean package

# Running:

Add the app to tomcats webapp folder. Use Postman or any other REST test tool and first perform an authentication:

    POST http://localhost:8280/simple/rest/authentication
    Content-Type: application/json
    
    {
        "username" : "bob",
        "password" : "YWRtaW4="
    }

The password must be base64 encoded.

This will return a cookie and a JSON object containing all the privileges and roles for this user. As long as the cookie is still available, and the app not restarted, then the user can perform subsequent calls. The following test API is also available, and validate the authorization:

    GET http://localhost:8280/simple/rest/test
    Content-Type: application/json
    
Result:

    [
        {
            "id": 1,
            "description": "The first value",
            "created": "2020-10-02T09:26:48.753715+02:00"
        },
        {
            "id": 2,
            "description": "The second value",
            "created": "2020-10-02T09:26:49.325435+02:00"
        }
    ]
