---
layout: post
title: "Mondrian: Modeling a Multivalued Dimension Attribute"
summary: This article explain how to model multivalued dimensions with Mondrian
date: 2017-03-20
categories: Mondrian
tags: Mondrian
published: false
--- 


A vivid reader called ... recently contacted me after reading my article on bridge tables: He was after a solution for a challenging scenario. The task was to create a model for students that receive grades for the courses they attend. A student can have one or more hobbies and one of the analysis questions was: What is the average grade for students with hobby X that attend course Y? This caught my interest and I was eager to find out how this would be implemented with **Mondrian**.

In our example we use the following data:

- Student Bob attended the Maths course and scored a 7 (grade). He also attended the Physics course and scored a 3 (grade). Bob's hobbies are Gaming and Reading.
- Student Lilian attended the Maths course and scored a 8 (grade). Her hobbies are Gaming, Jogging and Writing.

While I had some ideas only a talk with **Nelson Sousa** led to a viable solution: He suggested creating two seperate fact tables, one which stores the grades per student per course (naturally you would have a date as well, but we ignore it for simplicity sake). And another fact table which just stores the student and hobbies relationship. The latter one does not necessary have to have a measure in the fact table itself, however, I added one for explicity sake. And finally there is a student dimension serving as the link between the two fact tables:


`multivalued.dim_student`:

 student_tk | student_name 
------------|--------------
          1 | Bob
          2 | Lilian


`multivalued.fact_grades`:

 student_tk | course_name | grade 
------------|-------------|-------
          1 | Math        |     7
          1 | Physics     |     3
          2 | Math        |     8

`multivalued.fact_student_hobbies`:

 student_tk | hobby_name | cnt 
------------|------------|-----
          1 | Gaming     |   1
          1 | Reading    |   1
          2 | Gaming     |   1
          2 | Jogging    |   1
          2 | Writing    |   1
          
## Create standard Cubes to answer simple Questions

We can create a cube for each of the fact tables to answer specific questions:

**Q**: What is the average grade by student?

```sql
WITH
MEMBER [Measures].[Avg Grade] AS
  AVG(
    [Course Name].Children
    , (
        [Student.Student Name].CurrentMember
        , [Measures].[Grade]
      )
  )
SELECT
  [Student.Student Name].Children ON ROWS
  , [Measures].[Avg Grade] ON COLUMNS
FROM [Grades]
```

> **Important**: Grades have to be evaluated in the context of the student, otherwise the average will not be calcualted correctly!

Result:

Student Name | Avg Grade
-------------|-----------
Bob | 5
Lilian | 8


**Learning Exercise**:

Let's understand how a custom aggregation function works:

```sql
WITH
MEMBER [Measures].[x] AS (
  [Student.Student Name].CurrentMember
  , [Measures].[Grade]
)
MEMBER [Measures].[x2] AS 
  SUM(
    [Course Name.Course Name].Members, 
    (
      [Student.Student Name].CurrentMember
      , [Measures].[Grade]
    )
  )
SELECT 
  {[Measures].[x] , [Measures].[x2]} ON COLUMNS
  , [Student.Student Name].Members ON ROWS
FROM [Grades]
```

Student Name | x  | x2
-------------|----|------
All Student.Student Names | 18 | 36
Bob          | 10 | 20
Lilian       | 8  | 16

What measure x does is basically sum the grades by the student that is currently in scope (in this query the standard grade measure would just do the same). Intersting is then what happens with measure x2: For each course member, we sum up the grade measure in context of the current student (sum is defined as the aggregate function of the grade measure in the cube definition/mondrian schema). So if we look at Bob, he attends two courses, so if we sum his grades we get 10 returned (7 + 3). Since measure x2 evaluates the formula for each course, we get 20 (10 * 2 courses) returned in the final result. In general, the result isn't really useful in practical terms, however, it illustrates the power of custom aggregate functions nicely.

**Q**: What is the average grade by course?

```sql
WITH
MEMBER [Measures].[Avg Grade] AS
  AVG(
    [Student.Student Name].Children
    , ([Course Name].CurrentMember, [Measures].[Grade])
  )
SELECT
  [Course Name].Children ON ROWS
  , [Measures].[Avg Grade] ON COLUMNS
FROM [Grades]
```

Result: 

Course Name | Avg Grade
------------|----------
Math        | 7.5
Physics     | 3

