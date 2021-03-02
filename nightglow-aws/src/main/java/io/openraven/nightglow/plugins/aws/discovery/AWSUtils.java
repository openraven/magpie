package io.openraven.nightglow.plugins.aws.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AWSUtils {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final JsonNode NULL_NODE = MAPPER.nullNode();

  /**
   * @param resp will be provided the output from calling {@code fn}, or @param noresp a {@code NullNode} in the 403 or 404 case
   * @throws SdkServiceException if it is not one of the 403 or 404 status codes
   */
  public static <R> void getAwsResponse(Supplier<R> fn, Consumer<R> resp, Consumer<JsonNode> noresp) throws SdkClientException, SdkServiceException {
    try {
      R ret = fn.get();
      resp.accept(ret);
    }
    catch (SdkServiceException ex) {
      if (ex.statusCode() >= 400 && ex.statusCode() < 500) {
        noresp.accept(NULL_NODE);
      }
      else {
        throw ex;
      }
    }
  }
}
