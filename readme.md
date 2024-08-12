### Groovy Maven

#### Introduction

A simple project for you to run groovy scripts for POC.

### Usage


#### New project
Run and input your project name when prompted.
````shell
mvn -pl=template org.codehaus.gmaven:groovy-maven-plugin:execute@run
````


#### Run the script
Other than run the groovy script in the IDE, you can also trigger it through the command line.
Replace the <YOUR_PROJECT_NAME> with the name you used during project duplication.

````shell
mvn -pl=<YOUR_PROJECT_NAME> org.codehaus.gmaven:groovy-maven-plugin:execute@run
````