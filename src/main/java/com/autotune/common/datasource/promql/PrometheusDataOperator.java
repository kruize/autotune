/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.datasource.promql;

import com.autotune.common.datasource.KruizeDataSourceOperator;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.GenericRestApiClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class PrometheusDataOperator implements KruizeDataSourceOperator {
    private static PrometheusDataOperator prometheusDataOperator = null;
    private PrometheusDataOperator() {

    }

    public static PrometheusDataOperator getInstance() {
        if (null == prometheusDataOperator) {
            prometheusDataOperator = new PrometheusDataOperator();
        }
        return prometheusDataOperator;
    }

    @Override
    public Object extract(String url, String query) {
        GenericRestApiClient apiClient = new GenericRestApiClient(
                CommonUtils.getBaseDataSourceUrl(
                        url,
                        KruizeConstants.SupportedDatasources.PROMETHEUS
                )
        );
        if (null == apiClient) {
            return null;
        }
        try {
            JSONObject jsonObject = apiClient.fetchMetricsJson(
                    KruizeConstants.HttpConstants.MethodType.GET,
                    query);
            if (!jsonObject.has("status"))
                return null;
            if (!jsonObject.getString("status").equalsIgnoreCase("success"))
                return null;
            if (!jsonObject.has("data"))
                return null;
            if (!jsonObject.getJSONObject("data").has("result"))
                return null;
            if (jsonObject.getJSONObject("data").getJSONArray("result").isEmpty())
                return  null;
            JSONArray result = jsonObject.getJSONObject("data").getJSONArray("result");
            for (Object result_obj: result) {
                JSONObject result_json = (JSONObject) result_obj;
                if (result_json.has("value")
                        && !result_json.getJSONArray("value").isEmpty()) {
                    return result_json.getJSONArray("value").getString(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }
}
