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

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupJob;
import software.amazon.awssdk.services.backup.model.BackupJobState;
import software.amazon.awssdk.services.backup.model.ListBackupJobsRequest;

import java.time.Instant;
import java.time.Period;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BackupUtils {

  private static final Map<Region, BackupClient> CLIENTS = new ConcurrentHashMap<>();

  private static final Period HISTORY = Period.ofDays(45);

  public static List<BackupJob.Builder> listBackupJobs(String arn, Region region, MagpieAWSClientCreator clientCreator) {
    List<BackupJob.Builder> jobs = new LinkedList<>();
    String nextToken = null;
    try (final var client = clientCreator.apply(BackupClient.builder()).region(region).build()) {
      do {
        final var result = client.listBackupJobs(ListBackupJobsRequest.builder()
          .byResourceArn(arn)
          .byCreatedAfter(Instant.now().minus(HISTORY))
          .byState(BackupJobState.COMPLETED)
          .nextToken(nextToken)
          .build());
        jobs.addAll(result.backupJobs().stream().map(BackupJob::toBuilder).collect(Collectors.toList()));
        nextToken = result.nextToken();

      } while (nextToken != null);
    }
    return jobs;
  }
}
