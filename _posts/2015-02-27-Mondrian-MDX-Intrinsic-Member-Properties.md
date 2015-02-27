---
layout: post
title:  "Mondrian MDX: Intrinsic Member Properties"
summary: In this very short article I want to shed some light on intrinsic member proerties
date:   2015-02-27
categories: OLAP
tags: MDX, Mondrian
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
| not available | `MEMBER_TYPE` | **Problem**: returns an integer value when it should return something like this `MDMEMBER_TYPE_REGULAR`
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

Sources: 

- [Database Journal](http://www.databasejournal.com/features/mssql/article.php/10894_3754131_3/Intrinsic-Member-Properties-The-MEMBERKEY-Property.htm)
- [MS SSAS Docu](https://msdn.microsoft.com/en-us/library/ms145528.aspx)
- Possible values mainly source from [here](http://jira.pentaho.com/browse/MONDRIAN-90).