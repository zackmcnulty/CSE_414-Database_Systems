--------------------------------------------------------------------------------
-- CSE 414 -- AGGREGATES IN SQL
-- Readings: 6.3, 6.4
--------------------------------------------------------------------------------

-- In this lecture we will use the following schema:

create table Purchase
  (pid int primary key,
   product text,
   price float,
   quantity int,
   month varchar(15));

-- download the file 2-data.txt in the current directory
-- use .import to import the data; see .help
-- note that other database systems have different ways to import data

.import '2-data.txt' Purchase

update purchase set price = null where price = 'null';

-- the five basic aggregate operations

select count(*) from purchase;

select count(quantity) from purchase;

select sum(quantity) from purchase;

select avg(price) from purchase;

select max(quantity) from purchase;

select min(quantity) from purchase;

-- Null values are not used in the aggregate

insert into Purchase values(12, 'gadget', NULL, NULL, 'april');

select count(*) from purchase;
select count(quantity) from purchase;
select sum(quantity) from purchase;

-- Counting the number of distinct values

select count(product) from purchase;
select count(distinct product) from purchase;

--------------------------------------------------------------------------------
-- Aggregates With Group-by

select product, count(*)
from purchase
group by product;

select month, count(*)
from purchase
group by month;

-- compare the previous two queries:
--   1. for each PRODUCT compute count(*), v.s.
--   2. for each MONTH compute count(*)

-- aggregates over expressions

-- compute the total revenue for each product:
select product, sum(price*quantity)
from purchase
group by product;


-- compute the average revenue per sale, for each product:
select product, sum(price*quantity)/count(*)
from purchase
group by product;


-- what do these queries do ?
select product, max(month)
from purchase
group by product;

select product, min(month), max(month)
from purchase
group by product;

select product, month
from purchase
group by product;
-- note: sqlite is WRONG on the last query.  why ?

--------------------------------------------------------------------------------
-- Understanding goups

-- 11 tuples:
select * from purchase;

-- 4 groups:
select product, count(*)
from purchase
group by product;

-- 3 groups:
select product, count(*)
from purchase
where price > 2.0
group by product;


--------------------------------------------------------------------------------
-- "DISTINCT" is the same as "GROUP BY"

select month, count(*)
from purchase
group by month;

select month
from purchase
group by month;

select distinct month
from purchase;



--------------------------------------------------------------------------------
-- Ordering results by aggregate

select product, sum(price*quantity) as rev
from purchase
group by product
order by rev desc;

select month, sum(price*quantity)/count(*) as avgrev
from purchase
group by month
order by avgrev desc;

