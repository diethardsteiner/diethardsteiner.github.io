---
layout: post
title:  "Pentaho Data Integration: Timezones"
summary: This article takes a look at how work with timezones in Pentaho Data Integration
date: 2016-02-24
categories: PDI
tags: PDI
published: false
---

# Timezones

So I had an interesting chat with Dan Keeley and Nelson Sousa about Timezone support in PDI. He made a few suggestions that I'd like to share with you:

Databases store timestamps in UTC.

In the **UK** we are on the GMT timezone which also incorporates daylight saving. In 2015 clocks went forward on the 29 of March, so let's use this as an example for testing.

"In the UK the clocks go forward 1 hour at 1am on the last Sunday in March, and back 1 hour at 2am on the last Sunday in October. The period when the clocks are 1 hour ahead is called British Summer Time (BST). There's more daylight in the evenings and less in the mornings (sometimes called Daylight Saving Time).

When the clocks go back, the UK is on Greenwich Mean Time (GMT)." [Source](https://www.gov.uk/when-do-the-clocks-change)

When we import fields of type timestamp in PDI, we have to let PDI know in which this relates to. So how is this done?

> **Important**: When using the data type `Data` PDI validates the correctness of the date or datetime value based on the java Local (the time zone you are running PDI in). However, when using the data type `datetime`, no valdation against the time zone takes place. [CROSS CHECK THIS]

Depending on the form of input data, there are the following approaches:

1. Sometimes the timezone adjustment is already part of the timestamp value, e.g. the value is ending in `+0800` or `PST`. If not ...
2. We manipulate the timestamp in the DI process to have such a suffix or
3. We just use a **Select values** step and specify the **Date Time Zone**.


Let's have a look at how to define the time zone part for a format string:

pattern | date or time component | presentation | example
--------|------------------------|--------------|--------
z | Time zone | General time zone | Pacific Standard Time; PST; GMT-08:00
Z | Time zone | RFC 822 time zone | -0800
X | Time zone | ISO 8601 time zone | -08; -0800; -08:00

Table Source: [Java SimpleDateFormat](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)


So let's see how we can handle timezones in **Pentaho Data Integration**:

Imagine you source some data and the timestamp field is in UTC, however when you run it in **PDI** you get an error because **PDI** assumes the local timezone.

In example, we might have following input data set as String and use a **Select Values** step to convert it to **Date**:

| my_timestamp
| -------------
| 2015-03-28 00:00:00
| 2015-03-29 00:00:00
| 2015-03-29 01:00:00
| 2015-03-29 01:30:00
| 2015-03-29 02:00:00
| 2015-03-30 00:00:00


`my_timestamp` is defined as type `String` in this case.

## Approach 1: Convert to Date or Timestamp

The timestamp data is in **UTC**. If I convert this to a date or timestamp in **PDI** over here in London (which is on **GMT**), then **PDI** will throw an error, because `2015-03-29 00:00:00` does not exist: At `2015-03-29 01:00:00` clocks got switch forward by an hour to `2015-03-29 02:00:00`.

We get in example this error:

```
2016/02/26 22:11:21 - Select values 2.0 - my_timestamp String : couldn't convert string [2015-03-29 01:00:00] to a date using format [yyyy-MM-dd HH:mm:ss] on offset location 19
2016/02/26 22:11:21 - Select values 2.0 - 2015-03-29 01:00:00
```

## Approach 2: 

To solve this problem, set the **Date Time Zone** in the **Metadata** tab of a **Select Values** step to **UTC**. This is by far the easiest solution. The output looks like this now:


my_timestamp (input data) | my_timestamp (manipulated)
--------------------------|--------------------------
2015-03-28 00:00:00 | 2015-03-28 00:00:00
2015-03-29 00:00:00 | 2015-03-29 00:00:00
2015-03-29 01:00:00 | 2015-03-29 01:00:00
2015-03-29 01:30:00 | 2015-03-29 01:30:00
2015-03-29 02:00:00 | 2015-03-29 01:00:00
2015-03-30 00:00:00 | 2015-03-29 23:00:00

