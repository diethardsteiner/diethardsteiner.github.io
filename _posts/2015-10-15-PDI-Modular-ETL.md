---
layout: post
title: Modular ETL with Pentaho Data Integration
summary: 
categories: PDI
tags: PDI
published: true
---

**Modularity** is a concept applied in many programming languages (and many other parts of life) and you will be pleased to hear that **Pentaho Data Integration** embraces it fully as well. We want to keep our **ETL** processes lean and clean ... and easy to maintain. If there is a specific function required by various ETL processes, we do not want to replicated it each time, but define one global function and reference it from each of the ETL processes. This way, if something ever has to change, we have to amend it in one place only.

So how is modularity implemented in **Pentaho Data Integration** you might wonder? It's enabled by using the **Mapping** step in your parent transformation and pointing it to a child/sub-transformation, which must have a **Mapping input specification** step defined as the start of the stream and a **Mapping output specification** step as the end of the stream. The **Mapping** step in the parent transformation allows you define how the fields of the stream map to the fields in the stream of the sub-transformation. That's it in a nutshell!

> **Note**: There are two **Mapping** steps available in **PDI**: The **Simple Mapping** step, which allows only one input stream and output stream and the complex **Mapping** step, which allows multiple input and output streams. 

Here is an example of using the **Simple Mapping** step: Our **module** (the sub-transformation) looks up the project id from a web service (the step is label `Source Project ID` in the screenshot below):

![](/images/pdi-modular-etl-1.png)

The sub-transformations looks like this (note the usage of **Mapping input specification** and **Mapping output specification** steps):

![](/images/pdi-modular-etl-2.png)


In the **Mapping** step we define the sub-transformation we want to use and how the fields map:

![](/images/pdi-modular-etl-3.png)

And we also define the output fields mapping:

![](/images/pdi-modular-etl-4.png)

## How to pass any fields along

Sometimes your sub-transformation (your **module** so to speak) will just focus on a very specific task, e.g. looking up the state name for a specific zip code. This sub-transformation will very likely be used in transformations whose stream might look totally different apart from the zip code field, which they will have in common. 

As you've learned, we map the fields of the parent transformation to the ones in the sub-transformation, but how do we pass along any other fields?

The **Mapping input specification** step has an option called *Include unspecified fields, ordered by name*. Once you tick this opions, not only the fields defined in the mapping, but also any other fields will be passed along!

![](/images/pdi-modular-etl-5.png)

> **Important**: If you enable this option, you cannot use the **Select Values** step with the *Select and Alter* option in this sub-transformation, as this will effectively reduce the fieldset (as intended). Instead make sure you use the *Remove* option of this step: As we do not know at all times which fields are passed down to the sub-transformation (apart from the ones we manipulated), we tell **PDI** only which fields we do not want to keep in the stream.

