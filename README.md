# SWIM Feed Handler

This project provides an application that allows a user to consume US System Wide Information 
Management (SWIM) data.

## Contents
* [Overview](#overview)
* [Using the Application](#using-the-application)
* [License](#license)
* [Resources](#resources)


---

## Overview

As stated, this project provides a Spring Boot Auto-Configuration that consumes from the 
SWIM Cloud Distribution Service (SCDS) and then re-publishes to a local Solace PS+ broker 
instance.  The idea is that you may run multiple clients within your organization that
consume data from the US SWIM feed.  By running the SWIM Feed Handler, you can create
one consumer (may require multiple instances depending on subscriptions) that consumes
all data vs. each client making a connection to SCDS.  This saves bandwidth on both 
SCDS' egress and your organization's ingress.  By publishing to a local Solace PS+ instance
you have more flexibility in your client data needs using built in Solace broker capabilities
including protocol choices, enhanced filter capabilities and your choice of programming 
languages.

## Using the Application

The application uses Spring Boot, Spring Integration and Solace JMS with Spring.  The 
SCDS connection feed requires usage of JNDI to lookup various objects including connection
factory and queue names.  The application provides a set of services to handle the data
once consumed.  By default the Solace Publishing Service is enabled and is designed to 
publish the consumed message back into a local Solace PS+ broker instance.

Other sample service included are a FileOutputService, which writes consumed messages to
the file system, and an AWS Simple Storage Service (S3), whichs writes the payload of the 
consumed messages to S3 as a string.  

The additional services are disabled by default and show how the processing of the message
can be extended (e.g. writing the data to a database).

To use the application perform the following steps:
1. [Update application properties](#1-update-the-applicationproperties)
2. [Build/Package/Deploy the application](#2--buildpackagedeploy-the-application)
3. [Run the application](#3--run-the-application)

### 1. Update the application.properties
The application.properties file provides connection properties for both consumer and producer
connections.  Consumer properties are to connect to SCDS.  Producer properties are to
connect to your managed Solace broker either on premise or cloud based.  The property files 
can be found in the ```config/``` directory.

#### 1.1 Consumer connection
SCDS provides connection information when you create a subscription.  Currently each data
type available via SCDS is provided in a separate Message VPN.  We have provided a set
of application properties files, one for each of the data types and known Message VPNs.

When a subscription is created in SCDS, a set of connection information is provided once
the subscription is approved.  Using the connection information provided through the SCDS
portal update the consumer connection properties

```
solace.jms.consumer.host=tcps://[host]:[port]  (SCDS property: JMS Connection URL)
solace.jms.consumer.msgVpn=[messageVPN]  (i.e. STDDS, FDPS, ITWS)
solace.jms.consumer.clientUsername=[username]]  (SCDS property: Connection Username)
solace.jms.consumer.clientPassword=[password] (SCDS property: Connection Password)
solace.jms.consumer.connectionFactory=[connection-factory]  (SCDS property: Connection Factory i.e. usually in format of username.CF)

solace.jms.consumer.queueName.0=[queue-name]  (SCDS property: Queue Name i.e. format of username.MESSAGE_VPN.UUID.OUT)
```
Sometimes it will be necessary to create multiple connections to a single Message VPN to connect
to multiple queues due to data rates or specific filter needs.  For example, the STDDS data rate 
is one of the highest data rates provided by SCDS.  It may be necessary to separate the data into
different subscriptions with different filters, thus creating multiple subscriptions and an 
associated queue.

The application.properties file can support multiple queue configurations.  Simply use a zero based
index for the Queue name property.  The following is an example of multiple queues from the same 
VPN.
```
solace.jms.consumer.queueName.0=[queue 1 name]
solace.jms.consumer.queueName.1=[queue 2 name]
solace.jms.consumer.queueName.2=[queue 3 name]
...
solace.jms.consumer.queueName.n=[queue n+1 name]
```
The consumer code is multi-threaded.  The property ```solace.jms.consumer.maxListeners``` provides
a mechanism to limit the number of threads that are dedicated to consuming messages from the SCDS
queue.  Default of ```5``` threads will be sufficient for most of the SCDS flows.  Due to STDDS high
data rate it is recommended to set the value to greater than the default value of ```5```.  This will likely be a trial
and error scenario.  Monitor to the SCDS metric of expired messages to determine if the value
should increase or decrease.

#### 1.2 Producer configuration
Similar to the consumer configuration properties, there are configuration properties needed
for publication into your instance of a Solace Broker.  Provide the following properties for
the publishing service.
```
service.solace-publishing.jms.host=[protocol]://[host]:[port]  (i.e. tcp://localhost:55555)
service.solace-publishing.jms.msgVpn=[messageVPN]   (i.e. default)
service.solace-publishing.jms.clientUsername=[username]
service.solace-publishing.jms.clientPassword=[password]
service.solace-publishing.jms.connectionFactory=/jms/cf/default  (this is the default connection factory)
```

### 2.  Build/Package/Deploy the application
Build the project from source using Maven.

```mvn clean package```

Upon successful completion, a compressed tar file (```swim-feed-handler-VERSION.tar.gz```) will be generated in the 
```target``` subdirectory.  Copy the file to the final system and extract the contents with the following
command:

```tar zxvf swim-feed-handler-VERSION.tar.gz```


### 3.  Run the application
#### 3.1 Options
##### 3.1.1 From command line
After deployment, run the application using the following command:

```java -jar swim-feed-handler-VERSION.jar --spring.profiles.active=profile-name --spring.config.location=./config/```

The value for ```profile-name``` is based on the which application properties file is desired to be loaded into the 
application.  The name of the properties files are in the format of ```application-PROFILE-NAME.properties``` where 
```PROFILE-NAME``` is typically mappped to an SCDS dataset.  This is done to allow users to run multiple instances
of the application, one for each SCDS dataset/Message VPN.  For example, to run a configuration to consume STDDS
data, run:

 ```java -jar swim-feed-handler-VERSION.jar --spring.profiles.active=stdds --spring.config.location=./config/```
 
 This will invoke the properties found in the ```config/application-stdds.properties``` file.
 
 ##### 3.1.2 From Maven
 The application may also be run from the development environment using Maven.  After
 
 ```mvn spring-boot:run -Dspring-boot.run.profiles=profile_name```
 
 ##### 3.1.3 From IDE
 You can run the application from your IDE of choice.  To run from you IDE, create a Run Configuration and
 add ```-Dspring.profiles.active=profile-name``` to the VM options of the run command, replacing ```profile_name```
 with the desired application properties you want loaded.
 
 It is recommended to create a Run Configuration for each dataset you desire to consume and process.  This 
 allows you to run multiple instances at the same time without the need to change the parameters for each
 execution.
 
 #### 3.2 Monitor
 Log entries are written to ```log/messages.log``` that indicate current processing actions.  Basic information
 is provided on execution and should be sufficient to indicate health of the application.  Additional
 debug messages may be turned on by modifying the root log level from ```info``` to ```debug``` in the
 ```resources/logback.xml``` log configuration file.
 
 ### 4.  Additional Services
 The codebase is provided with additional services (i.e. File Output service and AWS S3 Put service) as examples.
 Any service may be enabled/disabled by setting the ```enabled``` property in the application properties file.  
 Multiple services can be run in parallel and each service will receive a copy of the consumed message for
 processing.
 
 You can create your own service (i.e. Database store service) if desired.  Use the existing services as 
 an example to create both the ```@Service``` and ```@ServiceActivator```.  
 

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources

For more information try these resources:

- The Solace Developer Portal website at: http://dev.solace.com
- Get a better understanding of [Solace technology](http://dev.solace.com/tech/).
- Check out the [Solace blog](http://dev.solace.com/blog/) for other interesting discussions around Solace technology
- Ask the [Solace community.](http://dev.solace.com/community/)


