SELECT W.day_of_week AS day_of_week,
	AVG(F.arrival_delay) AS delay
FROM FLIGHTS AS F, WEEKDAYS AS W
WHERE F.day_of_week_id = W.did
GROUP BY W.day_of_week
ORDER BY delay DESC
LIMIT 1;
