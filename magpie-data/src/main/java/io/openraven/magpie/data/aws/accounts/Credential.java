/*-
 * #%L
 * Magpie API
 * %%
 * Copyright (C) 2021 Open Raven Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
package io.openraven.magpie.data.aws.accounts;

public class Credential {
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

    @SuppressWarnings("unused")
    public Credential() {
    }

    /** Accepts the CSV emitted by AWS's credential report, and splits on {@code ,}. */
    public Credential(String line) {
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
