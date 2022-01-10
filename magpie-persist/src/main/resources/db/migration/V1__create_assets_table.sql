CREATE TABLE IF NOT EXISTS magpie.assets (
    documentid TEXT not null,
    resourcetype TEXT,
    primary key (documentid)
);

CREATE TABLE IF NOT EXISTS magpie.aws(
    arn TEXT,
    awsaccountid TEXT,
    awsregion TEXT,
    configuration JSONB,
    creatediso TIMESTAMPTZ,
    discoverymeta JSONB,
    discoverysessionid TEXT,
    resourceid TEXT,
    resourcename TEXT,
    supplementaryconfiguration JSONB,
    tags JSONB,
    updatediso TIMESTAMPTZ
) INHERITS(magpie.assets);

CREATE TABLE IF NOT EXISTS magpie.gcp (
    assetid TEXT,
    configuration JSONB,
    creatediso TIMESTAMPTZ,
    discoverymeta JSONB,
    discoverysessionid TEXT,
    gcpaccountid TEXT,
    projectid TEXT,
    resourceid TEXT,
    resourcename TEXT,
    supplementaryconfiguration JSONB,
    tags JSONB,
    updatediso TIMESTAMPTZ
) INHERITS(magpie.assets);
