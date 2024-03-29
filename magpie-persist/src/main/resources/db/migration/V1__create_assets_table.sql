CREATE TABLE IF NOT EXISTS aws(
    documentid TEXT primary key not null,
    arn TEXT,
    resourcename TEXT,
    resourceid TEXT,
    resourcetype TEXT,
    awsregion TEXT,
    awsaccountid TEXT,
    creatediso TIMESTAMPTZ,
    updatediso TIMESTAMPTZ,
    discoverysessionid TEXT,
    tags JSONB,
    configuration JSONB,
    supplementaryconfiguration JSONB,
    discoverymeta JSONB
);
CREATE INDEX IF NOT EXISTS aws_resource_type ON aws (resourcetype);

CREATE TABLE IF NOT EXISTS gcp (
    documentid TEXT primary key not null,
    assetid TEXT,
    resourcename TEXT,
    resourceid TEXT,
    resourcetype TEXT,
    region TEXT,
    gcpaccountid TEXT,
    projectid TEXT,
    creatediso TIMESTAMPTZ,
    updatediso TIMESTAMPTZ,
    discoverysessionid TEXT,
    tags JSONB,
    configuration JSONB,
    supplementaryconfiguration JSONB,
    discoverymeta JSONB
);
CREATE INDEX IF NOT EXISTS gcp_resource_type ON gcp (resourcetype);
