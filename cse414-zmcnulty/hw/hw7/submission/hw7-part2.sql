-- part 2.1

CREATE TABLE Sales(
	name varchar(40),
	discount varchar(10),
	month varchar(10),
	price int
);

.mode tabs
.separator "\t"
.import ./mrFrumbleData.txt Sales


-- part 2.2

-- strategy : to check a dependency A -> B, first group all the tuples by A. Then, for each group
-- 		reduce the tuples by removing duplicates of the attributes in B (i.e. so each tuple
--		in group has distinct values for the attributes in B). If all these tuples have
--		matching values for these attributes (i.e. A -> B holds for this group), only a single
-- 		tuple should remain. Filter out groups where this does not hold true, and count the total
-- 		number of tuples in all remaining groups. If A -> B holds for all tuples, this count
-- 		should match the total number of tuples.
-- 		the only way this test can fail is if all tuples have unique set of attribute values A
--		This can give a false dependency as then A will certainly apply B because all the A are different.
--		To check for this, I count the number of groups and ensure it is not the same as the number of
--		tuples.


-- total number tuples in table
SELECT COUNT(*) FROM Sales;
-- there are 427 tuples total in Sales table

.header on

-- the following queries returns number of tuples satisfying dependency month -> discount; 
-- should match total number of tuples for a valid dependency

-- month -> discount : returns 427 tuples, 13 groups = VALID DEPENDENCY
	-- Thus, trivially any dependency including month implies discount
	-- i.e. month, name, price -> discount // month, price -> discount
SELECT  SUM(sub.tuples) AS tuples, COUNT(sub.tuples) AS groups FROM (
	SELECT COUNT(S.discount) AS tuples
	FROM Sales S
	GROUP BY S.month -- A
	HAVING COUNT(DISTINCT S.discount) == 1 -- B
) AS sub;

-- name -> price : returns 427 tuples, 37 groups = VALID DEPENDENCY
SELECT  SUM(sub.tuples) AS tuples, COUNT(sub.tuples) AS groups FROM (
	SELECT COUNT(S.price) AS tuples
	FROM Sales S
	GROUP BY S.name -- A
	HAVING COUNT(DISTINCT S.price) == 1 -- B
) AS sub;


-- name, price -> discount : returns 1 tuple, 1 group = NOT valid dependency
	-- from this we can also conclude that name -> discount and price -> discount
	-- are also invalid dependencies (as name,price -> discount is a more strict condition, but 
	-- is also invalid)
SELECT  SUM(sub.tuples) AS tuples, COUNT(sub.tuples) AS groups FROM (
	SELECT COUNT(S.discount) AS tuples
	FROM Sales S
	GROUP BY S.name, S.price -- A
	HAVING COUNT(DISTINCT S.discount) == 1 -- B
) AS sub;


-- month, discount -> price : returns 1 tuple, 1 group = NOT valid dependency
	-- from this we can also conclude that month -> price and discount -> price
	-- are also invalid dependencies (as month,discount -> price is more strict but is also invalid)
SELECT  SUM(sub.tuples) AS tuples, COUNT(sub.tuples) AS groups FROM (
	SELECT COUNT(S.price) AS tuples
	FROM Sales S
	GROUP BY S.month, S.discount -- A
	HAVING COUNT(DISTINCT S.price) == 1 -- B
) AS sub;


-- month, price, discount -> name : returns 2 tuples, 2 groups = NOT VALID DEPENDENCY
-- Thus, any combination of month, price, and discount is also not a valid dependency i.e.
	-- month -> name //  price -> name // month, discount -> name are NOT valid dependencies either
SELECT  SUM(sub.tuples) AS tuples, COUNT(sub.tuples) AS groups FROM (
	SELECT COUNT(S.name) AS tuples
	FROM Sales S
	GROUP BY S.month, S.price, S.discount -- A
	HAVING COUNT(DISTINCT S.name) == 1 -- B
) AS sub;

