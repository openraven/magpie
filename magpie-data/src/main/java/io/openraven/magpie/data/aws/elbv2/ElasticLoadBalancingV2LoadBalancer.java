/*-
 * #%L
 * Magpie API
 * %%
 * Copyright (C) 2021 Open Raven Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openraven.magpie.data.aws.elbv2;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;

// v2 of ELB AWS SDK supports application and network load balancers
@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = ElasticLoadBalancingV2LoadBalancer.TABLE_NAME)
public class ElasticLoadBalancingV2LoadBalancer extends AWSResource {

  protected static final String TABLE_NAME = "awselasticloadbalancingv2loadbalancer";
    public static final String RESOURCE_TYPE = "AWS::ElasticLoadBalancingV2::LoadBalancer";

    public ElasticLoadBalancingV2LoadBalancer() {
        this.resourceType = RESOURCE_TYPE;
    }

    public ElasticLoadBalancingV2LoadBalancer(LoadBalancer loadBalancer, String region, String accountId) {
        this.resourceName = loadBalancer.dnsName();
        this.resourceId = loadBalancer.loadBalancerName();
        this.awsRegion = region;
        this.awsAccountId = accountId;
        this.resourceType = RESOURCE_TYPE;
        this.arn = loadBalancer.loadBalancerArn();
        this.configuration = PayloadUtils.update(loadBalancer);
        this.createdIso = loadBalancer.createdTime();
    }
}
