CREATE TABLE IF NOT EXISTS magpie.aws (
    documentid TEXT not null,
    arn TEXT,
    awsaccountid TEXT,
    awsregion TEXT,
    configuration JSONB,
    creatediso TIMESTAMPTZ,
    discoverymeta JSONB,
    discoverysessionid TEXT,
    resourceid TEXT,
    resourcename TEXT,
    resourcetype TEXT,
    supplementaryconfiguration JSONB,
    tags JSONB,
    updatediso TIMESTAMPTZ,
    primary key (documentid)
);

CREATE TABLE IF NOT EXISTS magpie.gcp (
    documentid TEXT not null,
    assetid TEXT,
    configuration JSONB,
    creatediso TIMESTAMPTZ,
    discoverymeta JSONB,
    discoverysessionid TEXT,
    gcpaccountid TEXT,
    projectid TEXT,
    resourceid TEXT,
    resourcename TEXT,
    resourcetype TEXT,
    supplementaryconfiguration JSONB,
    tags JSONB,
    updatediso TIMESTAMPTZ,
    primary key (documentid)
);
