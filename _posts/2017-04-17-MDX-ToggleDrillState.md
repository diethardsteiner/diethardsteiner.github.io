---
layout: post
title:  "MDX ToggleDrillState"
summary: We discuss an elegent use case for ToggleDrillState
date: 2017-04-17
categories: MDX
tags: MDX
published: true
---

For my last blog post [MDX DRILLDOWNMEMBER and CDE Dashboard Tables](/mdx/2017/04/09/CDE-and-MDX-Drilldownmember.html) I got a reply by a vivid reader of the name Raúl Micharet highlighting that drilling down or up can also be achieved by using the `ToggleDrillState` function - in an easier fashion than using `DrillDownMember`. 

The advantage of using `ToggleDrillState` is that it enables you to both drill down and up. The function is quite straight forward to use. Examples are based on the **Pentaho SteelWheels Cube**:

```sql
/* Simulating Drill Down */

/* Stage 1 */
WITH 
  MEMBER [Measures].[Ordinal]     AS '[Product].CurrentMEMBER.Ordinal'
  MEMBER [Measures].[UniqueName]  AS '[Product].CurrentMEMBER.UniqueName'
  MEMBER [Measures].[Childrens]   AS 'Count([Product].CurrentMEMBER.Children)'
SELECT
NON EMPTY {[Measures].[Sales], [Measures].[Ordinal], [Measures].[UniqueName], [Measures].[Childrens]} ON COLUMNS,
NON EMPTY 
  TOGGLEDRILLSTATE( 
    {[Product].[All Products]}, 
    {}
  )
ON ROWS
FROM [SteelWheelsSales]
WHERE 
 ([Time].[2004])
```

![](/images/mdx-toggledrillstate/Screenshot0.png)

```sql
/* Stage 2 */
WITH 
  MEMBER [Measures].[Ordinal]     AS '[Product].CurrentMEMBER.Ordinal'
  MEMBER [Measures].[UniqueName]  AS '[Product].CurrentMEMBER.UniqueName'
  MEMBER [Measures].[Childrens]   AS 'Count([Product].CurrentMEMBER.Children)'
SELECT
NON EMPTY {[Measures].[Sales], [Measures].[Ordinal], [Measures].[UniqueName], [Measures].[Childrens]} ON COLUMNS,
NON EMPTY 
  TOGGLEDRILLSTATE( 
    {[Product].[All Products]}, 
    {[Product].[All Products]}
  )
ON ROWS
FROM [SteelWheelsSales]
WHERE 
 ([Time].[2004])
```

![](/images/mdx-toggledrillstate/Screenshot1.png)

```sql
/* Stage 3 */
WITH 
  MEMBER [Measures].[Ordinal]     AS '[Product].CurrentMEMBER.Ordinal'
  MEMBER [Measures].[UniqueName]  AS '[Product].CurrentMEMBER.UniqueName'
  MEMBER [Measures].[Childrens]   AS 'Count([Product].CurrentMEMBER.Children)'
SELECT
NON EMPTY {[Measures].[Sales], [Measures].[Ordinal], [Measures].[UniqueName], [Measures].[Childrens]} ON COLUMNS,
NON EMPTY 
  TOGGLEDRILLSTATE(
    TOGGLEDRILLSTATE(
      {[Product].[All Products]}
      , {[Product].[All Products]}
    ) 
    , {[Product].[Line].[Motorcycles]}
  )
ON ROWS
FROM [SteelWheelsSales]
WHERE 
 ([Time].[2004])
```

![](/images/mdx-toggledrillstate/Screenshot2.png)

```sql
/* Stage 4 */
WITH 
  MEMBER [Measures].[Ordinal]     AS '[Product].CurrentMEMBER.Ordinal'
  MEMBER [Measures].[UniqueName]  AS '[Product].CurrentMEMBER.UniqueName'
  MEMBER [Measures].[Childrens]   AS 'Count([Product].CurrentMEMBER.Children)'
SELECT
NON EMPTY {[Measures].[Sales], [Measures].[Ordinal], [Measures].[UniqueName], [Measures].[Childrens]} ON COLUMNS,
NON EMPTY 
  TOGGLEDRILLSTATE(
    TOGGLEDRILLSTATE(
      TOGGLEDRILLSTATE(
        {[Product].[All Products]}
        , {[Product].[All Products]}
      ) 
      , {[Product].[Line].[Motorcycles]}
    )
    , [Product].[Motorcycles].[Highway 66 Mini Classics]
  )
ON ROWS
FROM [SteelWheelsSales]
WHERE 
 ([Time].[2004])
```

![](/images/mdx-toggledrillstate/Screenshot3.png)

```sql
/* Stage 5: Drill up to Classic Cars */
WITH 
  MEMBER [Measures].[Ordinal]     AS '[Product].CurrentMEMBER.Ordinal'
  MEMBER [Measures].[UniqueName]  AS '[Product].CurrentMEMBER.UniqueName'
  MEMBER [Measures].[Childrens]   AS 'Count([Product].CurrentMEMBER.Children)'
SELECT
NON EMPTY {[Measures].[Sales], [Measures].[Ordinal], [Measures].[UniqueName], [Measures].[Childrens]} ON COLUMNS,
NON EMPTY 
  TOGGLEDRILLSTATE(
    TOGGLEDRILLSTATE(
      TOGGLEDRILLSTATE(
        TOGGLEDRILLSTATE(
          {[Product].[All Products]}
          , {[Product].[All Products]}
        ) 
        , {[Product].[Line].[Motorcycles]}
      )
      , [Product].[Motorcycles].[Highway 66 Mini Classics]
    )
  , [Product].[Line].[Classic Cars]
  )
ON ROWS
FROM [SteelWheelsSales]
WHERE 
 ([Time].[2004])
```

![](/images/mdx-toggledrillstate/Screenshot4.png)

As you can see from the last result, existing drill downs dont' get closed when navigating several times up and down (see last screenshot). The approach we used basically records all the drill downs and ups you were performing, so over time the query can get quite verbose. You could certainly counteract this by including a bit more logic in CDE (so that lower levels get automatically excluded once you drill up / drill down from a higher level member).

Overall though this is a very useful and easy to use function and I am glad it got pointed out by a vivid reader of this very blog post. Raúl adjusted my example to take advantage of `ToggleDrillState` - you can download his version of the dashboard from [here](https://github.com/diethardsteiner/diethardsteiner.github.io/tree/master/sample-files/cde/MDX-ToggleDrillState-Example-By-Raul-Micharet).

