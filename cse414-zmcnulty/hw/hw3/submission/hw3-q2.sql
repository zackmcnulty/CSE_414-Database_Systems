   SELECT DISTINCT F.origin_city AS city 
   FROM FLIGHTS AS F
   GROUP BY F.origin_city
   HAVING MAX(F.actual_time) < 3*60 -- all flights are < 3 hours, ignoring NULLs
   ORDER BY F.origin_city