?? Is this correct ??

## Approach 3


We use a **User Defined Java Expression** step to manipulate these strings like so:

New field | Java expression | Value type
----------|-----------------|------------
timestamp_without_timezone | my_timestamp + ".000" | String
timestamp_with_timezone_v1 | my_timestamp + ".000+0000" | String
timestamp_with_timezone_v2 | my_timestamp + ".000 GMT" |String

There are various way to supply the **timezone** info, two of which are shown above.

We then use a **Select Values** step to change the **Metadata**: We convert the `String` to a `Timestamp` using a **Formatting Mask**:

Fieldname                 | Type   | Format
--------------------------|--------|------
my_timestamp_as_date      | Date   | yyyy-MM-dd 
timestamp_without_timezone | Timestamp | yyyy-MM-dd HH:mm:ss.SSS
timestamp_with_timezone_v1 | Timestamp | yyyy-MM-dd HH:mm:ss.SSSZ
timestamp_with_timezone_v2 | Timestamp | yyyy-MM-dd HH:mm:ss.SSS zzz

\** based on `my_timestamp`

And the **output** looks like this:

my_timestamp_as_date | timestamp_without_timezone | timestamp_with_timezone_v1 | timestamp_with_timezone_v2
2015-03-28 | 2015-03-28 00:00:00.000 | 2015-03-28 00:00:00.000+0000 | 2015-03-28 00:00:00.000 GMT
2015-03-29 | 2015-03-29 00:00:00.000 | 2015-03-29 00:00:00.000+0000 | 2015-03-29 00:00:00.000 GMT
2015-03-30 | 2015-03-30 00:00:00.000 | 2015-03-30 01:00:00.000+0100 | 2015-03-30 01:00:00.000 BST

Pay attention to the last entry!  You see that for `timestamp_with_timezone_v1` suddenly the timezone info changes to `+0100` and `my_timestamp_gmt` changes from **GMT** to **BST**.

> During **British Summer Time** (BST), civil time in the United Kingdom is advanced one hour forward of Greenwich Mean Time (GMT) (in effect, changing the time zone from UTC+0 to UTC+1), so that evenings have more daylight and mornings have less. Source: [Wikipedia](https://en.wikipedia.org/wiki/British_Summer_Time)


Finally we will try to output the data to a proper database, e.g. PostgreSQL ([Timestamp Support](http://www.postgresql.org/docs/9.1/static/datatype-datetime.html)):

```sql
CREATE TABLE test.timestamp_test
(
	my_timestamp_as_date DATE 
	, timestamp_without_timezone TIMESTAMP WITHOUT TIME ZONE
	, timestamp_with_timezone_v1 TIMESTAMP WITH TIME ZONE
	, timestamp_with_timezone_v2 TIMESTAMP WITH TIME ZONE
)
;
```

And after executing our data integration process, we can run a select on the final table:

my_timestamp_as_date | timestamp_without_timezone | timestamp_with_timezone_v1 | timestamp_with_timezone_v2
---------------------|----------------------------|----------------------------|---------------------------
2015-03-28 | 2015-03-28 00:00:00 | 2015-03-28 00:00:00+00 | 2015-03-28 00:00:00+00
2015-03-29 | 2015-03-29 00:00:00 | 2015-03-29 00:00:00+00 | 2015-03-29 00:00:00+00
2015-03-30 | 2015-03-30 00:00:00 | 2015-03-30 01:00:00+01 | 2015-03-30 01:00:00+01








[OPEN]

- Unix Z Timezone environment variable
- Importing timestamp data via JDBC: Will have time zone on it ... create an example. Connection > Advanced > Supports Timestamp Data Type (usually ticked by default)

http://stackoverflow.com/questions/23707255/process-iso8601-timestamps-with-offsets-in-pentaho-data-integration



[OPEN] Adding a day: Tip from Nelson: In the calculator step don't add just 1 day while taking into account daylight saving time. Instead, the correct way is to add 24 hours or 1440 minutes.