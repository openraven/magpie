package io.openraven.magpie.core.dmap.model;

public class EC2Target {
  private final String resourceId;
  private final String ipAddress;

  public EC2Target(String resourceId, String ipAddress) {
    this.resourceId = resourceId;
    this.ipAddress = ipAddress;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EC2Target ec2Target = (EC2Target) o;

    if (!resourceId.equals(ec2Target.resourceId)) return false;
    return ipAddress.equals(ec2Target.ipAddress);
  }

  @Override
  public int hashCode() {
    int result = resourceId.hashCode();
    result = 31 * result + ipAddress.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "EC2Target{" +
      "resourceId='" + resourceId + '\'' +
      ", ipAddress='" + ipAddress + '\'' +
      '}';
  }
}
