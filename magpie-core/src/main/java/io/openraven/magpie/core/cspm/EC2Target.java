package io.openraven.magpie.core.cspm;

public class EC2Target {
  private String serviceId;
  private String ipAddress;

  public EC2Target(String serviceId, String ipAddress) {
    this.serviceId = serviceId;
    this.ipAddress = ipAddress;
  }

  public String getServiceId() {
    return serviceId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EC2Target ec2Target = (EC2Target) o;

    if (!serviceId.equals(ec2Target.serviceId)) return false;
    return ipAddress.equals(ec2Target.ipAddress);
  }

  @Override
  public int hashCode() {
    int result = serviceId.hashCode();
    result = 31 * result + ipAddress.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "EC2Target{" +
      "serviceId='" + serviceId + '\'' +
      ", ipAddress='" + ipAddress + '\'' +
      '}';
  }
}
