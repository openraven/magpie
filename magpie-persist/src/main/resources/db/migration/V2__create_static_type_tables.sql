-- AWS TYPES:
create table awsaccount () INHERITS (aws);
create table awsathenadatacatalog () INHERITS (aws);
create table awsbackupbackupplan () INHERITS (aws);
create table awsbackupbackupvault () INHERITS (aws);
create table awsbatchcomputeenvironment () INHERITS (aws);
create table awsbatchjobdefinition () INHERITS (aws);
create table awsbatchjobqueue () INHERITS (aws);
create table awscassandrakeyspace () INHERITS (aws);
create table awscloudfrontdistribution () INHERITS (aws);
create table awscloudsearchdomain () INHERITS (aws);
create table awscloudtrailtrail () INHERITS (aws);
create table awsconfigurationrecorder () INHERITS (aws);
create table awsdynamodbglobaltable () INHERITS (aws);
create table awsdynamodbtable () INHERITS (aws);
create table awsec2eip () INHERITS (aws);
create table awsec2instance () INHERITS (aws);
create table awsec2networkacl () INHERITS (aws);
create table awsec2networkinterface () INHERITS (aws);
create table awsec2securitygroup () INHERITS (aws);
create table awsec2snapshot () INHERITS (aws);
create table awsec2volume () INHERITS (aws);
create table awsec2vpc () INHERITS (aws);
create table awsec2vpcpeeringconnection () INHERITS (aws);
create table awsecscluster () INHERITS (aws);
create table awsefsfilesystem () INHERITS (aws);
create table awsguarddutydetector () INHERITS (aws);
create table awsekscluster () INHERITS (aws);
create table awselasticachecluster () INHERITS (aws);
create table awselasticbeanstalk () INHERITS (aws);
create table awselasticloadbalancingloadbalancer () INHERITS (aws);
create table awselasticloadbalancingv2loadbalancer () INHERITS (aws);
create table awselasticsearchdomain () INHERITS (aws);
create table awsemrcluster () INHERITS (aws);
create table awsfsxfilesystem () INHERITS (aws);
create table awsglaciervault () INHERITS (aws);
create table awsiamgroup () INHERITS (aws);
create table awsiampolicy () INHERITS (aws);
create table awsiamrole () INHERITS (aws);
create table awsiamuser () INHERITS (aws);
create table awsiamaccount () INHERITS (aws);
create table awsiamcredentialsreport () INHERITS (aws);
create table awskmskey () INHERITS (aws);
create table awslakeformationresource () INHERITS (aws);
create table awslambdafunction () INHERITS (aws);
create table awslightsaildatabase () INHERITS (aws);
create table awslightsailinstance () INHERITS (aws);
create table awslightsailloadbalancer () INHERITS (aws);
create table awslocationgeofencecollection () INHERITS (aws);
create table awslocationmap () INHERITS (aws);
create table awslocationplaceindex () INHERITS (aws);
create table awslocationroutecalculator () INHERITS (aws);
create table awslocationtracker () INHERITS (aws);
create table awsneptunecluster () INHERITS (aws);
create table awsneptuneinstance () INHERITS (aws);
create table awsqldbledger () INHERITS (aws);
create table awsrdsdbinstance () INHERITS (aws);
create table awsrdsdbsnapshot () INHERITS (aws);
create table awsredshiftcluster () INHERITS (aws);
create table awsregion () INHERITS (aws);
create table awsroute53hostedzone () INHERITS (aws);
create table awss3bucket () INHERITS (aws);
create table awss3bucketobject () INHERITS (aws);
create table awssecretsmanager () INHERITS (aws);
create table awssecurityhubstandardsubscription () INHERITS (aws);
create table awssnssubscription () INHERITS (aws);
create table awssnstopic () INHERITS (aws);
create table awsstoragegatewaygateway () INHERITS (aws);
create table awswatchalarm () INHERITS (aws);
create table awswatchdashboard () INHERITS (aws);
create table awswatchlogsmetricfilter () INHERITS (aws);
create table awsusercredentialreport () INHERITS (aws);
create table awsssminstance () INHERITS (aws);
create table orvnshadowaccount () INHERITS (aws);
create table awswatchloggroup () INHERITS (aws);

