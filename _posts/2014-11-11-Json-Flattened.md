---
layout: post
title:  "Pentaho Data Integration: Rows to Json Output"
summary: A short article on how to output a flattened Json structure.
date:   2014-11-11
categories: PDI
tags: Pentaho, PDI
published: true
---

Currently the PDI **Json Output** step only allows you to create a fairly flat Json structure, but this was fine for this specific use case:

For my Pentaho Sparkl Project **Bissol Table Data Editor** (available via the Pentaho Marketplace on the BA Server) I needed some way to convert rows to **Json** and send a stringified version of it to the frontend. 

We can use the **Json Output** step for this:

![](/images/json-flattened/ktr.png)

By default the output **Nr rows in a block** value is set to *1*:

![](/images/json-flattened/config-1.png)

... which basically works on a row by row basis. The output is something like this:

![](/images/json-flattened/json-output-rows.png)

Not quite what I had in mind ... I just wanted one output. To achieve this, you can just set **Nr rows in a block** to *0*:

![](/images/json-flattened/config-0.png)

... and all your rows will be *flattened* to one output element:

![](/images/json-flattened/json-output-flattened.png)