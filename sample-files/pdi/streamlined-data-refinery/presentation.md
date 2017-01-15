# Preperation

- screen resolution: 1600 x 900 (16:9)
- start postgres: `service postgresql start` 

-------------------------------------

# The Pentaho Synergy

Originally Pentaho brought together 4 different projects: Kettle, Mondrian, JFreeReport, Weka. They tried to combine them:

- **Pentaho BI Server**: Run PRD reports, JPivot with Mondrian
- **PDI**: Report bursting, Mondrian Input, etc.
- **PRD**: PDI and Mondrian input data source
- **Agile BI Plugin**: Analyzer meets PDI
- **Streamlined Data Refinery** 
- **Pentaho Server**: DI and BA Server merge

-------------------------------------

# Streamlined Data Refinery

What is it?

- Users requests data based on certain attributes via web form (CDE)
- Metadata driven ETL job/transformation sources and combines data (PDI)
- A semantic layer gets generated and publish to BA Server (Mondrian, Metadata)
- Users can analyse requested data (Pentaho Analyzer)

It's a **solution**.

--------------------------------------

# Demo

- PDI steps
  - Annotate Stream
  - Shared Dimension
  - Build Model
  - Publish Model
- DET (Data Exploration Tool) EE only
- Publishing of Model
- Data Anlysis
