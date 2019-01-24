-- In this problem, we will try out a bunch of different outputs for 
-- our queries.

-- comma separated, no header
.header off
.mode csv
SELECT * FROM MyRestaurants;


-- comma separated, header
.mode csv
.header on
SELECT * FROM MyRestaurants;



-- list form, delimiter = "|", no header
.mode list
.header off
SELECT * FROM MyRestaurants;



-- list form, delimiter = "|", header
.mode list
.header on
SELECT * FROM MyRestaurants;



-- column form, column width = 15, no header
.mode column
.width 15
.header off
SELECT * FROM MyRestaurants;



-- column form, column width = 15, header
.mode column
.width 15
.header on
SELECT * FROM MyRestaurants;
