CREATE TABLE IF NOT EXISTS azure (
    documentid TEXT primary key not null,
    resourceid TEXT,
    resourcename TEXT,
    resourcetype TEXT,
    region TEXT,
    subscriptionid TEXT,
    containingEntity TEXT,
    containingEntityid TEXT,
    projectid TEXT,
    creatediso TIMESTAMPTZ,
    updatediso TIMESTAMPTZ,
    discoverysessionid TEXT,
    tags JSONB,
    configuration JSONB,
    supplementaryconfiguration JSONB,
    discoverymeta JSONB
);
CREATE INDEX IF NOT EXISTS azure_resource_type ON azure (resourcetype);

create table azuresqldatabase () INHERITS (azure);
create table azuresqlserver () INHERITS (azure);
create table azurestorageaccount () INHERITS (azure);
create table azurestorageblobcontainer () INHERITS (azure);
create table azuresubscription () INHERITS (azure);
