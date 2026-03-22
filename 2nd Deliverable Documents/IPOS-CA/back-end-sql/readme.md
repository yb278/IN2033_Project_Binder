# This will serve as the backend for IPOS-CA

It will consist of an SQLite database, it will handle everything including setup, adding/filling in tables

TO get it running you need:
+ MySQL, https://dev.mysql.com/downloads/installer/
+ First run `source path_to_ipos_ca_schema.sql`
+ Then all queries can be done or you can run the same command but with the test data and then test from there

## TODO
+ Look into java connector files 
+ Prep it for the front end