SELECT DISTINCT F1.origin_city AS origin_city, 
		F1.dest_city AS dest_city,
		F1.actual_time AS time 
FROM FLIGHTS AS F1, 
(SELECT F2.origin_city AS origin_city, MAX(F2.actual_time) AS max_time
FROM FLIGHTS AS F2
GROUP BY F2.origin_city
) AS sub

WHERE F1.origin_city = sub.origin_city AND
	F1.actual_time = sub.max_time 
ORDER BY origin_city, dest_city
