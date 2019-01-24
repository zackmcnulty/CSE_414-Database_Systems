SELECT DISTINCT F1.dest_city AS  city
FROM FLIGHTS AS F1
WHERE EXISTS ( -- a flight that can get there from seattle in one stop
    SELECT * 
    FROM FLIGHTS F2 
    WHERE F1.origin_city = F2.dest_city 
        AND F2.origin_city LIKE '%Seattle%')                              
AND NOT EXISTS ( -- a direct flight from seattle to here
    SELECT * 
    FROM FLIGHTS F3 
    WHERE F3.dest_city = F1.dest_city 
        AND F3.origin_city LIKE '%Seattle%')      
AND NOT F1.dest_city LIKE '%Seattle%'
