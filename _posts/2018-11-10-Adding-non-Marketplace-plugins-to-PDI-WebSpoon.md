---
layout: post
title:  "Adding non-Marketplace plugins to PDI WebSpoon"
summary: This article explains how to add a custom plugin to WebSpoon
date: 2018-11-10
categories: PDI
tags: PDI
published: true
---

# Adding non-Marketplace plugins to PDI WebSpoon

Every now and then you might want to test the bleeding edge version of a certain plugin that is not yet available via the **Pentaho Marketplace** or you might have just come across a plugin which is not available at all on the **Pentaho Marketplace**: In this case, how do you add it to **WebSpoon**?

The answer is rather simple: Make sure that the plugin is available in the `~/.kettle/plugins` folder on the container. The way to achieve this is to mount a local volume to the container on this directory, e.g. (not command structure is specific to Fedora, you might have to change certain bits of it to make it run on your OS):

```bash
sudo docker run -it --rm \
-e JAVA_OPTS="-Xms1024m -Xmx2048m" \
-p 8080:8080 \
-v ~/.kettle/plugins:/root/.kettle/plugins:z \
hiromuhota/webspoon:latest-full
```

Your local folder must have the unzipped version of the plugin. And this is it.


Alternatively, you can also make use of the predefined PDI parameter `KETTLE_PLUGIN_BASE_FOLDERS`:
 
```bash
sudo docker run -d \
-p 8080:8080 \
-e KETTLE_PLUGIN_BASE_FOLDERS=plugins,/root/.kettle/plugins \
-v ~/.kettle/plugins:/root/.kettle/plugins \
hiromuhota/webspoon:latest-full:z
```

Special thanks for Hiromu for providing WebSpoon itself as well as help on using it.