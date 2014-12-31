---
layout: post
title:  "OLAP Cube Member Properties"
summary: Have you ever wondered what Member Properties in a multidimensional cube are really good for?
date:   2014-12-29
categories: mondrian
tags: OLAP, Mondrian, Pentaho
published: true
---

## What are Member Properties?

Have you ever wondered what **Member Properties** in a multidimensional cube are really good for?

> **Member properties** are attributes of dimensional members that provide additional information, that you don't want to slice and dice on. In other words: member properties are attributes that don't make an ideal candidate for a dimension but still hold valueable information.  In example take a standard address: You might want to have country, state, city, maybe even zip code as a dimension, but there isn't much point in having the exact address (e.g. 1 Meadow Drive) as a dimensional value, because it is just purely too fine-grained. The address makes an ideal canditate for a property. These properties are still stored in the dimensional table alognside the standard dimensional values. Member properties have to have a one-to-one relationship with the member.

Another example: Imagine we have a product dimension table with product name, category and sub-category. Each product would also have retail price, which is unique to each product. It is clearly not a dimension level member, as we wouldn't want to slice and dice on the retail price. But in some reports/analysis it would be good to show the retail prices as additional information to the product, which makes retail price an ideal candidate for a member property.

Another use case could be the amount of days in month in a date dimension. You would define this as a property of month.

For our practical example we will take on the role of **health food store**. Our product dimension has the standard product name, category and sub-category as well as retail and wholesale price. Moreover for each individual product we have nutritional facts like calories and carbs.

We can reuse some of the tables we created in the [Advanced Data Modeling Techniques](http://diethardsteiner.github.io/mondrian/2014/12/26/Bridge-And-Closure-Table.html) tutorial and will just add the product dimension and a very simple fact table (find all the code [here to download](https://www.dropbox.com/sh/hkczwehh2c2a40g/AACPfi5wcVWK69m4lZaNG1KLa?dl=0)):

```sql
CREATE TABLE tutorial.dim_product
(
    product_tk INT4
    , product_name VARCHAR(125)
    , category VARCHAR(70)
    , sub_category VARCHAR(70)
    , retail_price NUMERIC
    , wholesale_price NUMERIC
    , calories NUMERIC
    -- Nutrion Facts
    -- per serving: serving size 1 tablespoon(25g)
    , fat_grams  NUMERIC
    , carbohydrates_grams NUMERIC
    , dietary_fibre_grams NUMERIC
    , protein_grams NUMERIC
)
;

INSERT INTO tutorial.dim_product
(
    product_tk
    , product_name
    , category
    , sub_category
    , retail_price
    , wholesale_price
    , calories
    , fat_grams 
    , carbohydrates_grams
    , dietary_fibre_grams
    , protein_grams
)
VALUES
    (1, 'Green Fruitmix 150g', 'Snacks', 'Dried Fruits', 2.10, 1.20, 71, 0, 16.8, 0.5, 0.5)
    , (2, 'Green Fruitmix 300g', 'Snacks', 'Dried Fruits', 3.50, 1.70, 71, 0, 16.8, 0.5, 0.5)
;

CREATE TABLE tutorial.fact_grocery_sales
(
    date_tk INT4
    , product_tk INT4
    , quantity INT4
)
;

INSERT INTO tutorial.fact_grocery_sales
VALUES
    (20141227, 1, 345)
    , (20141227, 2, 172)
    , (20141228, 1, 375)
    , (20141228, 2, 145)
;
```

With the sample data ready we can take a look at how to create a **Mondrian OLAP Cube definition** for this use case:

## Defining Member Properties in Mondrian

As you can see in the code below, the definition of properties is rather simple:

```xml
<Cube name="Groceries" visible="true" cache="true" enabled="true">
    <Table name="fact_grocery_sales" schema="tutorial"/>
    <DimensionUsage source="Date" name="Date" caption="Date" visible="true" foreignKey="date_tk"/>
    <Dimension name="Product" foreignKey="product_tk">
        <Hierarchy name="Products" hasAll="true" allMemberName="Total" primaryKey="product_tk">
            <Table name="dim_product" schema="tutorial"/>
            <Level name="Category" column="category" type="String" uniqueMembers="false" levelType="Regular"/>
            <Level name="Sub-Category" column="sub_category" type="String" uniqueMembers="false" levelType="Regular"/>
            <Level name="Product" column="product_name" type="String" uniqueMembers="false" levelType="Regular">
                <Property name="Retail Price" column="retail_price" type="Numeric"/>
                <Property name="Wholesale Price" column="wholesale_price" type="Numeric"/>
                <Property name="Calories" column="calories" type="Numeric"/>
                <Property name="Fat" column="fat_grams" type="Numeric"/>
                <Property name="Carbohydrates" column="carbohydrates_grams" type="Numeric"/>
                <Property name="Dietary Fibre" column="dietary_fibre_grams" type="Numeric"/>
                <Property name="Protein" column="protein_grams" type="Numeric"/>
            </Level>
        </Hierarchy>
    </Dimension>
    <Measure name="Quantity" column="quantity" datatype="Numeric" formatString="#,##0" aggregator="sum"/>
</Cube>
```

## Querying Member Properties

The retrieve the property values you first have to create a **calculated member** as shown in the example below:

```sql
WITH MEMBER [Measures].[Retail Price] AS
    [Product].CurrentMember.Properties("Retail Price")
SELECT
NON EMPTY {[Measures].[Quantity], [Measures].[Retail Price]} ON COLUMNS,
{[Product].[Product].Members}  ON ROWS
FROM [Groceries]
```

**Microsoft** also embrace the **DIMENSION PROPERTIES** syntax shown below to retrieve property values. While this syntax doesn't create an error when executing the MDX query on **Mondrian**, neither Saiku Analytics, OLAP4J Analytics nor Pentaho Schema Workbench show any results for the properties (but this might be because these tools don't have a feature implemented to display the properties in this way): 


