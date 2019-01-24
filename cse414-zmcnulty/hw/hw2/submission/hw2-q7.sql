SELECT SUM(F.capacity) AS capacity 
FROM FLIGHTS AS F, MONTHS AS M
WHERE F.day_of_month = 10 AND
	F.origin_city = "Seattle WA" AND
	F.dest_city = "San Francisco CA" AND
	M.month = "July" AND
	--join predicates
	F.month_id = M.mid;
