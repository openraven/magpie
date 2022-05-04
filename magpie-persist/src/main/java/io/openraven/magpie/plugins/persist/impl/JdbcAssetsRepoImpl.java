package io.openraven.magpie.plugins.persist.impl;

import io.openraven.magpie.data.Resource;
import io.openraven.magpie.plugins.persist.AssetsRepo;
import io.openraven.magpie.plugins.persist.PersistConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Table;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcAssetsRepoImpl implements AssetsRepo {

  //todo the problem here is that the tables in the current database schema do not all specify a unique constraint
  // on either arn or documentid and therefore upsert based on uniqueness isn't possible (TBC)
  private static final String UPDATE_SQL_MIDDLE = "(documentId, arn, resourcename, resourceid, resourcetype, awsregion, awsaccountid," +
    " creatediso, updatediso, discoverysessionid, tags, configuration, supplementaryconfiguration, discoverymeta) VALUES(?,?,?,?,?,?,?,?,?,?,? ::jsonb,? ::jsonb,? ::jsonb,? ::jsonb) "
    + "ON CONFLICT (arn) DO UPDATE SET ";

  private final Logger logger = LoggerFactory.getLogger(JdbcAssetsRepoImpl.class);

  private Connection connection;

  public JdbcAssetsRepoImpl(PersistConfig persistConfig) {
    getConnection(persistConfig);
  }

  private void getConnection(PersistConfig persistConfig) {
    try {
      Class.forName("org.postgresql.Driver");
      connection = DriverManager.getConnection("jdbc:postgresql://" + persistConfig.getHostname() + "/" + persistConfig.getDatabaseName(), persistConfig.getUser(), persistConfig.getPassword());
      connection.setAutoCommit(true);
    } catch (Exception e) {
      logger.error("Unable to connect to the database", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void upsert(Resource resource) {
    try {
      String tableName = tableNameForResource(resource);
      Class<? extends Resource> resourceClass = resource.getClass();

      Map<String, Object> fieldNameToValue = getResourceFields(resourceClass.getFields(), resource);
      String resourceUpdateSql = "INSERT INTO " + tableName + UPDATE_SQL_MIDDLE + buildUpdateSql(fieldNameToValue);
      PreparedStatement preparedStatement = connection.prepareStatement(resourceUpdateSql);
      populatePreparedStatement(fieldNameToValue, preparedStatement);
      preparedStatement.executeUpdate();
    } catch (Exception exception) {
      logger.error("Error during database upsert, rolling back.", exception);
      try {
        connection.rollback();
      } catch (SQLException ex) {
        logger.error("Error rolling back.", exception);
      }
    }
  }

  @Override
  public void upsert(List<Resource> awsResources) {
    Map<String, PreparedStatement> preparedStatementMap = new HashMap<>();
    try {
      for (Resource resource : awsResources) {
        String tableName = tableNameForResource(resource);
        Class<? extends Resource> resourceClass = resource.getClass();
        Map<String, Object> fieldNameToValue = getResourceFields(resourceClass.getFields(), resource);

        PreparedStatement preparedStatement = preparedStatementMap.computeIfAbsent(tableName, t -> {
          try {
            return connection.prepareStatement("INSERT INTO " + tableName + UPDATE_SQL_MIDDLE + buildUpdateSql(fieldNameToValue));
          } catch (SQLException ex) {
            logger.error("Error creating prepared statement.", ex);
            throw new RuntimeException(ex);
          }
        });
        populatePreparedStatement(fieldNameToValue, preparedStatement);
        preparedStatement.addBatch();
      }

      for (PreparedStatement preparedStatement : preparedStatementMap.values()) {
        preparedStatement.executeUpdate();
      }

    }  catch (Exception exception) {
      logger.error("Error during database upsert, rolling back.", exception);
      try {
        connection.rollback();
      } catch (SQLException ex) {
        logger.error("Error rolling back.", exception);
      }
    }
  }

  @Override
  public void executeNative(String query) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Map<String, Object>> queryNative(String query) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Long getAssetCount(String resourceType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    try {
      connection.close();
    } catch (SQLException e) {
      logger.error("Error closing connection.", e);
    }
  }

  private String tableNameForResource(Resource resource) throws SQLException {
    Class<? extends Resource> resourceClass = resource.getClass();
    Table table = resourceClass.getAnnotation(Table.class);

    if (table == null) {
      String error = "Unable to determine table name from given resource";
      logger.error(error);
      throw new SQLException(error);
    }

    return table.name();
  }

  private Map<String, Object> getResourceFields(Field[] resourceClass, Resource resource) throws IllegalAccessException {
    Map<String, Object> fieldNameToValue = new HashMap<>();

    for (Field f : resourceClass) {
      fieldNameToValue.put(f.getName(), f.get(resource));
    }
    return fieldNameToValue;
  }

  private void populatePreparedStatement(Map<String, Object> fieldNameToValue, PreparedStatement preparedStatement) throws SQLException {
    preparedStatement.setString(1, String.valueOf(fieldNameToValue.get("documentId")));
    preparedStatement.setString(2, String.valueOf(fieldNameToValue.get("arn")));
    preparedStatement.setString(3, String.valueOf(fieldNameToValue.get("resourceName")));
    preparedStatement.setString(4, String.valueOf(fieldNameToValue.get("resourceId")));
    preparedStatement.setString(5, String.valueOf(fieldNameToValue.get("resourceType")));
    preparedStatement.setString(6, String.valueOf(fieldNameToValue.get("awsRegion")));
    preparedStatement.setString(7, String.valueOf(fieldNameToValue.get("awsAccountId")));
    preparedStatement.setTimestamp(8, fieldNameToValue.get("createdIso") != null ? Timestamp.from((Instant) fieldNameToValue.get("createdIso")) : null);
    preparedStatement.setTimestamp(9, fieldNameToValue.get("updatedIso") != null ? Timestamp.from((Instant) fieldNameToValue.get("updatedIso")) : null);
    preparedStatement.setString(10,  String.valueOf(fieldNameToValue.get("discoverySessionId")));
    preparedStatement.setString(11,  String.valueOf(fieldNameToValue.get("tags")));
    preparedStatement.setString(12,  String.valueOf(fieldNameToValue.get("configuration")));
    preparedStatement.setString(13,  String.valueOf(fieldNameToValue.get("supplementaryConfiguration")));
    preparedStatement.setString(14,  String.valueOf(fieldNameToValue.get("discoveryMeta")));
  }

  private String buildUpdateSql(Map<String, Object> fieldNameToValue) {
    return "arn = '"
      + fieldNameToValue.get("arn").toString() + "', resourcename = '"
      + fieldNameToValue.get("resourceName").toString() + "', resourceid = '"
      + fieldNameToValue.get("resourceId").toString() + "', resourcetype = '"
      + fieldNameToValue.get("resourceType").toString() + "', awsregion = '"
      + fieldNameToValue.get("awsRegion").toString() + "', awsaccountid = '"
      + fieldNameToValue.get("awsaccountId").toString() + "', creatediso = '"
      + (fieldNameToValue.get("creatediso") != null ? fieldNameToValue.get("creatediso").toString() : "") + "', updatediso  = '"
      + (fieldNameToValue.get("updatediso") != null ? fieldNameToValue.get("updatediso").toString() : "") + "', discoverysessionid = '"
      + fieldNameToValue.get("discoverySessionId").toString() + "', tags = '"
      + fieldNameToValue.get("tags").toString() + "', configuration = '"
      + fieldNameToValue.get("configuration").toString() + "', supplementaryconfiguration = '"
      + fieldNameToValue.get("supplementaryConfiguration").toString() + "', discoverymeta = '"
      + fieldNameToValue.get("discoveryMeta").toString() + "';";
  }
}
