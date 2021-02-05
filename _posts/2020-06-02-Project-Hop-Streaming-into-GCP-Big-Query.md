---
typora-root-url: ..
layout: post
title: "Apache Hop: Streaming into GCP Big Query"
summary: A quick walkthrough on how to ...
date: 2020-06-02
categories: [Hop]
tags: 
published: false
---

### Beam Timestamp


Here we have two options available:

- **Pick a field from the Hop Stream**
- **Extract the timestamp from the messaging system**: Tick **get timestamp from stream** then it will access whatever field the the source system (e.g. Kafka or GCP Pub/Sub) added as a timestamp? **Stream** in this context doesn’t mean the **Hop stream**. Set the **Time field to use** to any freeform name (Note: Although it looks like a drop down menu you can just type a name into this field as well).

I believe technically the timestamp comes from the bundle of records in the PCollection associated with the Timestamp transform.
The PCollection has to be "unbouded" or "streaming".  Calling it a stream of records makes more sense IMO.