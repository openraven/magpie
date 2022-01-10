-- AWS TYPES:
create table magpie.awsaccount () INHERITS (magpie.aws);
create table magpie.awsathenadatacatalog () INHERITS (magpie.aws);
create table magpie.awsbackupbackupplan () INHERITS (magpie.aws);
create table magpie.awsbackupbackupvault () INHERITS (magpie.aws);
create table magpie.awsbatchcomputeenvironment () INHERITS (magpie.aws);
create table magpie.awsbatchjobdefinition () INHERITS (magpie.aws);
create table magpie.awsbatchjobqueue () INHERITS (magpie.aws);
create table magpie.awscassandrakeyspace () INHERITS (magpie.aws);
create table magpie.awscloudfrontdistribution () INHERITS (magpie.aws);
create table magpie.awscloudsearchdomain () INHERITS (magpie.aws);
create table magpie.awscloudtrailtrail () INHERITS (magpie.aws);
create table magpie.awsconfigurationrecorder () INHERITS (magpie.aws);
create table magpie.awsdynamodbglobaltable () INHERITS (magpie.aws);
create table magpie.awsdynamodbtable () INHERITS (magpie.aws);
create table magpie.awsec2eip () INHERITS (magpie.aws);
create table magpie.awsec2instance () INHERITS (magpie.aws);
create table magpie.awsec2networkacl () INHERITS (magpie.aws);
create table magpie.awsec2networkinterface () INHERITS (magpie.aws);
create table magpie.awsec2securitygroup () INHERITS (magpie.aws);
create table magpie.awsec2snapshot () INHERITS (magpie.aws);
create table magpie.awsec2volume () INHERITS (magpie.aws);
create table magpie.awsec2vpc () INHERITS (magpie.aws);
create table magpie.awsec2vpcpeeringconnection () INHERITS (magpie.aws);
create table magpie.awsecscluster () INHERITS (magpie.aws);
create table magpie.awsefsfilesystem () INHERITS (magpie.aws);
create table magpie.awsguarddutydetector () INHERITS (magpie.aws);
create table magpie.awsekscluster () INHERITS (magpie.aws);
create table magpie.awselasticachecluster () INHERITS (magpie.aws);
create table magpie.awselasticbeanstalk () INHERITS (magpie.aws);
create table magpie.awselasticloadbalancingloadbalancer () INHERITS (magpie.aws);
create table magpie.awselasticloadbalancingv2loadbalancer () INHERITS (magpie.aws);
create table magpie.awselasticsearchdomain () INHERITS (magpie.aws);
create table magpie.awsemrcluster () INHERITS (magpie.aws);
create table magpie.awsfsxfilesystem () INHERITS (magpie.aws);
create table magpie.awsglaciervault () INHERITS (magpie.aws);
create table magpie.awsiamgroup () INHERITS (magpie.aws);
create table magpie.awsiampolicy () INHERITS (magpie.aws);
create table magpie.awsiamrole () INHERITS (magpie.aws);
create table magpie.awsiamuser () INHERITS (magpie.aws);
create table magpie.awsiamaccount () INHERITS (magpie.aws);
create table magpie.awsiamcredentialsreport () INHERITS (magpie.aws);
create table magpie.awskmskey () INHERITS (magpie.aws);
create table magpie.awslakeformationresource () INHERITS (magpie.aws);
create table magpie.awslambdafunction () INHERITS (magpie.aws);
create table magpie.awslightsaildatabase () INHERITS (magpie.aws);
create table magpie.awslightsailinstance () INHERITS (magpie.aws);
create table magpie.awslightsailloadbalancer () INHERITS (magpie.aws);
create table magpie.awslocationgeofencecollection () INHERITS (magpie.aws);
create table magpie.awslocationmap () INHERITS (magpie.aws);
create table magpie.awslocationplaceindex () INHERITS (magpie.aws);
create table magpie.awslocationroutecalculator () INHERITS (magpie.aws);
create table magpie.awslocationtracker () INHERITS (magpie.aws);
create table magpie.awsneptunecluster () INHERITS (magpie.aws);
create table magpie.awsneptuneinstance () INHERITS (magpie.aws);
create table magpie.awsqldbledger () INHERITS (magpie.aws);
create table magpie.awsrdsdbinstance () INHERITS (magpie.aws);
create table magpie.awsrdsdbsnapshot () INHERITS (magpie.aws);
create table magpie.awsredshiftcluster () INHERITS (magpie.aws);
create table magpie.awsregion () INHERITS (magpie.aws);
create table magpie.awsroute53hostedzone () INHERITS (magpie.aws);
create table magpie.awss3bucket () INHERITS (magpie.aws);
create table magpie.awss3bucketobject () INHERITS (magpie.aws);
create table magpie.awssecretsmanager () INHERITS (magpie.aws);
create table magpie.awssecurityhubstandardsubscription () INHERITS (magpie.aws);
create table magpie.awssnssubscription () INHERITS (magpie.aws);
create table magpie.awssnstopic () INHERITS (magpie.aws);
create table magpie.awsstoragegatewaygateway () INHERITS (magpie.aws);
create table magpie.awswatchalarm () INHERITS (magpie.aws);
create table magpie.awswatchdashboard () INHERITS (magpie.aws);
create table magpie.awswatchlogsmetricfilter () INHERITS (magpie.aws);
create table magpie.extendawsiamusercredentialreport () INHERITS (magpie.aws);
create table magpie.extendawsssminstance () INHERITS (magpie.aws);
create table magpie.orvnshadowaccount () INHERITS (magpie.aws);
create table magpie.awswatchloggroup () INHERITS (magpie.aws);

