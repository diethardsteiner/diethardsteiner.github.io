---
typora-root-url: ..
typora-copy-images-to: ../images/project-hop
layout: post
title: "Project Hop: Testing as Part of your Development"
summary: A few ideas on how to improve your development workflow
date: 2020-05-12
categories: [Hop]
tags: 
published: true
---


Someone creating data processing flows via visual tools is not necessarily used to creating unit tests or more wider scoped tests as part of their endeavour. It's easy for people to quickly drag and drop a few transforms onto the canvas, link them up and apply a few settings and think they are done. At some point this hurry will bite you.

## We always should develop with data

Blindly developing a data processing pipeline is a show stopper. Don't just take requirement and straight away translate them to a series of transform instructions without having even any data available to test the logic. 

Your options the get hold of some data are e.g.:

- create synthetic data: There are a few tools out there that help you to generate sample data automatically. In the worst case, just create a small sample by hand.
- access to a testing dataset (test DB, test files etc)
- take a subset of the live data (caution here with sensitive data - you might have to mask/tokenise it or even remove it - make sure you have the permission to export live data)
- source data from some publicly available data source (in case it happens to be in the same structure)


Even if you sit in a top security office with no connection to the actual data store or in case you are developing a **Beam pipeline**, you can still use Hop's datasets to simulate the input data: In example, if you are using a *Table Input* transform, by assigning a test dataset to it Hop will not actually execute the *Table Input* transform itself but just read the sample data you provided. The same logic applies to any other input transform. Just create some synthetic data and configure Hop to read it as input dataset.

IMAGE OF DATASET USAGE


### Everything starts with a test

You must have heard about the best practice of creating a unit test before starting any programming. The same principal applies to creating a data pipeline. At the bare minimum make sure that you have the input dataset(s) ready before you start working on a data pipeline.

If you want to fully test your pipelines and workflows, make sure that your environment has all the dependencies in place: In example, are all the database tables created and loaded with the sample data? I tend to write a dedicated Hop workflow to initialise the environment, execute the main workflow and then finally validate the results. This can all be done with Hop. 

And don't be tempted to think: Oh that's too much work! If you do your due diligence and check the results of your pipeline manually with your query, you quite likely run a few other scripts manually to get you there. So in theory this is just about automating the process. This will very soon safe you time, the results are always reproducible , and it's also all documented in a very basic way: Ever arrived at a project mid-way or had to take a project over and had no trace of any tests every done? If you have a Hop workflow ready to start the test(s) automatically, wouldn't that be nice?

IMAGE OF SAMPLE TEST WORKFLOW


One very important thing to keep in mind here is that you must always make sure that your dev environment is clean: 

In example:

- All database tables are dropped
- All result and input files are removed
- All variables are unset


What you want to make sure is that if you were to deploy your code into a new environment, that it will work there straight away. By making sure that your test runs on a clean environment, you are setting the scene for this.

There are of course cases like CDC where you might have several runs without clearing down the environment, but these exception must be well understood.

As mentioned, if you can't simulate the environment locally, you can also use **Hop's datasets** to work around this. It's a very useful and handy feature - just keep in mind that you will still have to setup the test as described in the previous paragraph since this approach does not test the input instructions. You can run the full test in another environment then.



### Variables as first class citizens

Add variables straight from the beginning of your development efforts. It will be a lot more work to add them retrospectively. The important thing is that you also should straight away add them to Hop's environment definitions (see my article ...): This way, while you develop, the variables will be replaced by their actual values and you can properly preview the data.

## One transform instruction at a time

Now that we have data to play with, we want to add one transform instruction at a time. There is no point lining 10 transform instructions up, configuring them and then expecting it will just work. You are up to a tedious error hunt.

Be patient and just add one transform instruction at a time and preview the result to make sure it is working as expected. Most errors will emerge at this stage. It is a lot easier to fix an error this way.

IMAGE SHOWING PREVIEW OPTION

## Add a golden output dataset

One of the final actions your dedicated **Hop testing workflow** should do is to compare the generated output data with a gold dataset and determine of the result is correct. You can use Hop as well to generate a JUnit compatible unit test report and hence integrate your whole Hop testing workflow into some automated testing setup (see my article ...).


TO DO:

Prepare example:
- E2E testing
- Unit testing

One repo that covers both