```sql
SELECT
NON EMPTY {[Measures].[Quantity]} ON COLUMNS,
   NON EMPTY Product.Product.MEMBERS
   DIMENSION PROPERTIES 
              Product.Product.[Retail Price],
              Product.Product.[Wholesale Price]  ON ROWS
FROM [Groceries]
```

Finally one filter scenario:

```sql
SELECT
NON EMPTY {[Measures].[Quantity]} ON COLUMNS,
TopCount(
    Filter(
        [Product].[Product].Members
        ,  [Product].CurrentMember.Properties("Carbohydrates") < 20
    )
    , 10
    , [Measures].[Quantity]
) ON ROWS
FROM [Groceries]
```

For me the following open questions remain in using properties with Mondrian:

1. In the OLAP schema it is possible to define a `formatter` for properties, however only java classes are supported. Is there no way to specify a simple formatting string like `#,###`?
2. Is the **DIMENSION PROPERTIES** syntax supported?
3. Properties defined with data type of Numeric are not displayed as numeric (so in example 2.23 is displayed as 2). I assume I could just use data type Sting instead as a workaround, but if the data type had to be Numeric, how would I resolve this problem?

## Assign Pentaho Analyzer Chart Colors

This is taking everything on a totally different level, but I thought it is still worth mentioning for those people that are using **Pentaho BA Server Enterprise Edition**:

In charts it is often nice to assign a specific color to a specific series value so that it is consistently displayed across several reports. This can be done by specifying a member property. Find more details in the [Pentaho Infocentre](http://infocenter.pentaho.com/help/topic/analysis_guide/task_color.html?resultof=%22%43%48%41%52%54%5f%53%45%52%49%45%53%5f%43%4f%4c%4f%52%22%20).

## Sources

- [Mondrian Documentation](http://mondrian.pentaho.com/documentation/)
- [MDX with SSAS 2012 Cookbook](https://www.packtpub.com/big-data-and-business-intelligence/mdx-ssas-2012-cookbook)
- [SAS OLAP Cubes: Taking Advantage of OLAP Member Properties](http://bi-notes.com/2012/05/sas-olap-cubes-taking-advantage-of-olap-member-properties/)
- [User-Defined Member Properties (MDX)](http://msdn.microsoft.com/en-us/library/ms145613.aspx)
- [Dimension Day](http://wiki.bizcubed.com.au/xwiki/bin/view/Pentaho+Tutorial/Date+Dimension)
- [Use Member Properties to Store Associated Information About Dimension Members](http://technet.microsoft.com/en-us/library/aa224939(v=sql.80).aspx)
