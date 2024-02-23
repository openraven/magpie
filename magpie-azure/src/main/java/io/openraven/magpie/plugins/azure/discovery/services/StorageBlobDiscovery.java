package io.openraven.magpie.plugins.azure.discovery.services;

import com.azure.core.credential.TokenCredential;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;

public class StorageBlobDiscovery implements AzureDiscovery{

  private static final String SERVICE = "storage";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, TokenCredential creds, String account) {
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    logger.info("Discovering storage");
    // Let's start with Storage Blob Containers
    BlobServiceClient client;
    if(creds == null) {
        client = new BlobServiceClientBuilder()
                .connectionString(System.getenv("AZURE_STORAGE_CONNECTION_STRING"))
                .buildClient();
    } else {
        client = new BlobServiceClientBuilder().credential(creds).buildClient();
    }

    //List the blob containers in the storage account
    logger.info("Listing blob containers");
    try {
      System.out.println(mapper.writeValueAsString(client.getAccountName()));
      for (BlobContainerItem containerItem : client.listBlobContainers()) {
          System.out.println(mapper.writeValueAsString(containerItem));
        }
      } catch (JsonProcessingException e) {
      logger.error("Couldn't deserialize", e);
    }




  }
}
