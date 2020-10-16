---
typora-root-url: ..
typora-copy-images-to: ../images/project-hop-env
layout: post
title: "Project Hop: Project and Environment Configuration"
summary: A brief overview of how to create environment definitions for Hop projects
date: 2020-07-01
categories: [Hop]
tags: 
published: true
---

**Project Hop** received a major improvement as part of the [Environments: Support for separation of config and code](https://project-hop.atlassian.net/browse/HOP-467) ticket. I will briefly guide you through the steps to configure your **Hop project** correctly so that it works smoothly across environments (e.g. dev, test, prod).

There are two high level concepts:

- **Project**: Contains all your Hop pipelines, workflows, metadata, (unit test) datasets. Can also contain other code artefacts.
- **Environment**: The configuration related to a specific enviornment (e.g. dev, test, prod) for your project.

This achieves the separation of code and configuration.

Let's see how this hangs toghether in the **Hop** world:



![](/images/project-hop-env/hop-config.png)

The **configuration levels** and their **relationships** are as follows (top to down):

- By default each **instance of Hop** has *one* **Hop configuration** (aka system configuration), however, this can be overwritten by the environment variable `HOP_CONFIG_DIRECTORY`, which allows you to dynamically point to a custom location for the Hop config. So keeping this feature in mind, actually each **instance of Hop** can be pointed to *many* **Hop configurations**.
- Each **Hop configuration** (aka system configuration) can have *many* **project configurations** linked to it.

- Each **project configuration** can have *many* **environment configurations**.

You can set **variables** at any of these levels, but their scope - naturally - will be different.

Level	| Variable Scope	| Stored in
---	|---	|---
Hop	| JVM	| `${HOP_CONFIG_DIRECTORY}/hop-config.json`
Project	| Project	| `${PROJECT_HOME}/project.json`
Environment	| Project & Environment	| custom config files and locations

## Hop Configuration

This is sometimes also referred to as **system configuration**.

**Optional** first step: If you do not want the **system config** details to be stored in the default location (`<hop-install-dir>/config`), then you can change the location of the config folder like so (adjust to your needs):

```bash
export HOP_CONFIG_DIRECTORY=~/config/hop
```

Currently, if you change the path, the `hop-config.json` file will not be created automatically, so just run this:

```bash
echo "{}" > $HOP_CONFIG_DIRECTORY/hop-config.json
```

Otherwise you will see this error in the console (and ultimately your config will not be saved):

```
Hop configuration file not found, not serializing: /Users/diethardsteiner/config/hop/hop-config.json
```

## Project Configuration

When you start **Hop GUI**, you will find a **Project** and **Environment** specific section in the toolbar:

![Screenshot 2020-06-26 at 08.53.03](/images/project-hop-env/Screenshot 2020-06-26 at 08.53.03.png)

The project and environment configuration are first class citizens in the Hop world!

First we will create a **Project**: Click the **P+** button and the **Project dialog** will pop up:

![image-20200626090052683](/images/project-hop-env/image-20200626090052683.png)

First you define the **path to your project** and the **name** of the **project config file** (by default `project-config.json`).

The project config points to the **metadata**, **unit tests base** and **datasets** folders:

-  **Metadata**: This includes artefacts like database connections, run configurations etc. The important bit to understand here is that you are meant to configure these artefacts in a generic way using **variables**. 

  > **Important**: While you can hard code values (e.g. host name) for metadata artefacts this would not work well at all with projects that have to be propageted through multiple environments. The best practice is to separating config from code. A metadata artefact is considered part of the code base (the term code base here is quite liberally used) and hence you should use variables and not hard-coded values for metadata artefacts (in example when defining a database connection, set a variable for the hostname instead of the actual hardcoded value).

- **Unit test base**: The directory the unit tests should run from.

- **Datasets base**: The directory that holds datasets mainly to be used with unit tests.

Once you click **OK** (within the **Project dialog**) you will be asked (among other things) if you want to create an **environment**, which you confirm with **Yes**:

![image-20200626090158206](/images/project-hop-env/image-20200626090158206.png)

**Background info**: 

At this point you should have a file called `project-config.json` in your project's folder:

![Screenshot 2020-06-26 at 09.06.14](/images/project-hop-env/Screenshot 2020-06-26 at 09.06.14.png)

And your `hop-config.json` will have an additional entry similar to this one:

```json
  "projectsConfig" : {
    "enabled" : true,
    "openingLastProjectAtStartup" : true,
    "projectConfigurations" : [ {
      "projectName" : "project-a",
      "projectHome" : "/Users/diethardsteiner/git/hop-docker/tests/project-a",
      "configFilename" : "project-config.json"
    } ]
```



## Environment Configuration

If you just create a **project**, the **Project Lifecycle Environment** dialog show already be front and centre on your screen. If, however, you when through this earlier process earlier on and just want to define an additional **environment**, simply press the **E+** button in the toolbar.

![Screenshot 2020-06-26 at 09.25.01](/images/project-hop-env/Screenshot 2020-06-26 at 09.25.01.png)

For your environment you can define **one or more config files**. The idea behind this is that you can have some environment specific settings but you might also have one config file with settings that are shared (let's say between two environments). Remember that if you want to define global/system/Hop variables, they should reside in the project configuration and not on this level. 

First click **Add** and define the path to the to the config file. The file doesn't have to exist at this stage. Just type the full path into the **filename** field, e.g.:

```
/Users/diethardsteiner/config/project-a/project-a-dev.json
```

And then click **OK**.

Next click on the **Edit** button - if the file doesn't exist at this stage it will be cretaed for you. Now you can define your environment specific variables:

![image-20200626093507878](/images/project-hop-env/image-20200626093507878.png)

Click  **OK** twice.

> **Note**: With the functionality shown above Hop provides the flexibility to store your environment configuration details in git repos seperate from the code repo. 

**Background info**:

At this stage your custom environment specific config will would have been created in the defined location.

And your `hop-config.json` will have an additional entry similar to this one:

```json
    "lifecycleEnvironments" : [ {
      "name" : "project-a-dev",
      "purpose" : "Development",
      "projectName" : "project-a",
      "configurationFiles" : [ "/Users/diethardsteiner/config/project-a/project-a-dev.json" ]
    } ]
```



If we trigger the **Open File** dialog in the GUI now, it will open at the root of our project: 

![image-20200629085828272](/images/project-hop-env/image-20200629085828272.png)

## Command Line

Via the command line utility `hop-conf.sh` you can generate project and enviornment configurations as well:

```bash
# Create Hop project
./hop-conf.sh \
--project=project-a \
--project-create \
--project-home="/Users/diethardsteiner/git/hop-docker/tests/project-a" \
--project-config-file=project-config.json \
--project-metadata-base='${PROJECT_HOME}/metadata' \
--project-datasets-base='${PROJECT_HOME}/datasets' \
--project-unit-tests-base='${PROJECT_HOME}' \
--project-variables=VAR_PROJECT_TEST1=a,VAR_PROJECT_TEST2=b \
--project-enforce-execution=true

# Create Hop environment
./hop-conf.sh \
--environment=project-a-dev \
--environment-create \
--environment-project=project-a \
--environment-purpose=development \
--environment-config-files="/Users/diethardsteiner/config/project-a/project-a-dev.json"

# Set variables for the env config
./hop-conf.sh \
--config-file="/Users/diethardsteiner/config/project-a/project-a-dev.json" \
--config-file-set-variables=VAR_ENV_TEST1=c,VAR_ENV_TEST2=d
```

And `hop-run.sh` naturally has full support for these configuration artefacts as well:

```bash
./hop-run.sh \
  --file='/Users/diethardsteiner/git/hop-docker/tests/project-a/pipelines-and-workflows/simple.hpl' \
  --project=project-a \
  --environment=project-a-dev \
  --runconfig=classic
# or more conveniently
./hop-run.sh \
  --file='${PROJECT_HOME}/pipelines-and-workflows/simple.hpl' \
  --project=project-a \
  --environment=project-a-dev \
  --runconfig=classic
```

> **Note**: You do not have to specify the project if you specify the environment. Hop knows which environment belongs to which project. This works as long as the environment name is not shared across projects.

> **Note**: Creating or deleting projects or environments via the Hop commands never deletes or replaces existing files or directories.

# Deployment to higher level environments

You will start out work in the dev environment and then propaged your solution to (e.g.) a test and then prod environment. The project config file should not change for these environments, the environment config file, however, will very likely change.



So not that we have established that the **project configuration file** will **stay the same** for **all environments**, all we have to do is to let **Hop** know where the root of the project is once we deployed the code:

```bash
echo "Registering project config with Hop"

${DEPLOYMENT_PATH}/hop/hop-conf.sh \
--project=${HOP_PROJECT_NAME} \
--project-create \
--project-home="${HOP_PROJECT_DIRECTORY}" \
--project-config-file="${HOP_PROJECT_CONFIG_FILE_NAME}"
```

For the **environment configuration file** you have **two options**:

- Prepare a config file upfront
- Create a config file on the fly

For both options, we have to first let **Hop** know where the config file will reside. If the file doesn't exist at this stage, Hop will create it:

```bash
echo "Registering environment config with Hop"

${DEPLOYMENT_PATH}/hop/hop-conf.sh \
--environment=${HOP_ENVIRONMENT_NAME} \
--environment-create \
--environment-project=${HOP_PROJECT_NAME} \
--environment-config-files="${HOP_ENVIRONMENT_CONFIG_FILE_NAME_PATHS}"
```

In case the config file was not part of your deployment, you can populate it dynamically like so:

```bash
# Set variables for the env config
./hop-conf.sh \
--config-file="/path/to/config/file.json" \
--config-file-set-variables=VAR_ENV_TEST1=c,VAR_ENV_TEST2=d
# Or you can of course also fetch the values from the env if it's set up that way
./hop-conf.sh \
--config-file="/path/to/config/file.json" \
--config-file-set-variables=VAR_ENV_TEST1=${ENV_TEST1},VAR_ENV_TEST2=${ENV_TEST2}
```

## Predefined Variables

Currently following pre-defined variables exist in regards to project and environment configuration:

- `PROJECT_HOME`

requested ([Jira case](https://project-hop.atlassian.net/browse/HOP-501)):

- `ENVIRONMENT_NAME`

- `ENVIRONMENT_HOME`

- `PROJECT_NAME`