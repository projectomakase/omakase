# Omakase
**Democratizing Content Management**

### Introduction

Omakase is a modern content management system designed for managing billions of assets at cloud scale.

### Build

#### Pre-Requisites
* Oracle JDK 8
* Maven 3.2.x

```
mvn clean install
```

### Docker Image

#### Pre-Requisites
* Docker

To get the Omakase Docker images, either run:

```
mvn docker:build
```

to build then from source, or run:

```
docker pull projectomakase\omakase && docker pull projectomakase\omakase-worker
```

to pull then from the Project Omakase Docker Hub registry (requires access to the private images)

### Quickstart

#### Pre-Requisites
* Docker Compose

The quickest way to get up and running is via

```
docker-compose up -d
```

This starts the following containers:

* mysql - a MySQL instance with the Omakase database
* activemq - an ActiveMQ instance
* omakase - a Wildfly instance running Omakase
* omakase-worker - a Omakase Worker instance.

This may take a number of minutes the first time it is run as it has to pull a number of docker images. Subsequent runs are much faster.

omakase-worker mounts your home directory into the container as:

```
/data
```

and creates a volume under which can be used as a file repository

```
/opt/omakase/repositories
```

that can be used to create a filesystem repository in Omakase.

The following default users are available to login to Omakase:

* admin/password
* editor/password
* reader/password

It is highly recommended that the passwords be changed. This can be by executing the Wildfly addUser.sh script in the container. The script can also be used to create additional users, 
new users should have the same set of roles as the existing users.

To connect to the running Omakase instance you first need to look up the port via:

```docker-compose port omakase 8080```

You can then access:

- REST API Swagger UI via ```http://<docker-host>:<port>/omakase```
- HawtIO Console via ```http://<docker-host>:<port>/hawtio```

To start additional omakase or worker instances run:

```
docker-compose scale omakase=<number of instances> worker=<number of instances>
```

### Integration Tests

To execute the full integration test suite run:

```
mvn clean verify -P integration-tests
```

To execute the REST API integration test suite run:

```
mvn clean verify -P rest-integration-tests
```

The integration tests currently require a local install of Wildfly, ActiveMQ and MySQL. This requirement will be removed in the future.
