# Magpie
#### [Open Raven's](https://openraven.com) Open Source Cloud Security Framework

## Table of Contents
- [What is Magpie?](#what-is-magpie-)
- [Overview](#overview)
    * [Magpie Architecture](#magpie-architecture)
- [Building Magpie](#building-magpie)
    * [Clone and build Magpie](#clone-and-build-magpie)
- [Running Magpie](#running-magpie)
    * [Configuration](#configuration)
        + [Overriding config.yaml](#overriding-configyaml)
        + [Multiple Overrides](#multiple-overrides)
    * [Running via Docker](#running-via-docker)
    * [Plugins](#plugins)
    * [Community Contributed Plugins](#community-contributed-plugins)
    * [Cloud Provider Status](#cloud-provider-status)
        + [AWS](#aws)
            - [Per region discovery](#per-region-discovery)


## What is Magpie?
Magpie is a free, open-source framework and a collection of community developed plugins that can be used to build complete end-to-end security tools such as a CSPM or Cloud Security Posture Manager. The project was originally created and is maintained by Open Raven. We build commercial cloud native data security tools and in doing so have learned a great deal about how to discover AWS assets and their security settings at scale.

We also heard that many people were frustrated with their existing security tools that couldn't be extended  and couldn't work well with their other systems, so decided to create this Magpie framework and refactor and sync our core AWS commercial discovery code as the first plugin.

We plan to actively contribute additional modules to make Magpie a credible free open source alternative to commercial CSPM’s and welcome the community to join us in adding to the framework and building plugins.

## Overview

### Magpie Architecture
Magpie relies on plugins for all its integration capabilities.  They are the core of the framework and key to integration
with both cloud providers and downstream processing and storage.

*Magpie is essentially a series of layers separated by FIFOs.*

Depending on the configuration, these FIFOs are either **1) Java queues** (in the default configuration) or
**2) Kafka queues**.  Using Kafka queues allows Magpie to run in a distributed and highly scalable fashion where
each layer may exist on separate compute instances.

![Magpie Architecture](https://raw.githubusercontent.com/openraven/magpie-api/main/media/magpie_architecture.png?token=AAHX2PKUJYSKWMDS333MPSTALXTGC)

## Building Magpie

### Clone and build Magpie
```shell
git clone git@github.com:openraven/magpie.git
cd magpie
mvn clean package install && mvn --projects magpie-cli assembly:single
```

The distribution zip file will be located in `magpie-cli/target/magpie-<version>.zip`

Alternatively you can download the latest snapshot build by going to Action->(choose latest) and click the `magpie-cli` artifact,
which will download a zip distribution.  

## Running Magpie

*Java 11 is a prerequisite and must be installed to run Magpie.*

Out of the box Magpie supports AWS for the cloud provider and outputs discovery data to `stdout` in JSON format. The
AWS plugin utilizes the AWS Java SDK and will search for credentials as described in [Using Credentials](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html).

Assuming you have read credentials set up, you can start discovery by running:
```shell
./magpie
```

### Configuration
Magpie allows for complex configurations to be enabled via the YAML-based config file.  This file has 3 primary sections:

- **Layers**: each of which contain 1 or more plugins and are surrounded by at least 1 FIFO
- **FIFOs**: which are either local (in-process Java queues) or Kafka queues
- **Plugins**: Each running plugin must be explictly.  A plugin-specific configuration object may reside in the `config` subsection.


The simplest Magpie configuration is shown below. This configuration enables AWS discovery with a JSON output to `stdout`. To write to a file
simply redirect the output:
```shell
./magpie > output.json
```
Log messages are printed to `stderr` and will still show up as console output.

The simplest configuration:
```yaml
layers:
  enumerate:
    type: origin
    queue: default
    plugins:
      - magpie.aws.discovery
  output:
    type: terminal
    dequeue: default
    plugins:
      - magpie.json
fifos:
  default:
    type: local
plugins:
  magpie.aws.discovery:
    enabled: true
    config:
  magpie.json:
    enabled: true
    config:
```

#### Overriding config.yaml
It is possible to override *most* configuration values via environmental variables. This is most useful as an easy way to
script a Magpie instance on a one-per-aws-service basis.  To override configuration values, set an environmental variable
named `MAGPIE_CONFIG` and with a specially formed JSON object or array. For example, to perform an S3 *only* scan using
with the default configuration:

```bash
> MAGPIE_CONFIG="{'/plugins/magpie.aws.discovery/config/services': ['s3']}" ./magpie
```
The value of `MAGPIE_CONFIG` must be a JSON object where the key is a [JSON Pointer](https://tools.ietf.org/html/rfc6901)
and the value is legal JSON which should be inserted into the location referenced by the pointer.

In the case where multiple overrides are required you may instead use an array of the above formatted objects as such:
```bash
> MAGPIE_CONFIG="[{'/plugins/magpie.aws.discovery/enabled', false }, {'/plugins/magpie.aws.discovery/config/services': ['s3']}]" ./magpie
```

#### Multiple Overrides
If you have multiple values to set it may be easier to set multiple override variables instead of attempting to fit it
in a single env var.  Magpie will accept any and all environmental variables that match the regex `MAGPIE_CONFIG.*`. They
will be applied in Java's natural String ordering (lexicographic).  For example:
```bash
> export MAGPIE_CONFIG_1="[...]"
> export MAGPIE_CONFIG_2="[...]"
> ./magpie
```
Both variables will be applied, if any duplicate JSON Pointers are provided the last one applied will win.


### Running via Docker
Builds on the `main` branch are automatically uploaded to quay.io under the `latest` tag.  See https://quay.io/repository/openraven/magpie for
all available tags. Once a stable public release is made this will also be available there.  The Docker image uses the
default config and provides no AWS credentials.

- Credentials can be added at runtime via volume mapping or passing environmental variables into the container.
- Configuration overrides can be done via environmental variable.

For example: to pass environmental variables for both credentials and configuration:
```bash
docker run -a stdout -a stderr \
--env MAGPIE_CONFIG="{'/plugins/magpie.aws.discovery/config/services': ['s3']}" \
-e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_SESSION_TOKEN \
quay.io/openraven/magpie:latest
```

The two `-a` arguments map both `stdout` and `stderr` to your local terminals, so you can redirect output as you would
with a local Mapgie execution.

### Plugins

### Community Contributed Plugins

If you've written a plugin you'd like listed please create a Pull Request with it listed here.

### Cloud Provider Status

#### AWS
Magpie supports AWS as a core plugin out of the box. Checked boxes are complete and available today, the unchecked are on the roadmap for completion. We have already built the code for all services in the list, but need to port them over from a previous framework.

- [x] EC2
- [x] S3
- [x] Athena
- [x] Batch
- [x] Backup
- [x] Cassandra
- [x] Cloudfront
- [x] Cloudsearch
- [x] Cloudtrail
- [x] CloudWatch
- [x] DynamoDB
- [x] EB
- [x] ECS
- [x] EFS
- [x] EKS
- [x] Elastic Cache
- [x] ELB
- [x] ELBv2
- [x] EMR
- [x] ESS
- [x] FSX
- [x] Glacier
- [x] IAM
- [x] KMS
- [x] Lakeformation
- [x] Lambda
- [x] Lightsail
- [x] QLDB
- [x] RDS
- [x] Redshift
- [x] Route 53
- [x] Secrets Manager
- [x] SNS
- [x] Storage Gateway
- [x] VPC

##### Per region discovery
By default the Magpie AWS Plugin will run discovery in all regions.  To narrow down discovery to a subset
of regions edit the plugins.magpie.aws.discovery.config.regions value to an array of desired region names, for example:

```yaml
plugins:
  magpie.aws.discovery:
    enabled: true
    config:
      regions:
        - us-east-2
        - us-east-1
```

