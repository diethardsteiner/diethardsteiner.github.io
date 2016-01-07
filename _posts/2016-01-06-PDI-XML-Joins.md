---
layout: post
title: Generate XML Documents with Pentaho Data Integration
summary: 
categories: PDI
tags: PDI
published: true
---

**XML** isn't quite enjoying the popularity today as it did 10 years ago, but it is still quite a common data format to work with. **Pentaho Data Integration** offers various features to read, create, manipulate and output **XML** documents and fragments.

In this short blog post I'd like to explain how to create nested **XML** documents in **Pentaho Data Integration (PDI)**.

## About the Add XML Step

The PDI **Add XML** step allows you to create simple XML fragments. You can define a root element and nest other XML elements within it (**only child elements** and not any deeper nested structures) and optionally define attributes for them.

So you can create a structure like this:

```xml
<order>
  <item id="1"/>
  <item id="2"/>
  <instructions>Special Offer</instructions>
</order>
```

However, you cannot create deeply nested structures like this:

```xml
<order>
  <item id="1">
    <color>red</color>
  </item>
  <item id="2">
    <color>blue</color>
  </item>
  <instructions>Special Offer</instructions>
</order>
```

To achieve this output, you have to first create these two XML fragments:

```xml
<order>
  <item id="1"/>
  <item id="2"/>
  <instructions>Special Offer</instructions>
</order>
```

```xml
<color>red</color>
<color>blue</color>
```

and then **join** them using the PDI **XML Join** step.


## Example

> **Note**: There are various ways of achieving this within **PDI**. For the purpose of this blog post we will make use of the **XML** steps supplied with PDI.

Our aim is to create an **XML** document which looks like this:

```xml
<catalog>
  <category name="tele">
    <lens brand="Zuiko" focallength="135"/>
    <lens brand="Zuiko" focallength="50"/>
  </category>
  <category name="wide-angle">
    <lens brand="Zeiss" focallength="25"/>
    <lens brand="Zeiss" focallength="21"/>
  </category>
</catalog>
```

So imagine we are given this dataset to create this **XML document**:

lens_category | lens_brand | focal_length | quantity
-----|-----|-----|-----
tele | Zuiko | 135 | 2
tele | Zuiko | 50 | 4
wide-angle | Zeiss | 25 | 1
wide-angle | Zeiss | 21 | 2

So we will start with one approach on how **not** to create this document (just as a learning experience):

The first part of our process will create the XML fragments using the **Add XML** step for **categories**:

```xml
<category name="tele">
<category name="wide-angle">
```

and **lenses**:

```xml
<lens brand="Zuiko" focallength="135"/>
<lens brand="Zuiko" focallength="50"/>
<lens brand="Zeiss" focallength="25"/>
<lens brand="Zeiss" focallength="21"/>
```

Then we will nest the **lens** elements within the **category** elements using an **XML Join** step. In PDI terms the **category** elements are **target** of the join. We can call this the **bottom-up** approach.

The **PDI** transformation for this approach is shown below:

![](/images/pdi_xml_1.png)

You can **download** the **PDI transformation** from [here](/sample-files/pdi/xml-join/tr_xml_join_wrong.ktr).

In the **XML Join** we configured a complex join, so that a given hierarchy is joined to the correct dimension.

![](/images/pdi_xml_2.png)

We define this as the join **XPath** statement.

```
/category[@name="?"]
```

The question mark will be replaced by the field value of `lens_category`. In other words, we are telling PDI to join the lens XML elements to parents called `category` with an attribute called `name`. The `name` attribute should have to value specified in the `lens_category` field.

When we do a **Preview** on the **XML Join** step we get an error message like this one:

```
XPath statement returned no result [/category[@name="wide-angle"]]
```

So why do we really get this error? The reason is fairly simple. The main target of the join (the **category** XML element) is not a valid XML document. We have two rows with each one **category** XML fragment, which we use as the target of the join.

The target of an **XML Join** has to be in **one row** only, meaning there has to be one XML document/fragment only (in other words, the XML document/fragment exists in one field within one row only).

So the way to solve our problem is the following:

1. What we need is **one row** with a parent element to nest the two **category** elements in it!
2. We use an **XML Join** step to nest the **category** elements with the **catalog** element.
3. We use an **XML Join** step to nest the **lens** elements with the correct **category** elements.

In a nutshell: The **bottom-up** approach is not working, what we have to implement is a **top-down** approach!

The **PDI** transformation for this approach is shown below:

![](/images/pdi_xml_3.png)

Now we get the correct output:

```xml
<catalog>
  <category name="tele">
    <lens brand="Zuiko" focallength="135"/>
    <lens brand="Zuiko" focallength="50"/>
  </category>
  <category name="wide-angle">
    <lens brand="Zeiss" focallength="25"/>
    <lens brand="Zeiss" focallength="21"/>
  </category>
</catalog>
```

You can **download** the **PDI transformation** from [here](/sample-files/pdi/xml-join/tr_xml_join_correct.ktr).


