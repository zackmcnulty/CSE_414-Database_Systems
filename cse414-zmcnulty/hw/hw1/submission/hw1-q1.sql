-- Format the output of SQL Lite to be more readable
.header on
.mode column
.nullvalue NULL


-- Creates the edges table
CREATE TABLE Edges(Source int, Destination int);

--Adds the given values to the edges table
INSERT INTO Edges values(10,5);
INSERT INTO Edges values(6,25);
INSERT INTO Edges values(1,3);
INSERT INTO Edges values(4,4);


-- Returns all tuples from the edges table
SELECT * FROM Edges; 

-- Returns the Source column from the Edges table
SELECT Source FROM Edges;

-- Returns only the tuples where Source > Destination
SELECT * FROM Edges WHERE Source > Destination;

-- Insert the value -1;
-- No error is thrown because SQL lite uses type affinity, and
-- will automatically attempt to cast the given data ('-1','2000') which
-- are strings, into the desired type (int). In this case, the casting is
-- successful. 
INSERT INTO Edges VALUES('-1','2000');

-- However, in SQL lite this desired type is not actually required. If the casting
-- fails, the data will still be placed in the table without throwing an error.
-- The below insert provides an example.
-- INSERT INTO Edges VALUES("hello", "world");
