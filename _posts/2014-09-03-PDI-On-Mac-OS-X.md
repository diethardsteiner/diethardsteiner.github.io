---
layout: post
title:  "Tips for using Pentaho Data Integration on Mac OS X"
summary: This is a short article on some of the problems you might encounter when working with Pentaho Data Integration on Mac OS X
date:   2014-08-10
categories: PDI
tags: macOS
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
SHIFT + CMD + Space | Show variables list
SHIFT + CMD + arrow OR ALT + CMD + arrow | Align steps/job entries
ALT + CMD + up or down arrow | Move row up or down in a config table (e.g. Select Values step)
fn + DEL | Delete row in a config table
Ctrl+ Cmd + F5 | Open Metastore Browser

## Text and icons too small on the Retina display

That's an easy one: Just go to **Tools > Options ... > Look & Feel** and adjust the icon and font size.

## Data Grid and other configuration options

With the **Data Grid** step you will have to hit **Enter** once a value was entered, otherwise it will just disappear. Marcello Pontes also mentioned that you can use the **Tab** key instead as well.

Nelson Sousa mentioned: "When you have a field active to edit, press enter before clicking anywhere else. If not you get one of those array out of bounds things. Ignore it, continue with spoon, save and restart. :)"

## The menu bar options have disappeared

To solve this issue, just switch to another app and then back again ... voilà all menu bar options are back. Pure magic (no, not really).

