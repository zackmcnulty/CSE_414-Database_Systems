--------------------------------------------------------------------------------
-- CSE 414 -- MORE AGGREGATES IN SQL
-- Readings: 6.3, 6.4
--------------------------------------------------------------------------------

-- In this demo we will use the following schema:

create table Purchase
  (pid int primary key,
   product text,
   price float,
   quantity int,
   month varchar(15));

-- download the file 2-data.txt in the current directory
-- use .import to import the data; see .help
-- note that other database systems have different ways to import data

.import lec04-data.txt Purchase

update purchase set price = null where price = 'null';


--------------------------------------------------------------------------------
-- the HAVING clause

select month, count(*) 
from purchase
group by month;


select month,  count(*), sum(price*quantity)/count(*)
from purchase
group by month
having sum(price*quantity)/count(*) < 100.0;

-- Rule
--   WHERE condition is applied to individual rows: 
--         the rows may or may not contributed to the aggregate
--         no aggregates allowed here
--   HAVING condition is applied to the entire group:
--         entire group is returned, or not al all
--         may use aggregate functions in the group


--------------------------------------------------------------------------------
-- aggregates and joins

create table Product
        (pid int primary key,
         pname text,
         manufacturer text);

insert into product values(1, 'bagel', 'Sunshine Co.');
insert into product values(2, 'banana', 'BusyHands');
insert into product values(3, 'gizmo', 'GizmoWorks');
insert into product values(4, 'gadget', 'BusyHands');
insert into product values(5, 'powerGizmo', 'PowerWorks');


-- number of sales per manufacturer
select x.manufacturer, count(*)
from Product x, Purchase y
where x.pname = y.product
group by x.manufacturer;

-- number of sales per manufacturer and month
select x.manufacturer, y.month, count(*)
from Product x, Purchase y
where x.pname = y.product
group by x.manufacturer, y.month;


--------------------------------------------------------------------------------
--  Semantics of SQL queries with Group By
--
--     SELECT a1, a2, ..., agg1, agg2, ...
--     FROM R1, R2, ...
--     WHERE C
--     GROUP BY g1, g2, ...
--     HAVING D
--
-- Syntactic rules:
--    C is any condition on the attributes in R1, R2, ...
--    D is any condition on the attributes in R1, R2, ... AND aggregates
--    all attributes a1, a2, ... must occur in the GROUP BY clause (WHY ?)
--
-- Semantics:
--    Step 1. Evaluate the FROM-WHERE part of the query using the "nested loop" semantics
--    Step 2. Group answers by their values of g1, g2, ...
--    Step 3. Compute the aggregates in D for each goup: retain only groups where D is true
--    Step 4. Compute the aggregates in SELECT and return the answer
--
-- Important notes:
--    there is one row in the answer for each group
--    no group can be empty !  In particular, count(*) is never 0

--------------------------------------------------------------------------------
-- Aggregates on empty groups

-- number of sales per manufacturer: but PowerWorks does not appear !
select x.manufacturer, count(*)
from Product x, Purchase y
where x.pname = y.product
group by x.manufacturer;

select x.manufacturer, count(y.pid)
from Product x left outer join Purchase y on x.pname = y.product
group by x.manufacturer;
