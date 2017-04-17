---
layout: post
title:  "Customising Logging"
summary: 
date: 2017-04-17
categories: PDI
tags: PDI
published: false
---

# Log4j explained

Mostly copied from [here](https://logging.apache.org/log4j/1.2/manual.html):

- **Loggers**: Categorise the logging space. Loggers are named entities and follow a hierarchical naming rule, e.g. `com.foo`: `com` is the parent of `foo`. Loggers may be assigned **Levels**: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` and `FATAL` (from very to least verbose).
- **Appenders**: An appender is an output destination, e.g. console, file etc. More than one appender can be attached to a **logger**. Each enabled logging request for a given logger will be forwarded to all the appneders in that logger as well as the appenders higher in the hierarchy. For example, if a console appender is added to the root logger, then all enabled logging requests will at least print on the console. If in addition a file appender is added to a logger, say C, then enabled logging requests for C and C's children will print on a file and on the console. So in other words, children inherit appenders defined in the ancestors unless the **additivity flag** is set to `false`. 
- **Layouts**: This allows you to define what should go into the line of the log file. This can be configured via **patterns**.


# Configure PDI to log timestamp with millisecond precision

Inside the Community Edition of PDI (v5 or later) you find following folder: `plugins/kettle5-log4j-plugin`. Open the `log4j.xml` file lcoated inside this very folder.

Change:

```xml
<param name="ConversionPattern" value="%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n" />
...
<level value="INFO"/>
...
<priority value ="INFO"></priority>
```

To:

```xml
<param name="ConversionPattern" value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p %c{1}:%L - %m%n" />
...
<level value="INFO"/>
...
<priority value ="INFO"></priority>
```

When you start up Spoon now and watch the log, you'll realise that we have 3 different log formats now, e.g.:

```
Apr 13, 2017 5:01:34 PM org.apache.cxf.endpoint.ServerImpl initDestination
INFO: Setting the server's publish address to be /i18n
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/home/dsteiner/apps/pdi-ce-7.0/launcher/../lib/slf4j-log4j12-1.7.7.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/home/dsteiner/apps/pdi-ce-7.0/plugins/pentaho-big-data-plugin/lib/slf4j-log4j12-1.7.3.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.Log4jLoggerFactory]
2017/04/13 17:01:34 - RepositoriesMeta - Reading repositories XML file: /home/dsteiner/.kettle/repositories.xml
2017-04-13 17:01:34.795 INFO  di:61 - RepositoriesMeta - Reading repositories XML file: /home/dsteiner/.kettle/repositories.xml
```

Notice: `Apr 13, 2017 5:01:34 PM` vs `2017/04/13 17:01:34` vs `2017-04-13 17:01:34.795`. Not so cool.

To resolve this, add or change the following **Kettle Properties**:

```
KETTLE_REDIRECT_STDERR=Y
KETTLE_REDIRECT_STDOUT=Y
```


# Pentaho Server

## Show everything Mondrian is doing

Open the `log4j.xml` file, which is located in:

```
tomcat/webapps/pentaho/WEB-INF/classes/log4j.xml
```

You can see that there are various appenders configured for Mondrian:

```
<category name="mondrian">
...
<category name="mondrian.mdx">
...
<category name="mondrian.sql">
```

The first category `mondrian` prints everything that's Mondrian related. The other two ones are more specific, logging only the relevant sub-topic.
Here again you can see the hierarchical structure of log4j.

If you want to see absolutely everything logged about Mondrian, enable the first appender.
