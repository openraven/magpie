SELECT t.resourceid,
       t.awsregion,
       t.configuration ->> 'subnetId' as subnet_id,
       t.configuration ->> 'privateIpAddress' as private_ip_address,
       arr.group as security_group
FROM magpie.aws t, LATERAL (
   SELECT string_agg(cast(value as jsonb) ->> 'groupId', ',') as group
   FROM   jsonb_array_elements_text(t.configuration->'securityGroups')
   ) arr
WHERE t.resourcetype = 'AWS::EC2::Instance'
  AND t.configuration -> 'state' ->> 'name' = 'running'
