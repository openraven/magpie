SELECT asset_id
  FROM magpie.assets cloudtrail
  WHERE resource_type = 'AWS::CloudTrail::Trail'
    AND NOT EXISTS
      (SELECT *
       FROM magpie.assets s3bucket
       WHERE resource_type = 'AWS::S3::Bucket'
         AND s3bucket.resource_name = cloudtrail.supplementary_configuration->'trailDetails'->'trail'->>'s3BucketName'
         AND s3bucket.supplementary_configuration->'bucketLoggingConfiguration'->'loggingEnabled' != 'null');
