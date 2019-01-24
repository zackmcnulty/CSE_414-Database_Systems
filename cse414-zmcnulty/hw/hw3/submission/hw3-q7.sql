SELECT DISTINCT C.name AS carrier
FROM FLIGHTS AS F, CARRIERS AS C 
WHERE F.carrier_id = C.cid AND
	F.origin_city LIKE '%Seattle%' AND
	F.dest_city LIKE '%San Francisco%'
