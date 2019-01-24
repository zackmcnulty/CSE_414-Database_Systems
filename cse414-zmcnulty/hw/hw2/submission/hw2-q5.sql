SELECT C.name as name, (100.0*SUM(F.canceled) / COUNT(F.canceled)) AS percent
FROM FLIGHTS AS F, CARRIERS AS C
WHERE
F.origin_city = "Seattle WA" AND
--join predicates
F.carrier_id = C.cid
GROUP BY name
HAVING percent > 0.5
ORDER BY percent ASC;
