---
typora-root-url: ..
typora-copy-images-to: ../images/project-hop
layout: post
title: "Project Hop: Create Environment Definitions"
summary: A brief overview of how to create environment definitions for Hop projects
date: 2020-04-15
categories: [Hop]
tags: 
published: true
---

The team behind **Project Hop** has done an excellent job in cleaning up and improving the configuration of what was previously know as KETTLE/PDI. Finally we have built-in **environment management** available, a feature I've been waiting for for a decade. I must say **Hop** is shaping up really nicely: There is a lot of focus on Hop as a product instead of ticking marketing/sales checkboxes. It feels like a product that we as developers will be proud and happy to work with!

In this article we will dive into the **environment management** aspect of **Hop**. Usually our data processes are propagated through various environments (e.g. development, test, UAT, production). As a developer we should always aim to make our data processes configurable as much as possible, so that they can be easily adjusted to changing database connection details, paths etc. Configurable elements (like e.g. path) are usually represented by Parameters (`${PARAMATER}`), sometimes also called properties or variables (I am talking in general here, there might be fine nuances between them). The values of these parameters can be provided via a **Hop Environment Definition** (this is not an exclusive mechanism, there are various other ways available as well). The beauty of this approach is that we can define upfront which environment definition we want to use before running our data process. And this all done in a very easy way, because it is all built into Hop now.

The hop package contains a utility called `hop-conif.sh` which helps with managing the environment definitions. Let's have a look which functions it provides:

```
% ./hop-conf.sh --help
Usage: <main class> [-h] [-ec] [-ed] [-ee] [-el] [-em] [-eo]
                    [-e=<environmentName>] [-eh=<environmentHome>]
                    [-ev=<environmentVariables>[,<environmentVariables>...]]...
                    [-s=<systemProperties>[,<systemProperties>...]]...
  -e, -environment=<environmentName>
               The name of the environment to manage
      -ec, -environment-create
               Create an environment. Also specify the name and its home
      -ed, -environment-delete
               Delete an environment
      -ee, --environments-enable
               Enable the environments system
      -eh, --environments-home=<environmentHome>
               The home directory of the environment
      -el, -environment-list
               List the defined environments
      -em, -environment-modify
               Modify an environment
      -eo, --environments-open-last-used
               Open the last used environment in the Hop GUI
      -ev, --environment-variables=<environmentVariables>[,<environmentVariables>...]
               The variables to be set in the environment
  -h, --help   Displays this help message and quits.
  -s, --system-properties=<systemProperties>[,<systemProperties>...]
               A comma separated list of KEY=VALUE pairs
  ```


> **Note**: The `--environments-home` flag can only be used in tandem with `-environment-create` or `-environment-delete` or `-environment-modify`. It can't be used on its own just to set the path to the environments folder, this is what `HOP_CONFIG_DIRECTORY` is for.


Config Artefact	| Description
----	|----
`<hop-package>/config/config.json`	| Defines Hop related settings (`systemProperties` and `LocaleDefault`). Example System Property: `HOP_MAX_LOGGING_REGISTRY_SIZE`. 
`<hop-package>/config/environments/metastore`	| Metastore for the actual environment definitions

> **Note**: The old `~/.hop` folder is no longer in use.

A few lines from the `config.json`:

```json
{
  "systemProperties" : {
    "HOP_SERVER_JETTY_RES_MAX_IDLE_TIME" : null,
    "HOP_COMPATIBILITY_DB_IGNORE_TIMEZONE" : "N",
    "HOP_MAX_LOGGING_REGISTRY_SIZE" : "10000",
    "HOP_LOG_TAB_REFRESH_DELAY" : "1000",
    "HOP_FILE_OUTPUT_MAX_STREAM_LIFE" : "0",
    "HOP_PLUGIN_CLASSES" : null,
    "HOP_COMPATIBILITY_TEXT_FILE_OUTPUT_APPEND_NO_HEADER" : "N",
    "HOP_MAX_LOG_SIZE_IN_LINES" : "5000",
    "HOP_PIPELINE_PAN_JVM_EXIT_CODE" : null,
    "HOP_MAX_LOG_TIMEOUT_IN_MINUTES" : "1440",
```

Here a description of a few interesting properties:

-	`HOP_PLUGIN_CLASSES`: The classes variable is for development and integration is you want to direct the plugin registry straight to a list of plugins without scanning jar files for them and when the classes are already in the classpath. 
-	`HOP_PLUGIN_PACKAGES`: A list of packages to scan during boot time.

I will mainly talk you through the process of using custom locations for the config artefacts because this is the more complicated approach:

Within my sample project's git repo I created a `config` folder that I further sub-divided into `hop` and `project` related config artefacts - this is just for demo purposes for this article. In real-world projects you might want to stick them into separate git repos - separated from the main repo that holds the workflow and pipeline definitions.

