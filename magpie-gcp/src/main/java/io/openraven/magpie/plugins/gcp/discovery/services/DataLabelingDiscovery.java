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

package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.datalabeling.v1beta1.AnnotatedDataset;
import com.google.cloud.datalabeling.v1beta1.DataItem;
import com.google.cloud.datalabeling.v1beta1.DataLabelingServiceClient;
import com.google.cloud.datalabeling.v1beta1.DataLabelingServiceSettings;
import com.google.cloud.datalabeling.v1beta1.Dataset;
import com.google.cloud.secretmanager.v1.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.data.DataLabelingAnnotations;
import io.openraven.magpie.data.gcp.data.DataLabelingDataset;
import io.openraven.magpie.data.gcp.data.DataLabelingInstruction;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DataLabelingDiscovery implements GCPDiscovery {
  private static final String SERVICE = "dataLabeling";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    var builder = DataLabelingServiceSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);
    try (DataLabelingServiceClient dataLabelingServiceClient = DataLabelingServiceClient.create(builder.build())) {
      discoverDatasets(mapper, projectId, session, emitter, dataLabelingServiceClient);
      discoverInstructions(mapper, projectId, session, emitter, dataLabelingServiceClient);
      discoverAnnotationSpecSet(mapper, projectId, session, emitter, dataLabelingServiceClient);
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("DataLabeling", e);
    }
  }

  private void discoverDatasets(ObjectMapper mapper, String projectId, Session session, Emitter emitter, DataLabelingServiceClient dataLabelingServiceClient) {
    final String RESOURCE_TYPE = DataLabelingDataset.RESOURCE_TYPE;

    for (var dataset : dataLabelingServiceClient.listDatasets(ProjectName.of(projectId).toString(), "").iterateAll()) {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, dataset.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(dataset))
        .build();

      discoverAnnotatedDatasets(dataLabelingServiceClient, dataset, data);
      discoverDataItems(dataLabelingServiceClient, dataset, data);

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":dataset"), data.toJsonNode()));
    }
  }

  private void discoverAnnotatedDatasets(DataLabelingServiceClient dataLabelingServiceClient, Dataset dataset, MagpieGcpResource data) {
    final String fieldName = "annotatedDatasets";

    ArrayList<AnnotatedDataset.Builder> list = new ArrayList<>();
    dataLabelingServiceClient.listAnnotatedDatasets(dataset.getName(), "").iterateAll()
      .forEach(device -> list.add(device.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }

  private void discoverDataItems(DataLabelingServiceClient dataLabelingServiceClient, Dataset dataset, MagpieGcpResource data) {
    final String fieldName = "dataItems";

    ArrayList<DataItem.Builder> list = new ArrayList<>();
    dataLabelingServiceClient.listDataItems(dataset.getName(), "").iterateAll()
      .forEach(device -> list.add(device.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }

  private void discoverInstructions(ObjectMapper mapper, String projectId, Session session, Emitter emitter, DataLabelingServiceClient dataLabelingServiceClient) {
    final String RESOURCE_TYPE = DataLabelingInstruction.RESOURCE_TYPE;

    for (var instruction : dataLabelingServiceClient.listInstructions(ProjectName.of(projectId).toString(), "").iterateAll()) {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, instruction.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(instruction))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":instruction"), data.toJsonNode()));
    }
  }

  private void discoverAnnotationSpecSet(ObjectMapper mapper, String projectId, Session session, Emitter emitter, DataLabelingServiceClient dataLabelingServiceClient) {
    final String RESOURCE_TYPE = DataLabelingAnnotations.RESOURCE_TYPE;

    for (var annotationSpecSet : dataLabelingServiceClient.listAnnotationSpecSets(ProjectName.of(projectId).toString(), "").iterateAll()) {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, annotationSpecSet.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(annotationSpecSet))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":annotationSpecSet"), data.toJsonNode()));
    }
  }
}