-- GCP TYPES;
create table magpie.gcpbigqueryreservation () INHERITS (gcp);
create table magpie.gcpbigqueryreservationcapacity () INHERITS (gcp);
create table magpie.gcpbigtableinstance () INHERITS (gcp);
create table magpie.gcpaccess () INHERITS (gcp);
create table magpie.gcpasset () INHERITS (gcp);
create table magpie.gcpassetfeed () INHERITS (gcp);
create table magpie.gcpautomldataset () INHERITS (gcp);
create table magpie.gcpautomlmodel () INHERITS (gcp);
create table magpie.gcpbigquerydataset () INHERITS (gcp);
create table magpie.gcpbigquerydatatransfer () INHERITS (gcp);
create table magpie.gcpbillingaccount () INHERITS (gcp);
create table magpie.gcpcloudbuild () INHERITS (gcp);
create table magpie.gcpcloudbuildtrigger () INHERITS (gcp);
create table magpie.gcpcluster () INHERITS (gcp);
create table magpie.gcpcomputedisk () INHERITS (gcp);
create table magpie.gcpcomputeinstance () INHERITS (gcp);
create table magpie.gcpcontaineranalysisnote () INHERITS (gcp);
create table magpie.gcpcontaineranalysisoccurrence () INHERITS (gcp);
create table magpie.gcpdatacatalog () INHERITS (gcp);
create table magpie.gcpdatalabelig () INHERITS (gcp);
create table magpie.gcpdatalabelingannotations () INHERITS (gcp);
create table magpie.gcpdatalabelinginstruction () INHERITS (gcp);
create table magpie.gcpdialogflowconversation () INHERITS (gcp);
create table magpie.gcpdlpjob () INHERITS (gcp);
create table magpie.gcpdlpjobtrigger () INHERITS (gcp);
create table magpie.gcpdnszone () INHERITS (gcp);
create table magpie.gcperrorreporting () INHERITS (gcp);
create table magpie.gcpgameservice () INHERITS (gcp);
create table magpie.gcpiamrole () INHERITS (gcp);
create table magpie.gcpiamserviceaccount () INHERITS (gcp);
create table magpie.gcpiotdeviceregistry () INHERITS (gcp);
create table magpie.gcpkmskeyring () INHERITS (gcp);
create table magpie.gcploggingbucket () INHERITS (gcp);
create table magpie.gcploggingexclusion () INHERITS (gcp);
create table magpie.gcploggingmetric () INHERITS (gcp);
create table magpie.gcploggingsink () INHERITS (gcp);
create table magpie.gcpmemcacheinstance () INHERITS (gcp);
create table magpie.gcpmonitoringalertpolicy () INHERITS (gcp);
create table magpie.gcpmonitoringdashboard () INHERITS (gcp);
create table magpie.gcpmonitoringgroup () INHERITS (gcp);
create table magpie.gcpmonitoringservice () INHERITS (gcp);
create table magpie.gcposconfigpatchdeployment () INHERITS (gcp);
create table magpie.gcposconfigpatchjob () INHERITS (gcp);
create table magpie.gcpprojectinfo () INHERITS (gcp);
create table magpie.gcppubsublitesubscription () INHERITS (gcp);
create table magpie.gcppubsublitetopic () INHERITS (gcp);
create table magpie.gcppubsubschema () INHERITS (gcp);
create table magpie.gcppubsubsnapshots () INHERITS (gcp);
create table magpie.gcppubsubsubscription () INHERITS (gcp);
create table magpie.gcppubsubtopic () INHERITS (gcp);
create table magpie.gcprecaptchaenterprisekey () INHERITS (gcp);
create table magpie.gcpredisinstance () INHERITS (gcp);
create table magpie.gcpresourcemanagerfolder () INHERITS (gcp);
create table magpie.gcpresourcemanagerorganization () INHERITS (gcp);
create table magpie.gcpresourcemanagerproject () INHERITS (gcp);
create table magpie.gcpschedulerjob () INHERITS (gcp);
create table magpie.gcpsecretmanagersecret () INHERITS (gcp);
create table magpie.gcpsecurityscanconfig () INHERITS (gcp);
create table magpie.gcpservicediscoveryservice () INHERITS (gcp);
create table magpie.gcpspannerinstance () INHERITS (gcp);
create table magpie.gcpsqlinstance () INHERITS (gcp);
create table magpie.gcpstoragebucket () INHERITS (gcp);
create table magpie.gcptalenttenant () INHERITS (gcp);
create table magpie.gcptaskqueue () INHERITS (gcp);
create table magpie.gcptrace () INHERITS (gcp);
create table magpie.gcptranslateglossary () INHERITS (gcp);
create table magpie.gcpvisionproduct () INHERITS (gcp);
create table magpie.gcpvisionproductset () INHERITS (gcp);
create table magpie.gcpvpcfirewall () INHERITS (gcp);
create table magpie.gcpvpcnetwork () INHERITS (gcp);
create table magpie.gpcdataproccluster () INHERITS (gcp);
create table magpie.gpcdataprocjob () INHERITS (gcp);
create table magpie.gpcfunction () INHERITS (gcp);

