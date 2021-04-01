/*
 * ***********************************************************
 * Copyright, 2020, Open Raven Inc.
 * APACHE LICENSE, VERSION 2.0
 * https://www.openraven.com/legal/apache-2-license
 * *********************************************************
 */
package io.openraven.magpie.plugins.aws.discovery.services;

public class IAMCredential {
  public String user;
  public String arn;
  public String user_creation_time;
  public String password_enabled;
  public String password_last_used;
  public String password_last_changed;
  public String password_next_rotation;
  public String mfa_active;
  public String access_key_1_active;
  public String access_key_1_last_rotated;
  public String access_key_1_last_used_date;
  public String access_key_1_last_used_region;
  public String access_key_1_last_used_service;
  public String access_key_2_active;
  public String access_key_2_last_rotated;
  public String access_key_2_last_used_date;
  public String access_key_2_last_used_region;
  public String access_key_2_last_used_service;
  public String cert_1_active;
  public String cert_1_last_rotated;
  public String cert_2_active;
  public String cert_2_last_rotated;

  public IAMCredential() {
  }

  public IAMCredential(String line) {
    String[] values = line.split(",");

    this.user = values[0];
    this.arn = values[1];
    this.user_creation_time = getNullIfNotPresentOrValue(values[2]);
    this.password_enabled = values[3];
    this.password_last_used = getNullIfNotPresentOrValue(values[4]);
    this.password_last_changed = getNullIfNotPresentOrValue(values[5]);
    this.password_next_rotation = getNullIfNotPresentOrValue(values[6]);
    this.mfa_active = values[7];
    this.access_key_1_active = values[8];
    this.access_key_1_last_rotated = getNullIfNotPresentOrValue(values[9]);
    this.access_key_1_last_used_date = getNullIfNotPresentOrValue(values[10]);
    this.access_key_1_last_used_region = values[11];
    this.access_key_1_last_used_service = values[12];
    this.access_key_2_active = values[13];
    this.access_key_2_last_rotated = getNullIfNotPresentOrValue(values[14]);
    this.access_key_2_last_used_date = getNullIfNotPresentOrValue(values[15]);
    this.access_key_2_last_used_region = values[16];
    this.access_key_2_last_used_service = values[17];
    this.cert_1_active = values[18];
    this.cert_1_last_rotated = getNullIfNotPresentOrValue(values[19]);
    this.cert_2_active = values[20];
    this.cert_2_last_rotated = getNullIfNotPresentOrValue(values[21]);
  }

  private String getNullIfNotPresentOrValue(String string) {
    if(string.equals("N/A") || string.equals("not_supported")) {
      return null;
    } else {
      return string;
    }
  }
}

