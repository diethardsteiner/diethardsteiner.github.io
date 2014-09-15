---
layout: post
title:  "Tips for using Pentaho Data Integration on Mac OS X"
summary: This is a short article on some of the problems you might encounter when working with Pentaho Data Integration on Mac OS X
date:   2014-08-10
categories: Review
tags: Pentaho
published: true
---

This is a short article on some of the problems you might encounter when working with **Pentaho Data Integration** on **Mac OS X**:

## Cannot start Spoon via the app launcher

When starting Spoon via the app launcher you might get asked to install Java 6 (although you have already Java installed) or you might get an error message like this: "Data Integration is damaged and can't be opened. You should move it to trash."

Currently there doesn't seem to be a way around this problem. You can only start Spoon via the shell script.

## Fixing the unresponsive file dialog

[Jira case](http://jira.pentaho.com/browse/PDI-12824)

When opening or saving a file, you might see the spinning wheel for ages in the file dialog - nothing is happening. This is usually happening on Mac OS X Mavericks and jre 1.7.

Download the latest stable Mac SWT jar file from [Eclipse Standard Widget Toolkit](http://www.eclipse.org/swt/) website and add it to `<pdi-root>/libswt/osx64`.

Download the latest Oracle Java Version. As Nelson points out in the jira case mentioned above, version `1.7.0_51-b13` and later seem to have fixed this problem.

## Useful keyboard shortcuts

I just list shortcuts here which are not listed in the UI:

keyborad shortcut | description
------------------|------------
SHIFT + CDM + Space | Show variables list
SHIFT + CTRL + arrow | Align steps/job entries