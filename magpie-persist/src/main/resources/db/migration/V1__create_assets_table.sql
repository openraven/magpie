-- Generic resource table for various cloud provider assets
CREATE TABLE IF NOT EXISTS magpie.assets (
    document_id varchar(59) primary key,
    asset_id varchar(255),
    resource_name varchar(255),
    resource_id varchar(255),
    resource_type varchar(255),
    region varchar(50),
    project_id varchar(255),
    account_id varchar(255),
    created_iso timestamp with time zone,
    updated_iso timestamp with time zone,
    discovery_session_id varchar(255),
    max_size_in_bytes integer,
    size_in_bytes integer,
    configuration jsonb,
    supplementary_configuration jsonb,
    tags jsonb,
    discovery_meta jsonb
);
