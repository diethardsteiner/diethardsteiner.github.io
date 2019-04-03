---
layout: post
title:  "Mondrian MDX: Intrinsic Member Properties"
summary: In this very short article I want to shed some light on intrinsic member proerties
date:   2015-02-27
categories: Mondrian
tags: MDX Mondrian
published: true
---

## Intrinsic Member Properties

**MDX** is an extremely poweful query language. In this very short article I want to shed some light on **intrinsic member proerties**, which provide a lot of information about the member itself. Let's list them all first:

All properties shown below can be retrieved like this: 

```sql
CurrentMember.Properties("PARENT_UNIQUE_NAME")
``` 

The function listed in the first column can be used as an alternative approach.

| Function | Property | Description
|---|---|----
| not available | `MEMBER_KEY` | returns `column` value / key value
| `CurrentMember.Name` | `MEMBER_NAME` | returns `nameColumn` value
| `CurrentMember.Caption` | `MEMBER_CAPTION` | returns `captionColumn` value
| `CurrentMember.UniqueName` | `MEMBER_UNIQUE_NAME` | returns the fully qualified member name, e.g. `[Date.Weekly Calendar].[2024].[45]`
| not available | `MEMBER_TYPE` | **Problem**: returns an integer. The type of the member: MDMEMBER_TYPE_REGULAR (1), MDMEMBER_TYPE_ALL (2), MDMEMBER_TYPE_MEASURE (3), MDMEMBER_TYPE_FORMULA (4), MDMEMBER_TYPE_UNKNOWN (0). MDMEMBER_TYPE_FORMULA takes precedence over MDMEMBER_TYPE_MEASURE. For example, if there is a formula (calculated) member on the Measures dimension, it is listed as MDMEMBER_TYPE_FORMULA. Thanks to Roland Bouman for finding [this reference](https://technet.microsoft.com/de-de/subscriptions/ms126046%28v=sql.90%29.aspx)
| not available | `MEMBER_GUID` | **Problem**: does not return anything nor does it throw an error
| `CurrentMember.Ordinal` | `MEMBER_ORDINAL` | **Problem**: two different values returned: 2 for shortcut and -1 property
| not available | `DESCRIPTION` | returns description of the member
| `CurrentMember.Children.Count` | `CHILDREN_CARDINALITY` | returns the amount of children
| `CurrentMember.Parent.UniqueName` | `PARENT_UNIQUE_NAME` | returns the unique name of the parent
| `CurrentMember.Parent.Count` | `PARENT_COUNT` | counts number of parents 
| `CurrentMember.Parent.Level.Ordinal` | `PARENT_LEVEL` | returns parents level number
| `CurrentMember.Level.Ordinal` | `LEVEL_NUMBER` | returns the level's ordinal number
| `CurrentMember.Level.UniqueName` | `LEVEL_UNIQUE_NAME` | returns the levels unique name, e.g. `[Date.Weekly Calendar].[Week]`
| `CurrentMember.Hierarchy.UniqueName` | `HIERARCHY_UNIQUE_NAME`  | returns the hierarchies unique name, e.g. `[Date.Weekly Calendar]`
| `CurrentMember.Dimension.UniqueName` | `DIMENSION_UNIQUE_NAME` | returns the dimension's unqiue name, e.g. `[Date]`
| not available | `CUBE_NAME` | returns the cube name - **Problem**: doesn't show a value but doesn't cause an error either
| not available | `SCHEMA_NAME` | returns the schema name
| not available | `CATALOG_NAME` | returns the catalog name

Let's have a look at a practical example to understand how we can use **intrinsic member properties** in real world scenarios: The week a date dimensions might have a caption like this one: 2014-W3. This is not suitable for sorting the week in descending order. So we can use the member key (e.g. 201403) for sorting purposes. The key is normally not exposed, however, using the properties function we can retrieve this value:

```sql
WITH
MEMBER [Measures].[Key] AS
    [Date.Weekly Calendar].CurrentMember.Properties("MEMBER_KEY")
MEMBER [Measures].[Name] AS
    --[Date.Weekly Calendar].CurrentMember.Name
    [Date.Weekly Calendar].CurrentMember.Properties("MEMBER_NAME")
MEMBER [Measures].[Caption] AS
    --[Date.Weekly Calendar].CurrentMember.Caption
    [Date.Weekly Calendar].CurrentMember.Properties("MEMBER_CAPTION")
MEMBER [Measures].[Unique Name] AS
    --[Date.Weekly Calendar].CurrentMember.UniqueName
    [Date.Weekly Calendar].CurrentMember.Properties("MEMBER_UNIQUE_NAME")
MEMBER [Measures].[Type] AS
    --[Date.Weekly Calendar].CurrentMember.Type -- doesnt work
    [Date.Weekly Calendar].CurrentMember.Properties("MEMBER_TYPE")
MEMBER [Measures].[GUID] AS
    [Date.Weekly Calendar].CurrentMember.Properties("MEMBER_GUID")
MEMBER [Measures].[Member Ordinal] AS
    --[Date.Weekly Calendar].CurrentMember.Ordinal
    [Date.Weekly Calendar].CurrentMember.Properties("MEMBER_ORDINAL")
MEMBER [Measures].[Member Description] AS
    --[Date.Weekly Calendar].CurrentMember.Description -- doesnt work
    [Date.Weekly Calendar].CurrentMember.Properties("DESCRIPTION")
MEMBER [Measures].[Children Cardinality] AS
    --[Date.Weekly Calendar].CurrentMember.Children.Count
    [Date.Weekly Calendar].CurrentMember.Properties("CHILDREN_CARDINALITY")
MEMBER [Measures].[Parent Unique Name] AS
    [Date.Weekly Calendar].CurrentMember.Parent.UniqueName
    --[Date.Weekly Calendar].CurrentMember.Properties("PARENT_UNIQUE_NAME")
MEMBER [Measures].[Parent Count] AS
    --[Date.Weekly Calendar].CurrentMember.Parent.Count
    [Date.Weekly Calendar].CurrentMember.Properties("PARENT_COUNT")
MEMBER [Measures].[Parent Level] AS
    [Date.Weekly Calendar].CurrentMember.Parent.Level.Ordinal
    --[Date.Weekly Calendar].CurrentMember.Properties("PARENT_LEVEL")
MEMBER [Measures].[Level Number] AS
    --[Date.Weekly Calendar].CurrentMember.Level.Ordinal
	[Date.Weekly Calendar].CurrentMember.Properties("LEVEL_NUMBER") 
MEMBER [Measures].[Level Unique Name] AS
    --[Date.Weekly Calendar].CurrentMember.Level.UniqueName
    [Date.Weekly Calendar].CurrentMember.Properties("LEVEL_UNIQUE_NAME")
MEMBER [Measures].[Hierarchy Unique Name] AS
    --[Date.Weekly Calendar].CurrentMember.Hierarchy.UniqueName
    [Date.Weekly Calendar].CurrentMember.Properties("HIERARCHY_UNIQUE_NAME")
MEMBER [Measures].[Dimension Unique Name] AS
    --[Date.Weekly Calendar].CurrentMember.Dimension.UniqueName
    [Date.Weekly Calendar].CurrentMember.Properties("DIMENSION_UNIQUE_NAME")
MEMBER [Measures].[Cube Name] AS
    [Date.Weekly Calendar].CurrentMember.Properties("CUBE_NAME") 
MEMBER [Measures].[Schema Name] AS
    [Date.Weekly Calendar].CurrentMember.Properties("SCHEMA_NAME") 
MEMBER [Measures].[Catalog Name] AS
    [Date.Weekly Calendar].CurrentMember.Properties("CATALOG_NAME") 
SELECT
    {
    [Measures].[Key]
    , [Measures].[Name]
    , [Measures].[Caption]
    , [Measures].[Unique Name]
    , [Measures].[Type] 
    , [Measures].[GUID]
    , [Measures].[Member Ordinal]
    , [Measures].[Member Description]
    , [Measures].[Children Cardinality]
    , [Measures].[Parent Unique Name]
    , [Measures].[Parent Level]
    , [Measures].[Parent Count]
    , [Measures].[Level Number]
    , [Measures].[Level Unique Name]
    , [Measures].[Hierarchy Unique Name]
    , [Measures].[Dimension Unique Name]
    , [Measures].[Cube Name]
    , [Measures].[Schema Name]
    , [Measures].[Catalog Name]
    } ON 0
    , NON EMPTY 
        ORDER(
            DESCENDANTS(
                [Date.Weekly Calendar]
                , 2
                , SELF
            )
            , [Measures].[Key]
            , BDESC
        )
    ON 1
FROM [Subscriber Base]
```

There are many use case, when **intrinsic member properties** come in handy. I hope this short article made you aware that there are many **intrinsic member properties** available, which you can make good use of in your future **MDX** queries.

Update 2015-03-28: We had an interesting email conversation with **Roland Bouman** and I'd like to share his words and conclusions here:

## Roland Bouman's Thoughts

Today I compared my results with Diethards complete list from his post:

```sql
SELECT
  Measures.Quantity
ON COLUMNS,
  Time.Years.Members 
DIMENSION PROPERTIES 
  MEMBER_KEY,
  MEMBER_NAME,
  MEMBER_CAPTION,
  MEMBER_UNIQUE_NAME,
  MEMBER_TYPE,    
  MEMBER_GUID,
  MEMBER_ORDINAL,
  DESCRIPTION,
  CHILDREN_CARDINALITY,
  PARENT_UNIQUE_NAME,
  PARENT_COUNT,
  PARENT_LEVEL,
  LEVEL_NUMBER,
  LEVEL_UNIQUE_NAME,
  HIERARCHY_UNIQUE_NAME,
  DIMENSION_UNIQUE_NAME,
  CUBE_NAME,
  SCHEMA_NAME,
  CATALOG_NAME
ON ROWS
FROM
  SteelWheelsSales
```

I run this in pash [Pentaho Analysis Shell], and then used the network tab from my browser developer tools to look at the response. The tuples for the rows axis look like this:

```xml
<Member Hierarchy="Time">
 <UName>[Time].[2004]</UName>
 <Caption>2004</Caption>
 <LName>[Time].[Years]</LName>
 <LNum>1</LNum>
 <DisplayInfo>131076</DisplayInfo>
 <MEMBER_KEY>2004</MEMBER_KEY>
 <MEMBER_NAME>2004</MEMBER_NAME>
 <MEMBER_CAPTION>2004</MEMBER_CAPTION>
 <MEMBER_UNIQUE_NAME>[Time].[2004]</MEMBER_UNIQUE_NAME>
 <MEMBER_TYPE>1</MEMBER_TYPE>
 <MEMBER_ORDINAL>-1</MEMBER_ORDINAL>
 <CHILDREN_CARDINALITY>4</CHILDREN_CARDINALITY>
 <PARENT_UNIQUE_NAME>[Time].[All Years]</PARENT_UNIQUE_NAME>
 <PARENT_COUNT>1</PARENT_COUNT>
 <PARENT_LEVEL>0</PARENT_LEVEL>
 <LEVEL_NUMBER>1</LEVEL_NUMBER>
 <LEVEL_UNIQUE_NAME>[Time].[Years]</LEVEL_UNIQUE_NAME>
 <HIERARCHY_UNIQUE_NAME>[Time]</HIERARCHY_UNIQUE_NAME>
 <DIMENSION_UNIQUE_NAME>[Time]</DIMENSION_UNIQUE_NAME>
 <SCHEMA_NAME>SteelWheels</SCHEMA_NAME>
</Member>
```

The lower case properties UName, Caption, LName, LNum and DisplayInfo are always returned, at least when using XML/A

These are the so-called default properties. See [here]( 
https://msdn.microsoft.com/en-us/library/windows/desktop/ms725398%28v=vs.85%29.aspx).

Of these, an underestimated and frankly quite obscure property is `DisplayInfo`. It is rather useful when rendering pivot tables with toggles to drill up/down.
It's a 4 byte number that is itself divided in at least 3 fields: 

1. the 2 least significant bytes form a 2 byte number that contains an estimate of the number of children. So if it's 0, you can't drill down.
2. the first, least significant bit of the 3rd byte is a flag that is set if the member is drilled down.
3. the second bit of the 3rd byte is also a flag that tells you if the current member has the same parent as the previous member.

(More info in that link above about how to use this to render pivot tables)

The upper case properties are from Diethards post. There are even more: see [here](https://msdn.microsoft.com/en-us/library/ms145528.aspx) (although I would not be surprised if many of these are not supported in Mondrian).
I get more or less the same results as Diethard, with these differences:

- MEMBER_TYPE works as expected. It's just that the values are an enumeration. See [here](https://technet.microsoft.com/de-de/subscriptions/ms126046%28v=sql.90%29.aspx)
- I don't see DESCRIPTION, nor CATALOG_NAME

It might be worth pointing out that, at least in the XML/A case, MEMBER_UNIQUE_NAME and UName, LEVEL_NUMBER and LNum, and MEMBER_CAPTION and Caption, and possibly LNum and LEVEL_UNIQUE_NAME are duplicates.
Also, CHILDREN_CARDINALITY may be an estimate, just like the cardinality part of DisplayInfo. 

In other words, in XML/A one need never ask for these properties explicitly as they are implicitly given already.



Sources: 

- [Database Journal](http://www.databasejournal.com/features/mssql/article.php/10894_3754131_3/Intrinsic-Member-Properties-The-MEMBERKEY-Property.htm)
- [MS SSAS Docu](https://msdn.microsoft.com/en-us/library/ms145528.aspx)
- Possible values mainly source from [here](http://jira.pentaho.com/browse/MONDRIAN-90).