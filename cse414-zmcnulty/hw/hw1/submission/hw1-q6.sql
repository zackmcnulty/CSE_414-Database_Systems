-- Returns all restaurants that I liked, but have not visted within the last three months
SELECT * FROM MyRestaurants WHERE Date_last_visit < date('now', '-3 month') and Liked;
