# Magpie
#### [Open Raven's](https://openraven.com) Open Source Cloud Security Framework 


## What is Magpie?
Magpie is a free, open-source framework and a collection of community developed plugins that can be used to build complete end-to-end security tools such as a CSPM or Cloud Security Posture Manager. The project was originally created and is maintained by Open Raven. We build commercial cloud native data security tools and in doing so have learned a great deal about how to discover AWS assets and their security settings at scale.

We also heard that many people were frustrated with their existing security tools that couldn't be extended  and couldn't work well with their other systems, so decided to create this Magpie framework and refactor and sync our core AWS commercial discovery code as the first plugin.

We plan to actively contribute additional modules to make Magpie a credible free open source alternative to commercial CSPM’s and welcome the community to join us in adding to the framework and building plugins.

## Overview

### Magpie Architecture
Magpie relies on plugins for all its integration capabilities.  They are the core of the framework and key to integration
with both cloud providers and downstream processing and storage.

*Magpie is essentially a series of layers separated by FIFOs.*

Depending on the configuration, these FIFOs are either **1) Java queues** (in the default configuratgion) or
**2) Kafka queues**.  Using Kafka queues allows Magpie to run in a distributed and highly scalable fashion where
each layer may exist on separate compute instances.

![Magpie Architecture](https://raw.githubusercontent.com/openraven/magpie-api/main/media/magpie_architecture.png?token=AAHX2PKUJYSKWMDS333MPSTALXTGC)

## Building Magpie

### Clone and build Magpie
```shell
git clone git@github.com:openraven/magpie.git
cd magpie
mvn clean package && mvn --projects magpie-cli assembly:single
```
git clone git@github.com:openraven/magpie.git
cd magpie
mvn clean package && mvn --projects magpie-cli assembly:single

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
### Plugins

### Community Contributed Plugins

If you've written a plugin you'd like listed please create a Pull Request with it listed here.

### Cloud Provider Status

#### AWS
Magpie supports AWS as a core plugin out of the box. Checked boxes are complete and available today, the unchecked are on the roadmap for completion. We have already built the code for all services in the list, but need to port them over from a previous framework. 

- [x] EC2
- [x] S3
- [ ] Athena
- [ ] Backup
- [ ] Batch
- [ ] Cassandra
- [ ] Cloudfront
- [ ] Cloudsearch
- [ ] DynamoDB
- [ ] EB
- [x] ECS
- [ ] EFS
- [ ] EKS
- [ ] Elastic Cache
- [ ] ELB
- [ ] ELBv2
- [ ] EMR
- [ ] ESS
- [ ] FSX
- [ ] Glacier
- [x] KMS
- [ ] Lakeformation
- [ ] Lambda
- [ ] Lightsail
- [ ] QLDB
- [x] RDS
- [ ] Redshift
- [ ] Route 53
- [ ] Secrets Manager
- [ ] Storage Gateway
- [x] VPC



