---
layout: post
title:  "Setting up a Haddop Environment for Pentaho Data Integration"
summary: This article explains how to set up a vanilla Hadoop Distribution and configure Pentaho Data Integration to access it
date:   2015-04-22
categories: PDI
tags: PDI, Hadoop
published: false
---

Recently I realized that I hadn't written a blogpost about **Pentaho Data Integration** (Kettle) for a long time, so it's time to focus on this again: [Dan Keeley](https://dankeeley.wordpress.com) published an interesting blogpost on installing the Cloudera Hadoop distribution some time ago to illustrate a way to test PDI with Hadoop on an environment with limited resources. 

In this article I'd like to explain how to set up **Vanilla Hadoop**. For our development evironment we might only need HDFS and Hive. Although setting up Hadoop might sound like an extermely complex task, the reality is that it is usually fairly straight forward. We will first install **Apache Hadoop**, then **Apache Hive** and finally configure **Pentaho Data Integration** to access these services. I bet you are really motivated by now, so let's see how this is done:
 