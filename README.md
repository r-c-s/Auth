## Auth

A simple SpringBoot service for user authentication

<br>
<hr>
<br>

#### Dependencies
* [AuthApi](https://github.com/r-c-s/AuthApi) (for integration tests)
* [MySQL](https://dev.mysql.com/downloads/)

<br>
<hr>
<br>

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

Node: An admin must exist in the DB with username "testAdmin" and password "password." Register the user using the REST API (see below) so that the service can encrypt the password correctly, then manually set the user's authority to 1 (corresponding to Admin) in the DB.

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

<br>
<hr>
<br>

##### Register

<pre>
curl -X POST host:port/auth/api/users -H "Content-type:application/json" -d "{\"username\":\"USERNAME\",\"password\":\"PASSWORD\"}"
</pre>

##### Login

<pre>
curl -X POST host:port/auth/login -d "username=USERNAME&password=PASSWORD" -c cookies
</pre>