insert into magpie.aws (documentid, resourcetype, resourceid, configuration)
values
('1', 'AWS::EC2::Instance',	'i-0b2bff5afdc58ef7a',	'{"tags": [{"key": "aws:cloudformation:logical-id", "value": "BastionHost"}, {"key": "Name", "value": "openraven-00g1j8byur0Mk9jaN0h8-bastion"}, {"key": "aws:cloudformation:stack-name", "value": "jason-test-moped"}, {"key": "aws:cloudformation:stack-id", "value": "arn:aws:cloudformation:us-east-1:723176279592:stack/jason-test-moped/ac976080-cc3b-11ea-8760-1277bf9feb05"}, {"key": "project", "value": "openraven"}], "state": {"code": 80, "name": "stopped"}, "vpcId": "vpc-054e57edbb9944478", "imageId": "ami-00b6bdd6691d44c19", "keyName": "or-cluster", "kernelId": null, "licenses": null, "platform": null, "publicIp": "54.224.59.255", "subnetId": "subnet-0e0c65d2da128849f", "placement": {"hostId": null, "tenancy": "default", "affinity": null, "groupName": "", "spreadDomain": null, "partitionNumber": null, "availabilityZone": "us-east-1a", "hostResourceGroupArn": null}, "ramdiskId": null, "cpuOptions": {"coreCount": 1, "threadsPerCore": 1}, "enaSupport": true, "hypervisor": "xen", "instanceId": "i-0b2bff5afdc58ef7a", "launchTime": "2020-07-22T16:52:59Z", "monitoring": {"state": "disabled"}, "outpostArn": null, "clientToken": "jason-Basti-1DRUGUU1W52GB", "stateReason": null, "architecture": "x86_64", "ebsOptimized": false, "instanceType": "t2.micro", "productCodes": [], "publicDnsName": "ec2-54-224-59-255.compute-1.amazonaws.com", "amiLaunchIndex": 0, "enclaveOptions": {"enabled": false}, "privateDnsName": "ip-10-128-0-253.ec2.internal", "rootDeviceName": "/dev/xvda", "rootDeviceType": "ebs", "securityGroups": [{"groupId": "sg-00a0b7c747d5bc8af", "groupName": "jason-test-moped-K8sWorkerSg-JOOSVEFPG5XW"}], "metadataOptions": {"state": "applied", "httpTokens": "optional", "httpEndpoint": "enabled", "httpPutResponseHopLimit": 1}, "publicIpAddress": "54.224.59.255", "sourceDestCheck": true, "sriovNetSupport": null, "privateIpAddress": "10.128.0.253", "instanceLifecycle": null, "networkInterfaces": [{"vpcId": "vpc-054e57edbb9944478", "groups": [{"groupId": "sg-00a0b7c747d5bc8af", "groupName": "jason-test-moped-K8sWorkerSg-JOOSVEFPG5XW"}], "status": "in-use", "ownerId": "723176279592", "subnetId": "subnet-0e0c65d2da128849f", "attachment": {"status": "attached", "attachTime": "2020-07-22T16:52:59Z", "deviceIndex": 0, "attachmentId": "eni-attach-056cd11296a0474d3", "networkCardIndex": 0, "deleteOnTermination": true}, "macAddress": "0e:bf:64:3e:23:7b", "association": {"publicIp": "54.224.59.255", "carrierIp": null, "ipOwnerId": "amazon", "publicDnsName": "ec2-54-224-59-255.compute-1.amazonaws.com"}, "description": "", "interfaceType": "interface", "ipv6Addresses": [], "privateDnsName": "ip-10-128-0-253.ec2.internal", "sourceDestCheck": true, "privateIpAddress": "10.128.0.253", "networkInterfaceId": "eni-027a4934ad04c6fea", "privateIpAddresses": [{"primary": true, "association": {"publicIp": "54.224.59.255", "carrierIp": null, "ipOwnerId": "amazon", "publicDnsName": "ec2-54-224-59-255.compute-1.amazonaws.com"}, "privateDnsName": "ip-10-128-0-253.ec2.internal", "privateIpAddress": "10.128.0.253"}]}], "hibernationOptions": {"configured": false}, "iamInstanceProfile": null, "virtualizationType": "hvm", "blockDeviceMappings": [{"ebs": {"status": "attached", "volumeId": "vol-0160c14720c48a5d4", "attachTime": "2020-07-22T16:53:00Z", "deleteOnTermination": true}, "deviceName": "/dev/xvda"}], "capacityReservationId": null, "spotInstanceRequestId": null, "stateTransitionReason": "", "elasticGpuAssociations": null, "capacityReservationSpecification": {"capacityReservationTarget": null, "capacityReservationPreference": "open"}, "elasticInferenceAcceleratorAssociations": null}'),
('2', 'AWS::EC2::Instance',	'i-0a85540439e24fa0c',	'{"tags": [{"key": "k8s.io/role/worker", "value": ""}, {"key": "aws:cloudformation:stack-id", "value": "arn:aws:cloudformation:us-east-1:723176279592:stack/jason-test-moped/ac976080-cc3b-11ea-8760-1277bf9feb05"}, {"key": "kubernetes.io/cluster/00g1j8byur0Mk9jaN0h8", "value": "member"}, {"key": "project", "value": "openraven"}, {"key": "aws:cloudformation:logical-id", "value": "K8sWorkerAsg"}, {"key": "k8s.io/cluster-autoscaler/enabled", "value": ""}, {"key": "k8s.io/cluster-autoscaler/node-template/label/node-role.kubernetes.io/worker", "value": ""}, {"key": "kubernetes.io/role/worker", "value": ""}, {"key": "aws:autoscaling:groupName", "value": "jason-test-moped-K8sWorkerAsg-1FUM7S4H59OEG"}, {"key": "k8s.io/cluster-autoscaler/00g1j8byur0Mk9jaN0h8", "value": ""}, {"key": "Role", "value": "worker"}, {"key": "aws:cloudformation:stack-name", "value": "jason-test-moped"}], "state": {"code": 16, "name": "running"}, "vpcId": "vpc-054e57edbb9944478", "imageId": "ami-00b6bdd6691d44c19", "keyName": "or-cluster", "kernelId": null, "licenses": null, "platform": null, "subnetId": "subnet-022a263e58d1ada34", "placement": {"hostId": null, "tenancy": "default", "affinity": null, "groupName": "", "spreadDomain": null, "partitionNumber": null, "availabilityZone": "us-east-1a", "hostResourceGroupArn": null}, "ramdiskId": null, "cpuOptions": {"coreCount": 2, "threadsPerCore": 2}, "enaSupport": true, "hypervisor": "xen", "instanceId": "i-0a85540439e24fa0c", "launchTime": "2020-07-22T17:05:44Z", "monitoring": {"state": "disabled"}, "outpostArn": null, "clientToken": "da85cddd-f3b9-bfe0-5c63-47ceb920ce21", "stateReason": null, "architecture": "x86_64", "ebsOptimized": false, "instanceType": "m5.xlarge", "productCodes": [], "publicDnsName": "", "amiLaunchIndex": 0, "enclaveOptions": {"enabled": false}, "privateDnsName": "ip-10-128-101-122.ec2.internal", "rootDeviceName": "/dev/xvda", "rootDeviceType": "ebs", "securityGroups": [{"groupId": "sg-00a0b7c747d5bc8af", "groupName": "jason-test-moped-K8sWorkerSg-JOOSVEFPG5XW"}], "metadataOptions": {"state": "applied", "httpTokens": "optional", "httpEndpoint": "enabled", "httpPutResponseHopLimit": 1}, "publicIpAddress": null, "sourceDestCheck": true, "sriovNetSupport": null, "privateIpAddress": "10.128.101.122", "instanceLifecycle": null, "networkInterfaces": [{"vpcId": "vpc-054e57edbb9944478", "groups": [{"groupId": "sg-00a0b7c747d5bc8af", "groupName": "jason-test-moped-K8sWorkerSg-JOOSVEFPG5XW"}], "status": "in-use", "ownerId": "723176279592", "subnetId": "subnet-022a263e58d1ada34", "attachment": {"status": "attached", "attachTime": "2020-07-22T17:05:44Z", "deviceIndex": 0, "attachmentId": "eni-attach-00155f5a705b44066", "networkCardIndex": 0, "deleteOnTermination": true}, "macAddress": "0e:23:22:ee:b3:a7", "association": null, "description": "", "interfaceType": "interface", "ipv6Addresses": [], "privateDnsName": "ip-10-128-101-122.ec2.internal", "sourceDestCheck": true, "privateIpAddress": "10.128.101.122", "networkInterfaceId": "eni-00a0cf4f021714cff", "privateIpAddresses": [{"primary": true, "association": null, "privateDnsName": "ip-10-128-101-122.ec2.internal", "privateIpAddress": "10.128.101.122"}]}], "hibernationOptions": {"configured": false}, "iamInstanceProfile": {"id": "AIPA2QYFKJYULCD27AFMK", "arn": "arn:aws:iam::723176279592:instance-profile/jason-test-moped-WorkerIamInstance-1STK9AAY45KQL"}, "virtualizationType": "hvm", "blockDeviceMappings": [{"ebs": {"status": "attached", "volumeId": "vol-0946b8c010fe935eb", "attachTime": "2020-07-22T17:05:45Z", "deleteOnTermination": true}, "deviceName": "/dev/xvda"}, {"ebs": {"status": "attached", "volumeId": "vol-07ab0947852eb4f2c", "attachTime": "2020-07-22T17:08:08Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbj"}, {"ebs": {"status": "attached", "volumeId": "vol-0ba70b8e4d61796a8", "attachTime": "2020-07-22T17:08:13Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbu"}, {"ebs": {"status": "attached", "volumeId": "vol-064a433c0af7f10f0", "attachTime": "2020-12-01T01:54:02Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdcz"}, {"ebs": {"status": "attached", "volumeId": "vol-0227bedecb38cfc4d", "attachTime": "2020-12-01T02:01:07Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbo"}, {"ebs": {"status": "attached", "volumeId": "vol-0d834d3992a780eca", "attachTime": "2021-03-28T02:20:51Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbc"}, {"ebs": {"status": "attached", "volumeId": "vol-0a9a61aa67f936569", "attachTime": "2021-03-28T02:20:51Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbl"}, {"ebs": {"status": "attached", "volumeId": "vol-015dd7161af23d4ca", "attachTime": "2021-03-28T02:20:51Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbh"}, {"ebs": {"status": "attached", "volumeId": "vol-066738c58a3330d9a", "attachTime": "2021-03-28T02:20:51Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbn"}, {"ebs": {"status": "attached", "volumeId": "vol-0d1016b7ad7c5e9ef", "attachTime": "2021-04-13T06:37:27Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbi"}, {"ebs": {"status": "attached", "volumeId": "vol-05577bd99f93bf81e", "attachTime": "2021-04-13T06:37:33Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdcp"}], "capacityReservationId": null, "spotInstanceRequestId": null, "stateTransitionReason": "", "elasticGpuAssociations": null, "capacityReservationSpecification": {"capacityReservationTarget": null, "capacityReservationPreference": "open"}, "elasticInferenceAcceleratorAssociations": null}'),
('3', 'AWS::EC2::Instance',	'i-0b08e2ff264f600e2',	'{"tags": [{"key": "aws:autoscaling:groupName", "value": "jason-test-moped-K8sWorkerAsg-1FUM7S4H59OEG"}, {"key": "aws:cloudformation:logical-id", "value": "K8sWorkerAsg"}, {"key": "aws:cloudformation:stack-name", "value": "jason-test-moped"}, {"key": "k8s.io/cluster-autoscaler/00g1j8byur0Mk9jaN0h8", "value": ""}, {"key": "Role", "value": "worker"}, {"key": "k8s.io/cluster-autoscaler/node-template/label/node-role.kubernetes.io/worker", "value": ""}, {"key": "kubernetes.io/cluster/00g1j8byur0Mk9jaN0h8", "value": "member"}, {"key": "aws:cloudformation:stack-id", "value": "arn:aws:cloudformation:us-east-1:723176279592:stack/jason-test-moped/ac976080-cc3b-11ea-8760-1277bf9feb05"}, {"key": "k8s.io/role/worker", "value": ""}, {"key": "project", "value": "openraven"}, {"key": "kubernetes.io/role/worker", "value": ""}, {"key": "k8s.io/cluster-autoscaler/enabled", "value": ""}], "state": {"code": 16, "name": "running"}, "vpcId": "vpc-054e57edbb9944478", "imageId": "ami-00b6bdd6691d44c19", "keyName": "or-cluster", "kernelId": null, "licenses": null, "platform": null, "subnetId": "subnet-022a263e58d1ada34", "placement": {"hostId": null, "tenancy": "default", "affinity": null, "groupName": "", "spreadDomain": null, "partitionNumber": null, "availabilityZone": "us-east-1a", "hostResourceGroupArn": null}, "ramdiskId": null, "cpuOptions": {"coreCount": 2, "threadsPerCore": 2}, "enaSupport": true, "hypervisor": "xen", "instanceId": "i-0b08e2ff264f600e2", "launchTime": "2020-07-22T17:05:44Z", "monitoring": {"state": "disabled"}, "outpostArn": null, "clientToken": "da85cddd-f3b9-bfe0-5c63-47ceb920ce21", "stateReason": null, "architecture": "x86_64", "ebsOptimized": false, "instanceType": "m5.xlarge", "productCodes": [], "publicDnsName": "", "amiLaunchIndex": 1, "enclaveOptions": {"enabled": false}, "privateDnsName": "ip-10-128-104-104.ec2.internal", "rootDeviceName": "/dev/xvda", "rootDeviceType": "ebs", "securityGroups": [{"groupId": "sg-00a0b7c747d5bc8af", "groupName": "jason-test-moped-K8sWorkerSg-JOOSVEFPG5XW"}], "metadataOptions": {"state": "applied", "httpTokens": "optional", "httpEndpoint": "enabled", "httpPutResponseHopLimit": 1}, "publicIpAddress": null, "sourceDestCheck": true, "sriovNetSupport": null, "privateIpAddress": "10.128.104.104", "instanceLifecycle": null, "networkInterfaces": [{"vpcId": "vpc-054e57edbb9944478", "groups": [{"groupId": "sg-00a0b7c747d5bc8af", "groupName": "jason-test-moped-K8sWorkerSg-JOOSVEFPG5XW"}], "status": "in-use", "ownerId": "723176279592", "subnetId": "subnet-022a263e58d1ada34", "attachment": {"status": "attached", "attachTime": "2020-07-22T17:05:44Z", "deviceIndex": 0, "attachmentId": "eni-attach-01fcffb84b189e523", "networkCardIndex": 0, "deleteOnTermination": true}, "macAddress": "0e:e7:71:2b:e1:55", "association": null, "description": "", "interfaceType": "interface", "ipv6Addresses": [], "privateDnsName": "ip-10-128-104-104.ec2.internal", "sourceDestCheck": true, "privateIpAddress": "10.128.104.104", "networkInterfaceId": "eni-03122d1e9228df0a0", "privateIpAddresses": [{"primary": true, "association": null, "privateDnsName": "ip-10-128-104-104.ec2.internal", "privateIpAddress": "10.128.104.104"}]}], "hibernationOptions": {"configured": false}, "iamInstanceProfile": {"id": "AIPA2QYFKJYULCD27AFMK", "arn": "arn:aws:iam::723176279592:instance-profile/jason-test-moped-WorkerIamInstance-1STK9AAY45KQL"}, "virtualizationType": "hvm", "blockDeviceMappings": [{"ebs": {"status": "attached", "volumeId": "vol-08bcffc064960bd93", "attachTime": "2020-07-22T17:05:45Z", "deleteOnTermination": true}, "deviceName": "/dev/xvda"}, {"ebs": {"status": "attached", "volumeId": "vol-0cb619ec66c687602", "attachTime": "2020-12-01T01:58:50Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbs"}, {"ebs": {"status": "attached", "volumeId": "vol-0a6117311d55965ea", "attachTime": "2021-04-13T06:29:34Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdcr"}, {"ebs": {"status": "attached", "volumeId": "vol-05b817feb8d32dec9", "attachTime": "2021-04-13T06:29:34Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdcv"}, {"ebs": {"status": "attached", "volumeId": "vol-0d5b6261dfc353fe4", "attachTime": "2021-04-13T06:43:37Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbn"}, {"ebs": {"status": "attached", "volumeId": "vol-0ce24a00c4ebb4058", "attachTime": "2021-04-13T06:43:44Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdcu"}], "capacityReservationId": null, "spotInstanceRequestId": null, "stateTransitionReason": "", "elasticGpuAssociations": null, "capacityReservationSpecification": {"capacityReservationTarget": null, "capacityReservationPreference": "open"}, "elasticInferenceAcceleratorAssociations": null}'),
('4', 'AWS::EC2::Instance',	'i-0a4314cb5c5f3f65b',	'{"tags": [{"key": "k8s.io/cluster-autoscaler/node-template/label/node-role.kubernetes.io/worker", "value": ""}, {"key": "kubernetes.io/role/worker", "value": ""}, {"key": "aws:cloudformation:stack-name", "value": "jason-test-moped"}, {"key": "aws:autoscaling:groupName", "value": "jason-test-moped-K8sWorkerAsg-1FUM7S4H59OEG"}, {"key": "kubernetes.io/cluster/00g1j8byur0Mk9jaN0h8", "value": "member"}, {"key": "project", "value": "openraven"}, {"key": "aws:cloudformation:stack-id", "value": "arn:aws:cloudformation:us-east-1:723176279592:stack/jason-test-moped/ac976080-cc3b-11ea-8760-1277bf9feb05"}, {"key": "Role", "value": "worker"}, {"key": "k8s.io/cluster-autoscaler/00g1j8byur0Mk9jaN0h8", "value": ""}, {"key": "k8s.io/cluster-autoscaler/enabled", "value": ""}, {"key": "aws:cloudformation:logical-id", "value": "K8sWorkerAsg"}, {"key": "k8s.io/role/worker", "value": ""}], "state": {"code": 16, "name": "running"}, "vpcId": "vpc-054e57edbb9944478", "imageId": "ami-00b6bdd6691d44c19", "keyName": "or-cluster", "kernelId": null, "licenses": null, "platform": null, "subnetId": "subnet-022a263e58d1ada34", "placement": {"hostId": null, "tenancy": "default", "affinity": null, "groupName": "", "spreadDomain": null, "partitionNumber": null, "availabilityZone": "us-east-1a", "hostResourceGroupArn": null}, "ramdiskId": null, "cpuOptions": {"coreCount": 2, "threadsPerCore": 2}, "enaSupport": true, "hypervisor": "xen", "instanceId": "i-0a4314cb5c5f3f65b", "launchTime": "2020-07-22T17:05:44Z", "monitoring": {"state": "disabled"}, "outpostArn": null, "clientToken": "da85cddd-f3b9-bfe0-5c63-47ceb920ce21", "stateReason": null, "architecture": "x86_64", "ebsOptimized": false, "instanceType": "m5.xlarge", "productCodes": [], "publicDnsName": "", "amiLaunchIndex": 2, "enclaveOptions": {"enabled": false}, "privateDnsName": "ip-10-128-99-21.ec2.internal", "rootDeviceName": "/dev/xvda", "rootDeviceType": "ebs", "securityGroups": [{"groupId": "sg-00a0b7c747d5bc8af", "groupName": "jason-test-moped-K8sWorkerSg-JOOSVEFPG5XW"}], "metadataOptions": {"state": "applied", "httpTokens": "optional", "httpEndpoint": "enabled", "httpPutResponseHopLimit": 1}, "publicIpAddress": null, "sourceDestCheck": true, "sriovNetSupport": null, "privateIpAddress": "10.128.99.21", "instanceLifecycle": null, "networkInterfaces": [{"vpcId": "vpc-054e57edbb9944478", "groups": [{"groupId": "sg-00a0b7c747d5bc8af", "groupName": "jason-test-moped-K8sWorkerSg-JOOSVEFPG5XW"}], "status": "in-use", "ownerId": "723176279592", "subnetId": "subnet-022a263e58d1ada34", "attachment": {"status": "attached", "attachTime": "2020-07-22T17:05:44Z", "deviceIndex": 0, "attachmentId": "eni-attach-0760851825462fc24", "networkCardIndex": 0, "deleteOnTermination": true}, "macAddress": "0e:98:39:25:94:4f", "association": null, "description": "", "interfaceType": "interface", "ipv6Addresses": [], "privateDnsName": "ip-10-128-99-21.ec2.internal", "sourceDestCheck": true, "privateIpAddress": "10.128.99.21", "networkInterfaceId": "eni-0c9e1429c7fd6c7bc", "privateIpAddresses": [{"primary": true, "association": null, "privateDnsName": "ip-10-128-99-21.ec2.internal", "privateIpAddress": "10.128.99.21"}]}], "hibernationOptions": {"configured": false}, "iamInstanceProfile": {"id": "AIPA2QYFKJYULCD27AFMK", "arn": "arn:aws:iam::723176279592:instance-profile/jason-test-moped-WorkerIamInstance-1STK9AAY45KQL"}, "virtualizationType": "hvm", "blockDeviceMappings": [{"ebs": {"status": "attached", "volumeId": "vol-0e1454de4e6a020e4", "attachTime": "2020-07-22T17:05:45Z", "deleteOnTermination": true}, "deviceName": "/dev/xvda"}, {"ebs": {"status": "attached", "volumeId": "vol-0d88335afb6b850ae", "attachTime": "2021-04-13T06:40:43Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdbp"}, {"ebs": {"status": "attached", "volumeId": "vol-0786b9e4071ac71a1", "attachTime": "2021-04-13T06:40:43Z", "deleteOnTermination": false}, "deviceName": "/dev/xvdcz"}], "capacityReservationId": null, "spotInstanceRequestId": null, "stateTransitionReason": "", "elasticGpuAssociations": null, "capacityReservationSpecification": {"capacityReservationTarget": null, "capacityReservationPreference": "open"}, "elasticInferenceAcceleratorAssociations": null}'),
('5', 'AWS::EC2::Instance',	'i-02d707a66b2c4d632',	'{"tags": [{"key": "project", "value": "openraven"}, {"key": "kubernetes.io/role/master", "value": "master"}, {"key": "aws:autoscaling:groupName", "value": "jason-test-moped-K8sMasterAsg-19R2HBMVTJXW0"}, {"key": "Role", "value": "master"}, {"key": "aws:cloudformation:logical-id", "value": "K8sMasterAsg"}, {"key": "aws:cloudformation:stack-name", "value": "jason-test-moped"}, {"key": "aws:cloudformation:stack-id", "value": "arn:aws:cloudformation:us-east-1:723176279592:stack/jason-test-moped/ac976080-cc3b-11ea-8760-1277bf9feb05"}, {"key": "k8s.io/role/master", "value": "master"}, {"key": "kubernetes.io/cluster/00g1j8byur0Mk9jaN0h8", "value": "member"}], "state": {"code": 16, "name": "running"}, "vpcId": "vpc-054e57edbb9944478", "imageId": "ami-00b6bdd6691d44c19", "keyName": "or-cluster", "kernelId": null, "licenses": null, "platform": null, "subnetId": "subnet-022a263e58d1ada34", "placement": {"hostId": null, "tenancy": "default", "affinity": null, "groupName": "", "spreadDomain": null, "partitionNumber": null, "availabilityZone": "us-east-1a", "hostResourceGroupArn": null}, "ramdiskId": null, "cpuOptions": {"coreCount": 1, "threadsPerCore": 2}, "enaSupport": true, "hypervisor": "xen", "instanceId": "i-02d707a66b2c4d632", "launchTime": "2020-07-29T19:20:38Z", "monitoring": {"state": "disabled"}, "outpostArn": null, "clientToken": "7415ce70-13c2-22fc-3289-6af7e79aecf7", "stateReason": null, "architecture": "x86_64", "ebsOptimized": false, "instanceType": "t3.medium", "productCodes": [], "publicDnsName": "", "amiLaunchIndex": 0, "enclaveOptions": {"enabled": false}, "privateDnsName": "ip-10-128-111-91.ec2.internal", "rootDeviceName": "/dev/xvda", "rootDeviceType": "ebs", "securityGroups": [{"groupId": "sg-07a6077c9af3c6801", "groupName": "jason-test-moped-K8SMasterSg-G8DVY8HL584O"}], "metadataOptions": {"state": "applied", "httpTokens": "optional", "httpEndpoint": "enabled", "httpPutResponseHopLimit": 1}, "publicIpAddress": null, "sourceDestCheck": true, "sriovNetSupport": null, "privateIpAddress": "10.128.111.91", "instanceLifecycle": null, "networkInterfaces": [{"vpcId": "vpc-054e57edbb9944478", "groups": [{"groupId": "sg-07a6077c9af3c6801", "groupName": "jason-test-moped-K8SMasterSg-G8DVY8HL584O"}], "status": "in-use", "ownerId": "723176279592", "subnetId": "subnet-022a263e58d1ada34", "attachment": {"status": "attached", "attachTime": "2020-07-29T19:20:38Z", "deviceIndex": 0, "attachmentId": "eni-attach-04d469cb8592aa216", "networkCardIndex": 0, "deleteOnTermination": true}, "macAddress": "0e:af:29:2c:9e:29", "association": null, "description": "", "interfaceType": "interface", "ipv6Addresses": [], "privateDnsName": "ip-10-128-111-91.ec2.internal", "sourceDestCheck": true, "privateIpAddress": "10.128.111.91", "networkInterfaceId": "eni-01610c78f1d97295f", "privateIpAddresses": [{"primary": true, "association": null, "privateDnsName": "ip-10-128-111-91.ec2.internal", "privateIpAddress": "10.128.111.91"}]}], "hibernationOptions": {"configured": false}, "iamInstanceProfile": {"id": "AIPA2QYFKJYUC2ABMJC7N", "arn": "arn:aws:iam::723176279592:instance-profile/jason-test-moped-MasterIamInstance-1IG08MPMYZCRB"}, "virtualizationType": "hvm", "blockDeviceMappings": [{"ebs": {"status": "attached", "volumeId": "vol-0e4c92176704973ed", "attachTime": "2020-07-29T19:20:39Z", "deleteOnTermination": true}, "deviceName": "/dev/xvda"}], "capacityReservationId": null, "spotInstanceRequestId": null, "stateTransitionReason": "", "elasticGpuAssociations": null, "capacityReservationSpecification": {"capacityReservationTarget": null, "capacityReservationPreference": "open"}, "elasticInferenceAcceleratorAssociations": null}'),
('6', 'AWS::EC2::Instance',	'i-0bcfc92093c876165',	'{"tags": [{"key": "aws:cloudformation:logical-id", "value": "K8sMasterAsg"}, {"key": "aws:cloudformation:stack-name", "value": "jason-test-moped"}, {"key": "kubernetes.io/cluster/00g1j8byur0Mk9jaN0h8", "value": "member"}, {"key": "k8s.io/role/master", "value": "master"}, {"key": "aws:autoscaling:groupName", "value": "jason-test-moped-K8sMasterAsg-19R2HBMVTJXW0"}, {"key": "Role", "value": "master"}, {"key": "aws:cloudformation:stack-id", "value": "arn:aws:cloudformation:us-east-1:723176279592:stack/jason-test-moped/ac976080-cc3b-11ea-8760-1277bf9feb05"}, {"key": "kubernetes.io/role/master", "value": "master"}, {"key": "project", "value": "openraven"}], "state": {"code": 16, "name": "running"}, "vpcId": "vpc-054e57edbb9944478", "imageId": "ami-00b6bdd6691d44c19", "keyName": "or-cluster", "kernelId": null, "licenses": null, "platform": null, "subnetId": "subnet-022a263e58d1ada34", "placement": {"hostId": null, "tenancy": "default", "affinity": null, "groupName": "", "spreadDomain": null, "partitionNumber": null, "availabilityZone": "us-east-1a", "hostResourceGroupArn": null}, "ramdiskId": null, "cpuOptions": {"coreCount": 1, "threadsPerCore": 2}, "enaSupport": true, "hypervisor": "xen", "instanceId": "i-0bcfc92093c876165", "launchTime": "2020-10-31T22:58:35Z", "monitoring": {"state": "disabled"}, "outpostArn": null, "clientToken": "3f25d603-8909-7641-6111-4639763e6feb", "stateReason": null, "architecture": "x86_64", "ebsOptimized": false, "instanceType": "t3.medium", "productCodes": [], "publicDnsName": "", "amiLaunchIndex": 0, "enclaveOptions": {"enabled": false}, "privateDnsName": "ip-10-128-111-142.ec2.internal", "rootDeviceName": "/dev/xvda", "rootDeviceType": "ebs", "securityGroups": [{"groupId": "sg-07a6077c9af3c6801", "groupName": "jason-test-moped-K8SMasterSg-G8DVY8HL584O"}], "metadataOptions": {"state": "applied", "httpTokens": "optional", "httpEndpoint": "enabled", "httpPutResponseHopLimit": 1}, "publicIpAddress": null, "sourceDestCheck": true, "sriovNetSupport": null, "privateIpAddress": "10.128.111.142", "instanceLifecycle": null, "networkInterfaces": [{"vpcId": "vpc-054e57edbb9944478", "groups": [{"groupId": "sg-07a6077c9af3c6801", "groupName": "jason-test-moped-K8SMasterSg-G8DVY8HL584O"}], "status": "in-use", "ownerId": "723176279592", "subnetId": "subnet-022a263e58d1ada34", "attachment": {"status": "attached", "attachTime": "2020-10-31T22:58:35Z", "deviceIndex": 0, "attachmentId": "eni-attach-046a2351366e58479", "networkCardIndex": 0, "deleteOnTermination": true}, "macAddress": "0e:48:52:50:ca:2d", "association": null, "description": "", "interfaceType": "interface", "ipv6Addresses": [], "privateDnsName": "ip-10-128-111-142.ec2.internal", "sourceDestCheck": true, "privateIpAddress": "10.128.111.142", "networkInterfaceId": "eni-0b4b06d196b543d91", "privateIpAddresses": [{"primary": true, "association": null, "privateDnsName": "ip-10-128-111-142.ec2.internal", "privateIpAddress": "10.128.111.142"}]}], "hibernationOptions": {"configured": false}, "iamInstanceProfile": {"id": "AIPA2QYFKJYUC2ABMJC7N", "arn": "arn:aws:iam::723176279592:instance-profile/jason-test-moped-MasterIamInstance-1IG08MPMYZCRB"}, "virtualizationType": "hvm", "blockDeviceMappings": [{"ebs": {"status": "attached", "volumeId": "vol-007a44386e41a5b88", "attachTime": "2020-10-31T22:58:36Z", "deleteOnTermination": true}, "deviceName": "/dev/xvda"}], "capacityReservationId": null, "spotInstanceRequestId": null, "stateTransitionReason": "", "elasticGpuAssociations": null, "capacityReservationSpecification": {"capacityReservationTarget": null, "capacityReservationPreference": "open"}, "elasticInferenceAcceleratorAssociations": null}'),
('7', 'AWS::EC2::Instance',	'i-0c4779103ad377ecb',	'{"tags": [{"key": "aws:autoscaling:groupName", "value": "jason-test-moped-K8sMasterAsg-19R2HBMVTJXW0"}, {"key": "k8s.io/role/master", "value": "master"}, {"key": "Role", "value": "master"}, {"key": "project", "value": "openraven"}, {"key": "aws:cloudformation:stack-name", "value": "jason-test-moped"}, {"key": "kubernetes.io/role/master", "value": "master"}, {"key": "aws:cloudformation:stack-id", "value": "arn:aws:cloudformation:us-east-1:723176279592:stack/jason-test-moped/ac976080-cc3b-11ea-8760-1277bf9feb05"}, {"key": "kubernetes.io/cluster/00g1j8byur0Mk9jaN0h8", "value": "member"}, {"key": "aws:cloudformation:logical-id", "value": "K8sMasterAsg"}], "state": {"code": 16, "name": "running"}, "vpcId": "vpc-054e57edbb9944478", "imageId": "ami-00b6bdd6691d44c19", "keyName": "or-cluster", "kernelId": null, "licenses": null, "platform": null, "subnetId": "subnet-022a263e58d1ada34", "placement": {"hostId": null, "tenancy": "default", "affinity": null, "groupName": "", "spreadDomain": null, "partitionNumber": null, "availabilityZone": "us-east-1a", "hostResourceGroupArn": null}, "ramdiskId": null, "cpuOptions": {"coreCount": 1, "threadsPerCore": 2}, "enaSupport": true, "hypervisor": "xen", "instanceId": "i-0c4779103ad377ecb", "launchTime": "2021-05-05T17:44:27Z", "monitoring": {"state": "disabled"}, "outpostArn": null, "clientToken": "d735e4f6-85fa-0de9-5c04-06c275c40354", "stateReason": null, "architecture": "x86_64", "ebsOptimized": false, "instanceType": "t3.medium", "productCodes": [], "publicDnsName": "", "amiLaunchIndex": 0, "enclaveOptions": {"enabled": false}, "privateDnsName": "ip-10-128-104-217.ec2.internal", "rootDeviceName": "/dev/xvda", "rootDeviceType": "ebs", "securityGroups": [{"groupId": "sg-07a6077c9af3c6801", "groupName": "jason-test-moped-K8SMasterSg-G8DVY8HL584O"}], "metadataOptions": {"state": "applied", "httpTokens": "optional", "httpEndpoint": "enabled", "httpPutResponseHopLimit": 1}, "publicIpAddress": null, "sourceDestCheck": true, "sriovNetSupport": null, "privateIpAddress": "10.128.104.217", "instanceLifecycle": null, "networkInterfaces": [{"vpcId": "vpc-054e57edbb9944478", "groups": [{"groupId": "sg-07a6077c9af3c6801", "groupName": "jason-test-moped-K8SMasterSg-G8DVY8HL584O"}], "status": "in-use", "ownerId": "723176279592", "subnetId": "subnet-022a263e58d1ada34", "attachment": {"status": "attached", "attachTime": "2021-05-05T17:44:27Z", "deviceIndex": 0, "attachmentId": "eni-attach-0c771e496168cdbaa", "networkCardIndex": 0, "deleteOnTermination": true}, "macAddress": "0e:1e:6c:28:e9:35", "association": null, "description": "", "interfaceType": "interface", "ipv6Addresses": [], "privateDnsName": "ip-10-128-104-217.ec2.internal", "sourceDestCheck": true, "privateIpAddress": "10.128.104.217", "networkInterfaceId": "eni-03557dbc5d906f398", "privateIpAddresses": [{"primary": true, "association": null, "privateDnsName": "ip-10-128-104-217.ec2.internal", "privateIpAddress": "10.128.104.217"}]}], "hibernationOptions": {"configured": false}, "iamInstanceProfile": {"id": "AIPA2QYFKJYUC2ABMJC7N", "arn": "arn:aws:iam::723176279592:instance-profile/jason-test-moped-MasterIamInstance-1IG08MPMYZCRB"}, "virtualizationType": "hvm", "blockDeviceMappings": [{"ebs": {"status": "attached", "volumeId": "vol-079cbf84d0f37e9e4", "attachTime": "2021-05-05T17:44:28Z", "deleteOnTermination": true}, "deviceName": "/dev/xvda"}], "capacityReservationId": null, "spotInstanceRequestId": null, "stateTransitionReason": "", "elasticGpuAssociations": null, "capacityReservationSpecification": {"capacityReservationTarget": null, "capacityReservationPreference": "open"}, "elasticInferenceAcceleratorAssociations": null}'),
('8', 'AWS::EC2::Instance',	'i-072c5dcd933c2218a',	'{"tags": [{"key": "aws:cloudformation:logical-id", "value": "ClusterInit"}, {"key": "aws:cloudformation:stack-name", "value": "jason-test-moped"}, {"key": "Role", "value": "master"}, {"key": "aws:cloudformation:stack-id", "value": "arn:aws:cloudformation:us-east-1:723176279592:stack/jason-test-moped/ac976080-cc3b-11ea-8760-1277bf9feb05"}, {"key": "project", "value": "openraven"}, {"key": "Name", "value": "openraven-00g1j8byur0Mk9jaN0h8-init"}], "state": {"code": 16, "name": "running"}, "vpcId": "vpc-054e57edbb9944478", "imageId": "ami-00b6bdd6691d44c19", "keyName": "or-cluster", "kernelId": null, "licenses": null, "platform": null, "subnetId": "subnet-022a263e58d1ada34", "placement": {"hostId": null, "tenancy": "default", "affinity": null, "groupName": "", "spreadDomain": null, "partitionNumber": null, "availabilityZone": "us-east-1a", "hostResourceGroupArn": null}, "ramdiskId": null, "cpuOptions": {"coreCount": 1, "threadsPerCore": 2}, "enaSupport": true, "hypervisor": "xen", "instanceId": "i-072c5dcd933c2218a", "launchTime": "2020-07-22T16:55:19Z", "monitoring": {"state": "disabled"}, "outpostArn": null, "clientToken": "jason-Clust-1WDFQGZVLR2J2", "stateReason": {"code": "Client.InstanceInitiatedShutdown", "message": "Client.InstanceInitiatedShutdown: Instance initiated shutdown"}, "architecture": "x86_64", "ebsOptimized": false, "instanceType": "t3.medium", "productCodes": [], "publicDnsName": "", "amiLaunchIndex": 0, "enclaveOptions": {"enabled": false}, "privateDnsName": "ip-10-128-110-70.ec2.internal", "rootDeviceName": "/dev/xvda", "rootDeviceType": "ebs", "securityGroups": [{"groupId": "sg-07a6077c9af3c6801", "groupName": "jason-test-moped-K8SMasterSg-G8DVY8HL584O"}, {"groupId": "sg-00a0b7c747d5bc8af", "groupName": "jason-test-moped-K8sWorkerSg-JOOSVEFPG5XW"}], "metadataOptions": {"state": "applied", "httpTokens": "optional", "httpEndpoint": "enabled", "httpPutResponseHopLimit": 1}, "publicIpAddress": null, "sourceDestCheck": true, "sriovNetSupport": null, "privateIpAddress": "10.128.110.70", "instanceLifecycle": null, "networkInterfaces": [{"vpcId": "vpc-054e57edbb9944478", "groups": [{"groupId": "sg-07a6077c9af3c6801", "groupName": "jason-test-moped-K8SMasterSg-G8DVY8HL584O"}], "status": "in-use", "ownerId": "723176279592", "subnetId": "subnet-022a263e58d1ada34", "attachment": {"status": "attached", "attachTime": "2020-07-22T16:55:19Z", "deviceIndex": 0, "attachmentId": "eni-attach-0b607338fd2e44c2d", "networkCardIndex": 0, "deleteOnTermination": true}, "macAddress": "0e:d2:46:21:63:9d", "association": null, "description": "", "interfaceType": "interface", "ipv6Addresses": [], "privateDnsName": "ip-10-128-110-70.ec2.internal", "sourceDestCheck": true, "privateIpAddress": "10.128.110.70", "networkInterfaceId": "eni-0a342c9de5737ffaa", "privateIpAddresses": [{"primary": true, "association": null, "privateDnsName": "ip-10-128-110-70.ec2.internal", "privateIpAddress": "10.128.110.70"}]}], "hibernationOptions": {"configured": false}, "iamInstanceProfile": {"id": "AIPA2QYFKJYUGYL74QS5C", "arn": "arn:aws:iam::723176279592:instance-profile/jason-test-moped-ClusterInitInstProfile-1X311LDRVGBL9"}, "virtualizationType": "hvm", "blockDeviceMappings": [{"ebs": {"status": "attached", "volumeId": "vol-0751403fbc1382e8c", "attachTime": "2020-07-22T16:55:20Z", "deleteOnTermination": true}, "deviceName": "/dev/xvda"}], "capacityReservationId": null, "spotInstanceRequestId": null, "stateTransitionReason": "User initiated", "elasticGpuAssociations": null, "capacityReservationSpecification": {"capacityReservationTarget": null, "capacityReservationPreference": "open"}, "elasticInferenceAcceleratorAssociations": null}')
;

-- Expected grouping:
--
--  Group 0    "i-0b2bff5afdc58ef7a"	"subnet-0e0c65d2da128849f"	"sg-00a0b7c747d5bc8af"  -- Stopped (excluded)
--
--  Group 1    "i-0a85540439e24fa0c"	"subnet-022a263e58d1ada34"	"sg-00a0b7c747d5bc8af"
--             "i-0b08e2ff264f600e2"	"subnet-022a263e58d1ada34"	"sg-00a0b7c747d5bc8af"
--             "i-0a4314cb5c5f3f65b"	"subnet-022a263e58d1ada34"	"sg-00a0b7c747d5bc8af"
--
--  Group 2    "i-02d707a66b2c4d632"	"subnet-022a263e58d1ada34"	"sg-07a6077c9af3c6801"
--             "i-0bcfc92093c876165"	"subnet-022a263e58d1ada34"	"sg-07a6077c9af3c6801"
--             "i-0c4779103ad377ecb"	"subnet-022a263e58d1ada34"	"sg-07a6077c9af3c6801"
--
--  Group 3    "i-072c5dcd933c2218a"	"subnet-022a263e58d1ada34"	"sg-07a6077c9af3c6801,sg-00a0b7c747d5bc8af"