```
project-a % tree config -I history                                                                                                                                    (master)project-hop-in-the-cloud
config
├── hop
│   └── config
│       ├── config.json
│       ├── config.json.old
│       └── environments
│           └── metastore
│               ├── Hop\ Environment
│               │   └── project-a-dev.xml
│               └── Hop\ Environment\ Configuration
│                   ├── system.xml
│                   └── system_backup.xml
└── project
    └── metastore
        ├── Pipeline\ Run\ Configuration
        │   ├── Standard.xml
        │   ├── classic.xml
        │   ├── kettle-classic.xml
        │   └── test.xml
        ├── Relational\ Database\ Connection
        │   └── postgresql-localhost.xml
        └── Workflow\ Run\ Configuration
            └── classic.xml
```

`config/hop/config` normally resides in `<hop-package>/config`. Because we have a custom setup, we will define where our **Hop Config Directory** resides. This can be done via the `HOP_CONFIG_DIRECTORY` environment variable. This one expects the full path to the config folder (including the final `config` folder). Example:


```
export HOP_CONFIG_DIRECTORY=/Users/diethardsteiner/git/project-hop-in-the-cloud/project-a/config/hop/config
```


# Creating Environment Definition

Hop comes with an environment switcher support built-in. When you create an environment definition, you can specify where the Hop config details are located. This includes:

-	Deployment path (`ENVIRONMENT_HOME`)
-	Path to Metastore (`HOP_METASTORE_FOLDER`): Directory where the metastore folder is located. Don’t include the `metastore` folder your path.
-	Unit tests base path (`UNIT_TESTS_BASE_PATH`): Directory that should be used as the base path of the unit tests. Usually this is the same as the environment home.
-	Datasets CSV folder (`DATASETS_BASE_PATH`): Directory where you store you dataset for unit tests.

Before you start `hop-gui`, you might want to set `HOP_CONFIG_DIRECTORY` to a custom location (the default location is the user's home directory). Example:

```
export HOP_CONFIG_DIRECTORY=/Users/diethardsteiner/git/project-hop-in-the-cloud/project-a/config/hop/config
./hop-gui.sh
```


To **create a new environment** choose **File > New > Environment** and fill out the form.
Once you click **Ok** you will be asked if you want to switch to the new environment. If you just created an environment definition for your local dev environment, then you might want to switch to it - otherwise not.

Setting	| Example Value
---	|---
`environmentHomeFolder`	| `/Users/diethardsteiner/git/project-hop-in-the-cloud/project-a`
`metaStoreBaseFolder`	| `${ENVIRONMENT_HOME}/config/project`
`unitTestsBasePath`	| `${ENVIRONMENT_HOME}`
`dataSetsCsvFolder`	| `${ENVIRONMENT_HOME}/datasets`


When you execute `hop-run`, you can specify this environment config now by using the `-e` or `--environment` flags:

```
export HOP_CONFIG_DIRECTORY=/Users/diethardsteiner/git/project-hop-in-the-cloud/project-a/config/hop/config
~/apps/hop/hop-run.sh \
  --file=/Users/diethardsteiner/git/project-hop-in-the-cloud/project-a/pipelines-and-workflows/main.hwf \
  --runconfig=classic \
  --parameters=PARAM_LOG_MESSAGE=Hello,PARAM_WAIT_FOR_X_MINUTES=1 \
  --environment=project-a-dev
```


# Amending Environment Configurations

From the main menu choose **File > Edit Metastore Element > Environment**.

# Using the command line

There are main options available, I will just cover a few here:

Show available Hop environments:

```
% export HOP_CONFIG_DIRECTORY=/Users/diethardsteiner/git/project-hop-in-the-cloud/project-a/config/hop/config
% ./hop-conf.sh -el                                                                                                                              
2020/05/05 21:02:13 - hop-config - Environments:
2020/05/05 21:02:13 - hop-config - project-a-dev : /files/project-hop-in-the-cloud/project-a
2020/05/05 21:02:13 - hop-config -   PROP_SAY_SOMETHING = Hello World
2020/05/05 21:02:13 - hop-config - project-a-local : /Users/diethardsteiner/git/project-hop-in-the-cloud/project-a
2020/05/05 21:02:13 - hop-config -   PROP_SAY_SOMETHING = Hello World
```

As we can see, we have currently two environments defined:

- `project-a-dev`
- `project-a-local`

To create a new environment, you can use these arguments:

```
./hop-conf.sh \
  -environment=project-a-uat \
  -environment-create \
  --environments-home=/opt/project-hop-in-the-cloud/project-a/config/hop \
  --environment-variables=PROP_PATH_A=/tmp/a,PROP_PATH_B=/tmp/b
# or
./hop-conf.sh \
  -e=project-a-prod \
  -ec \
  -eh=/opt/project-hop-in-the-cloud/project-a/config/hop \
  -ev=PROP_PATH_A=/opt/a,PROP_PATH_B=/opt/b
```

> **Note**: Pay attention to the dashes for the fully spelled out arguments! Some have a single dash whereas others have double dashes.