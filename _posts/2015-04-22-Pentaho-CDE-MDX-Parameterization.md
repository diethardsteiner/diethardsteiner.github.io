---
layout: post
title:  "Pentaho CDE: MDX Parameterization"
summary: In this article we will discuss how to parameterize a MDX query in your Pentaho dashboard.
date:   2015-04-22
categories: CDE
tags: CDE, MDX
published: true
---

In this article we will discuss how to parameterize a MDX query in your Pentaho dashboard. I will not discuss on how to create the dashboard, but mainly focus on how to implement the parameterization.

Start off by creating a simple dashboard: 

In the **Data Source** panel create two **MDX over MondrianJndi** queries, both using the **SampleData** JNDI connection and the **SteelWheels** Mondrian Schema. Name one query `qry_lines` and the other one `qry_total_sales`.

In the **Layout** panel create placeholders for a select input and the main content.

In the **Components** panel add a **Simple parameter** and name it `param_line`. Add a **Select component**, set its parameter to `param_line`, assign the `qry_lines` datasource and link it up with the right html object.

Then add a **Query Component**, make it listen to `param_line` and as parameters define `param_line` as well. Link it to the `qry_total_sales` datasource and the correct html object. Name the **Result Var** `myResult`. As **PostExecution** function define:

```javascript
function(){
    $('#content').text(Dashboards.numberFormat(myResult[0][0],'#,###'));
} 
```

Replace `#content` with your html object id. 

Go back to the **Data Source** panel and define `param_line` as a string parameter for the `qry_total_sales` MDX query.


## One Parameter Value

In the easiest case you just have to cater for one parameter value. Take this query as an example:

```sql
SELECT
    {[Measures].[Sales]} ON 0
FROM [SteelWheelsSales]
WHERE [Product].[Classic Cars]
```

We can pass a line value to the MDX query like this (`qry_total_sales`):

```
SELECT
    {[Measures].[Sales]} ON 0
FROM [SteelWheelsSales]
WHERE [Product].[Line].[${param_line}]
```

And the query that populates the input select component would look like this (`qry_line`):

```sql
SELECT
    {} ON 0
    , {[Product].[Line].Members} ON 1
FROM [SteelWheelsSales]
```

Output:

Line |
----|
Classic Cars |
Motorcycles |
Planes |
Ships |
Trains |
Trucks and Buses |
Vintage Cars | 

## Catering for the All value

What if we also want to give the option to select the **All** member? We can amend our line input query by simply adding `[Product].[All Products]`. 

```sql
SELECT
    {} ON 0
    , {[Product].[All Products], [Product].[Line].Members} ON 1
FROM [SteelWheelsSales]
```

Output:

Line |
----|
All Products |
Classic Cars |
Motorcycles |
Planes |
Ships |
Trains |
Trucks and Buses |
Vintage Cars | 

However, the original main query might not work if we supply the all member: This is because the unique name of the all member might be different. In our case it would still be possible to use the original approach, however, imagine we were working with a deeper level like Vendor, in which case we would have a problem. So let's make this a bit more flexible:

This time we have to supply the unique name of the members to the main query (`qry_total_sales`):

```sql
SELECT
    {[Measures].[Sales]} ON 0
FROM [SteelWheelsSales]
WHERE { ${param_line} }
```

And we adjust the line input query to return the unique name of the members:

```sql
WITH 
MEMBER [Measures].[Unique Name] AS
    [Product].CurrentMember.UniqueName
SELECT
    {[Measures].[Unique Name]} ON 0
    , {[Product].[All Products], [Product].[Line].Members} ON 1
FROM [SteelWheelsSales]
```

In the CDA datasource definition we have to change the column order: Set **Output Options** to `["1","0"]`. Check the output of the CDA query: It should look something like this:

Partial Output:

Unique Name | Product
----|----
[Product].[All Products] | All Products
[Product].[Classic Cars] | Classic Cars
[Product].[Motorcycles] | Motorcycles
[Product].[Planes] | Planes
[Product].[Ships] | Ships
[Product].[Trains] | Trains
[Product].[Trucks and Buses] | Trucks and Buses
[Product].[Vintage Cars] | Vintage Cars

The reason why we swapped the columns around is that we can now use the unique name as the key and the product value as display value within the **Select** component. To configure this, set **Value as id** to `False` (in the Select component).

## Multiselect

What happens if you want to support multiple values, e.g.:

```sql
SELECT
    {[Measures].[Sales]} ON 0
FROM [SteelWheelsSales]
WHERE {[Product].[Classic Cars], [Product].[Motorcycles]}
```

This doesn't look that easy any more. Luckily, most of our work is already done as we already started using the unique names for the members. All that is left to do is to replace the **Select** component with a **MultiSelect** component (set it up in a similar way) and change the `qry_lines` input query (as in this case we do not want to use the all member):

```sql
WITH 
MEMBER [Measures].[Unique Name] AS
    [Product].CurrentMember.UniqueName
SELECT
    {[Measures].[Unique Name]} ON 0
    , {[Product].[Line].Members} ON 1
FROM [SteelWheelsSales]
```

We do not have to change the `qry_total_sales`, but we have to change the parameter settings slightly for it. In this case we are not passing a `String` any more, but a `StringArray`.

You can easily check this by typing `param_line` into the **JavaScript** console of your browser (once you loaded the dashboard and selected a few values) and you should see something like this: `["[Product].[Ships]", "[Product].[Trucks and Buses]"]`.

Conclusion: Using the unique member name makes it much easier to set up multivalue parameters.
