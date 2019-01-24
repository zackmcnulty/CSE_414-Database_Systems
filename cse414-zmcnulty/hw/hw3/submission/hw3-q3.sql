SELECT F1.origin_city AS origin_city,  
		(100.0*sub.less_than_3/ count(*)) AS percentage --count(*) counts the null values 
FROM FLIGHTS AS F1 LEFT OUTER JOIN ( --outer join handles case where we have no flights < 3 hours
         SELECT F2.origin_city AS origin_city, 
		count(*) AS less_than_3
         FROM FLIGHTS AS F2
         WHERE F2.actual_time < 3*60
         GROUP BY F2.origin_city
        ) AS sub
ON F1.origin_city = sub.origin_city --join predicate
GROUP BY F1.origin_city, sub.less_than_3
ORDER BY percentage
