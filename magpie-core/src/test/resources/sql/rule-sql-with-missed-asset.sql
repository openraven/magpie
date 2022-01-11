SELECT arn
  FROM magpie.awscloudtrail cloudtrail
  WHERE resourcetype = 'AWS::CloudTrail::Trail'
    AND NOT EXISTS
      (SELECT *
       FROM magpie.awss3bucket s3bucket
       WHERE resource_type = 'AWS::S3::Bucket'
         AND s3bucket.resource_name = cloudtrail.supplementaryconfiguration->'trailDetails'->'trail'->>'s3BucketName'
         AND s3bucket.supplementaryconfiguration->'bucketLoggingConfiguration'->'loggingEnabled' != 'null');
`
