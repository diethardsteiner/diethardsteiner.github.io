---
typora-copy-images-to: ../images
typora-root-url: ../images
layout: post
title: "Visual Side-by-Side Diffs with Git"
summary: Steps on how to set up git with external diff tools
date: 2021-01-07
categories: [Git]
tags: 
published: true
---



Sometimes you are working on remote servers with limited possibilites or you are environment is extremely locked down or you are just a big fan of vim - whatever the scenario, how do you do a **visual side-by-side diff** between two versions of a given file? (This guide will also work for setting up other external diff tools than vimdiff).

We have to use the existing toolset available on our **Linux/Unix** machines: Most Unix/Linux variants will ship with **vim** and **vimdiff**. Latter command is mainly the same as using `vim -d`. **vimdiff** is our first best friend and **git difftool** our second one. These two tools linked together will help use achieve our mission!

> **Note**: [git difftool](https://git-scm.com/docs/git-difftool) is a frontend to git diff and accepts the same options
>   and arguments. 

First we have to tell **Git** that we would like to use **vimdiff** as our preferred diff tool:

```shell
git config --global diff.tool vimdiff
```

If you do not want to set this globally but rather repo specific, omit the `--global` flag.

To see any changes that have been made since the last commit (run this inside the root directory of your git repo):

```shell
git difftool
```


> **Important**: For `git difftool` to show changes the files must have been previously committed and there must be local changes to them!

![image-20210107143947717](/images/image-20210107143947717.png)

To exit out of a side-by-side comparison of a particular file use the vim command `:qa`.

Since **git difftool** accepts the same arguments as **git diff**, we can also look for very specific changes, e.g.: *What is the difference for given file A between branch B and branch C*?


Get list of files that are different between branches (here we compare `master` to `dev` branch):

```shell
git diff --name-status master..dev
```

Then inspect changes for a particular file:

```shell
git difftool -y master..dev /path/to/file
```

Note that the `-y` flag automatically confirms the prompt to open the diff in the editor.