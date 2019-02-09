---
typora-root-url: /home/dsteiner/git/diethardsteiner.github.io
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
- Where do the config details get stored? **A**: Config is stored in separate metastore in `~/.kettle/environment`. 
- If I ran kitchen in another environment, how would I pick up the environment details from the **command line**? **A**: Check out the [kettle-needful-things](https://github.com/mattcasters/kettle-needful-things) project for a "better pan/kitchen" called Maitre. You might need other things in that project...
- **SpoonGit Project**: How do I pick this? **A**: This is still not fully developed.
- **Enforce execution in environment home**? What does this option mean? **A**: It will prevent you from running any transformation or job that is located outside the environment base folder.
<<<<<<< HEAD
- Can I define a **default environment**? **A**: Yes, just click the **Create Default** button at the bottom of the dialog instead of the `+` icon at the top of the dialog. Tip: You can leave the _Environment base folder_ unspecified and untick _Enforce executions in environment home?_. This way this default environment will behave exactly the same way as when this plugin is not installed.
- Can I create a **new environment** from the **command line**? **A**: Release 1.4.0 introduced an extension point to allow creation of new environments from the command line via `maitre.sh`.
- Does this plugin integration with **GitSpoon**? **A**: Yes, as of release 1.3.0.
- Can I **export** and **import** the **environment definitions**? Yes, they can be exported as JSON file and you can import them as well.

Matt Casters provides an example usage here:

- [Github: Kettle Beam examples](https://github.com/mattcasters/kettle-beam-examples)

# Installing the Kettle Beam Examples

Matt Casters also provides a Git repo with a few Kettle Beam Examples. Apart from covering Beam, it also showcases good practices by using **unit testing** and **environment specification**. It is a totally self contained git repo.

To properly use this git repo, you have to install following PDI plugins:

- [GitSpoon](https://github.com/HiromuHota/pdi-git-plugin)
- [Pentaho PDI Datasets Plugin](https://github.com/mattcasters/kettle-environment)
- [Kettle Beam Plugin](https://github.com/mattcasters/kettle-beam)

Next clone the [Kettle Beam Examples Git repo](https://github.com/mattcasters/kettle-beam-examples) to a convenient location (somewhere outside the PDI directory).

Once you start up **Spoon**, you should be presented with an **Environments** dialog. Click the `+` icon to create a new environment.

![](/images/kettle-beam/kettle-env-1.png)

All you have to do is provide a **name**, **description** and **environment base folder** (last one is directory in which the git repo is stored) and job done.