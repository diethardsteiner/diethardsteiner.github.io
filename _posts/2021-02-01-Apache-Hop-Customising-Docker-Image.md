---
typora-root-url: ..
layout: post
title: "Apache Hop: Customising the Docker Image"
summary: A quick walkthrough on how to customise the Hop Docker Image
date: 2021-02-01
categories: [Hop]
tags: 
published: true
---

The **Apache Hop** project provides a **Docker image** for anyone to have an easy start into the containerised world. We - the Apache Hop team - have tried to provide an image that caters for many needs but certainly it can't cater for all needs. So in this article we will take a look at how to extend the provided Docker image with additional features.

One point you'd be interested in straight away is on how to maken your Hop workflows and pipelines available to the container. The Hop Docker image provides a **mount point** on `/files` which allows you to easily map it to a local folder (if you are just experimenting on your laptop) or mount a volume that is prepopulated with the Hop project files (workflows, pipelines, config files, etc). You can also make **JDBC drivers** and additional **plugins** available this way. So you see, out-of-the-box, there are quite a few options available.

The **Apache Hop** **Docker image** can be found on [Docker Hub](https://hub.docker.com/r/apache/incubator-hop). Consult the **Tags** page to get a recent version and just copy the command to pull the image from there, e.g.:

```shell
docker pull apache/incubator-hop:0.70-SNAPSHOT
```

An up-to-date **documentation** on how to run the container can be found [here](https://github.com/apache/incubator-hop/tree/master/docker).

Let's have a test ride: Clone my **minimal Hop project** into a convenient directory:

```shell
git clone https://github.com/diethardsteiner/apache-hop-minimal-project.git
```

 Try to run it via a Docker container (adjust variables to your setup):

```shell
HOP_DOCKER_IMAGE=apache/incubator-hop:0.70-SNAPSHOT
# path to host directory where the Hop minimal project is stored
PATH_TO_LOCAL_PROJECT_DIR=/Users/diethardsteiner/git/apache-hop-minimal-project

docker run -it --rm \
  --env HOP_LOG_LEVEL=Basic \
  --env HOP_FILE_PATH='${PROJECT_HOME}/main.hwf' \
  --env HOP_PROJECT_DIRECTORY=/files \
  --env HOP_PROJECT_NAME=apache-hop-minimum-project \
  --env HOP_ENVIRONMENT_NAME=dev \
  --env HOP_ENVIRONMENT_CONFIG_FILE_NAME_PATHS=/files/dev-config.json \
  --env HOP_RUN_CONFIG=local \
  -v ${PATH_TO_LOCAL_PROJECT_DIR}:/files \
  --name my-simple-hop-container \
  ${HOP_DOCKER_IMAGE}
  
```

> **Note**: When you run this on your laptop (which acts as the host), you map a local directory to the mount point `/files` on the Docker container: `-v ${PATH_TO_LOCAL_PROJECT_DIR}:/files`.

We ask **Docker** to spin up the **Hop container** and run a workflow (`main.hwf`) using the variables for the `dev` environment and the `local` Hop *run configuration*. Once the workflow is finished, the container will be automatically destroyed (feature enabled via the `--rm` flag).

The output on the **Terminal** should be something like this:

```
2021/02/03 17:28:28 - Running a single hop workflow / pipeline (${PROJECT_HOME}/main.hwf)
2021/02/03 17:28:32 - HopRun - Referencing environment 'dev' for project apache-hop-minimum-project' in null
2021/02/03 17:28:32 - HopRun - Enabling project 'apache-hop-minimum-project'
2021/02/03 17:28:43 - main - Start of workflow execution
2021/02/03 17:28:43 - main - Starting action [Run main pipeline]
2021/02/03 17:28:43 - Run main pipeline - Using run configuration [local]
2021/02/03 17:28:43 - main - Executing this pipeline using the Local Pipeline Engine with run configuration 'local'
2021/02/03 17:28:43 - main - Execution started for pipeline [main]
2021/02/03 17:28:43 - Generate rows.0 - Finished processing (I=0, O=0, R=0, W=1, U=0, E=0)
2021/02/03 17:28:43 - Write to log.0 - 
2021/02/03 17:28:43 - Write to log.0 - ------------> Linenr 1------------------------------
2021/02/03 17:28:43 - Write to log.0 - =====================
2021/02/03 17:28:43 - Write to log.0 - 
2021/02/03 17:28:43 - Write to log.0 - VAR_TEST: My test value for the dev environment
2021/02/03 17:28:43 - Write to log.0 - 
2021/02/03 17:28:43 - Write to log.0 - =====================
2021/02/03 17:28:43 - Write to log.0 - 
2021/02/03 17:28:43 - Write to log.0 - 
2021/02/03 17:28:43 - Write to log.0 - ====================
2021/02/03 17:28:43 - Write to log.0 - Finished processing (I=0, O=0, R=1, W=1, U=0, E=0)
2021/02/03 17:28:43 - main - Pipeline duration : 0.407 seconds [  0.407" ]
2021/02/03 17:28:43 - main - Execution finished on a local pipeline engine with run configuration 'local'
2021/02/03 17:28:43 - main - Starting action [Success]
2021/02/03 17:28:43 - main - Finished action [Success] (result=[true])
2021/02/03 17:28:43 - main - Finished action [Run main pipeline] (result=[true])
2021/02/03 17:28:43 - main - Workflow execution finished
2021/02/03 17:28:43 - main - Workflow duration : 0.628 seconds [  0.628" ]
```

Ok, so far we have proven that we can spin up a **Docker container** and run a custom **Hop process**.

The next challenge we face is: How do we get the **code** (Hop pipelines and workflows) into our container once it is running in the cloud? Options:

- **Mount external volume**, like we did locally, just that in the cloud we would have to provision it and start a process that populates the volume with a clone of our git repo. Chances are that our code repo is fairly small and that there are no small size volumes available. To make use of the spare capacity we could use the volume to store some temporary data that Hop creates. That might be handy in some situations. Another point to remember is that our containers will be fairly short-lived.
- Store the code on the **local disk** that is available within the container. How do we get it there? Well quite likely we have to install a Linux package that can take care of this, e.g. git. Or if we are on AWS we would install the AWS CLI package. If we wanted to avoid this at all costs, yes, we could quite likely use Hop as well to copy the files. But let's stick with installing a package for now.

## Installing Git

So how do we go about adding the **git package** to the Hop Docker image?

We could take a look at the Dockerfile in the source code on GitHub and use this as a starting point to build our own Dockerfile, or we can just create a new Dockerfile and use the existing Docker image as a base for it. So it's kind of a **layered approach**: We add another layer on top of the existing Docker image with any custom packages, files, etc that we require for our project.

Let's put this into practice. Create a new file called `custom.Dockerfile` in a folder called `git` with following content:

```dockerfile
FROM apache/incubator-hop:0.70-SNAPSHOT

USER root

RUN apk update \
  && apk add --no-cache git \
  && cd /home/hop \
  && git clone https://github.com/diethardsteiner/apache-hop-minimal-project.git \
  && chown -R hop:hop /home/hop/apache-hop-minimal-project

USER hop
```

As mentioned, first we base our new image on the existing **Hop Docker** image. Then we switch the user to `root`, because in the Hop Docker image the last set user was `hop`. After this we install the **git package** and then clone our minimal project into user hop's home directory ( `/home/hop`) and make user `hop` the owner of it. Finally, we switch back to user `hop`.

Let's **build** the **image**:

```shell
docker build . -f custom.Dockerfile -t ds/custom-hop:latest
```

And now let's try to run our workflow again:

```shell
HOP_DOCKER_IMAGE=ds/custom-hop:latest
PROJECT_DEPLOYMENT_DIR=/home/hop/apache-hop-minimal-project

docker run -it --rm \
  --env HOP_LOG_LEVEL=Basic \
  --env HOP_FILE_PATH='${PROJECT_HOME}/main.hwf' \
  --env HOP_PROJECT_DIRECTORY=${PROJECT_DEPLOYMENT_DIR} \
  --env HOP_PROJECT_NAME=apache-hop-minimum-project \
  --env HOP_ENVIRONMENT_NAME=dev \
  --env HOP_ENVIRONMENT_CONFIG_FILE_NAME_PATHS=${PROJECT_DEPLOYMENT_DIR}/dev-config.json \
  --env HOP_RUN_CONFIG=local \
  --name my-simple-hop-container \
  ${HOP_DOCKER_IMAGE}
```

The implementation above is not really ideal: The main **shortcoming** is that we are now stuck with a very customised image that cannot be easily reused (because it has our code base at a specific version). Instead of making the code base part of the image, it would be more elegant to only download the code base once we start the container - this way we keep the image generic enough so that it can be applied to more use cases.

To make the **Apache Hop Docker image** even more flexible, we added a `HOP_CUSTOM_ENTRYPOINT_EXTENSION_SHELL_FILE_PATH` variable to the base image that accepts a **path to a custom shell script** (that you provide). This shell script will run when you start the container before your Hop project is registered with the container's Hop config and before your Hop workflow or pipeline gets kicked off.

We create a simple **bash script** called `clone-git-repo.sh` in a sub-folder called `resources`:

```shell
#!/bin/bash
cd /home/hop
git clone ${GIT_REPO_URI}
chown -R hop:hop /home/hop/${GIT_REPO_NAME}
```

We also make it **parameter**-driven, so any other team can use it. We adjust our **Dockerfile** like so:

```dockerfile
FROM apache/incubator-hop:0.70-SNAPSHOT
ENV GIT_REPO_URI=
# example value: https://github.com/diethardsteiner/apache-hop-minimal-project.git
ENV GIT_REPO_NAME=
# example value: apache-hop-minimal-project
USER root
RUN apk update \
  && apk add --no-cache git  
# copy custom entrypoint extension shell script
COPY --chown=hop:hop ./resources/clone-git-repo.sh /home/hop/clone-git-repo.sh
USER hop
```

Note that apart from defining the new environment variables (that go in line with the parameters we defined in the `clone-git-repo.sh` earlier on ), we also `COPY` the `clone-git-repo.sh` file to user hop's home directory.

Next let's build a small script which builds our **custom image** and then tests it by spinning up a container and running a workflow:

```shell
#!/bin/zsh

DOCKER_IMG_CHECK=$(docker images | grep ds/custom-hop)

if [ ! -z "${DOCKER_IMG_CHECK}" ]; then
  echo "removing existing ds/custom-hop image"
  docker rmi ds/custom-hop:latest
fi

docker build . -f custom.Dockerfile -t ds/custom-hop:latest

echo " ==== TESTING ====="


HOP_DOCKER_IMAGE=ds/custom-hop:latest
PROJECT_DEPLOYMENT_DIR=/home/hop/apache-hop-minimal-project

docker run -it --rm \
  --env HOP_LOG_LEVEL=Basic \
  --env HOP_FILE_PATH='${PROJECT_HOME}/main.hwf' \
  --env HOP_PROJECT_DIRECTORY=${PROJECT_DEPLOYMENT_DIR} \
  --env HOP_PROJECT_NAME=apache-hop-minimum-project \
  --env HOP_ENVIRONMENT_NAME=dev \
  --env HOP_ENVIRONMENT_CONFIG_FILE_NAME_PATHS=${PROJECT_DEPLOYMENT_DIR}/dev-config.json \
  --env HOP_RUN_CONFIG=local \
  --env HOP_CUSTOM_ENTRYPOINT_EXTENSION_SHELL_FILE_PATH=/home/hop/clone-git-repo.sh \
  --env GIT_REPO_URI=https://github.com/diethardsteiner/apache-hop-minimal-project.git \
  --env GIT_REPO_NAME=apache-hop-minimal-project \
  --name my-simple-hop-container \
  ${HOP_DOCKER_IMAGE}
```



## Wrapping up

We had a look at a simple example on how to customise the existing **Apache Hop** Docker image. I hope that this example gave you some ideas on how to go the extra mile with the base image.