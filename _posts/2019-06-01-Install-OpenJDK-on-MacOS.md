---
layout: post
title:  "Installing JDK on MacOS"
summary: This article covers the basics of installing the Java Development Kit
date:   2019-06-01
categories: Java
tags: Java
published: true
---

This is a very brief article on how I installed **OpenJDK** on **MacOS**. The easiest way to install JDK on MacOS is via package manager **Homebrew**.

## Installation

Make sure you have [Homebrew](https://brew.sh) installed.

To install the latest **OpenJDK** run the following command in the **Terminal**:

```bash
brew cask install java
```

JDK will be installed in `/Library/Java/JavaVirtualMachines/`, e.g.:

```
/Library/Java/JavaVirtualMachines/openjdk-11.0.2.jdk/
```

As long as any JDK is moved to this location, it should automatically register with the OS. There is no need to run another command to register JDK with the OS. 

**How to list the current active JDK** on the system level:

```bash
$ /usr/libexec/java_home
/Library/Java/JavaVirtualMachines/openjdk-11.0.2.jdk/Contents/Home
```

**How to list all installed JDKs**:

```
$ /usr/libexec/java_home -V
Matching Java Virtual Machines (2):
    11.0.2, x86_64:	"OpenJDK 11.0.2"	/Library/Java/JavaVirtualMachines/openjdk-11.0.2.jdk/Contents/Home
    1.8.0_212, x86_64:	"AdoptOpenJDK 8"	/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home
```

### Installing a specific version

The older **Oracle JDKs** are not available via **Homebrew** any more. Use **OpenJDK** instead. To get access to the older OpenJDK versions, you have to add an external repo to **Homebrew**:

```bash
# add open jdk repo
brew tap adoptopenjdk/openjdk
# see available versions
brew search openjdk
# install specific version
brew cask install adoptopenjdk8
```

## Switching Java Versions

```bash
# see available java versions
/usr/libexec/java_home -V
# pick a specific version - no need to specify all the version details:
# 1.8.0_05 vs 1.8
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
```

Add the last line to `.bash_profile`. Then run:

```bash
source ~/.bash_profile
```

> **Important**: This change doesn't make this version available system wide, meaning, quite likely standard apps still won't pick up this particular version, only scripts you execute from the Terminal. To work around this, you can open an app from the **Terminal** like so:

```bash
open -a "Neo4j Desktop"
```

You could also go a step further and create aliases to switch between versions, e.g.:

```bash
alias j11="export JAVA_HOME=/usr/libexec/java_home -v 11.0.2; java -version"
alias j8="export JAVA_HOME=/usr/libexec/java_home -v 1.8; java -version"
```

Sources:

- [Open Applications Using Terminal On Mac](https://www.wikihow.com/Open-Applications-Using-Terminal-on-Mac)
- [How to set or change the default Java JDK version](https://stackoverflow.com/questions/21964709/how-to-set-or-change-the-default-java-jdk-version-on-os-x)
- [How to install Java 8 on MacOS Mojave with Homebrew](https://stackoverflow.com/questions/53772023/how-to-install-java-8-on-osx-macos-mojave-with-homebrew)
