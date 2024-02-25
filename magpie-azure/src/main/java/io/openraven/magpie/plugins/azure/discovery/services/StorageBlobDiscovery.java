package io.openraven.magpie.plugins.azure.discovery.services;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.Configuration;
import com.azure.core.util.ConfigurationBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;

import java.util.Map;

public class StorageBlobDiscovery implements AzureDiscovery{

  private static final String SERVICE = "storage";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile) {
    logger.info("Discovering storage");

      azrm.storageAccounts().list().forEach(sa -> {
        logger.info("Found StorageAccount: {}", sa);
      });
//      storageAccounts.forEach(storageAccount ->
//              storageAccount.);
    //List the blob containers in the storage account
//    logger.info("Listing blob containers");
//    try {
//      System.out.println(mapper.writeValueAsString(client.getAccountName()));
//      for (BlobContainerItem containerItem : client.listBlobContainers()) {
//          System.out.println(mapper.writeValueAsString(containerItem));
//        }
//      } catch (JsonProcessingException e) {
//      logger.error("Couldn't deserialize", e);
//    }
//
//
//

  }
}