-- GCP TYPES;
create table gcpbigqueryreservation () INHERITS (gcp);
create table gcpbigqueryreservationcapacity () INHERITS (gcp);
create table gcpbigtableinstance () INHERITS (gcp);
create table gcpaccess () INHERITS (gcp);
create table gcpasset () INHERITS (gcp);
create table gcpassetfeed () INHERITS (gcp);
create table gcpautomldataset () INHERITS (gcp);
create table gcpautomlmodel () INHERITS (gcp);
create table gcpbigquerydataset () INHERITS (gcp);
create table gcpbigquerydatatransfer () INHERITS (gcp);
create table gcpbillingaccount () INHERITS (gcp);
create table gcpcloudbuild () INHERITS (gcp);
create table gcpcloudbuildtrigger () INHERITS (gcp);
create table gcpcluster () INHERITS (gcp);
create table gcpcomputedisk () INHERITS (gcp);
create table gcpcomputeinstance () INHERITS (gcp);
create table gcpcontaineranalysisnote () INHERITS (gcp);
create table gcpcontaineranalysisoccurrence () INHERITS (gcp);
create table gcpdatacatalog () INHERITS (gcp);
create table gcpdatalabelig () INHERITS (gcp);
create table gcpdatalabelingannotations () INHERITS (gcp);
create table gcpdatalabelinginstruction () INHERITS (gcp);
create table gcpdialogflowconversation () INHERITS (gcp);
create table gcpdlpjob () INHERITS (gcp);
create table gcpdlpjobtrigger () INHERITS (gcp);
create table gcpdnszone () INHERITS (gcp);
create table gcperrorreporting () INHERITS (gcp);
create table gcpgameservice () INHERITS (gcp);
create table gcpiamrole () INHERITS (gcp);
create table gcpiamserviceaccount () INHERITS (gcp);
create table gcpiotdeviceregistry () INHERITS (gcp);
create table gcpkmskeyring () INHERITS (gcp);
create table gcploggingbucket () INHERITS (gcp);
create table gcploggingexclusion () INHERITS (gcp);
create table gcploggingmetric () INHERITS (gcp);
create table gcploggingsink () INHERITS (gcp);
create table gcpmemcacheinstance () INHERITS (gcp);
create table gcpmonitoringalertpolicy () INHERITS (gcp);
create table gcpmonitoringdashboard () INHERITS (gcp);
create table gcpmonitoringgroup () INHERITS (gcp);
create table gcpmonitoringservice () INHERITS (gcp);
create table gcposconfigpatchdeployment () INHERITS (gcp);
create table gcposconfigpatchjob () INHERITS (gcp);
create table gcpprojectinfo () INHERITS (gcp);
create table gcppubsublitesubscription () INHERITS (gcp);
create table gcppubsublitetopic () INHERITS (gcp);
create table gcppubsubschema () INHERITS (gcp);
create table gcppubsubsnapshots () INHERITS (gcp);
create table gcppubsubsubscription () INHERITS (gcp);
create table gcppubsubtopic () INHERITS (gcp);
create table gcprecaptchaenterprisekey () INHERITS (gcp);
create table gcpredisinstance () INHERITS (gcp);
create table gcpresourcemanagerfolder () INHERITS (gcp);
create table gcpresourcemanagerorganization () INHERITS (gcp);
create table gcpresourcemanagerproject () INHERITS (gcp);
create table gcpschedulerjob () INHERITS (gcp);
create table gcpsecretmanagersecret () INHERITS (gcp);
create table gcpsecurityscanconfig () INHERITS (gcp);
create table gcpservicediscoveryservice () INHERITS (gcp);
create table gcpspannerinstance () INHERITS (gcp);
create table gcpsqlinstance () INHERITS (gcp);
create table gcpstoragebucket () INHERITS (gcp);
create table gcptalenttenant () INHERITS (gcp);
create table gcptaskqueue () INHERITS (gcp);
create table gcptrace () INHERITS (gcp);
create table gcptranslateglossary () INHERITS (gcp);
create table gcpvisionproduct () INHERITS (gcp);
create table gcpvisionproductset () INHERITS (gcp);
create table gcpvpcfirewall () INHERITS (gcp);
create table gcpvpcnetwork () INHERITS (gcp);
create table gpcdataproccluster () INHERITS (gcp);
create table gpcdataprocjob () INHERITS (gcp);
create table gpcfunction () INHERITS (gcp);

-- GDrive TYPES;

create table gdrivedrive () INHERITS (gdrive);
