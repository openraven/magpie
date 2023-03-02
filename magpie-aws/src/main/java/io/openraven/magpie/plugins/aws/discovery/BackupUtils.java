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
package io.openraven.magpie.plugins.aws.discovery;

import io.openraven.magpie.data.aws.backup.BackupPlan;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupJob;
import software.amazon.awssdk.services.backup.model.BackupJobState;
import software.amazon.awssdk.services.backup.model.ListBackupJobsRequest;

import java.time.Instant;
import java.time.Period;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BackupUtils {

    private static final Period HISTORY = Period.ofDays(45);
    public static final String UNSUPPORTED_RESOURCE_TYPE_STRING_INDICATOR = "Unsupported resource type";

    public static List<BackupJob.Builder> listBackupJobs(String arn, Region region, MagpieAWSClientCreator clientCreator, Logger logger) {
        var retries = new AtomicInteger(5);
        List<BackupJob.Builder> jobs = new LinkedList<>();
        while (retries.get() > 0) {
            try {
                try (final var client = clientCreator.apply(BackupClient.builder()).region(region).build()) {
                    final var builder = ListBackupJobsRequest.builder()
                            .byResourceArn(arn)
                            .byCreatedAfter(Instant.now().minus(HISTORY))
                            .maxResults(1000)
                            .byState(BackupJobState.COMPLETED);
                    final var result = client.listBackupJobsPaginator(builder.build());
                    result.forEach(response -> jobs.addAll(response.backupJobs().stream().map(BackupJob::toBuilder).collect(Collectors.toList())));
                    break;
                }
            } catch (SdkClientException ex) {

                if (retries.get() == 0) {
                    throw ex;
                }

                logger.warn("Couldn't list backup jobs for {}, retrying {} more times", arn, retries);
            }
            // if we get any exceptions here we continue, although some are ok/expected
            catch (Exception ex) {
                // if we get "Unsupported resource type" this is a-ok (it just means we're trying to get backup jobs
                // for resources AWS have told us to, but aren't released yet).
                if (String.valueOf(ex.getMessage()).contains(UNSUPPORTED_RESOURCE_TYPE_STRING_INDICATOR)) {
                    break;
                }
                DiscoveryExceptions.onDiscoveryException(BackupPlan.RESOURCE_TYPE, arn, region, ex);
            }finally {
                retries.decrementAndGet();
            }
        }
        return jobs;
    }
}
