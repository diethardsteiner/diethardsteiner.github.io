---
layout: post
title:  "Pentaho Mondrian: The MDX Generate Function"
summary: In this article we will discuss how to use the MDX Generate function
date:   2015-04-23
categories: CDE
tags: CDE, MDX
published: true
---

This is a brief overview of how you can use the **MDX** `GENERATE()` function. All the examples below work with the **SteelWheelsSales** Cube, which ships with the **Pentaho BA Server**. Try the examples listed below either with **Pentaho Analyzer**, the **Saiku CE** plugin, **Pivot4J** or Roland Bouman's **Pentaho Analysis Shell** (Pash) (The last 3 ones are plugins, which you can download via the **Pentaho Marketplace**).

The `GENERATE()` function has two different applications: 

- If the second parameter is a set, it will return the members that exist in both sets ( a `UNION()` of the sets). You can think of it as a kind of for-each function which checks if members in the first set exist in the second one. `ALL` can be used as a third parameter to keep any duplicates. 
- If the second parameter is a string expression then the members will be concatenated.

Examples:

Find the members that are in both sets:

```sql
SELECT
    [Measures].[Sales] ON 0
    , NON EMPTY GENERATE(
        {[Markets].[Country].[Australia], [Markets].[Country].[USA]}
        , [Markets].[Country].[Australia]
    ) ON 1
FROM [SteelWheelsSales]
```

Country | Sales
----|-----
Australia | 630,623

In this case Australia survives, because it is a member of the first as well as in the second set.

For each member of the first set retrieve the first child:

```sql
SELECT
    [Measures].[Sales] ON 0
    , NON EMPTY GENERATE(
        {[Markets].[Country].[Australia], [Markets].[Country].[USA]}
        , [Markets].currentMember.firstChild
    ) ON 1
FROM [SteelWheelsSales]
```

State Province | Sales
----|----
NSW | 305,567
CA | 1,505,542


Next we'd like to know the top performing country within each territory:

```sql
SELECT
    [Measures].[Sales] ON 0
    , NON EMPTY GENERATE(
        [Markets].[Territory].Members
        , TOPCOUNT([Markets].CurrentMember.Children, 1, [Measures].[Sales])
    ) ON 1
FROM [SteelWheelsSales]
```

Country | Sales
-----|------
Australia | 630623
Spain | 1215687
Japan | 188168
USA | 3627983

Taking the previous example a bit further, we can get the top performing country and customer combination based on sales for each territory:

```sql
SELECT
    [Measures].[Sales] ON 0
    , NON EMPTY GENERATE(
        [Markets].[Territory].Members
        , TOPCOUNT([Markets].CurrentMember.Children * [Customers].[Customer].Members, 1, [Measures].[Sales])
    ) ON 1
FROM [SteelWheelsSales]
```

Country | Customer | Sales
----|-----|-----
Australia | Australian Collectors, Co. | 200,995
Spain | Euro+ Shopping Channel | 912,294
Singapore | Dragon Souveniers, Ltd. | 172,990
USA | Mini Gifts Distributors Ltd. | 654,858

There is also an option to retain duplicates in the results using the `ALL` flag as the third argument:

```sql
SELECT
    [Measures].[Sales] ON 0
    , NON EMPTY GENERATE(
        {[Markets].[Country].[Australia], [Markets].[Country].[Australia]}
        , [Markets].currentMember.firstChild
        , ALL
    ) ON 1
FROM [SteelWheelsSales]
```

State Province | Sales
----|----
NSW | 305,567
NSW | 305,567

Now let's have a look at a completely different way we can use the `GENERATE()` function:

Let's show the territories with all their countries next to each other in one line. We can define a delimiter as the third parameter of the `GENERATE()` function:

```sql
WITH 
MEMBER [Measures].[Countries] AS
    GENERATE(
            [Markets].CurrentMember.Children
            , [Markets].CurrentMember.Name
            , "-"
        )
SELECT
    {[Measures].[Countries]} ON 0
    , NON EMPTY [Markets].[Territory].Members ON 1
FROM [SteelWheelsSales]
```

Territory | Countries
----|-----
#null | Germany-Ireland-Israel-Netherlands-Poland-Portugal-Russia-Singapore-South Africa-Spain-Switzerland
APAC | Australia-New Zealand-Singapore
EMEA | Austria-Belgium-Denmark-Finland-France-Germany-Ireland-Italy-Norway-Spain-Sweden-Switzerland-UK
Japan | Hong Kong-Japan-Philippines-Singapore
NA | Canada-USA

As mentioned earlier, this is just a brief intro the extremely useful `GENERATE()` function. I hope that the example gave you some ideas on how to use this function.