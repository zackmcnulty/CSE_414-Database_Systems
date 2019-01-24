SELECT DISTINCT C.name AS carrier
FROM CARRIERS AS C, (
	SELECT *
	FROM FLIGHTS AS F
	WHERE F.origin_city LIKE '%Seattle%' AND
		F.dest_city LIKE '%San Francisco%'
) AS sub
WHERE sub.carrier_id = C.cid
