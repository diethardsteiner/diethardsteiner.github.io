---
layout: post
title:  "Using Pentaho Data Integration with Docker: Part 1"
summary: This provides detailled instructions on how to use PDI with Docker
date: 2016-04-21
categories: PDI
tags: PDI Docker
published: true
---

Over the last few years **Docker** has enjoyed an enormous surge in popularity. It is an invaluable tool to automate the setup and configuration of a given environment. While this article is not intended to be an introduction to **Docker**, we will be looking at creating a `Dockerfile` for **Pentaho Data Integration** from scratch, which should help you to learn the essentials.

Our **workfow** will be the following:

1. Create a **Docker** image with **PDI** on it
2. To set up several instances, or in other words, to create our **application**, use `docker-compose`, which will allow you to **spin up several containers** based on that one image.

As we do not want to reinvent the wheel, we perform a search on Google and see what's already out there: There are at least two good examples out there on how to build **Docker** images for **Pentaho Data Integration**:

- [PDI Docker Image by Wellington Marinho](https://github.com/wmarinho/docker-pentaho/blob/master/data-integration/)
- [PDI Docker Image by Aloysius Lim](https://github.com/aloysius-lim/docker-pentaho-di)

The second one offers a few more options than the first one.

As a learning experience, we will create one ourselves, basically by picking the best bits from the above `Dockerfile`s and trying to add a few improvements.

# An easy start

If you are in a rush and just want to quickly start up some Docker containers, use aloysius-lim's image:

```bash
$ git clone https://github.com/aloysius-lim/docker-pentaho-di.git
$ cd docker-pentaho-di
$ docker build -t aloysius-lim:pdi .
$ docker run -d --rm -p 8181:8181 -e PDI_RELEASE=6.0 -e PDI_VERSION=6.0.1.0-386 -e CARTE_PORT=8181 --name myPdiContainer aloysius-lim:pdi
# or alternatively in interactive mode
# docker run -it --rm -p 8181:8181 -e PDI_RELEASE=6.0 -e PDI_VERSION=6.0.1.0-386 -e CARTE_PORT=8181 --name myPdiContainer aloysius-lim:pdi
$ docker-machine ip
192.168.99.100
```

Then head over to your favourite web browser and take a look at the **Carte** website on `http:/192.168.99.100:8181` (adjust IP if required).

For all the ones that are interested in **building the Dockerfile**, here we go:

# Setting up PDI for Docker

Create a dedicated project folder to store all the relevant files.

## Preparing the Carte config templates

First, let's prepare all the Carte config templates, one for the master and one for the slaves:

`carte-master.config.xml`

```xml
<slave_config>

  <slaveserver>
    <name>CARTE_NAME</name>
    <network_interface>CARTE_NETWORK_INTERFACE</network_interface>
    <port>CARTE_PORT</port>
    <username>CARTE_USER</username>
    <password>CARTE_PASSWORD</password>
    <master>CARTE_IS_MASTER</master>
  </slaveserver>

</slave_config>
```

`carte-slave.config.xml`:

```xml
<slave_config>

  <masters>

    <slaveserver>
      <name>CARTE_MASTER_NAME</name>
      <hostname>CARTE_MASTER_HOSTNAME</hostname>
      <port>CARTE_MASTER_PORT</port>
      <username>CARTE_MASTER_USER</username>
      <password>CARTE_MASTER_PASSWORD</password>
      <master>CARTE_MASTER_IS_MASTER</master>
    </slaveserver>

  </masters>

  <report_to_masters>CARTE_REPORT_TO_MASTERS</report_to_masters>

  <slaveserver>
    <name>CARTE_NAME</name>
    <network_interface>CARTE_NETWORK_INTERFACE</network_interface>
    <port>CARTE_PORT</port>
    <username>CARTE_USER</username>
    <password>CARTE_PASSWORD</password>
    <master>CARTE_IS_MASTER</master>
  </slaveserver>

</slave_config>
```

If you've ever set up a Carte server in the past, this should be quite familiar to you. All the variables in the above templates will be replaced later on when the **Docker containers** get created.

## Preparing the Docker entrypoint script

There is also a shell script which will run shortly after a container starts up called `docker-entrypoint.sh`:

```bash
#!/bin/bash
# based on https://github.com/aloysius-lim/docker-pentaho-di/blob/master/docker/Dockerfile
set -e

if [ "$1" = 'carte.sh' ]; then
  # checking if env vars get passed down
  # echo "$KETTLE_HOME/carte.config.xml"
  # echo "$PENTAHO_HOME"
  if [ ! -f "$KETTLE_HOME/carte.config.xml" ]; then
    # Set variables to defaults if they are not already set
    : ${CARTE_NAME:=carte-server}
    : ${CARTE_NETWORK_INTERFACE:=eth0}
    : ${CARTE_PORT:=8181}
    : ${CARTE_USER:=cluster}
    : ${CARTE_PASSWORD:=cluster}
    : ${CARTE_IS_MASTER:=Y}

    : ${CARTE_INCLUDE_MASTERS:=N}

    : ${CARTE_REPORT_TO_MASTERS:=Y}
    : ${CARTE_MASTER_NAME:=carte-master}
    : ${CARTE_MASTER_HOSTNAME:=localhost}
    : ${CARTE_MASTER_PORT:=8181}
    : ${CARTE_MASTER_USER:=cluster}
    : ${CARTE_MASTER_PASSWORD:=cluster}
    : ${CARTE_MASTER_IS_MASTER:=Y}

    # Copy the right template and replace the variables in it
    if [ "$CARTE_INCLUDE_MASTERS" = "Y" ]; then
      cp $PENTAHO_HOME/templates/carte-slave.config.xml "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_REPORT_TO_MASTERS/$CARTE_REPORT_TO_MASTERS/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_NAME/$CARTE_MASTER_NAME/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_HOSTNAME/$CARTE_MASTER_HOSTNAME/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_PORT/$CARTE_MASTER_PORT/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_USER/$CARTE_MASTER_USER/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_PASSWORD/$CARTE_MASTER_PASSWORD/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_IS_MASTER/$CARTE_MASTER_IS_MASTER/" "$KETTLE_HOME/carte.config.xml"
    else
      cp $PENTAHO_HOME/templates/carte-master.config.xml "$KETTLE_HOME/carte.config.xml"
    fi
    sed -i "s/CARTE_NAME/$CARTE_NAME/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_NETWORK_INTERFACE/$CARTE_NETWORK_INTERFACE/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_PORT/$CARTE_PORT/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_USER/$CARTE_USER/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_PASSWORD/$CARTE_PASSWORD/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_IS_MASTER/$CARTE_IS_MASTER/" "$KETTLE_HOME/carte.config.xml"
  fi
fi

# Run any custom scripts
if [ -d $PENTAHO_HOME/docker-entrypoint.d ]; then
  for f in $PENTAHO_HOME/docker-entrypoint.d/*.sh; do
    [ -f "$f" ] && . "$f"
  done
fi

exec "$@"
```

## Finding a Docker Base Image

First we try to search for an existing **Java 8** image:

```bash
$ docker search java
NAME                   DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
node                   Node.js is a JavaScript-based platform for...   1917      [OK]       
java                   Java is a concurrent, class-based, and obj...   762       [OK]    
```

Now that we know the exact name of the image, let's pull it:

```bash
$ docker pull java
```

There's time for a good cup of coffee now until the download finished. Once finished:

```bash
$ docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
java                latest              759d5a0d3218        2 days ago          642.9 MB
```

As you can see, at 642.9 MB this is quite a large image. Maybe we can improve this? In most cases you want to use either JRE or JDK and a specific version of it.

Alternatively to the search command used earlier on, you could have just searched for the image on [Docker Hub](https://hub.docker.com). You will also find a detailled description of the image there. To understand which java versions are available, have a look at [this page](https://github.com/docker-library/docs/blob/master/java/tag-details.md) which lists all the tags. We are interested in JRE only and version 8:

```bash
$ docker pull java:8-jre
REPOSITORY          TAG                 IMAGE ID            CREATED             
java                8-jre               e340215bec4d        11 days ago         311 MB
```

This image is nearly half the size. Out of curiousity let's inspect where Java is located in this image: Let's start a container in interactive mode:

```bash
$ docker run -it --rm --name myJavaContainer java:8-jre
root@156ae2ade518:/# ls /usr/lib/jvm/
.java-1.8.0-openjdk-amd64.jinfo  java-1.8.0-openjdk-amd64/        java-8-openjdk-amd64/
```

## Creating the Dockerfile

In our dedicated project folder create a file named `Dockerfile` without any extension.

Our initial attempt to build a **Dockerfile** for **PDI** looks like this:

```
FROM java:8-jre

MAINTAINER Diethard Steiner

# Set required environment vars
ENV PDI_RELEASE=6.0 \
    PDI_VERSION=6.0.1.0-386 \
    CARTE_PORT=8181 \
    PENTAHO_JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
    PENTAHO_HOME=/home/pentaho

# Create user
RUN mkdir ${PENTAHO_HOME} && \
    groupadd -r pentaho && \
    useradd -s /bin/bash -d ${PENTAHO_HOME} -r -g pentaho pentaho && \
    chown pentaho:pentaho ${PENTAHO_HOME}

# Switch to the pentaho user
USER pentaho

# Download PDI
RUN /usr/bin/wget \
    --progress=dot:giga \
    http://downloads.sourceforge.net/project/pentaho/Data%20Integration/${PDI_RELEASE}/pdi-ce-${PDI_VERSION}.zip \
    -O /tmp/pdi-ce-${PDI_VERSION}.zip && \
    /usr/bin/unzip -q /tmp/pdi-ce-${PDI_VERSION}.zip -d  $PENTAHO_HOME && \
    rm /tmp/pdi-ce-${PDI_VERSION}.zip

# We can only add KETTLE_HOME to the PATH variable now
# as the path gets eveluated - so it must already exist
ENV KETTLE_HOME=$PENTAHO_HOME/data-integration \
    PATH=$KETTLE_HOME:$PATH

# Add files
RUN mkdir $PENTAHO_HOME/docker-entrypoint.d $PENTAHO_HOME/templates $PENTAHO_HOME/scripts

COPY carte-*.config.xml $PENTAHO_HOME/templates/

COPY docker-entrypoint.sh $PENTAHO_HOME/scripts/

# Expose Carte Server
EXPOSE ${CARTE_PORT}

# As we cannot use env variable with the entrypoint and cmd instructions
# we set the working directory here to a convenient location
# We set it to KETTLE_HOME so we can start carte easily
WORKDIR $KETTLE_HOME


ENTRYPOINT ["../scripts/docker-entrypoint.sh"]

# Run Carte - these parameters are passed to the entrypoint
CMD ["carte.sh", "carte.config.xml"]
```

So what's going on here? Let's see:

1. We base our image on another one using the `FROM` instruction. As **PDI** requires **Java** to run, we might as well choose an existing **Java** image. Moreover it is best practice to have one image only do one particular thing, so that it scales better and is easier to maintain etc etc. See [this Best Practices page](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/).
2. With the `MAINTAINER` keyword we let everyone know - yes, you guessed it - who is the proud owner and maintainer of this very image.
3. The `ENV` instruction allows us to set various **environment variables**.
4. Next, via the `RUN` instruction, we execute some shell commands to create a `pentaho` user with their own home directory.
5. As theoretically non of the succeeding instructions have to be run as a root user, we switch to the `pentaho` user using the `USER` instruction. Again, this is best practice.
6. Again using the `RUN` instruction, we download **PDI** using `wget` (we could have used `curl` as well) and extract it into its own little folder inside the `pentaho` home directory. Finally we remove the zip file to keep the image size down.
7. We set the `KETTLE_HOME` environment variable and add it to the `PATH` evnvironment variable. The reason why we only do this at this stage is because once you add `KETTLE_HOME` to the `PATH`, the path to the **PDI** folder gets evaluated (so it has to exist).
8. Next we create a few folders inside the `pentaho` users home directory (using `RUN`) and copy some local files (which have to reside in the same folder as the **Dockerfile**) to the image (using the `COPY` instruction).
9. We open a given port (on the container) to be accessible to the outside world. This is done via the `EXPOSE` instruction.
10. The final bits are the `ENTRYPOINT` and the `CMD` instruction. The `ENTRYPOINT` allows you to specify a script or command which is run as the main process. In this case, via `CMD` you specify default parameters to be passed to the `ENTRYPOINT`.

Let's build the image now. The commands below have to be executed in the same folder as the **Dockerfile** is located. If you are working on **Mac OS X** or **Windows** make sure you have the **Docker Quickstart Terminal** ready:

```bash
$ docker build -t diethardsteiner:pdi .
```

You will get a **File is not executable** error: Initially I was getting this error: `/bin/bash: /home/pentaho/scripts/docker-entrypoint.sh: Permission denied`. So when you switch in the **Dockerfile** to a new `USER` and use e.g. `COPY` as one of the next instructions, you would expect this to happen with the credentials of the new user (you just switched to), right? Nope! That's not the case. The `COPY` command gets executed on behave of the root user. And there is a pretty long discussion on the [Docker Github Issue List](https://github.com/docker/docker/issues/6119) on this topic.

You can actually simply check the permissions of a file or folder in the would-be container by send an `ls -l` command to the entry point of the image:

```bash
docker run --rm --entrypoint /bin/bash diethardsteiner:pdi -c "ls -l /home/pentaho/scripts"
```

Various resources on the internet suggested using `RUN chmod +x $PENTAHO_HOME/scripts/docker-entrypoint.sh` to solve the problem. On the first try I got following error: `chmod: changing permissions of ‘/home/pentaho/scripts/docker-entrypoint.sh’: Operation not permitted`. Then I moved the instruction to the section which is run as a `root` user, which eventually solved the problem. So running `chmod` in Docker can only be done with the `root` user. I also made sure that the `pentaho` user owned the newly added folders (because when the folders initially get created, they are owned by the root user. The same applies when you `ADD` files and folders). 

The main issue is though that the `ADD` or `COPY` instructions will always add the files or folders as the `root` user, no matter if you switched to another user before using the `USER` instruction or not. 

One way to solve this problem, as said, is to change the owner of the files and folders while running as `root` user:

```
FROM java:8-jre

MAINTAINER Diethard Steiner

# Set required environment vars
ENV PDI_RELEASE=6.0 \
    PDI_VERSION=6.0.1.0-386 \
    CARTE_PORT=8181 \
    PENTAHO_JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
    PENTAHO_HOME=/home/pentaho

# Create user
RUN mkdir ${PENTAHO_HOME} && \
    groupadd -r pentaho && \
    useradd -s /bin/bash -d ${PENTAHO_HOME} -r -g pentaho pentaho && \
    chown pentaho:pentaho ${PENTAHO_HOME}

# Add files
RUN mkdir $PENTAHO_HOME/docker-entrypoint.d $PENTAHO_HOME/templates $PENTAHO_HOME/scripts

COPY carte-*.config.xml $PENTAHO_HOME/templates/

COPY docker-entrypoint.sh $PENTAHO_HOME/scripts/

RUN chown -R pentaho:pentaho $PENTAHO_HOME 
# chmod +x $PENTAHO_HOME/scripts/docker-entrypoint.sh && \

# Switch to the pentaho user
USER pentaho

# Download PDI
RUN /usr/bin/wget \
    --progress=dot:giga \
    http://downloads.sourceforge.net/project/pentaho/Data%20Integration/${PDI_RELEASE}/pdi-ce-${PDI_VERSION}.zip \
    -O /tmp/pdi-ce-${PDI_VERSION}.zip && \
    /usr/bin/unzip -q /tmp/pdi-ce-${PDI_VERSION}.zip -d  $PENTAHO_HOME && \
    rm /tmp/pdi-ce-${PDI_VERSION}.zip

# We can only add KETTLE_HOME to the PATH variable now
# as the path gets eveluated - so it must already exist
ENV KETTLE_HOME=$PENTAHO_HOME/data-integration \
    PATH=$KETTLE_HOME:$PATH

# Expose Carte Server
EXPOSE ${CARTE_PORT}

# As we cannot use env variable with the entrypoint and cmd instructions
# we set the working directory here to a convenient location
# We set it to KETTLE_HOME so we can start carte easily
WORKDIR $KETTLE_HOME


ENTRYPOINT ["../scripts/docker-entrypoint.sh"]

# Run Carte - these parameters are passed to the entrypoint
CMD ["carte.sh", "carte.config.xml"]
```

> A note on **referencing environment variables** with the `CMD` instruction: If you plan to reference environment variables with `CDM` you have to use the shell style instructions. E.g. this will not work: `CMD ["$PENTAHO_HOME/data-integration/carte.sh"]`, however, this works: `CMD $PENTAHO_HOME/data-integration/carte.sh`. This would work just fine if we were not using `ENTRYPOINTS`, but with `ENTRYPOINTS` we have to use the **JSON** style declartion (see [here](https://docs.docker.com/engine/reference/builder/#cmd)), as the values will be passed on as parameters to the `ENTRYPOINT` shell file. The workaround is to specify the `WORKDIR` upfront and set it to a convenient path.

Ok, it looks like we are finally in the position to build our image and have a test drive with a container:

Let's rebuild the image:

```bash
$ docker build -t diethardsteiner:pdi .
```

> **Note**: This time the image is created a lot quicker. This is because **Docker** cached most of the instructions and only reruns the sections that changed (and the ones thereafter - to be precise).

Let's create a **container** based on our image now:

```bash
$ docker run -it --rm -p 8181:8181 -e PDI_RELEASE=6.0 -e PDI_VERSION=6.0.1.0-386 -e CARTE_PORT=8181 --name myPdiContainer diethardsteiner:pdi
```

The last command starts the container in **interactive mode**, meaning we'll see what's going on inside the container. You should see the container starting up now. Follow the log output - see how **Carte** gets exectued. If you do not know the IP of your `docker-machine` (Mac OS X and Windows users), then press `CTRL+C` and start the container in the detached (background) mode instead and then run `docker-machine ip`:

```
$ docker run -d \
-p 8181:8181 \
-e PDI_RELEASE=6.0 \
-e PDI_VERSION=6.0.1.0-386 \
-e CARTE_PORT=8181 \
--name myPdiContainer \
diethardsteiner:pdi
$ docker-machine ip
192.168.99.100
```

So now we can open our favourite **web browser** and check if **Carte** is available on `http://192.168.99.100:8181`. As we didn't provide a custom user name and password environment variable, the default ones are `cluster` and `cluster`. After logging in, click on the **Show status** link. 

If you are very curious and want to have a look around our container, simply issue the following command which will connect you to the **Bash shell** of the container:

```bash
$ docker exec -it myPdiContainer bash
```

Once you have satisfied your curiousity, just use the `exit` command to get back to your local shell.

Let's also spin up a **Carte slave** to see if all is working as expected:

```
$ docker run -d \
-p 8182:8182 \
-e PDI_RELEASE=6.0 \
-e PDI_VERSION=6.0.1.0-386 \
-e CARTE_PORT=8182 \
-e CARTE_IS_MASTER=N \
-e CARTE_INCLUDE_MASTERS=Y \
-e CARTE_MASTER_HOSTNAME=myPdiContainer \
-e CARTE_MASTER_PORT=8181 \
--name myPdiSlaveContainer \
diethardsteiner:pdi
```

> **Note**: We set `CARTE_MASTER_HOSTNAME` to `myPdiContainer`, which is the alias of the **Carte master** Docker container we are already running.

At this stage we want to make sure that the config file has been created correctly for the slave. We just enter the **container** like so (some sections not shown below for brevity):

```bash
$ docker exec -it myPdiSlaveContainer bash
pentaho@52b603495745:~/data-integration$ cat carte.config.xml 
<slave_config>
  <masters>

    <slaveserver>
      <name>carte-master</name>
      <hostname>myPdiContainer</hostname>
      <port>8181</port>
      <username>cluster</username>
      <password>cluster</password>
      <master>Y</master>
    </slaveserver>

  </masters>

  <report_to_masters>Y</report_to_masters>

  <slaveserver>
    <name>carte-server</name>
    <network_interface>eth0</network_interface>
    <port>8182</port>
    <username>cluster</username>
    <password>cluster</password>
    <master>N</master>
  </slaveserver>

</slave_config>
```

This all looks fine, however, if you try to access the slave via your favourite web browser, you will not be able to access it. Let's inspect what's going on:

```bash
$ docker stop myPdiSlaveContainer
$ docker rm myPdiSlaveContainer
$ docker run -it --rm \
-p 8182:8182 \
-e PDI_RELEASE=6.0 \
-e PDI_VERSION=6.0.1.0-386 \
-e CARTE_PORT=8182 \
-e CARTE_IS_MASTER=N \
-e CARTE_INCLUDE_MASTERS=Y \
-e CARTE_MASTER_HOSTNAME=myPdiContainer \
-e CARTE_MASTER_PORT=8181 \
--name myPdiSlaveContainer \
diethardsteiner:pdi
```

So all seems to be working fine: The **slave** seems to be able to connect to the **master**, however, we still cannot access the **Carte website** for the slave.

Let's double check that that we can indeed reach the master from the slave:

```bash
$ docker run -d \
-p 8182:8182 \
-e PDI_RELEASE=6.0 \
-e PDI_VERSION=6.0.1.0-386 \
-e CARTE_PORT=8182 \
-e CARTE_IS_MASTER=N \
-e CARTE_INCLUDE_MASTERS=Y \
-e CARTE_MASTER_HOSTNAME=myPdiContainer \
-e CARTE_MASTER_PORT=8181 \
--name myPdiSlaveContainer \
diethardsteiner:pdi
$ docker exec -it myPdiContainer bash
pentaho@5216dfb06c3a:~/data-integration$ ping myPdiContainer
PING myPdiContainer (172.17.0.2): 56 data bytes
64 bytes from 172.17.0.2: icmp_seq=0 ttl=64 time=0.104 ms
64 bytes from 172.17.0.2: icmp_seq=1 ttl=64 time=0.096 ms
$ exit
```

And it's working: The slave can communicate with the master.

Let's see what happens if we *officially* create a **network connection** between the slave and the master. The old and not recommended way to achieve this is to use the `--link` flag with the `docker run` command. (The recommended way is to use `docker-compose` instead, which we will explore in a bit). To check if the containers can communicate with each other, just the use `ping` command with the **container alias**. If you want further info, [this](https://rominirani.com/2015/07/31/docker-tutorial-series-part-8-linking-containers/) is an excellent article on linking two containers.

```bash
$ docker stop myPdiSlaveContainer
$ docker rm myPdiSlaveContainer
$ docker run -d \
-p 8182:8182 \
-e PDI_RELEASE=6.0 \
-e PDI_VERSION=6.0.1.0-386 \
-e CARTE_PORT=8182 \
-e CARTE_IS_MASTER=N \
-e CARTE_INCLUDE_MASTERS=Y \
-e CARTE_MASTER_HOSTNAME=myPdiContainer \
-e CARTE_MASTER_PORT=8181 \
--name myPdiSlaveContainer \
--link myPdiContainer \
diethardsteiner:pdi
$ docker exec -it myPdiContainer bash
pentaho@5216dfb06c3a:~/data-integration$ ping myPdiContainer
PING myPdiContainer (172.17.0.2): 56 data bytes
64 bytes from 172.17.0.2: icmp_seq=0 ttl=64 time=0.104 ms
64 bytes from 172.17.0.2: icmp_seq=1 ttl=64 time=0.096 ms
64 bytes from 172.17.0.2: icmp_seq=2 ttl=64 time=0.091 ms
$ exit
```

Wait a few minutes until **Carte** has fully started and then try to access the website of the slave via `

If we check the master now for a list of slaves on `http://192.168.99.100:8181/kettle/getSlaves/` we get this returned:

```xml
<?xml version="1.0"?>
<SlaveServerDetections>
	<SlaveServerDetection>
		<slaveserver>
			<name>Dynamic slave [172.17.0.3:8182]</name>
			<hostname>172.17.0.3</hostname>
			<port>8182</port>
			<webAppName/>
			<username>cluster</username>
			<password>Encrypted 2be98afc86aa7f2e4cb1aa265cd86aac8</password>
			<proxy_hostname/>
			<proxy_port/>
			<non_proxy_hosts/>
			<master>N</master>
			<sslMode>N</sslMode>
		</slaveserver>
		<active>Y</active>
		<last_active_date>2016/04/17 17:21:39.923</last_active_date>
		<last_inactive_date/>
	</SlaveServerDetection>
</SlaveServerDetections>
```

All is good now!

As the work load increases, we could also start up another slave server:

```
$ docker run -d \
-p 8183:8183 \
-e PDI_RELEASE=6.0 \
-e PDI_VERSION=6.0.1.0-386 \
-e CARTE_PORT=8183 \
-e CARTE_IS_MASTER=N \
-e CARTE_INCLUDE_MASTERS=Y \
-e CARTE_MASTER_HOSTNAME=myPdiContainer \
-e CARTE_MASTER_PORT=8181 \
--name myPdiSlaveContainer2 \
--link myPdiContainer \
diethardsteiner:pdi
```

What if we have to scale even further? There must be an easier way of doing this?!

## Building the Carte Cluster

`docker-compose` is used to build your **applications**, normally combining various components like a database and a webservice. In our case, however, the setup will only include the PDI image we created earlier on - several containers acting in different roles to form a cluster:

First up we have to create a `docker-compose.yml` file to define the components of our **application** (see as well [official docu - overview](https://docs.docker.com/compose/overview/) and [official docu - compose file reference](https://docs.docker.com/compose/compose-file/)):

```
version: '2'
services:
  master:
    image:
      diethardsteiner:pdi
    ports:
      - "8181:8181"
    environment:
      - PDI_RELEASE=6.0
      - PDI_VERSION=6.0.1.0-386
      - CARTE_PORT=8181
      - CARTE_IS_MASTER=Y
      - CARTE_INCLUDE_MASTERS=N
  slave:
    image:
      diethardsteiner:pdi
    ports:
      - "8182"
    environment:
      - PDI_RELEASE=6.0
      - PDI_VERSION=6.0.1.0-386
      - CARTE_PORT=8182
      - CARTE_IS_MASTER=N
      - CARTE_INCLUDE_MASTERS=Y
      - CARTE_MASTER_HOSTNAME=master
      - CARTE_MASTER_PORT=8181
    links:
      - master
```

> **Note**: To make sure the slave container starts after the master container, we could use `depends_on`, however, `links` already does the trick: As [the official docu](https://docs.docker.com/compose/compose-file/#links) states, "Links also express dependency between services in the same way as `depends_on`, so they determine the order of service startup."

> **Important**: We already took some precautions in specifying the **startup order** implicitly via the `link` property. However, this is not enough for our purposes! **Docker** will not wait until the **Carte** is ready, only until it can issue the command to start running **Carte** (see [Controlling startup order in Compose](https://docs.docker.com/compose/startup-order/)). In a nutshell, our application should retry to connect to the carte master if it could not connect the first time round (or at any time). **This is still an open point in this setup here and will have to be discussed in the next blog post**. There is a command listed further down which allows you restart you the slave service in case the slave servers do not manage to reconnect. 

> **Important**: **Docker** provides the convenient `docker-compose scale` command to create new instances of a service. One thing to keep in mind is that we cannot just provide an **explicit host to container port mapping** any more for the slaves. Originally I defined in the `docker-compose.yml` file the following mapping:

```
ports:
 - "8182:8182"
```

I changed the ports specification above to the following:

```
ports:
 - "8182"
```

Now we only define the **container port** and **Docker** will **dynamically** assign the **host port**. This is important as when we start up e.g. two slave instances, we cannot just map to the same host port twice, as we only ever have **one host**, but may have **multiple containers**.

To find out which **host port** a certain container port is mapped to, run the following:

```bash
$ docker-compose ps
    Name                  Command               State                 Ports               
-----------------------------------------------------------------------------------------
pdi_master_1   ../scripts/docker-entrypoi ...   Up      0.0.0.0:8181->8181/tcp            
pdi_slave1_1   ../scripts/docker-entrypoi ...   Up      8181/tcp, 0.0.0.0:32770->8182/tcp 
```

In the last entry we can see that container port `8182` was mapped to host port `32770`. If you want to learn more about **Docker networking**, read [this article](https://www.ctl.io/developers/blog/post/docker-networking-rules/).

Then we can run this command to **start the cluster**:

```bash
$ docker-compose up
Creating network "pdi_default" with the default driver
Creating pdi_master_1
Creating pdi_slave_1
Attaching to pdi_master_1, pdi_slave_1
```

This will be followed by a long log showing all the carte servers starting up.

> **Note**: If you are running this command on an older Mac you might get following error: `Illegal instruction: 4`. This even happens if you run `docker-compose --version`, so this is not indicating a problem with the YAML file. [This thread](https://github.com/docker/compose/issues/1885) dicsusses the fix, which is running the following command `pip install --upgrade docker-compose`.

Alright, wait a few minutes until all Carte servers are running - observe the log output. Once you are confident, that the services are working as expected, shut them down by pressing `CTRL+C`. Once all the containers are stopped, we can run the same command just with the `-d` (for daemon) so that the process runs in the background:

```bash
$ docker-compose up -d
Starting pdi_master_1
Starting pdi_slave_1
```
  
Next we can follow the log output like so:

```bash
$ docker-compose logs
```

This will basically provide you the same logs that you would normally see when you run the `docker-compose up` command in the foreground (as above).

We can also be more specific and specify a particular service:

```bash
$ docker-compose logs master
```

To see all the running containers:

```bash
$ docker-compose ps
    Name                  Command               State                Ports               
----------------------------------------------------------------------------------------
pdi_master_1   ../scripts/docker-entrypoi ...   Up      0.0.0.0:8181->8181/tcp           
pdi_slave_1   ../scripts/docker-entrypoi ...   Up      8181/tcp, 0.0.0.0:8182->8182/tcp 
```

Notice that our service names are suffixed with `_1`, indicating the container instance number for this particular service.

Note that the `docker-compose ps` outputs pretty similar info to `docker ps`.

If you are worried at any time that your configuration files inside a container are not quite right, you can always just inspect them like so (In this example we access the bash command line inside container `pdi_master_1`):

```bash
$ docker exec -it pdi_master_1 bash
pentaho@a7c1920a7a23:~/data-integration$
```

Make sure that the **slaves** registered with the **master** by looking at following page:

```
http://<master-ip>:8181/kettle/getSlaves/
```

Wait a few seconds until all slaves are ready (if the services just started up), if you cannot see them being registered by then, restart the slaves like so:

```bash
$ docker-compose restart slave
```

If you ever have to stop the cluster, just execute this command within the same folder as the yaml file is located:

```bash
$ docker-compose stop
```

Finally if you want to remove all the containers run this:

```bash
$ docker-compose rm
```

You might have been wondering how we could so easily get the **slave servers** talking to the **master server**, considering they all run within their own little **container**. The **hostname** of each server happens to be the same as the **service name**. You can easily test this by running the following command on one of the slaves to check if we can connect to the master server:

```bash
$ docker exec -it pdi_slave_1 bash
pentaho@0019d90756bf:~/data-integration$ ping master
PING master (172.18.0.2): 56 data bytes
64 bytes from 172.18.0.2: icmp_seq=0 ttl=64 time=0.088 ms
64 bytes from 172.18.0.2: icmp_seq=1 ttl=64 time=0.088 ms
64 bytes from 172.18.0.2: icmp_seq=2 ttl=64 time=0.081 ms
64 bytes from 172.18.0.2: icmp_seq=3 ttl=64 time=0.077 ms
```

## Sizing the Cluster: Adding extra Nodes

If at some point we want to scale the running cluster, we can simply issue this command: 

```bash
$ docker-compose scale slave=2
$ docker-compose ps
    Name                  Command               State                 Ports               
-----------------------------------------------------------------------------------------
pdi_master_1   ../scripts/docker-entrypoi ...   Up      0.0.0.0:8181->8181/tcp            
pdi_slave_1    ../scripts/docker-entrypoi ...   Up      8181/tcp, 0.0.0.0:32771->8182/tcp 
pdi_slave_2    ../scripts/docker-entrypoi ...   Up      8181/tcp, 0.0.0.0:32772->8182/tcp
```

As you can see we just added one more **Carte slave** to our setup!

Now we can check if the new slave registered with the **Carte master** using following URL (adjust IP if required):

```
http://192.168.99.100:8181/kettle/getSlaves/
```

And you should get the following returned:

```xml
<?xml version="1.0"?>
<SlaveServerDetections>
	<SlaveServerDetection>
		<slaveserver>
			<name>Dynamic slave [172.18.0.3:8182]</name>
			<hostname>172.18.0.3</hostname>
			<port>8182</port>
			<webAppName/>
			<username>cluster</username>
			<password>Encrypted 2be98afc86aa7f2e4cb1aa265cd86aac8</password>
			<proxy_hostname/>
			<proxy_port/>
			<non_proxy_hosts/>
			<master>N</master>
			<sslMode>N</sslMode>
		</slaveserver>
		<active>Y</active>
		<last_active_date>2016/04/21 18:36:10.534</last_active_date>
		<last_inactive_date/>
	</SlaveServerDetection>
	<SlaveServerDetection>
		<slaveserver>
			<name>Dynamic slave [172.18.0.4:8182]</name>
			<hostname>172.18.0.4</hostname>
			<port>8182</port>
			<webAppName/>
			<username>cluster</username>
			<password>Encrypted 2be98afc86aa7f2e4cb1aa265cd86aac8</password>
			<proxy_hostname/>
			<proxy_port/>
			<non_proxy_hosts/>
			<master>N</master>
			<sslMode>N</sslMode>
		</slaveserver>
		<active>Y</active>
		<last_active_date>2016/04/21 18:36:10.540</last_active_date>
		<last_inactive_date/>
	</SlaveServerDetection>
</SlaveServerDetections>
```

This shows us that the **Carte slave** successfully registered with the **Carte master**. This is pretty impressive, isn't it?

Using the output of the `docker-compose ps` command (which we issued above), we can also find the details for connect to the **Carte website** of our new slave:

```
http://192.168.99.100:32772/kettle/status/
```

## Conclusion

Setting up a **Carte** cluster with **Docker** opens up interesting possibilities. I am certain you appreciate how easy it is to scale the cluster. In the next blog post we will have a look at actually running a **PDI** transformation on our cluster.

You can **download** the **PDI jobs and transformation** from [here](https://github.com/diethardsteiner/diethardsteiner.github.io/tree/master/sample-files/pdi/docker-pdi).

