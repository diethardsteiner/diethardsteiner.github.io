---
layout: post
title:  "Pentaho Data Integration: How to fix the GLib-CRITICAL problem"
summary: This article explains how to fix the GLib-CRITICAL problem in a very easy fashion
date: 2015-06-07
categories: PDI
tags: PDI
published: true
--- 

Since upgrading to Fedora 22 Spoon (the client tool of PDI) was not working properly any more: Although I could start Spoon properly and create transformations etc, once I wanted to execute a transformation or sometimes even when trying to open settings, nothing was working and the terminal window showed several `(SWT:20352): GLib-CRITICAL` error messages. In a nutshell, Spoon was rendered useless.

On this wonderful Sunday morning - clear blue sky in Britain - I finally figured out how to fix this dilemma: 

1. Go to the [Eclipse Download Page](https://www.eclipse.org/downloads/) and download the latest 64bit verion of **Eclipse IDE for Java EE Developers**.
2. Unzip the file and search for **swt**. A search result will show a few files, but the ones interesting for us are (your version number might be different): `org.eclipse.swt_3.103.2.v20150203-1313.jar` and `org.eclipse.swt.gtk.linux.x86_64_3.103.2.v20150203-1351.jar`.
3. Copy the first one of these files into `<PDI_HOME>/libswt/linux/x86` and the second into `<PDI_HOME>/libswt/linux/x86_64`.
4. Both folders still have the original jar files in them. Rename them to `swt-jar-old` (Note: no extension, so that they are not picked up).
5. Start spoon. There will be a few error messages shown, but so far Spoon is working way better for me then before.

Good luck! Now it's time for me to enjoy some of this splendid sunshine - before clouds come in again - which happens all too often and all too quickly.
