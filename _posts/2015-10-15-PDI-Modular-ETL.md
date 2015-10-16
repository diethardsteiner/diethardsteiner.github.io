---
layout: post
title: Modular ETL with Pentaho Data Integration
summary: 
categories: PDI
tags: PDI
published: false
---

**Modularity** is a concept applied to many things and you will be please to hear that **Pentaho Data Integration** embraces it fully. We want to keep our ETL processes lean and clean ... and easy to maintain. If there is a specific function required by various ETL processes, we do not want to replicated it each time, but define one global function and reference it from each of the ETL processes. This way, if something ever has to change, we have to amend it in one place only.

So how is modularity implemented in **Pentaho Data Integration** you might wonder? It's enabled by using the **Mapping** step in your parent transformation and pointing it to a child/sub-transformation, which must have a **Mapping input specitication** step defined as the start of the stream and a **Mapping output specification** as the end of the stream. The **Mapping** step in the parent transformation you define how the fields of the stream map to the fields in the stream of the sub-transformation. That's it in a nutshell!

## How to pass any fields along

Sometimes your sub-transformation (your **module** so to speak) will just focus on a very specific task, e.g. looking up the state name for a specific zip code. This sub-transformation will very likely be used in transformations whose stream might look totally different apart from the zip code field, which they will have in common. 

As you've learned, we map the fields of the parent transformation to the ones in the sub-transformation, but how do we pass along any other fields?

The **Mapping input specitication** has an option called *Include unspecified fields, ordered by name*. Once you tick this opions, not only the fields defined in the mapping, but also any other fields will be passed along!

> **Important**: If you enable this option, you cannot use the **Select Values** step with the *Select and Alter* option in this sub-transformation, as this will effectively reduce the fieldset again (as intended). Instead make sure you use the *Remove* option of this step. 

