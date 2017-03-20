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

We can create a cube for each of the fact tables to answer specific questions:

**Q**: What is the average grade by student?

```sql
WITH
MEMBER [Measures].[Avg Grade] AS
  AVG(
    [Course Name].Members
    , (
        [Student.Student Name].CurrentMember
        , [Measures].[Grade]
      )
  )
SELECT
  [Student.Student Name].Members ON ROWS
  , [Measures].[Avg Grade] ON COLUMNS
FROM [Grades]
```

> **Important**: Grades have to be evaluated in the context of the student, otherwise the average will not be calcualted correctly!

```
Axis #0:
{}
Axis #1:
{[Measures].[Avg Grade]}
Axis #2:
{[Student.Student Name].[All Student.Student Names]}
{[Student.Student Name].[Bob]}
{[Student.Student Name].[Lilian]}
Row #0: 12
Row #1: 6.667
Row #2: 8
```

Wrong result! We have to make sure that in the calculated measures we use `Children` instead of `Members` for `Course Name`:

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
  [Student.Student Name].Members ON ROWS
  , [Measures].[Avg Grade] ON COLUMNS
FROM [Grades]
```

Result:

```
Axis #0:
{}
Axis #1:
{[Measures].[Avg Grade]}
Axis #2:
{[Student.Student Name].[All Student.Student Names]}
{[Student.Student Name].[Bob]}
{[Student.Student Name].[Lilian]}
Row #0: 9 --> WRONG!!!
Row #1: 5
Row #2: 8
```

**Q**: What is the average grade by course?

```sql
WITH
MEMBER [Measures].[Avg Grade] AS
  AVG(
    [Course Name].Members
    , (
        [Student.Student Name].CurrentMember
        , [Measures].[Grade]
      )
  )
SELECT
  [Course Name].Children ON ROWS
  , [Measures].[Avg Grade] ON COLUMNS
FROM [Grades]
```

Result: WRONG!!!

```
Axis #0:
{}
Axis #1:
{[Measures].[Avg Grade]}
Axis #2:
{[Course Name].[Math]}
{[Course Name].[Physics]}
Row #0: 12
Row #1: 12
```

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

```
Axis #0:
{[Hobby Name].[Gaming]}
Axis #1:
{[Measures].[Count Hobbies]}
Axis #2:
{[Student.Student Name].[All Student.Student Names]}
{[Student.Student Name].[Bob]}
{[Student.Student Name].[Lilian]}
Row #0: 2
Row #1: 1
Row #2: 1
```

And then we can create a **virtual cube** to answer questions that span both fact tables. We call this cube **Student Grades and Hobbies**. Let's first get a list of students that have Gaming as a hobby:

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

Next let's look at the original question:

**Q**: What is the average grade of students that attended the math course and have Gaming as a hobby?


```sql 
WITH
MEMBER [Measures].[Avg Grade] AS
  AVG(
    [Course Name].Members
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
      , [Measures].[Count Hobbies] 
    ) > 0
)
SELECT
  STUDENTS ON ROWS
  , [Measures].[Avg Grade] ON COLUMNS
FROM [Grades and Hobbies]
WHERE
  [Course Name].[Math]
```

The trick here is to get a list of students that have a specific hobby before asking for the grades by course.

Result:

```
Axis #0:
{[Course Name].[Math]}
Axis #1:
{[Measures].[Avg Grade]}
Axis #2:
```
