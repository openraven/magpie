ALTER TABLE magpie.awss3bucket ADD CONSTRAINT magpie_unique_s3_bucketname UNIQUE (arn);
