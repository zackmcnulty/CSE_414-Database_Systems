SELECT C.name AS carrier, MAX(F.price) AS max_price
FROM FLIGHTS AS F, CARRIERS AS C
WHERE F.origin_city = "Seattle WA" AND
	F.dest_city = "New York NY" AND
	-- join predicates
	F.carrier_id = C.cid
GROUP BY carrier;
