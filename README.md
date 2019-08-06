###### Pre-requisites:
* Java8
* Maven

###### To build application:
`mvn clean package`

###### To run application:
`java -jar target/money-transfer.jar`

If port 8080 already occupied in your machine you can override one by running

`java -Dhttp.port=8888 -jar target/money-transfer.jar`
