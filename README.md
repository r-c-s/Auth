## Auth

A simple service for user authentication

#### Dependencies
* [AuthApi](https://github.com/r-c-s/AuthApi)
* [MySQL](https://dev.mysql.com/downloads/)

##### Build

<pre>
mvn clean package
</pre>

##### Run unit tests

<pre>
mvn test
</pre>

##### Run integration tests

<pre>
mvn clean test-compile failsafe:integration-test -Dapp.properties=APP_PROPERTIES_FILE
</pre>

An admin must exist in the DB with username "testAdmin" and password "password." Create the user using the API so that the service can encrypt the password correctly, then manually set the user's authority to 1 in the DB.

##### Run application

<pre>
java -jar "Auth-1.0-SNAPSHOT.jar" --app.properties=APP_PROPERTIES_FILE 
</pre>


##### App properties

<pre>
spring.datasource.url=DATASOURCE_URL
spring.datasource.username=USERNAME
spring.datasource.password=PASSWORD
server.port=SERVER_PORT
</pre>