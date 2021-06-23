package io.openraven.magpie.plugins.persist;

import org.flywaydb.core.Flyway;

import static java.lang.String.format;

public class FlywayMigrationService {

  private static final String POSTGRES_URL = "jdbc:postgresql://%s:%s/%s";

  public static void initiateDBMigration(PersistConfig config) {
    String databaseUrl = format(POSTGRES_URL, config.getHostname(), config.getPort(), config.getDatabaseName());
    Flyway
      .configure()
      .dataSource(databaseUrl, config.getUser(), config.getPassword())
      .load()
      .migrate();
  }
}