-- name, price, discount -> month : returns 1 tuple, 1 group = NOT VALID DEPENDENCY
-- Thus, any combination of name, price, and discount is also not a valid dependency i.e.
	-- name -> month //  price -> month // name, discount -> month are NOT valid dependencies either
SELECT  SUM(sub.tuples) AS tuples, COUNT(sub.tuples) AS groups FROM (
	SELECT COUNT(S.month) AS tuples
	FROM Sales S
	GROUP BY S.name, S.price, S.discount -- A
	HAVING COUNT(DISTINCT S.month) == 1 -- B
) AS sub;


-- Also, as we can see that as nothing implies either name or month this implies we do not have to
-- check any other dependencies including name/month on the right hand side. i.e.
	-- price, discount -> name, month
	-- price, name -> discount, month
	-- price, month -> name, discount
-- are all clearly invalid dependencies

-- At this point, we have checked all possible dependencies A -> B where B is just a single attribute. 
-- We have seen that nothing implies name or month, and nothing implies price or discount besides name or
-- month respectively. However, this is sufficient to check all dependencies as we can construct any 
-- valid larger dependencies from these smaller ones.

-- Combining our known dependencies, we can see our full list of nontrivial dependencies must be:
	-- month -> discount
	-- name -> price
	-- month, name -> price, discount
	-- month, anything else -> discount
	-- name, anything else -> price






-- part 2.3)

--Sales(name, month, price, discount); name -> price, month -> discount

-- Choose the first dependency: name -> price
-- {name}+ = {name, price} is not complete, so name is not a superkey
-- even though name -> price is a nontrivial dependency. Thus, decompose
-- Sales on this dependency.
-- Y = {name, price} \ {name} = {price}
-- Z = {all attributes} \ {name, price} = {month, discount}

-- Decompose sales into Y U {name} = P(name, price) and
-- Z U {name} = Q(name, month, discount).

-- The only dependency on P is name -> price, which clearly implies name is
-- superkey. Thus, P is in BCNF.

-- For Q, consider the dependency month -> discount
-- {month}+ = {month, discount} is NOT all of Q; thus month is not a superkey
-- despite having a nontrivial dependency. Thus, decompose Q on this dependency
-- Y = {month, discount} \ {month} = {discount}
-- Z = {all attributes} \ {month, discount} = {name}

-- decompose Q into Y U {month} = S(month, discount)
-- and Z U {month} = T(month, name).
-- The only dependency on S is month -> discount so month is clearly a superkey.
-- Thus S is in BCNF.
-- T has no nontrivial dependencies so T is in BCNF

-- Thus, Sales(name, month, price, discount) decomposes into

-- P(name, price), S(month, discount), T(month, name)
-- rename these P = Prices, S = Discounts, T = Names


CREATE TABLE Prices(
	name varchar(50),
	price int,

	PRIMARY KEY (name)
);

CREATE TABLE Discounts(
	month varchar(3),
	discount varchar(3),
	
	PRIMARY KEY (month)
);

CREATE TABLE Names(
	name varchar(50),
	month varchar(3),
	PRIMARY KEY (name, month),
	FOREIGN KEY (name) REFERENCES Prices(name),
	FOREIGN KEY (month) REFERENCES Discounts(month)
);




-- part 1.4


INSERT INTO Prices (name, price)
SELECT DISTINCT S.name as name, S.price as price
FROM Sales S;

INSERT INTO Discounts (month, discount)
SELECT DISTINCT S.month, S.discount
FROM Sales S;

INSERT INTO Names (name, month)
SELECT DISTINCT S.name, S.month
FROM Sales S;




-- there are 37 tuples in Prices
SELECT COUNT(*) FROM Prices;

-- there are 427 tuples in Names
SELECT COUNT(*) FROM Names; 

-- there are 13 tuples in Discounts
SELECT COUNT(*) FROM Discounts;
