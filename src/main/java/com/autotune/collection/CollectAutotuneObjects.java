/*******************************************************************************
 * Copyright (c) 2020 Red Hat, IBM Corporation and others.
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
package com.autotune.collection;

import com.autotune.application.Application;
import com.autotune.application.Layer;
import com.autotune.application.Query;
import com.autotune.application.Tunable;
import com.autotune.env.EnvInfo;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class CollectAutotuneObjects
{
    public static ArrayList<AutotuneObject> autotuneObjectList = new ArrayList<>();;
    static final String group = "recommender.com";
    static final String scope = "Namespaced";
    static final String plural = "autotunes";
    static final String apiVersion = "v1beta1";
    static final String resourceName = "autotunes.recommender.com";
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectAutotuneObjects.class);

    private static final String mainConfigLocation = "config/main_config.json";
    private static final String layerConfigRoot = "config/layer_configs/level";
    private static final String getAllApps = "getAllAppsQuery";

    /**
     * Get Autotune objects from kubernetes, and watch for any additions, modifications or deletions.
     * Add obtained autotune objects to monitoring list and match autotune object with pods.
     *
     * @throws IOException if unable to get Kubernetes config
     */
    public void getAutotuneObjects() throws IOException
    {
        KubernetesClient client = new DefaultKubernetesClient();

        CustomResourceDefinitionContext autotuneCrdContext = new CustomResourceDefinitionContext
                .Builder()
                .withGroup(group)
                .withScope(scope)
                .withPlural(plural)
                .withVersion(apiVersion)
                .withName(resourceName)
                .build();

        /* Watch for events (additions, modifications or deletions) of autotune objects */
        Watcher<String> autotuneObjectWatcher = new Watcher<String>() {
            @Override
            public void eventReceived(Action action, String resource) {
                switch (action.toString().toUpperCase()) {
                    case "ADDED":
                        addAutotuneObjectToList(resource);
                        matchPodsToAutotuneObject(client);
                        try {
                            CollectAutotuneObjects.readConfigFiles();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onClose(KubernetesClientException e) { }
        };

        /* Register custom watcher for autotune object */
        client.customResource(autotuneCrdContext).watch(autotuneObjectWatcher);
    }

    /**
     * Match pods in the cluster using the labels and value provided in the autotune object.
     *
     * @param client KubernetesClient to get pods in cluster
     */
    private void matchPodsToAutotuneObject(KubernetesClient client)
    {
        for (AutotuneObject autotuneObject : autotuneObjectList)
        {
            String labelKey = autotuneObject.getSelectorInfo().getMatchLabel();
            String labelValue = autotuneObject.getSelectorInfo().getMatchValue();
            String type = autotuneObject.getTypeInfo().getTypeName();

            PodList podList = client.pods().withLabel(labelKey).list();

            for (Pod pod : podList.getItems())
            {
                ObjectMeta podMetadata = pod.getMetadata();
                if (podMetadata.getLabels().get(labelKey).equals(labelValue))
                {
                    LOGGER.info("Pod: " + pod.getMetadata().getName() + " matched to autotune object " + autotuneObject.getName());
                    //TODO check if already present
                    Application application = new Application(podMetadata.getName(), podMetadata.getNamespace(), type);
                    autotuneObject.applicationsMap.put(podMetadata.getName(), application);
                }
            }
        }
    }

    /**
     * Add Autotune object to monitoring list
     *
     * @param autotuneObject JSON string of the autotune object
     */
    private void addAutotuneObjectToList(String autotuneObject)
    {
        JSONObject autotuneObjectJson = new JSONObject(autotuneObject);
        TypeInfo typeInfo = new TypeInfo();
        SelectorInfo selectorInfo = new SelectorInfo();

        JSONObject specJson = autotuneObjectJson.getJSONObject("spec");

        JSONObject slaJson = specJson.getJSONObject("sla");
        typeInfo.setTypeName(slaJson.getString("name"));
        typeInfo.setTypeValue(slaJson.getString("value"));

        JSONObject selectorJson = specJson.getJSONObject("selector");
        selectorInfo.setMatchLabel(selectorJson.getString("matchLabel"));
        selectorInfo.setMatchValue(selectorJson.getString("matchValue"));
        selectorInfo.setMatchRoute(selectorJson.getString("matchRoute"));
        //CHANGE RUI TO URI LATER
        selectorInfo.setMatchURI(selectorJson.getString("matchRUI"));
        selectorInfo.setMatchService(selectorJson.getString("matchService"));

        String mode = specJson.getString("mode");
        int replicas = specJson.getInt("replicas");

        String name = autotuneObjectJson.getJSONObject("metadata").getString("name");
        String namespace = autotuneObjectJson.getJSONObject("metadata").getString("namespace");

        AutotuneObject autotuneObjectInfo = new AutotuneObject();
        autotuneObjectInfo.setName(name);
        autotuneObjectInfo.setNamespace(namespace);
        autotuneObjectInfo.setSelectorInfo(selectorInfo);
        autotuneObjectInfo.setTypeInfo(typeInfo);
        autotuneObjectInfo.setMode(mode);
        autotuneObjectInfo.setReplicas(replicas);

        LOGGER.info("Added autotune object " + name);
        autotuneObjectList.add(autotuneObjectInfo);
    }

    /**
     * Read config files and add layer config to the autotune object
     *
     * @throws IOException if unable to read the file.
     */
    public static void readConfigFiles() throws IOException
    {
        String mainConfig = getFileContent(mainConfigLocation);

        if (mainConfig != null)
        {
            JSONObject mainConfigJSON = new JSONObject(mainConfig);

            JSONArray layers = mainConfigJSON.getJSONArray("layers");

            for (Object layer : layers)
            {
                JSONObject jsonObject = (JSONObject) layer;
                String level = jsonObject.getJSONObject("layer").getString("level");

                JSONArray configFiles = jsonObject.getJSONObject("layer").getJSONArray("config_files");

                for (Object configFile : configFiles)
                {
                    String file = (String) configFile;
                    String fileName = layerConfigRoot + level + "/" + file;

                    String layerConfig = getFileContent(fileName);

                    if (layerConfig != null)
                    {
                        JSONObject layerConfigJson = new JSONObject(layerConfig);
                        addLayerConfig(level, layerConfigJson);
                    }
                    else
                    {
                        LOGGER.info("Could not find config file " + fileName);
                    }
                }
            }
        }
    }

    /**
     * Parse layer config file and add layer, queries and tunables info to matching autotune object
     *
     * @param level Level of the layer
     * @param layerConfigJson JSON String containing layer config
     */
    private static void addLayerConfig(String level, JSONObject layerConfigJson)
    {
        String getAllAppsQuery = layerConfigJson.getJSONObject(getAllApps).getString(EnvInfo.getDataSource());

        //If getAllAppsQuery is absent, assume it is global and add the query to all applications monitored IF class matches.
        if (getAllAppsQuery == null || getAllAppsQuery.equals(""))
        {
            JSONArray queriesArray = layerConfigJson.getJSONArray("queries");

            for (AutotuneObject autotuneObject : autotuneObjectList)
            {
                addLayerInfoToAutotuneObject(level, layerConfigJson, queriesArray, autotuneObject);
            }
        }
    }

    /**
     * Add layer, queries and tunables info to the autotuneObject
     *
     * @param level Level of layer
     * @param layerConfigJson JSON String containing layer config
     * @param queriesArray JSONArray of queries for the layer
     * @param autotuneObject Matching autotune object
     */
    private static void addLayerInfoToAutotuneObject(String level, JSONObject layerConfigJson, JSONArray queriesArray, AutotuneObject autotuneObject)
    {
        ArrayList<Query> queryArrayList = new ArrayList<>();
        for (Object queryObject : queriesArray)
        {
            JSONObject queryJson = (JSONObject) queryObject;
            String type = queryJson.getString("type");

            if (autotuneObject.getTypeInfo().getTypeName().equals(type))
            {
                String query = queryJson.getString(EnvInfo.getDataSource());
                String queryDetails = queryJson.getString("query_details");
                String valueType = queryJson.getString("value_type");

                Query queryOutput = new Query(queryDetails, query, valueType);

                for (Object tunable : queryJson.getJSONArray("tunables"))
                {
                    Tunable tunableOutput = new Tunable((String) tunable);
                    queryOutput.getTunables().add(tunableOutput);
                }
                queryArrayList.add(queryOutput);
            }
        }
        String layerName = layerConfigJson.getString("layer_name");
        String details = layerConfigJson.getString("details");


        Layer layer = new Layer(layerName, level, details);
        layer.setQueries(queryArrayList);

        for (String application : autotuneObject.applicationsMap.keySet())
        {
            Application applicationTunables = autotuneObject.applicationsMap.get(application);
            LOGGER.info("Added layer " + layerName +  " to autotuneObject " + autotuneObject.getName());
            applicationTunables.getLayers().add(layer);
        }
    }

    /**
     * Read contents of file and return it
     *
     * @param fileName Read and return file content
     * @return Content of the file if it exists
     * @throws IOException if file does not exist
     */
    private static String getFileContent(String fileName) throws IOException
    {
        // The class loader that loaded the class
        ClassLoader classLoader = CollectAutotuneObjects.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        StringBuilder response = new StringBuilder();

        // the stream holding the file content
        if (inputStream == null)
        {
            return null;
        }
        else
        {
            try (InputStreamReader streamReader =
                         new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(streamReader))
            {

                String line;
                while ((line = reader.readLine()) != null)
                {
                    response.append(line);
                }
            }
        }
        return response.toString();
    }
}
