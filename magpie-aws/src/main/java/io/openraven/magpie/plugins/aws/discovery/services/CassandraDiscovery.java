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

package io.openraven.magpie.plugins.aws.discovery.services;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.cassandra.CassandraKeyspace;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.aws.mcs.auth.SigV4AuthProvider;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class CassandraDiscovery implements AWSDiscovery {

  private static final String SERVICE = "cassandra";

  private static final String RESOURCE_TYPE = CassandraKeyspace.RESOURCE_TYPE;

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    // https://docs.aws.amazon.com/keyspaces/latest/devguide/programmatic.endpoints.html
    return Arrays.asList(
      Region.US_EAST_1,
      Region.US_EAST_2,
      Region.US_WEST_1,
      Region.US_WEST_2,
      Region.AP_EAST_1,
      Region.AP_SOUTH_1,
      Region.AP_NORTHEAST_1,
      Region.AP_NORTHEAST_2,
      Region.AP_SOUTHEAST_1,
      Region.AP_SOUTHEAST_2,
      Region.CA_CENTRAL_1,
      Region.CN_NORTH_1,
      Region.CN_NORTHWEST_1,
      Region.EU_CENTRAL_1,
      Region.EU_WEST_1,
      Region.EU_WEST_2,
      Region.EU_WEST_3,
      Region.EU_NORTH_1,
      Region.SA_EAST_1);
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    List<InetSocketAddress> contactPoints = Collections.singletonList(
      InetSocketAddress.createUnresolved(format("cassandra.%s.amazonaws.com", region), 9142));
    try {
      SSLContext sslContext;
      try {
        sslContext = SSLContext.getDefault();

        try (CqlSession cqlSession = CqlSession.builder()
          .addContactPoints(contactPoints)
          .withSslContext(sslContext)
          .withLocalDatacenter(region.id())
          .withAuthProvider(new SigV4AuthProvider(region.id()))
          .build()) {
          doRun(region, cqlSession, emitter, logger, mapper, session, account);
        }
      } catch (NoSuchAlgorithmException e) {
        logger.error("NoSuchAlgorithmException when getting SSLContext");
      }
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void doRun(Region region, CqlSession cqlSession, Emitter emitter, Logger logger, ObjectMapper mapper, Session session, String account) {
    var keyspaces = cqlSession.execute("select * from system_schema.keyspaces");

    keyspaces.forEach(keyspace -> {
      try {
        String keyspaceName = keyspace.getString("keyspace_name");

        String arn = format("arn:aws:cassandra:keyspace:%s::%s", region, keyspaceName);
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
          .withResourceName(keyspaceName)
          .withResourceId(keyspaceName)
          .withResourceType(RESOURCE_TYPE)
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        discoverTables(cqlSession, keyspaceName, data);
        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":keyspace"), data.toJsonNode()));
      } catch (Exception ex) {
        DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
      }
    });
  }

  private void discoverTables(CqlSession session, String keyspaceName, MagpieAwsResource data) {
    var tables = new ArrayList<String>();

    String tablesQuery = String.format("SELECT keyspace_name, table_name, status FROM system_schema_mcs.tables WHERE keyspace_name = '%s'", keyspaceName);
    session.execute(tablesQuery).forEach(table -> tables.add(table.getString("table_name")));

    AWSUtils.update(data.supplementaryConfiguration, Map.of("Tables", tables));
  }
}
