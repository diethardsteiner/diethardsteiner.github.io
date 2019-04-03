---
layout: post
title: "Pentaho Standardised Git Repo Setup First Release"
summary: A few comments on the first release
date: 2017-11-14
categories: PDI
tags: [Git, Pattern]
published: true
---

In big projects with multiple teams and even small projects, **standards are key**. Make everything as simple as possible and keep it consistent. The **Pentaho Standardised Git Repo Setup** utility tries to combine these two concepts and creates a basic git structure. A version control system like **Git** is essential to every setup.

This implementation does not require an additional packages (only relies on bash which is usually installed by default). It is one method of putting all the theory outline [here](https://github.com/diethardsteiner/pentaho-standardised-git-repo-setup/blob/master/presentations/pcm2017.md) into place. 

The main goal is to give teams a **starter package** in the form of a predefined Git folder structures with checks in place that a minimum set up rules is followed.

The **code** is available [here](https://github.com/diethardsteiner/pentaho-standardised-git-repo-setup). The main functionality is implemented, but there are further aspects like **CI** that are at very early stages. Currently this project is in the **early alpha stage**. Any feedback is appreciated.