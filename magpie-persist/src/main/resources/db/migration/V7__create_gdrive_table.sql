CREATE TABLE IF NOT EXISTS gdrive (
                                      documentid TEXT primary key not null,
                                      assetid TEXT,
                                      resourcename TEXT,
                                      resourceid TEXT,
                                      resourcetype TEXT,
                                      drive TEXT,
                                      domain TEXT,
                                      creatediso TIMESTAMPTZ,
                                      updatediso TIMESTAMPTZ,
                                      discoverysessionid TEXT,
                                      tags JSONB,
                                      configuration JSONB,
                                      supplementaryconfiguration JSONB,
                                      discoverymeta JSONB
);
CREATE INDEX IF NOT EXISTS gdrive_resource_type ON gdrive (resourcetype);
