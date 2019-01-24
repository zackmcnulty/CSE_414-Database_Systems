-- CSE 414 Unit 2 -- Relational Data Model
-- Reading: 2.1, 2.2, 2.3
-- 
--
-- 1 Creating tables
--
create table Company
  (cname varchar(20) primary key,
   country varchar(20),
   no_employees int,
   for_profit char(1));

insert into Company  values ('GizmoWorks', 'USA',  20000,'y');
insert into Company  values ('Canon',     'Japan', 50000,'y');
insert into Company  values ('Hitachi',   'Japan', 30000,'y');
insert into Company  values('Charity',    'Canada',  500,'n');

select * from Company;

-- Making sure SQL Lite shows us the data in a nicer format
-- These commands are specific to SQLite!
.header on
.mode column
.nullvalue NULL

-- 
-- 
-- Comment: upper/lower case; name conflicts
--    -- Company, company, COMPANY  = all the same
--    -- Company(cname, country), Person(pname, country) = repeated 'country' OK
--    -- Company(cname, country), Person(pname, company) = the attribute 'company' not ok

-- Null values: whenever we don't know the value, we can set it to NULL

insert into Company values('MobileWorks', 'China', null, null);
select * from Company;

-- Deleting tuples from the database:

delete from Company where cname = 'Hitachi';
select * from Company;

delete from Company where for_profit = 'n';
-- what happens here??

select * from Company;

-- cname is a key and SQL will ensure that it is unique:
insert into Company values('Canon', 'Japan', null, null);
-- Error: UNIQUE constraint failed

-- Alerts: sql lite is REALLY light: it accepts many erroneous commands,
-- which other RDBMS would not accept.  We will flag these as alerts.
-- Alert 1: sqlite allows a key to be null

insert into Company values(NULL, 'Somewhere', 0, 'n');
select * from Company;

-- this is dangerous, since we cannot uniquely identify the tuple
-- better delete it before we get into trouble

delete from Company where country = 'Somewhere';
select * from Company;

-- Discussion in class:
--   tables are NOT ordered. They represent sets or bags.
--   tables do NOT prescribe how they should be implemented: PHYSICAL DATA INDEPENDENCE!
--   tables are FLAT (all attributes are base types)

------------------------------------------
-- 2  Altering a table  in SQL

-- Add/Drop attribute(s)
-- let's drop the for_profit attribute:

-- Note: SQL Lite does not support dropping an attribute:
-- ALTER TABLE Company DROP for_profit;  -- doesn't work

ALTER TABLE Company ADD ceo varchar(20);
select * from Company;

UPDATE Company SET ceo='Brown' WHERE cname = 'Canon';

SELECT * FROM Company;

-- A peek at the physical implementation:

-- What happens when you alter a table ?  Consider row-wise and column-wise.

------------------------------------------
-- 3 Multiple Tables, and Keys/Foreign-Keys
-- Now alter Company to add the products that they manufacture.
-- Problem: can't add an attribute that is a LIST OF PRODUCTS. What should we do??
--
--

-- Create a separate table Product, with a foreign key to the company:
create table Product
  (pname varchar(20) primary key,
   price float,
   category varchar(20),
   manufacturer varchar(20) references Company);

-- Alert 2: sqlite does NOT enforce foreign keys by default. To enable
-- foreign keys use the following command. The command will have no
-- effect if your version of SQLite was not compiled with foreign keys
-- enabled. Do not worry about it.

PRAGMA foreign_keys=ON;

insert into Product values('Gizmo',      19.99, 'gadget', 'GizmoWorks');
insert into Product values('PowerGizmo', 29.99, 'gadget', 'GizmoWorks');
insert into Product values('SingleTouch', 149.99, 'photography', 'Canon');
insert into Product values('MultiTouch', 199.99, 'photography', 'MobileWorks');
insert into Product values('SuperGizmo', 49.99, 'gadget', 'MobileWorks');

-- now it enforces foreign kyes:
insert into Product values('Hoverboard', 299.99, 'entertainment', 'NewCompany');

-- Error: FOREIGN KEY constraint failed
