/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.target;

import com.autotune.common.target.common.exception.TargetHandlerConnectException;
import com.autotune.common.target.common.exception.TargetHandlerException;
import com.autotune.common.target.common.main.TargetHandler;
import com.autotune.common.target.kubernetes.KubernetesTargetHandler;
import com.autotune.common.target.kubernetes.model.ContainerConfigData;
import com.google.gson.Gson;
import org.json.JSONObject;

import static com.autotune.common.target.kubernetes.params.KubeConstants.*;

/**
 * Test class to demonstrate TargetHandler abstraction
 * Currently Target considered for Kubernetes only.
 */
public class TargetHandlerMainTest {
    public static void main(String[] args) {
        /**
         * Create TargetHandler object and call implemented methode like
         * deployApplication
         * collectMetrics
         * getNamespaces
         * etc.
         */
        JSONObject containerConfigJSON = initiate();

        TargetHandler targetHandler = new KubernetesTargetHandler();
        try {
            targetHandler.deployApplication(new JSONObject()
                    .put(NAMESPACE, "monitoring")
                    .put(DEPLOYMENT_NAME, "tfb")
                    .put(CONTAINER_CONFIG_DATA, containerConfigJSON)
            );
        } catch (TargetHandlerException e) {
            e.printStackTrace();
        }finally {
            targetHandler.shutdownConnection();
        }
    }

    private static JSONObject initiate() {
        ContainerConfigData containerConfigData = new ContainerConfigData();
        String containerConfigJSONStr = new Gson().toJson(containerConfigData);
        return new JSONObject(containerConfigJSONStr);
    }
}
