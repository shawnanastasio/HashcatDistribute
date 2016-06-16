HashcatDistribute
=================
HashcatDistribute is a server written in Java that allows multiple computers running [oclHashcat](https://github.com/hashcat/oclHashcat) and a HashcatDistribute client to communicate over an HTTP API and split up a workload in order to crack hashes faster.

How it works
------------
First, each workload is split up into multiple chunks by the server. Then, clients connect and request chunks which they compute locally. Once they are done they report back to the server for a new chunk, or in the case a hash was found, with the solution to hash. All server data is kept in a MySQL database.

Usage
-----
To use HashcatDistribute you need a Java 7 or later Runtime Environment is needed, as well as maven and a MySQL server.

First, import the mysql database. You can do this from the command line by doing the following:
```
$ mysql -u<username> -p <databasename> < database.sql
```

Then, build the server with maven using the command
````
$ mvn clean compile assembly:single
```

This will output an executable JAR file in the `target` directory.

Now, run the server and generate a configuration file:
```
$ cd target
$ java -jar hashcatdistribute*.jar
```

Finally, edit the newly generated `serverconfig.json` configuration file with your MySQL database credentials and run the server again.

New clients may be created by adding a row to the `clients` table in the database.

New workloads may be created by adding a row to the `jobs` table in the database.

A separate program to provide an interface for managing HashcatDistribute servers is planned, as well as implementations of a HashcatDistribute client.
