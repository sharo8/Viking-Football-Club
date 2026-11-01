UPDATE staff_members SET listed_in = 'ADMINISTRATION'
WHERE UPPER(full_name) LIKE '%HABIMANA%'
   OR UPPER(full_name) LIKE '%MATABARO%';
