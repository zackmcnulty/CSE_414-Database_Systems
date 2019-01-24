SELECT DISTINCT F1.dest_city AS city
FROM (SELECT DISTINCT F.dest_city AS dest_city FROM FLIGHTS AS F WHERE F.dest_city NOT LIKE '%Seattle%') AS F1
WHERE
F1.dest_city NOT IN ( -- a direct flight 
    SELECT F2.dest_city
    FROM FLIGHTS F2 
    WHERE  F2.origin_city LIKE '%Seattle%')  
 AND F1.dest_city NOT IN ( -- a nondirect flight from seattle to here
    SELECT F4.dest_city    
    FROM FLIGHTS AS F3, FLIGHTS AS F4
    WHERE F3.origin_city LIKE '%Seattle%' AND 
        F3.dest_city = F4.origin_city ) 
