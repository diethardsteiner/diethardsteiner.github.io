---
typora-root-url: ..
typora-copy-images-to: ../images/kettle-environment-plugin
layout: post
title: "Pentaho Data Integration/Kettle: Environment Plugin"
summary: This article explains how to get started with a dynamic environment setup
date: 2018-12-16
categories: PDI
tags: PDI
published: true
---

Lately things are getting better around PDI/Kettle: This data integration tool has been around for over a decade, however, basic features like built-in **environment configuration** and **unit testing** have been missing ... until now that is, since Kettle founder **Matt Casters** recently has been building plugins to support this functionality.

In this brief blog post we will focus on the **Kettle Environment Plugin**:
![](/images/kettle-environment-plugin/kettle-environment-1.png)

- What does it do for you? **A**: It allows you define for each project an environment (e.g. `dev`, `prod`) and all the essential settings that come with it (see screenshot above). Notably it supports also unit testing settings (see also my blog post on [unit testing](http://diethardsteiner.github.io/big/data/2016/01/30/PDI-Unit-Testing.html)).
- Where do I get the plugin from? **A**: [GitHub](https://github.com/mattcasters/kettle-environment). Follow the build instructions and installation instruction from the readme. Once you start Spoon, you will be presented by the environment dialog, which also allows you to define a new environment.
- Where do I find the **documentation**? **A**: [Here](https://github.com/mattcasters/kettle-environment/blob/master/README.md)
- Where do the config details get stored? **A**: Config is stored in separate metastore in `~/.kettle/environment`. 
- If I ran kitchen in another environment, how would I pick up the environment details from the **command line**? **A**: Check out the [kettle-needful-things](https://github.com/mattcasters/kettle-needful-things) project for a "better pan/kitchen" called **Maitre**. See [Maitre Docu](https://github.com/mattcasters/kettle-needful-things/wiki/Maitre).
- Can I **export** and **import** the **environment definitions**? **A**: Yes, they can be exported as JSON file and you can import them as well. You can also specify this JSON file when calling **Maitre** via the `-e`  or `(--environment)` option. 
- **Enforce execution in environment home**? What does this option mean? **A**: It will prevent you from running any transformation or job that is located outside the environment base folder.
- Can I define a **default environment**? **A**: Yes, just click the **Create Default** button at the bottom of the dialog instead of the `+` icon at the top of the dialog. Tip: You can leave the _Environment base folder_ unspecified and untick _Enforce executions in environment home?_. This way this default environment will behave exactly the same way as when this plugin is not installed.
- Can I create a **new environment** from the **command line**? **A**: Release 1.4.0 introduced an extension point to allow creation of new environments from the command line via `maitre.sh`.
- Does this plugin integration with **GitSpoon**? **A**: Yes, as of release 1.3.0.

> **Important**: The environments plugin does not change the `KETTLE_HOME` variable. The `KETTLE_HOME` directory is meant for system settings not environment settings. 

Matt: "The issue lies in the fact that Spoon itself uses `KETTLE_HOME` which makes it hard to reliably switch to a different `KETTLE_HOME` all the time. You can still put e.g. database connection settings into `kettle.properties`, however, that information would be for all environments, which is not what you usually want."

So instead of defining your **variables** in the `kettle.properties` file (which resides under `KETTLE_HOME`), you  can define them now in the **environment specification**. The environment specification itself will be deployed to the `KETTLE_HOME` folder (as stated above in more detail). 

> **In a nutshell**: Don't use `kettle.properties` any more.



What's the point of having the KETTLE_HOME folder in the environment definition?

# Environment Example Setup Via GUI

Matt Casters also provides a Git repo with a few Kettle Beam Examples. Apart from covering Beam, it also showcases good practices by using **unit testing** and **environment specification**. It is a totally self contained git repo.

To properly use this git repo, you have to install following **PDI plugins** (or alternatively download **Kettle Remix** from [here](http://www.kettle.be) which has all these plugins already built-in):

- [Kettle Environments Plugin](https://github.com/mattcasters/kettle-environment)
- [Maitre - Kettle Needful Things Plugin](https://github.com/mattcasters/kettle-needful-things)
- [GitSpoon Plugin](https://github.com/HiromuHota/pdi-git-plugin)
- [Pentaho PDI Dataset Plugin](https://github.com/mattcasters/pentaho-pdi-dataset)
- [Kettle Beam Plugin](https://github.com/mattcasters/kettle-beam)

Next clone the [Kettle Beam Examples Git repo](https://github.com/mattcasters/kettle-beam-examples) to a convenient location (somewhere outside the PDI directory):

```bash
git clone https://github.com/mattcasters/kettle-beam-examples.git
```

Once you start up **Spoon**, you should be presented with an **Environments** dialog. Click the `+` icon to create a new environment.

![](/images/kettle-environment-plugin/kettle-environment-0.png)

On the next screen, all you have to do is provide a **name**, **description** and **environment base folder** (last one is the directory in which the git repo is stored) and job done:

![](/images/kettle-environment-plugin/kettle-environment-1.png)

You set the parameters values here, you do not pass parameters in. You can reference existing parameters as is shown in the last screenshot.

Mandatory fields:

- Description
- Environment base folder
- Metastore base folder

Good practice/Nice to have:

- Unit tests base path: This is related to unit testing.
- Data sets CSV folder: This is related to unit testing.

> **Note**: The paths that you set in this dialog will be available later on also via the upper case parameters mentioned in the brackets. So e.g. when you specify the **Environment base folder**, this path will be available via the `ENVIRONMENT_HOME` variable once this environment is selected.

The last section of the dialog allows you to define additional parameters that you want to use within this environment for your jobs and transformations. In this case it is not required, but it is very likely that for your project you will define these extra parameters.

| Variable                 | Description                                                  | Example                                                      |
| ------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| ENVIRONMENT_HOME         | You can think of this directory as the deployment directory of your pdi code. | `~/git/ds-kettle-beam-examples/dskbe-code/pdi/jobs-and-transformations` |
| KETTLE_HOME              | Directory where the `.kettle` folder is located. Don't include the `.kettle` folder in your path. | `~/git/ds-kettle-beam-examples/dskbe-config-dev/pdi`         |
| PENTAHO_METASTORE_FOLDER | Directory where the `metastore` folder is located. Don't include the `metastore` folder your path. | `~/git/ds-kettle-beam-examples/dskbe-code/pdi`               |
| UNIT_TESTS_BASE_PATH     | Directory that should be used as the base path of the unit tests. Usually this is the same as the environment home. | `${ENVIRONMENT_HOME}`                                        |
| DATASETS_BASE_PATH       | Directory where you store you dataset (e.g. for unit tests)  | `~/git/ds-kettle-beam-examples/dskbe-code/pdi/unit-test-datasets` |



# Maitre

Maitre is a replacement for both `kitchen.sh` and `pan.sh`. It also allows to reference the environment configuration created via Spoon (once you export it to JSON).

## Installation

Download release file from [here](https://github.com/mattcasters/kettle-needful-things/releases) and extract it. Then:

* Copy library `kettle-needful-things-.jar` into the `lib/` folder of your PDI distribution.
* Copy library `lib/picocli-.jar` into the `lib/` folder of your PDI distribution
* Copy scripts `Maitre.bat` and/or `maitre.sh` into your PDI distribution

## Create an Environment

Use following command to create an environment called `test`:

```bash
sh maitre.sh \
  --create-environment test=/home/project-a/test
```

Instead of the long form `--create-environment` you can also use the short form `-C` flag:

```bash
sh maitre.sh \
  -C test=/home/project-a/test
```

In addition you can also define variables via the `-V` flag:


```bash
sh maitre.sh \
  -C test=/home/project-a/test \
  -V PARAM_COLOR=red \
  -V PARAM_TEMP=30
```

In case you mention passwords a variable values, it's good practise to obfuscate (or encrypt - see also [here](https://wiki.pentaho.com/display/EAI/PDI+Two-Way+Password+Encoding+plugins)) them:

```bash
sh encr.sh -kettle 'mypassword'
```


## Example Usage

Create a simple transformation call `test.ktr` and save it in the `transformations` folder within the `kettle-beam-examples` git repo folder. The transformation should look like this (just logging the variables that get set via the environments plugin):

![](/images/kettle-environment-plugin/kettle-environment-3.png)

In my case the name of the environment is `kettle-beam-examples-dev-env`. To execute our transformation for this git repo/project with this environment with **Maitre** run the following:

```bash
./maitre.sh \
  -e kettle-beam-examples-dev-env \
  -f '${ENVIRONMENT_HOME}/transformations/tr_test.ktr'
# since version 0.8 of the plugin
./maitre.sh \
  -e kettle-beam-examples-dev-env \
  -f 'transformations/tr_test.ktr'
```

> **Important**: Make sure to pass the variable between **single quotes** so that the environment variable is not instantly replaced with its actual value.

> **Note**: As of version 0.8.0 of the kettle-needful-things plugin `${ENVIRONMENT_HOME}` does not have to be specified any more.

Maitre expects the environment details to be available in `~/.kettle/environment`. If you move to a new environment, the environments file might not be in this location. You can export the config file to a **JSON file** and store it in a dedicated git repo so you can version control it. You can use Maitre to import this JSON file in the new environment (importing here basically means that Maitre will copy it from your custom location to `~/.kettle/environment`). Alternatively you could also set `KETTLE_HOME` to the custom location within your git repo (as long as you structure it correctly) and hence avoid the import step.

To export your environment config to **JSON**, bring up the environment switcher (**Environment > Switch Environment**) in **Spoon**, highligh the environment you'd like to export and click on the **Export** button:

![](/images/kettle-environment-plugin/kettle-environment-1.png)

Then save the **JSON file** to a convenient location.

To import this file (to `~/.kettle/environments`), run the following command:

```bash
./maitre.sh -I ~/Downloads/kettle-beam-examples-dev-env.json 
# or
./maitre.sh --import-environment ~/Downloads/kettle-beam-examples-dev-env.json
```

Take a look at the [Maitre Docu](https://github.com/mattcasters/kettle-needful-things/wiki/Maitre) for more command line options, or alternatively run:

```bash
./maitre.sh -h
```



## Environment Setup via Command Line

An environment can be created via the command line as well. Maitre offers the `--create-environment` option (You can also use the **-C** shortcut option) to define the environment and the `--add-variable-to-environment` option (or `-V` shorthand) to set variables in the environment.

```bash
sh maitre.sh \
  -C Examples=/home/kettle/examples \
  -V P1_HOSTNAME=192.168.1.15 \
  -V P1_USERNAME=peter \
  -V "P1_PASSWORD=Encrypted 2be98afc86aa7f2e4bf0bb66189c5f88d"
```



