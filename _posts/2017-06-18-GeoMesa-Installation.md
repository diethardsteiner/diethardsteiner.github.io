---
layout: post
title: "Big Data Geospatial Analysis with Apache Spark, GeoMesa and Accumulo - Part 1: Installation"
summary: This article walks you through the installation procudure for GeoMesa.
date: 2017-06-18
categories: GIS Spatial-analysis
tags: Spark
published: true
---  

This article is intended as an introduction to **GeoMesa**. It walks you through all the installation steps for a **local dev environment** and shows you how to run a few basic examples. The examples (which will be introduced in Part 3) are all borrowed from the **GeoMesa** help site (copying quite a lot of their content) - while this site provides a lot of good info, it doesn't lend itself that well to a consistent practical introduction.

Here are a few useful links:

- [GeoMesa Website](http://www.geomesa.org/)
- [GeoMesa FAQ](http://www.geomesa.org/faq/)
- [Project Overview](https://www.slideshare.net/mobile/jgarnett/locationtech-projects-38993728)
- [Maven Repository](https://repo.locationtech.org/content/repositories/releases/org/locationtech/geomesa/)
- [Github](https://github.com/locationtech/geomesa), see for SBT dependencies.
- [GeoMesa: Scaling up Geospatial Analysis](http://www.eclipse.org/community/eclipse_newsletter/2014/march/article3.php)
- [GeoMesa Spark: Basic Analysis](http://www.geomesa.org/documentation/tutorials/spark.html)
- [GeoMesa Spark](http://www.geomesa.org/documentation/user/spark/index.html#geomesa-spark)
- [Using the GeoMesa Accumulo Spark Runtime](http://www.geomesa.org/documentation/user/spark/accumulo_spark_runtime.html#using-the-geomesa-accumulo-spark-runtime)
- [geomesa-accumulo-compute example](https://github.com/locationtech/geomesa/tree/master/geomesa-accumulo/geomesa-accumulo-compute): Source data from Spark and analyse it
- [Command Line Tools](http://www.geomesa.org/documentation/user/accumulo/commandline_tools.html#accumulo-tools)

**GeoMesa** is an **add-on** to one of the following **data stores**:

- Accumulo
- Kafka
- HBase
- Bigtable
- Cassandra

## Installation

The instructions below are for a local dev environment only!

### Hadoop

If you haven't got Hadoop installed yet, follow my instructions outlined in [Setting up a Hadoop Dev Environment for Pentaho Data Integration](http://diethardsteiner.github.io/pdi/2015/06/06/PDI-Hadoop-Dev-Env.html) - just the first part, there is no need to install Pentaho Data Integration.

### Zookeeper

To install **Zookeeper**, follow the script below and adjust where required:

```
# adjust working directory if required
export WORK_DIR=~/apps

# ########### ZOOKEEPER #############
# https://zookeeper.apache.org/releases.html
# requires: 

cd $WORK_DIR

export VERSION=3.4.10
wget http://apache.mirrors.nublue.co.uk/zookeeper/zookeeper-$VERSION/zookeeper-$VERSION.tar.gz
tar -xzvf zookeeper-$VERSION.tar.gz

echo " " >> ~/.bashrc
echo "# ======== ZOOKEEPER ======== #" >> ~/.bashrc
echo "export ZOOKEEPER_HOME=$WORK_DIR/zookeeper-$VERSION" >> ~/.bashrc
echo 'export PATH=$PATH:$ZOOKEEPER_HOME/bin' >> ~/.bashrc

source ~/.bashrc

cd zookeeper-$VERSION
cp conf/zoo_sample.cfg conf/zoo.cfg

# define permanent data dir
mkdir ~/zookeeper-data
# IMPORTANT: CHANGE USER HOME DIR PATH
perl -0777 -i.original -pe 's@/tmp/zookeeper@/home/dsteiner/zookeeper-data@igs' conf/zoo.cfg
```

Now start service:

```
sh ./bin/zkServer.sh start
```

### Accumulo 

> **Important**: Geomesa version 1.3.1 is only compatible with Accumulo version 1.7.x.

[A note about Accumulo 1.8](http://www.geomesa.org/documentation/user/accumulo/install.html#a-note-about-accumulo-1-8): "GeoMesa supports Accumulo 1.8 when built with the accumulo-1.8 **profile**. Accumulo 1.8 introduced a dependency on **libthrift** version 0.9.3 which is not compatible with Accumulo 1.7/libthrift 0.9.1. **The default supported version for GeoMesa is Accumulo 1.7.x** and the published jars and distribution artifacts reflect this version. To upgrade, build locally using the accumulo-1.8 profile."

Again, follow the script below and adjust where required:

```
# ########### ACCUMULO #############
# https://accumulo.apache.org/downloads/
# requires: hdfs, zookeeper

cd $WORK_DIR

# export VERSION=1.8.1
export VERSION=1.7.3
wget http://www.mirrorservice.org/sites/ftp.apache.org/accumulo/$VERSION/accumulo-$VERSION-bin.tar.gz
tar -xzvf accumulo-$VERSION-bin.tar.gz

echo " " >> ~/.bashrc
echo "# ======== ACCUMULO ======== #" >> ~/.bashrc
echo "export ACCUMULO_HOME=$WORK_DIR/accumulo-$VERSION" >> ~/.bashrc
echo 'export PATH=$PATH:$ACCUMULO_HOME/bin' >> ~/.bashrc

source ~/.bashrc

cd accumulo-$VERSION

./bin/build_native_library.sh

cp conf/examples/1GB/standalone/* conf/

# by default, Accumulo's HTTP monitor binds only to the local network interface. 
# to be able to access it over the Internet, you have to set the value 
# of ACCUMULO_MONITOR_BIND_ALL to true.
sed 's/# export ACCUMULO_MONITOR_BIND_ALL/export ACCUMULO_MONITOR_BIND_ALL/' < conf/accumulo-env.sh > conf/accumulo-env.test.sh
rm conf/accumulo-env.sh
mv conf/accumulo-env.test.sh conf/accumulo-env.sh
chmod 700 conf/accumulo-env.sh
# instance.volumes: where to store the data on HDFS
perl -0777 -i.original -pe 's@<name>instance.volumes</name>\n\s+<value></value>@<name>instance.volumes</name>\n<value>hdfs://localhost:8020/accumulo</value>@igs' conf/accumulo-site.xml
# change instance.secret
perl -0777 -i.original -pe 's@<value>DEFAULT</value>@<value>password</value>@igs' conf/accumulo-site.xml
```

To avoid the following warning message change `hdfs-site.xml`:

```
WARN : dfs.datanode.synconclose set to false in hdfs-site.xml: data loss is possible on hard system reset or power loss
```

Adjust your Hadoop config.

Before running first time, **initialisation** is required, this is a manual process. Make sure Hadoop is running!

```
./bin/accumulo init
```

Name the instance something like `BISSOL_CONSULTING`, provide password `password`

Once command completes, you can start **Accumulo**:

```
./bin/start-all.sh
```

The **Web UI** available on http://localhost:9995/

> **Note**: Whenever you put your machine/laptop to sleep, Accumulo will stop. So once you are back, you have to start it up again.

### Troubleshooting

[Troubleshooting](https://github.com/apache/accumulo/blob/master/docs/src/main/asciidoc/chapters/troubleshooting.txt)

#### Waiting for Accumulo to be initialized

If you get this error message, check **Zookeeper**:

```
$ZOOKEEPER_HOME/bin/zkCli.sh
ls /accumulo
# should not be empty
ls /accumulo/instances
# should show instance names
```

If you get:

```
ls /accumulo
Node does not exist: /accumulo
```

"Well that's your problem! Your ZooKeeper is empty. Accumulo
uses the information it places in ZooKeeper to bootstrap and find the data in HDFS." [Source](http://apache-accumulo.1065345.n5.nabble.com/Accumulo-not-starting-anymore-td9166.html)

Make sure you configure the ZooKeeper `dataDir` to be something other than `/tmp`? 

Change the `dataDir` property in `$ZOOKEEPER_HOME/conf/zoo.cfg` to something like this:

```
dataDir=/var/lib/zookeeper/ 
```

Now stop Accumulo, restart Zookeeper, remove the existing HDFS `/accumulo` directory, init accumulo and then start it.

#### Changing Root User's Password

If you ever plan to change the root user's password, you must not use `accumulo init --reset-security` for this purpose as it deletes all the users when you run it, in addition to changing the Accumulo root password. You can change a single user's password in the accumulo shell with the `passwd` command.

```
accumulo shell -u root
passwd
```

### GeoMesa

#### Standard Install

Follow the script and adjust where required:

```
# ########### GEOMESA ACCUMULO ADD-ON #############
# http://www.geomesa.org/#downloads
# http://www.geomesa.org/documentation/user/accumulo/install.html
cd $WORK_DIR

# download and unpackage the most recent distribution
export VERSION=1.3.1
wget http://repo.locationtech.org/content/repositories/geomesa-releases/org/locationtech/geomesa/geomesa-accumulo-dist_2.11/$VERSION/geomesa-accumulo-dist_2.11-$VERSION-bin.tar.gz
tar xvf geomesa-accumulo-dist_2.11-$VERSION-bin.tar.gz
cd geomesa-accumulo_2.11-$VERSION


echo " " >> ~/.bashrc
echo "# ======== GEOMESA ======== #" >> ~/.bashrc
echo "export GEOMESA_ACCUMULO_HOME=$WORK_DIR/geomesa-accumulo_2.11-$VERSION" >> ~/.bashrc
echo 'export PATH=$PATH:$GEOMESA_ACCUMULO_HOME/bin' >> ~/.bashrc

source ~/.bashrc
```

##### Installing the distributed runtime library 

There are **two options**:

 - copy jars to Accumulo tablet servers in the cluster (**old approach**, do not use)
 - use Accumulo namespace 

There are two runtime JARs available, with and without raster support. Only one is needed and including both will cause classpath issues.

As of **Accumulo 1.6** we can use **namespaces** to isolate the GeoMesa classpath from the rest of Accumulo.

There is a utility script available to do this:

```
./bin/setup-namespace.sh -u root -n myNamespace 
```

However, this didn't work correctly last time I tried it, so we can use the manual approach below instead:

```bash
# alternatively you can use this

accumulo shell -u root -p password
> createnamespace myNamespace
> grant NameSpace.CREATE_TABLE -ns myNamespace -u root
> config -s general.vfs.context.classpath.myNamespace=hdfs://localhost:8020/accumulo/classpath/myNamespace/[^.].*.jar
> config -ns myNamespace -s table.classpath.context=myNamespace
> exit
```

Then **copy the distributed runtime jar into HDFS** under the path you specified:

```
hdfs dfs -mkdir -p /accumulo/classpath/myNamespace
hdfs dfs -copyFromLocal dist/accumulo/geomesa-accumulo-distributed-runtime_2.11-$VERSION.jar /accumulo/classpath/myNamespace 
```

> **Note**: When connecting to a data store using Accumulo **namespaces**, you must **prefix the tableName** parameter with the namespace. For example, refer to the my_catalog table as myNamespace.my_catalog.


##### Prepare command line tools

You can configure environment variables and classpath settings  in `geomesa-accumulo_2.11-$VERSION/bin/geomesa-env.sh`.
This is not required in our case as we have all the correct env variables set already.

Run this script:

```
./bin/geomesa configure
```

Due to licensing restrictions, dependencies for shape file support and raster ingest must be separately installed:

Run these interactive scripts:

```bash
./bin/install-jai.sh
./bin/install-jline.sh
```

Test the command that invokes the GeoMesa Tools:

```bash
geomesa
```

For more details, see [Command Line Tools](http://www.geomesa.org/documentation/user/accumulo/commandline_tools.html#accumulo-tools)

#### Build from Source

> **Note**: Latest builds are also available directly from the [Locationtech Artifactory](https://repo.locationtech.org/#nexus-search;quick~geomesa-accumulo-dist)

As an alternative you can build **GeoMesa** from source.

Just a brief description here:

[Source](http://www.geomesa.org/documentation/developer/introduction.html#building-from-source)

```
$ git clone https://github.com/locationtech/geomesa.git
$ cd geomesa
$ git checkout -b geomesa-1.3.1 geomesa_2.11-1.3.1
$ mvn clean install -DskipTests=true
# OR TO COMPILE THE LATEST VERSION FOR ACCUMULO 1.8.1
$ git checkout master
$ mvn clean install -Paccumulo-1.8 -DskipTests=true
```

In case you are wondering what `maven install` does: "This command tells Maven to build all the modules, and to install it in the local repository. The local repository is created in your home directory (or alternative location that you created it), and is the location that all downloaded binaries and the projects you built are stored. That's it! If you look in the target subdirectory, you should find the build output and the final library or application that was being built. Note: Some projects have multiple modules, so the library or application you are looking for may be in a module subdirectory." [Source](https://maven.apache.org/run-maven/)

> **Note**: As Jim from the GoeMesa mailing list pointed out: "you'll need to make sure that sbt picks up the artifacts which you have built locally".

If you need the very latest, you can also build of the master branch. Then you can e.g. just copy the `geomesa-accumulo-dist` target `tar.gz` file and unzip it in a convenient directory:

```
cp geomesa/geomesa-accumulo/geomesa-accumulo-dist/target/geomesa-accumulo_2.11-1.3.2-SNAPSHOT-bin.tar.gz .
tar -xzvf geomesa-accumulo_2.11-1.3.2-SNAPSHOT-bin.tar.gz
```

### Spark

Follow the install script and adjust where required:

```bash
# ########### SPARK #############
# 

cd $WORK_DIR
wget http://d3kbcqa49mib13.cloudfront.net/spark-2.1.0-bin-hadoop2.7.tgz
tar -zxvf spark-2.1.0-bin-hadoop2.7.tgz
rm -rf spark-2.1.0-bin-hadoop2.7.tgz

echo " " >> ~/.bashrc
echo "# ======== SPARK ======== #" >> ~/.bashrc
echo "export SPARK_HOME=$WORK_DIR/spark-2.1.0-bin-hadoop2.7" >> ~/.bashrc
echo 'export PATH=$PATH:$SPARK_HOME/bin' >> ~/.bashrc

source ~/.bashrc
echo "--- Finished installing SPARK ---"
```

### GeoServer

- [GeoServer Linux binary Installation](http://docs.geoserver.org/stable/en/user/installation/linux.html)


The below commands will help you to install **GeoServer**:

```bash
# ########### GEOSERVER #############
# http://geoserver.org/download/
# choose platform independent version
# http://docs.geoserver.org/stable/en/user/installation/linux.html
# http://docs.geoserver.org/stable/en/user/index.html

cd $WORK_DIR

export VERSION=2.9.1
wget https://freefr.dl.sourceforge.net/project/geoserver/GeoServer/$VERSION/geoserver-$VERSION-bin.zip
unzip geoserver-$VERSION-bin.zip

echo " " >> ~/.bashrc
echo "# ======== GEOSERVER ======== #" >> ~/.bashrc
echo "export GEOSERVER_HOME=$WORK_DIR/geoserver-$VERSION" >> ~/.bashrc
echo 'export PATH=$PATH:$GEOSERVER_HOME/bin' >> ~/.bashrc

source ~/.bashrc

# change port
cd $GEOSERVER_HOME
perl -0777 -i.original -pe 's@jetty.port=8080@jetty.port=8077@igs' start.ini
```

Next we have to [install the WPS extension](http://docs.geoserver.org/stable/en/user/services/wps/install.html): On the downloads page click on the **Archived** tab to find the plugin for version 2.9.1.: Click on **WPS** (and not WPS Hazelcast) the **Extension > Services** section 

```bash
# install WPS extension
cd webapps/geoserver/WEB-INF/lib/
wget https://netix.dl.sourceforge.net/project/geoserver/GeoServer/$VERSION/extensions/geoserver-$VERSION-wps-plugin.zip
unzip geoserver-$VERSION-wps-plugin.zip
rm geoserver-$VERSION-wps-plugin.zip
cd $GEOSERVER_HOME
```

Next let's add the **GeoMesa Accumulo dependencies** as described in [Installing GeoMesa Accumulo in GeoServer](http://www.geomesa.org/documentation/user/accumulo/install.html#installing-geomesa-accumulo-in-geoserver):

```bash
# install GeoMesa’s Accumulo data store as a GeoServer plugin
# interactive dialog. option to install it fully automatically also available, 
# see docu: http://www.geomesa.org/documentation/user/accumulo/install.html#installing-geomesa-accumulo-in-geoserver
cd $GEOMESA_ACCUMULO_HOME
sh ./bin/manage-geoserver-plugins.sh --install

# install Accumulo, Zookeeper, Hadoop, and Thrift  dependencies
sh ./bin/install-hadoop-accumulo.sh $GEOSERVER_HOME/webapps/geoserver/WEB-INF/lib/

cd $GEOSERVER_HOME
# start service
nohup sh ./bin/startup.sh &
```

The **Web UI** will be available on:

```
http://localhost:8077/geoserver/web/
```

Check on the Welcome page that **WPS** is listed under **Service Capabilities**. 

The **default username and password** is `admin` and `geoserver`.

Further reading (but not required for this setup):

- [Load Balancer Setup with Apache HTTP](http://geoserver.geo-solutions.it/edu/en/clustering/load_balancing/apache_http.html)

### Jupyter

[Deploying GeoMesa Spark with Jupyter Notebook](http://www.geomesa.org/documentation/user/spark/jupyter.html). 

This might vary depending on your OS. See above docu for more details.

Since my system had all the essential dev tools installed, the setup was simple like this:

```
# ########### JUPYTER #############
# http://jupyter.org/install.html

export GEOMESA_ACCUMULO_VERSION=1.3.1
cd $WORK_DIR

# ensure that you have the latest pip
pip3 install --upgrade pip

# then install the Jupyter Notebook using:
sudo pip3 install jupyter
```

Next we have to install to **Spark** kernel **Toree**. In this case I did not following the [docu](http://www.geomesa.org/documentation/user/spark/jupyter.html) but the [Toree Readme](https://github.com/apache/incubator-toree/blob/master/README.md) as this seemed to be more up to date

```bash
sudo pip3 install https://dist.apache.org/repos/dist/dev/incubator/toree/0.2.0/snapshots/dev1/toree-pip/toree-0.2.0.dev1.tar.gz
```

**Important**: DO NOT install Toree yet with Jupyter as we have to add the geomesa dependencies.

Next we have to build the specific **Geomesa Jupyter** visualisation plugins for **Leaftlet** and **Vegas** from source:

```bash
git clone https://github.com/locationtech/geomesa.git
cd geomesa
git checkout -b geomesa-$GEOMESA_ACCUMULO_VERSION geomesa_2.11-$GEOMESA_ACCUMULO_VERSION
mvn clean install -Pvegas -pl geomesa-jupyter/geomesa-jupyter-vegas
```

> **Note**: Vegas is build with the `vegas` profile.

Next let's install all these GeoMesa dependencies with Jupyter.
I am now following again [the official instructions](http://www.geomesa.org/documentation/user/spark/jupyter.html#configure-toree-and-geomesa)

Three files are referenced:

1. geomesa dependency
2. geomesa-vega dependency
3. geomesa-leaflet dependency

```bash
# requires following vars to be set 
# GEOMESA_ACCUMULO_HOME
# GEOMESA_ACCUMULO_VERSION
# SPARK_HOME
# WORK_DIR
export jars="file://$GEOMESA_ACCUMULO_HOME/dist/spark/geomesa-accumulo-spark-runtime_2.11-$GEOMESA_ACCUMULO_VERSION.jar,file://$WORK_DIR/geomesa/geomesa-jupyter/geomesa-jupyter-vegas/target/original-geomesa-jupyter-vegas_2.11-$GEOMESA_ACCUMULO_VERSION.jar,file://$WORK_DIR/geomesa/geomesa-jupyter/geomesa-jupyter-leaflet/target/geomesa-jupyter-leaflet_2.11-$GEOMESA_ACCUMULO_VERSION.jar"
jupyter toree install \
    --replace \
    --user \
    --kernel_name "GeoMesa Spark $GEOMESA_ACCUMULO_VERSION" \
    --spark_home=${SPARK_HOME} \
    --spark_opts="--master yarn --jars $jars"
```

> **Note**: This is the very basic setup. Read the online doc if you require support for Shapefiles, Converters or GeoTools RDD Provider.

Finally, let's start the **Jupyter notebook server**:

```bash
jupyter notebook
```

This will automatically open the web page on following URL:

```
http://localhost:8888
```

Via the **Jupyter** website create a new **notebook**.

> **Important**: Just after you created the new notebook, watch the Jupyter logs to make sure that all the dependencies are correctly picked up. You might see some errors for wrong file references etc.

#### Errors

##### Spark context stopped while waiting for backend

```
ERROR SparkContext: Error initializing SparkContext.
java.lang.IllegalStateException: Spark context stopped while waiting for backend
```

This is an issue with Java 8 and Yarn. Use the workaround described [here](http://stackoverflow.com/questions/38988941/running-yarn-with-spark-not-working-with-java-8): Add the following to `yarn-site.xml` within your Hadoop folder:

```xml
<property>
    <name>yarn.nodemanager.pmem-check-enabled</name>
    <value>false</value>
</property>

<property>
    <name>yarn.nodemanager.vmem-check-enabled</name>
    <value>false</value>
</property>
```

We have everything set up now - stay tuned for **Part 2** where we will look at **GeoMesa** and **Accumulo** Basics.