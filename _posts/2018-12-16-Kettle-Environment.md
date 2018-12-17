---
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
- If I ran kitchen in another environment, how would I pick up the environment details from the command line? **A**: Check out the [kettle-needful-things](https://github.com/mattcasters/kettle-needful-things) project for a "better pan/kitchen" called Maitre. You might need other things in that project...
- SpoonGit Project: How do I pick this? **A**: This is still not fully developed.

Matt Casters provides an example usage here:

- [Github: Kettle Beam examples](https://github.com/mattcasters/kettle-beam-examples)