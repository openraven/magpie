SELECT t.resource_id,
       t.region,
       t.configuration ->> 'subnetId' as subnet_id,
       t.configuration ->> 'privateIpAddress' as private_ip_address,
       arr.group as security_group
FROM assets t, LATERAL (
   SELECT string_agg(value::jsonb ->> 'groupId', ',') as group
   FROM   jsonb_array_elements_text(t.configuration->'securityGroups')
   ) arr
WHERE t.resource_type = 'AWS::EC2::Instance'
  AND t.configuration -> 'state' ->> 'name' = 'running'
