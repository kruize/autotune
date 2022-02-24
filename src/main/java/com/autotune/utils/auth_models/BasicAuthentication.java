/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.utils.auth_models;

import java.util.Base64;

public class BasicAuthentication {
    private String userName;
    private String password;
    private String base64String;

    public BasicAuthentication(String userName, String password) {
        this.userName = userName;
        this.password = password;
        String usernamePassword = this.userName + ":" + this.password;
        this.setBase64String(Base64.getEncoder().encodeToString(usernamePassword.getBytes()));
    }

    public BasicAuthentication(String base64String) {
        this.base64String = base64String;
    }

    private String getBase64String() {
        return base64String;
    }

    private void setBase64String(String base64String) {
        this.base64String = base64String;
    }

    public String getAuthHeader() {
        return "Basic " + this.base64String;
    }
}