**Q**: How many students have Gaming as a Hobby?

```sql
SELECT
  NON EMPTY [Student.Student Name].Members ON ROWS
  , [Measures].[Count Hobbies] ON COLUMNS
FROM [Hobbies]
WHERE
  [Hobby Name].[Gaming]
```

Result:

Student Name | Count Hobbies
-------------|-----------------
All Student.Student Names | 2
Bob    | 1
Lilian | 1


## Create a Virtual Cube to answer complex Questions

And then we can create a **virtual cube** to answer questions that span both fact tables. We call this cube **Grades and Hobbies**. 
I recommend reading through the official 
[Mondrian Docu on Virtual Cubes](http://mondrian.pentaho.com/documentation/schema.php#Virtual_cubes) to understand how to construct virtual cubes.

### Virtual Cube: How to join base cubes

Use a **global (conforming) dimension** for this purpose. In our case the `Student` dimension links the two fact tables and is defined as global dimension.

```xml
<VirtualCubeDimension name="Student"/>
```

**Q**: Does this then in effect require the definition of `CubeUsage` as well since in this case the `VirtualCubeDimension` element does not specify a value for the `cubeName`? Otherwise how does Mondrian know that this global dimension exists in both base cubes and that it can hence use it to join the base cubes?

**A**: `CubeUsage` is optional and not required. If you reference a global dimension in the **virtual cube**, Mondrian will check the base cubes defined for the **virtual measures** to see if this **global dimension** is referenced in the **base cubes**.


Let's create a list of students with a count of hobbies and the sum of grades (last one is completely useless, for now we just want to proove we can query across the base cubes):

```sql
SELECT
  [Student.Student Name].Children ON ROWS
  , {[Measures].[Grade], [Measures].[Count Hobbies]} ON COLUMNS
FROM [Grades and Hobbies]
```

Student Name | Grade | Count Hobbies
-------------|-------|-------------
Bob | 10 | 2
Lilian | 8 | 3

This shows quite an intersting result: count of hobbies is only available on the All level, but grades are available everywhere:

```sql
SELECT
    [Student.Student Name].Members * [Course Name].Members ON ROWS
  , {[Measures].AllMembers} ON COLUMNS
FROM [Grades and Hobbies]
```

Student Name | Course Name | Count Hobbies | Grade
-------------|-------------|---------------|--------
All Student.Student Names | All Course Names | 5 | 18
 | Math | | 15
 | Physics | | 3
Bob |All Course Names | 2 | 10
 | Math | | 7
 | Physics | | 3
Lilian | All Course Names | 3 | 8
 | Math | |	8
 | Physics | | 
 
This makes sense though, as the counts of hobbies can only be available on an All Courses level as we do not store them by course!

Let's get a list of students that have Gaming as a hobby:

```sql
SELECT
  FILTER(
  [Student.Student Name].Children
  , (
      [Hobby Name].[Gaming]
      , [Measures].[Count Hobbies] 
    ) > 0
) ON ROWS
  , {} ON COLUMNS
FROM [Grades and Hobbies]
```

Result:

```
Axis #0:
{}
Axis #1:
Axis #2:
{[Student.Student Name].[Bob]}
{[Student.Student Name].[Lilian]}
```

**Learning Exercise**

Let's go back to understanding some basics: Pay attention to the slicer (`WHERE`) in the following query:

```sql
WITH SET STUDENTS AS
FILTER(
  [Student.Student Name].Children
  , (
      [Hobby Name].[Reading]
      , [Measures].[Count Hobbies] 
    ) > 0
)
SELECT
  STUDENTS ON ROWS
  , {[Measures].[Grade]} ON COLUMNS
FROM [Grades and Hobbies]
WHERE [Course Name].[Course Name].[Math]
```

Interstingly enough we do not get any records returned! **WHY?**. However, if we move the constrain from the slicer to one of the axis, all is fine:

```
WITH SET STUDENTS AS
FILTER(
  [Student.Student Name].Children
  , (
      [Hobby Name].[Reading]
      , [Measures].[Count Hobbies] 
    ) > 0
)
SELECT
  STUDENTS *  [Course Name].[Course Name].[Math] ON ROWS
  , {[Measures].[Grade]} ON COLUMNS
FROM [Grades and Hobbies]
```
Result:

Student Name | Course Name | Grade
-------------|-------------|-------
Bob | Math | 7

The reason for this is that the slicer also influences calculated members and sets directly, whereas if we move the contrain into one of axis, calculated members and sets will be evaluated without this constrain??? In the virtual cube is seems like we cannot directly join both base cubes together at the same time???

Next let's look at the original question:

**Q**: What is the average grade of students that attended the math course and have Gaming as a hobby?


This query returns all students that are interest in Gaming and attended the math course:

```sql
WITH
SET STUDENTS AS
FILTER(
  [Student.Student Name].Children
  , (
      [Hobby Name].[Gaming]
      , [Measures].[Count Hobbies] 
    ) > 0
)
SELECT
  STUDENTS *  [Course Name].[Course Name].[Math] ON ROWS
  , {[Measures].[Grade]} ON COLUMNS
FROM [Grades and Hobbies]
```

Student Name | Course Name | Grade
-------------|-------------|---------
Bob | Math | 7
Lilian | Math | 8

Let's try to answer the original question now:


```sql 
WITH
MEMBER [Measures].[Avg Grade] AS
  AVG(
    [Course Name].Children
    , (
        [Student.Student Name].CurrentMember
        , [Measures].[Grade]
      )
  )
SET STUDENTS AS
FILTER(
  [Student.Student Name].Children
  , (
      [Hobby Name].[Gaming]
      , [Course Name].[All Course Names]
      , [Measures].[Count Hobbies] 
    ) > 0
)
SELECT
  STUDENTS *  [Course Name].[Course Name].[Math] ON ROWS
  , {[Measures].[Grade]} ON COLUMNS
FROM [Grades and Hobbies]
WHERE [Course Name].[Course Name].[Math]
```

The trick here is to get a list of students that have a specific hobby before asking for the grades by course.
Also, note that we move the constrain on the Math course into the slicer (`WHERE` clause). Since this influences the calculated set as well, we have to explicitly add `[Course Name].[All Course Names]` there as well.

Result:

```
Axis #0:
{[Course Name].[Math]}
Axis #1:
{[Measures].[Avg Grade]}
Axis #2:
```


---------------

WITH
MEMBER [Measures].[Avg Grade] AS
  AVG(
    [Student.Student Name].Children
    , ([Course Name].CurrentMember, [Measures].[Grade])
  )
SET STUDENTS AS
FILTER(
  [Student.Student Name].Children
  , (
      [Hobby Name].[Gaming]
      , [Measures].[Count Hobbies] 
    ) > 0
)
SET COURSES AS 
EXTRACT(
  NONEMPTYCROSSJOIN(
    [Course Name].[Course Name].[Math]
    , NONEMPTYCROSSJOIN(
      STUDENTS
      , [Measures].[Grades]
    )
  )
  , [Course Name.Course Name]
)
SELECT
   COURSES ON ROWS
  , {[Measures].[Avg Grade]} ON COLUMNS
FROM [Grades and Hobbies]

/*  does not return anything  */

/* Below query does it not return something because Course is no a global/conformed dim? */
SELECT
    [Course Name].Members ON ROWS
  , {[Measures].[Measures Grade]} ON COLUMNS
FROM [Grades and Hobbies]

/* This works (without using slicer), but result is still not ok: Returns 8 */

WITH
SET STUDENTS AS
FILTER(
  [Student.Student Name].Children
  , (
      [Hobby Name].[Gaming]
      , [Measures].[Count Hobbies] 
    ) > 0
)
MEMBER [Measures].[Avg Grade] AS
  AVG(
    STUDENTS
    , [Measures].[Grade]
  )
SELECT
   [Course Name].[Course Name].[Math] ON ROWS
  , {[Measures].[Avg Grade]} ON COLUMNS
FROM [Grades and Hobbies]

/* This is correct, but does not take the context of hobbies into account */

WITH
SET STUDENTS AS
FILTER(
  [Student.Student Name].Children
  , (
      [Hobby Name].[Gaming]
      , [Measures].[Count Hobbies] 
    ) > 0
)
MEMBER [Measures].[Avg Grade] AS
  AVG(
    [Student.Student Name].Children
    , [Measures].[Grade]
  )
SELECT
   [Course Name].[Course Name].[Math] ON ROWS
  , {[Measures].[Avg Grade]} ON COLUMNS
FROM [Grades and Hobbies]

/* I tried to use Extract further up to take the hobbies context into account, but this did not return anything */
	 	 

 
