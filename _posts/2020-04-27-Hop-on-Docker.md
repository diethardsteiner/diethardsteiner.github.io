---
typora-root-url: ..
typora-copy-images-to: ../images/project-hop
layout: post
title: "Project Hop: Hop on Docker"
summary: A brief overview on how to create a Docker image for Hop
date: 2020-04-27
categories: [Hop]
tags: 
published: true
---

In this brief article we will take a look at how to create a simple Docker image for [Hop](https://www.project-hop.org), the new **open source** data processing tool.

If your are not really eager to learn how the **Docker image** was created but rather want to use it straight away, you can download it from [Docker Hub](https://hub.docker.com/repository/docker/diethardsteiner/project-hop).

# Building the Docker Hop image


I expect that you know how to create a `Dockerfile`, so I just give you a brief rundown of my setup:


```
FROM openjdk:8-alpine
MAINTAINER Diethard Steiner
# path to where the artefacts should be deployed to
ENV DEPLOYMENT_PATH=/opt/project-hop
# specify the hop log level
ENV HOP_LOG_LEVEL=Basic
# path to hop workflow or pipeline e.g. ~/project/main.hwf
ENV HOP_FILE_PATH=
# file path to hop log file, e.g. ~/hop.err.log
ENV HOP_LOG_PATH=$DEPLOYMENT_PATH/hop.err.log
# hop run configuration to use
ENV HOP_RUN_CONFIG=
# parameters that should be passed on to the hop-run command
# specify as comma separated list, e.g. PARAM_1=aaa,PARAM_2=bbb
ENV HOP_RUN_PARAMETERS=


RUN apk update \
  && apk add --no-cache bash curl procps \ 
  && rm -rf /var/cache/apk/* \
  && mkdir ${DEPLOYMENT_PATH} \
  && adduser -D -s /bin/bash -h /home/hop hop \
  && chown hop:hop ${DEPLOYMENT_PATH}

# copy the hop package from the local resources folder to the container image directory
COPY --chown=hop:hop ./resources/ ${DEPLOYMENT_PATH}

# make volume available so that hop pipeline and workflow files can be provided easily
# this one should also include the .pod config
VOLUME ["/home/hop"]
USER hop
ENV PATH=$PATH:${DEPLOYMENT_PATH}/hop
WORKDIR /home/hop
ENTRYPOINT ["/opt/project-hop/load-and-execute.sh"]
```

As you can see, it's rather simple: 

1. We base our image on a small **Alpine** image that has **OpenJDK** preinstalled.
2. We define a few **environment variables** that are essential for running **Hop**. (At this point in time Hop doesn't support all of the environment variables that we are used to from the PDI/Kettle world - but no doubt, it will catch up soon).
3. Then we install a few utilities, create the **deployment path**, add the **user** `hop` with a dedicated **home directory** and make the `hop` user the **owner** of the **deployment folder**.
4. Next we copy **Hop** and the `load-and-execute.sh` script from our **local resource folder** to the **deployment folder**.
5. Finally we define a mountpoint `/home/hop` which will enable us to mount **volume** containing **Hop workflows and pipelines** as well as the **Hop environment configuration**. We change to active user to `hop`, add the `hop` directory to the path so we can easily call the hop utilities, set the working directory to the hop user's home and eventually define an entrypoint.

The entrypoint script `load-and-execute.sh` looks like this:

```bash
if [ -z "${HOP_FILE_PATH}" ]
then
  echo "=== >> === >> === >> === >> === >> === >> === >> === >> === >> === >> "
  echo "Since no file name was provided, we will start hop server."
  echo "=== >> === >> === >> === >> === >> === >> === >> === >> === >> === >> "
  ${DEPLOYMENT_PATH}/hop/hop-server.sh 127.0.0.1 8080 -u cluster -p cluster
else
  # Run main job
  ${DEPLOYMENT_PATH}/hop/hop-run.sh \
    --file=${HOP_FILE_PATH} \
    --runconfig=${HOP_RUN_CONFIG} \
    --level=${HOP_LOG_LEVEL} \
    --parameters=${HOP_RUN_PARAMETERS} \
    2>&1 | tee ${HOP_LOG_PATH}
fi
```

If no Hop pipeline or workflow file path was provided, we start the **Hop server** (currently  this is all very static - I am planning to make this configurable as a next step), otherwise we execute the Hop process via `hop-run`. This provides us the flexibility to run either a **long-lived container** (`hop-server`) or a **short-lived container** (`hop-run`).

# Using the Docker image: Running the Hop Container

The container image is available on [Docker Hub](https://hub.docker.com/r/diethardsteiner/project-hop).

## Container Folder Structure


Directory	| Description
---	|---
`/opt/project-hop`	| location of the hop package
`/home/hop`	| here you should mount a directory that contains `.hop` at the root as well your workflows and pipelines.

## Environment Variables

You can provide values for the following environment variables:


Environment Variable	| Description
---	|----
`ENV HOP_LOG_LEVEL`	| Specify the log level. Default: `Basic`. Optional.
`ENV HOP_FILE_PATH`	| Path to hop workflow or pipeline
`ENV HOP_LOG_PATH`	| File path to hop log file
`ENV HOP_RUN_CONFIG`	| Hop run configuration to use
`ENV HOP_RUN_PARAMETERS`	| Parameters that should be passed on to the hop-run command. Specify as comma separated list, e.g. `PARAM_1=aaa,PARAM_2=bbb`. Optional.


## How to run the Container

The most common use case will be that you run a **short-lived container** to just complete one Hop workflow or pipeline.

Example for running a **workflow**:

```bash
docker run -it --rm \
  --env HOP_LOG_LEVEL=Basic \
  --env HOP_FILE_PATH=/home/hop/pipelines-and-workflows/main.hwf \
  --env HOP_RUN_CONFIG=classic \
  --env HOP_RUN_PARAMETERS=PARAM_TEST=Hello \
  -v /Users/diethardsteiner/git/project-a:/home/hop \
  --name my-simple-hop-container \
  diethardsteiner/project-hop:0.20-20200429.230019-56
```

If you need a **long-lived container**, this option is also available. Run this command e.g.:

```bash
docker run -it --rm \
  -v /Users/diethardsteiner/git/project-a:/home/hop \
  --name my-simple-hop-container \
  diethardsteiner/project-hop:0.20-20200429.230019-56
```

## Shortcomings

- **Metastore**: Currently `HOP_METASTORE_HOME` does not seem to be picked up by Hop. Moreover the Metastore is expected to reside in the default location (`$HOME/.hop/metastore`). Hence it is also not recommended to change `HOP_HOME`. The Metastore is require for the run configuration.



