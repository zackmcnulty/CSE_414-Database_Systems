SELECT DISTINCT C.name as name
FROM FLIGHTS AS F, CARRIERS AS C
WHERE
-- join predicates
F.carrier_id = C.cid
GROUP BY F.carrier_id, F.month_id, F.day_of_month
HAVING count(*) > 1000;
