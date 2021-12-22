SELECT asset_id
  FROM magpie.assets
  WHERE resource_type = 'AWS::IAM::CredentialsReport'
    AND ((configuration->>'access_key_1_active' = 'true'
          AND (configuration->>'access_key_1_last_used_date' IS NULL
               OR configuration->>'access_key_1_last_used_date' = 'N/A'))
         OR (configuration->>'access_key_2_active' = 'true'
             AND (configuration->>'access_key_2_last_used_date' IS NULL
                  OR configuration->>'access_key_2_last_used_date' = 'N/A')));
