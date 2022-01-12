CREATE TABLE IF NOT EXISTS magpie.aws(
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
CREATE INDEX IF NOT EXISTS aws_resource_type ON magpie.aws (resourcetype);

CREATE TABLE IF NOT EXISTS magpie.gcp (
    documentid TEXT primary key not null,
    assetid TEXT,
    resourcename TEXT,
    resourceid TEXT,
    resourcetype TEXT,
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
CREATE INDEX IF NOT EXISTS gcp_resource_type ON magpie.gcp (resourcetype);
