package com.autotune.collection;

import com.autotune.env.EnvInfo;
import com.autotune.query.ApplicationTunables;
import com.autotune.query.Layer;
import com.autotune.query.Query;
import com.autotune.tunables.Tunable;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class CollectAutotuneObjects
{
    public static ArrayList<AutotuneObject> autotuneInfoList;
    static final String group = "recommender.com";
    static final String scope = "Namespaced";
    static final String plural = "autotunes";
    static final String apiVersion = "v1beta1";
    static final String resourceName = "autotunes.recommender.com";

    static
    {
        autotuneInfoList = new ArrayList<>();
    }

    public void getAutotuneObjects() throws IOException
    {
        KubernetesClient client = new DefaultKubernetesClient();
        final CountDownLatch isWatchClosed = new CountDownLatch(3);

        CustomResourceDefinitionContext autotuneCrdContext = new CustomResourceDefinitionContext
                .Builder()
                .withGroup(group)
                .withScope(scope)
                .withPlural(plural)
                .withVersion(apiVersion)
                .withName(resourceName)
                .build();

        Watcher<String> autotuneObjectWatcher = new Watcher<String>() {
            @Override
            public void eventReceived(Action action, String resource) {
                switch (action.toString().toUpperCase()) {
                    case "ADDED":
                        System.out.println("Added!");
                        addAutotuneObjectToList(resource);
                        matchPodsToAutotuneObject(client);
                        try {
                            CollectAutotuneObjects.readConfigFiles();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "MODIFIED":
                        //modifyAutotuneObject(resource, client, autotuneCrdContext);
                        break;
                    default:
                        //addAutotuneObjectToList(resource, client, autotuneCrdContext);
                        break;
                }
            }

            @Override
            public void onClose(KubernetesClientException e) {
                isWatchClosed.countDown();
            }
        };
        client.customResource(autotuneCrdContext).watch(autotuneObjectWatcher);

        try {
            isWatchClosed.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void matchPodsToAutotuneObject(KubernetesClient client)
    {
        for (AutotuneObject autotuneObject : autotuneInfoList)
        {
            String labelKey = autotuneObject.getSelectorInfo().getMatchLabel();
            String labelValue = autotuneObject.getSelectorInfo().getMatchValue();
            String type = autotuneObject.getSlaInfo().getSlaName();

            System.out.println("Label Key: " + labelKey + " value: " + labelValue);

            PodList podList = client.pods().withLabel(labelKey).list();

            for (Pod pod : podList.getItems())
            {
                ObjectMeta podMetadata = pod.getMetadata();
                System.out.println(podMetadata.getName());
                if (podMetadata.getLabels().get(labelKey).equals(labelValue))
                {
                    //TODO check if already present
                    ApplicationTunables applicationTunable = new ApplicationTunables(podMetadata.getName(), podMetadata.getNamespace(), type);
                    autotuneObject.applicationTunablesMap.put(podMetadata.getName(), applicationTunable);
                }
            }
        }
    }

    private void addAutotuneObjectToList(String autotuneObject)
    {
        JSONObject autotuneObjectJson = new JSONObject(autotuneObject);
        SlaInfo slaInfo = new SlaInfo();
        SelectorInfo selectorInfo = new SelectorInfo();

        JSONObject specJson = autotuneObjectJson.getJSONObject("spec");

        JSONObject slaJson = specJson.getJSONObject("sla");
        slaInfo.setSlaName(slaJson.getString("name"));
        slaInfo.setSlaValue(slaJson.getString("value"));

        //System.out.println(specJson.toString());
        JSONObject selectorJson = specJson.getJSONObject("selector");
        selectorInfo.setMatchLabel(selectorJson.getString("matchLabel"));
        selectorInfo.setMatchValue(selectorJson.getString("matchValue"));
        selectorInfo.setMatchRoute(selectorJson.getString("matchRoute"));
        //CHANGE RUI TO URI LATER
        selectorInfo.setMatchURI(selectorJson.getString("matchRUI"));
        selectorInfo.setMatchService(selectorJson.getString("matchService"));

        String mode = specJson.getString("mode");
        int replicas = specJson.getInt("replicas");

        AutotuneObject autotuneObjectInfo = new AutotuneObject();
        autotuneObjectInfo.setAutotuneObject(autotuneObject);
        autotuneObjectInfo.setSelectorInfo(selectorInfo);
        autotuneObjectInfo.setSlaInfo(slaInfo);
        autotuneObjectInfo.setMode(mode);
        autotuneObjectInfo.setReplicas(replicas);

        autotuneInfoList.add(autotuneObjectInfo);
    }

    public static void readConfigFiles() throws IOException
    {
        String mainConfig = getFileContent("config/main_config.json");
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
                    String fileName = "config/layer_configs/level" + level + "/" + file;

                    String layerConfig = getFileContent(fileName);

                    if (layerConfig != null)
                    {
                        JSONObject layerConfigJson = new JSONObject(layerConfig);
                        addLayerConfig(level, layerConfigJson);
                    }
                }
            }
        }
    }

    private static void addLayerConfig(String level, JSONObject layerConfigJson)
    {
        String getAllAppsQuery = layerConfigJson.getJSONObject("getAllAppsQuery").getString(EnvInfo.getDataSource());

        //If getAllAppsQuery is absent, assume it is global and add the query to all applications monitored IF class matches.
        if (getAllAppsQuery == null || getAllAppsQuery.equals(""))
        {
            JSONArray queriesArray = layerConfigJson.getJSONArray("queries");

            for (AutotuneObject autotuneObject : autotuneInfoList)
            {
                ArrayList<Query> queryArrayList = new ArrayList<>();
                for (Object queryObject : queriesArray)
                {
                    JSONObject queryJson = (JSONObject) queryObject;
                    String type = queryJson.getString("type");

                    if (autotuneObject.getSlaInfo().getSlaName().equals(type))
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

                for (String application : autotuneObject.applicationTunablesMap.keySet())
                {
                    ApplicationTunables applicationTunables = autotuneObject.applicationTunablesMap.get(application);
                    applicationTunables.getLayers().add(layer);
                }
            }
        }
    }

    // print input stream
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
