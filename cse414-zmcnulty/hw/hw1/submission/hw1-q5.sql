-- Returns the name and distance of all restaurants no more than 20 minutes away,
-- sorting them alphabetically by restaurant name
SELECT Name, Distance FROM MyRestaurants WHERE Distance <= 20 ORDER BY Name;
