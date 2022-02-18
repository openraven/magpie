# Magpie
## [Open Raven's](https://openraven.com) Open Source Cloud Security Framework 

# What is Magpie?
Magpie is a free, open-source framework and a collection of community developed plugins that can be used to build complete end-to-end security tools such as a CSPM or Cloud Security Posture Manager. The project was originally created and is maintained by Open Raven. We build commercial cloud native data security tools and in doing so have learned a great deal about how to discover AWS assets and their security settings at scale.

We also heard that many people were frustrated with their existing security tools that couldn't be extended  and couldn't work well with their other systems, so decided to create this Magpie framework and refactor and sync our core AWS commercial discovery code as the first plugin.

We plan to actively contribute additional modules to make Magpie a credible free open source alternative to commercial CSPM’s and welcome the community to join us in adding to the framework and building plugins.

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

Out of the box Magpie supports AWS and GCP and outputs discovery data to `stdout` in JSON format. The
AWS plugin utilizes the AWS Java SDK and will search for credentials as described in [Using Credentials](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html).

Assuming you have read credentials set up, you can start discovery by running:
```shell
./magpie-discovery
```

Out of the box Magpie does not persist discovered assets, instead writing them as JSON objects to stdout.

With persistence set up (via config.yaml), you can run the policy analyzer against persisted assets:
```shell
./magpie-policy
```
All help and support via Slack https://join.slack.com/t/open-raven-research/shared_invite/zt-np27xiev-N5rL4AcTmrQt8YkE81BIaw
