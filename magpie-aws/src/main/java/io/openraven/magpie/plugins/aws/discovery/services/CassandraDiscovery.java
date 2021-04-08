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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.aws.mcs.auth.SigV4AuthProvider;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static java.lang.String.format;

public class CassandraDiscovery implements AWSDiscovery {

  private static final String SERVICE = "cassandra";

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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    List<InetSocketAddress> contactPoints = Collections.singletonList(
      InetSocketAddress.createUnresolved(format("cassandra.%s.amazonaws.com", region), 9142));

    SSLContext sslContext;
    try {
      sslContext = SSLContext.getDefault();

      try (CqlSession cqlSession = CqlSession.builder()
        .addContactPoints(contactPoints)
        .withSslContext(sslContext)
        .withLocalDatacenter(region.id())
        .withAuthProvider(new SigV4AuthProvider(region.id()))
        .build()) {
        doRun(region, cqlSession, emitter, logger, mapper, session);
      }
    } catch (NoSuchAlgorithmException e) {
      logger.error("NoSuchAlgorithmException when getting SSLContext");
    }
  }

  private void doRun(Region region, CqlSession cqlSession, Emitter emitter, Logger logger, ObjectMapper mapper, Session session) {
    var keyspaces = cqlSession.execute("select * from system_schema.keyspaces");

    keyspaces.forEach(keyspace -> {
      try {
        String keyspaceName = keyspace.getString("keyspace_name");

        var data = mapper.createObjectNode();
        data.putPOJO("name", keyspaceName);
        data.put("region", region.toString());

        discoverTables(cqlSession, keyspaceName, data);
        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":keyspace"), data));
      }
      catch (Exception e){
        logger.debug("Keyspaces discovery error in {}.", region, e);
      }
    });
  }

  private void discoverTables(CqlSession session, String keyspaceName, ObjectNode data) {
    var tables = new ArrayList<String>();

    String tablesQuery = String.format("SELECT keyspace_name, table_name, status FROM system_schema_mcs.tables WHERE keyspace_name = '%s'", keyspaceName);
    session.execute(tablesQuery).forEach(table -> tables.add(table.getString("table_name")));

    AWSUtils.update(data, Map.of("tables", tables));
  }
}
