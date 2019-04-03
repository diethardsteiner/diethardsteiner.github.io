---
layout: post
title: Mondrian Date Dimension
summary: 
date: 2015-05-23
categories: Mondrian
tags: OLAP
published: false
---

I've always been a big fan of cube hierarchies. One dimension that usually contains several hierarchies and will always exist in a cube is the **date** dimensions. In my opinion this dimension is always **a fine balancing act**: You can either go overboard with it, or have to little in. I find that a date dimension in most cases has to be customised for each client - I always pays off sitting down with your client to discuss what exactly they expect to have in a date dimension and what not.

I have seen fatal date dimension hierarchies throughout my career, e.g.: `[yyyy].[M].[w].[d]`. The fact that I still keep on seeing these kind of scenarios is worrying. A date dimension is used everywhere: In any single project I worked on, in any single cube I saw or created. Needless to say, it is an utterly important dimension. If you have to use weeks in your date dimension, make sure you are aware of **ISO8601**. Enough said on this, let's move on to the next subject:

Hierarchies: I enjoy writing MDX like this: `[Date].[Monthly Calendar].[2015].[Mar].[2]`. There is no redundant data in any of the levels and the report just looks clean. However, there is a drawback, which we have to be aware of (and Alexander Schurman pointed this out in a recent discussion): As the level members are not unique (e.g. `Mar` can show up for `2014` and `2015` (any year for that matter)), **Mondrian** has to find an ancestor level which is unique (so run several queries). If month were unique (e.g. `201503`), then **Mondrian** only would have to run one query, so it's much faster. But hold on, wouldn't my MDX query then look quite weird: 

```sql
[Date].[Monthly Calendar].[2015].[201503].[20150301]
``` 

It turns out, that now, as the member is **unique**, we can actually just write:

```sql
[Date].[Monthly Calendar].[Date].[20150301]
``` 

So in this case we can reference the level and member directly. This way we can still write a clean **MDX** query. 

However, these member values is not suitable for end user consumption. Now the idea is that you use this integer representation as the key specifying it as the Mondrian level `column` attribute, which will allow quick SQL query execution (as you can properly index these columns - where available). The **Mondrian** level element also provides the `nameColumn` and `captionColumn` attributes. Image in we have these three DB columns available for month values:

- `calendar_year_week_key`: sample value `201503` 
- `calendar_year_week_label??`: sample value `2015-W3`
- `calendar_year_week_caption`: sample value `2015 Week 3`

The are all representing the same kind of information, just in a different format.

Coming back to the **Mondrian** level attributes:

- `column`: This is the DB column that Mondrian uses in the SQL query it generates.
- `nameColumn`: 
- 



[OPEN]: In star schema setup, to join **Mondrian** will use the key defined in the dimension ... so why would using keys for levels make much of a difference? Is there really any benefit?