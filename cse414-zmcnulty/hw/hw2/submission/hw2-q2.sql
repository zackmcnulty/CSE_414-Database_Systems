SELECT C.name AS name, 
	F1.flight_num AS f1_flight_num, 
	F1.origin_city AS f1_origin_city,
	F1.dest_city AS f1_dest_city,
	F1.actual_time AS f1_actual_time,
	F2.flight_num AS f2_flight_num,
	F2.origin_city AS f2_origin_city,
	F2.dest_city AS f2_dest_city,
	F2.actual_time AS f2_actual_time,
	F1.actual_time + F2.actual_time AS actual_time

FROM FLIGHTS AS F1, FLIGHTS AS F2, MONTHS AS M, CARRIERS AS C
WHERE F1.origin_city = "Seattle WA" AND
	F2.dest_city = "Boston MA" AND
	M.month = "July" AND -- Flights are in July
	F1.day_of_month = 15 AND -- flights are on the 15th
	F1.actual_time + F2.actual_time < 7*60 AND --flight time is < 7 hours (in minutes)
	-- join predicates	
	F1.dest_city = F2.origin_city AND -- flights have compatible layover city
	F1.carrier_id = F2.carrier_id AND -- flights are on same airline
	F1.day_of_month = F2.day_of_month AND -- flights are on same day
	F1.month_id = F2.month_id AND --flights are during same month
	F1.month_id = M.mid AND
	F1.carrier_id = C.cid;

