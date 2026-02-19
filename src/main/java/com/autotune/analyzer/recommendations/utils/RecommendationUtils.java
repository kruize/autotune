package com.autotune.analyzer.recommendations.utils;

import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.analyzer.kruizeLayer.impl.TunableSpec;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricMetadataResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.system.info.device.ContainerDeviceList;
import com.autotune.common.data.system.info.device.accelerator.NvidiaAcceleratorDeviceData;
import com.autotune.common.data.system.info.device.accelerator.metadata.AcceleratorMetaDataService;
import com.autotune.common.data.system.info.device.accelerator.metadata.AcceleratorProfile;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.google.gson.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;

public class RecommendationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationUtils.class);

    public static RecommendationConfigItem getCurrentValue(Map<Timestamp, IntervalResults> filteredResultsMap,
                                                           Timestamp timestampToExtract,
                                                           AnalyzerConstants.ResourceSetting resourceSetting,
                                                           AnalyzerConstants.RecommendationItem recommendationItem,
                                                           ArrayList<RecommendationConstants.RecommendationNotification> notifications) {
        Double currentValue = null;
        String format = null;
        RecommendationConfigItem recommendationConfigItem = null;
        AnalyzerConstants.MetricName metricName = null;
        for (Timestamp timestamp : filteredResultsMap.keySet()) {
            if (!timestamp.equals(timestampToExtract))
                continue;
            IntervalResults intervalResults = filteredResultsMap.get(timestamp);
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU)
                    metricName = AnalyzerConstants.MetricName.cpuRequest;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY)
                    metricName = AnalyzerConstants.MetricName.memoryRequest;
            }
            if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU)
                    metricName = AnalyzerConstants.MetricName.cpuLimit;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY)
                    metricName = AnalyzerConstants.MetricName.memoryLimit;
            }
            if (null != metricName) {
                if (intervalResults.getMetricResultsMap().containsKey(metricName)) {
                    Optional<MetricResults> metricResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(metricName));
                    currentValue = metricResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(null);
                    format = metricResults.map(m -> m.getAggregationInfoResult().getFormat()).orElse(null);
                }
                if (null == currentValue) {
                    setNotificationsFor(resourceSetting, recommendationItem, notifications);
                }
                return new RecommendationConfigItem(currentValue, format);
            }
        }
        setNotificationsFor(resourceSetting, recommendationItem, notifications);
        return null;
    }

    public static RecommendationConfigItem getCurrentValueForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap,
                                                                       Timestamp timestampToExtract,
                                                                       AnalyzerConstants.ResourceSetting resourceSetting,
                                                                       AnalyzerConstants.RecommendationItem recommendationItem,
                                                                       ArrayList<RecommendationConstants.RecommendationNotification> notifications) {
        Double currentNamespaceValue = null;
        String format = null;
        AnalyzerConstants.MetricName metricName = null;
        for (Timestamp timestamp : filteredResultsMap.keySet()) {
            if (!timestamp.equals(timestampToExtract))
                continue;
            IntervalResults intervalResults = filteredResultsMap.get(timestamp);
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU)
                    metricName = AnalyzerConstants.MetricName.namespaceCpuRequest;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY)
                    metricName = AnalyzerConstants.MetricName.namespaceMemoryRequest;
            }
            if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU)
                    metricName = AnalyzerConstants.MetricName.namespaceCpuLimit;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY)
                    metricName = AnalyzerConstants.MetricName.namespaceMemoryLimit;
            }
            if (null != metricName) {
                if (intervalResults.getMetricResultsMap().containsKey(metricName)) {
                    Optional<MetricResults> metricResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(metricName));
                    currentNamespaceValue = metricResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(null);
                    format = metricResults.map(m -> m.getAggregationInfoResult().getFormat()).orElse(null);
                }
                if (null == currentNamespaceValue) {
                    setNotificationsFor(resourceSetting, recommendationItem, notifications);
                }
                return new RecommendationConfigItem(currentNamespaceValue, format);
            }
        }
        setNotificationsFor(resourceSetting, recommendationItem, notifications);
        return null;
    }

    private static void setNotificationsFor(AnalyzerConstants.ResourceSetting resourceSetting,
                                            AnalyzerConstants.RecommendationItem recommendationItem,
                                            ArrayList<RecommendationConstants.RecommendationNotification> notifications) {
        // Check notifications is null, If it's null -> return.
        if (null == notifications)
            return;
        // Check if the item is CPU
        if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU) {
            // Check if the setting is REQUESTS
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.CRITICAL_CPU_REQUEST_NOT_SET
                );
            }
            // Check if the setting is LIMITS
            else if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.WARNING_CPU_LIMIT_NOT_SET
                );
            }
        }
        // Check if the item is Memory
        else if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY) {
            // Check if the setting is REQUESTS
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_REQUEST_NOT_SET
                );
            }
            // Check if the setting is LIMITS
            else if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_LIMIT_NOT_SET
                );
            }
        }
    }

    public static boolean markAcceleratorDeviceStatusToContainer(ContainerData containerData,
                                                              String maxDateQuery,
                                                              String namespace,
                                                              String workload,
                                                              String workload_type,
                                                              DataSourceInfo dataSourceInfo,
                                                              Map<String, Terms> termsMap,
                                                              Double measurementDurationMinutesInDouble,
                                                              String acceleratorDetectionQuery)
            throws IOException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, ParseException, FetchMetricsError {

        SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);
        String containerName = containerData.getContainer_name();
        String queryToEncode = null;
        long interval_end_time_epoc = 0;
        long interval_start_time_epoc = 0;

        LOGGER.debug("maxDateQuery: {}", maxDateQuery);
        queryToEncode = maxDateQuery
                .replace(AnalyzerConstants.NAMESPACE_VARIABLE, namespace)
                .replace(AnalyzerConstants.CONTAINER_VARIABLE, containerName)
                .replace(AnalyzerConstants.WORKLOAD_VARIABLE, workload)
                .replace(AnalyzerConstants.WORKLOAD_TYPE_VARIABLE, workload_type);

        String dateMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATE_ENDPOINT_WITH_QUERY,
                dataSourceInfo.getUrl(),
                URLEncoder.encode(queryToEncode, CHARACTER_ENCODING)
        );

        LOGGER.debug(dateMetricsUrl);
        GenericRestApiClient client = new GenericRestApiClient(dataSourceInfo);
        client.setBaseURL(dateMetricsUrl);
        JSONObject genericJsonObject = client.fetchMetricsJson(KruizeConstants.APIMessages.GET, "");
        JsonObject jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
        JsonArray resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);

        if (null == resultArray || resultArray.isEmpty()) {
            // Need to alert that container max duration is not detected
            // Ignoring it here, as we take care of it at generate recommendations
            return false;
        }

        resultArray = resultArray.get(0)
                .getAsJsonObject().getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.VALUE);
        long epochTime = resultArray.get(0).getAsLong();
        String timestamp = sdf.format(new Date(epochTime * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC));
        Date date = sdf.parse(timestamp);
        Timestamp dateTS = new Timestamp(date.getTime());
        interval_end_time_epoc = dateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                - ((long) dateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
        int maxDay = Terms.getMaxDays(termsMap);
        LOGGER.debug(KruizeConstants.APIMessages.MAX_DAY, maxDay);
        Timestamp startDateTS = Timestamp.valueOf(Objects.requireNonNull(dateTS).toLocalDateTime().minusDays(maxDay));
        interval_start_time_epoc = startDateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                - ((long) startDateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);

        acceleratorDetectionQuery = acceleratorDetectionQuery.replace(AnalyzerConstants.NAMESPACE_VARIABLE, namespace)
                .replace(AnalyzerConstants.CONTAINER_VARIABLE, containerName)
                .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDurationMinutesInDouble.intValue()))
                .replace(AnalyzerConstants.WORKLOAD_VARIABLE, workload)
                .replace(AnalyzerConstants.WORKLOAD_TYPE_VARIABLE, workload_type);

        String podMetricsUrl;
        try {
            podMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATASOURCE_ENDPOINT_WITH_QUERY_RANGE,
                    dataSourceInfo.getUrl(),
                    URLEncoder.encode(acceleratorDetectionQuery, CHARACTER_ENCODING),
                    interval_start_time_epoc,
                    interval_end_time_epoc,
                    measurementDurationMinutesInDouble.intValue() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
            LOGGER.debug(podMetricsUrl);
            client.setBaseURL(podMetricsUrl);
            genericJsonObject = client.fetchMetricsJson(KruizeConstants.APIMessages.GET, "");

            jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
            resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);

            if (null != resultArray && !resultArray.isEmpty()) {
                for (JsonElement result : resultArray) {
                    JsonObject resultObject = result.getAsJsonObject();
                    JsonArray valuesArray = resultObject.getAsJsonArray(KruizeConstants.DataSourceConstants
                            .DataSourceQueryJSONKeys.VALUES);

                    for (JsonElement element : valuesArray) {
                        JsonArray valueArray = element.getAsJsonArray();
                        double value = valueArray.get(1).getAsDouble();
                        // TODO: Check for non-zero values to mark as GPU workload
                        break;
                    }

                    JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.JSONKeys.METRIC);
                    String modelName = metricObject.get(KruizeConstants.JSONKeys.MODEL_NAME).getAsString();
                    if (null == modelName)
                        continue;

                    boolean isSupportedMig = checkIfModelIsKruizeSupportedMIG(modelName);
                    if (isSupportedMig) {
                        NvidiaAcceleratorDeviceData acceleratorDeviceData = new NvidiaAcceleratorDeviceData(metricObject.get(KruizeConstants.JSONKeys.MODEL_NAME).getAsString(),
                                metricObject.get(KruizeConstants.JSONKeys.HOSTNAME).getAsString(),
                                metricObject.get(KruizeConstants.JSONKeys.UUID).getAsString(),
                                metricObject.get(KruizeConstants.JSONKeys.DEVICE).getAsString(),
                                null, isSupportedMig, false);


                        if (null == containerData.getContainerDeviceList()) {
                            ContainerDeviceList containerDeviceList = new ContainerDeviceList();
                            containerData.setContainerDeviceList(containerDeviceList);
                        }
                        containerData.getContainerDeviceList().addDevice(AnalyzerConstants.DeviceType.ACCELERATOR, acceleratorDeviceData);
                        // TODO: Currently we consider only the first mig supported GPU
                        return true;
                    }
                }
            }
            return false;
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException |
                 JsonSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean markAcceleratorPartitionDeviceStatusToContainer (ContainerData containerData,
                                                                           String maxDateQuery,
                                                                           String namespace,
                                                                           String workload,
                                                                           String workload_type,
                                                                           DataSourceInfo dataSourceInfo,
                                                                           Map<String, Terms> termsMap,
                                                                           Double measurementDurationMinutesInDouble,
                                                                           String acceleratorPartitionDetectionQuery,
                                                                           String uuid,
                                                                           String gpuProfile)
            throws IOException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, ParseException, FetchMetricsError {

        SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);
        String containerName = containerData.getContainer_name();
        String queryToEncode = null;
        long interval_end_time_epoc = 0;
        long interval_start_time_epoc = 0;

        LOGGER.debug("maxDateQuery: {}", maxDateQuery);
        queryToEncode = maxDateQuery
                .replace(AnalyzerConstants.NAMESPACE_VARIABLE, namespace)
                .replace(AnalyzerConstants.CONTAINER_VARIABLE, containerName)
                .replace(AnalyzerConstants.WORKLOAD_VARIABLE, workload)
                .replace(AnalyzerConstants.WORKLOAD_TYPE_VARIABLE, workload_type);

        String dateMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATE_ENDPOINT_WITH_QUERY,
                dataSourceInfo.getUrl(),
                URLEncoder.encode(queryToEncode, CHARACTER_ENCODING)
        );

        LOGGER.debug(dateMetricsUrl);
        GenericRestApiClient client = new GenericRestApiClient(dataSourceInfo);
        client.setBaseURL(dateMetricsUrl);
        JSONObject genericJsonObject = client.fetchMetricsJson(KruizeConstants.APIMessages.GET, "");
        JsonObject jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
        JsonArray resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);

        if (null == resultArray || resultArray.isEmpty()) {
            // Need to alert that container max duration is not detected
            // Ignoring it here, as we take care of it at generate recommendations
            return false;
        }

        resultArray = resultArray.get(0)
                .getAsJsonObject().getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.VALUE);
        long epochTime = resultArray.get(0).getAsLong();
        String timestamp = sdf.format(new Date(epochTime * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC));
        Date date = sdf.parse(timestamp);
        Timestamp dateTS = new Timestamp(date.getTime());
        interval_end_time_epoc = dateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                - ((long) dateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
        int maxDay = Terms.getMaxDays(termsMap);
        LOGGER.debug(KruizeConstants.APIMessages.MAX_DAY, maxDay);
        Timestamp startDateTS = Timestamp.valueOf(Objects.requireNonNull(dateTS).toLocalDateTime().minusDays(maxDay));
        interval_start_time_epoc = startDateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                - ((long) startDateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);

        acceleratorPartitionDetectionQuery = acceleratorPartitionDetectionQuery.replace(AnalyzerConstants.UUID_VARIABLE, uuid)
                .replace(AnalyzerConstants.PROFILE_VARIABLE, gpuProfile)
                .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDurationMinutesInDouble.intValue()));

        String podMetricsUrl;
        try {
            podMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATASOURCE_ENDPOINT_WITH_QUERY_RANGE,
                    dataSourceInfo.getUrl(),
                    URLEncoder.encode(acceleratorPartitionDetectionQuery, CHARACTER_ENCODING),
                    interval_start_time_epoc,
                    interval_end_time_epoc,
                    measurementDurationMinutesInDouble.intValue() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
            LOGGER.debug(podMetricsUrl);
            client.setBaseURL(podMetricsUrl);
            genericJsonObject = client.fetchMetricsJson(KruizeConstants.APIMessages.GET, "");

            jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
            resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);

            if (null != resultArray && !resultArray.isEmpty()) {
                for (JsonElement result : resultArray) {
                    JsonObject resultObject = result.getAsJsonObject();
                    JsonArray valuesArray = resultObject.getAsJsonArray(KruizeConstants.DataSourceConstants
                            .DataSourceQueryJSONKeys.VALUES);

                    for (JsonElement element : valuesArray) {
                        JsonArray valueArray = element.getAsJsonArray();
                        double value = valueArray.get(1).getAsDouble();
                        // TODO: Check for non-zero values to mark as GPU workload
                        break;
                    }

                    JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.JSONKeys.METRIC);
                    String modelName = metricObject.get(KruizeConstants.JSONKeys.MODEL_NAME).getAsString();
                    String profile = metricObject.get(KruizeConstants.JSONKeys.GPU_PROFILE).getAsString();
                    System.out.println("GPU MIG Profile: " + profile);
                    if (null == modelName)
                        continue;

                    boolean isSupportedMig = checkIfModelIsKruizeSupportedMIG(modelName);
                    if (isSupportedMig) {
                        NvidiaAcceleratorDeviceData acceleratorDeviceData = new NvidiaAcceleratorDeviceData(metricObject.get(KruizeConstants.JSONKeys.MODEL_NAME).getAsString(),
                                metricObject.get(KruizeConstants.JSONKeys.HOSTNAME).getAsString(),
                                metricObject.get(KruizeConstants.JSONKeys.UUID).getAsString(),
                                metricObject.get(KruizeConstants.JSONKeys.DEVICE).getAsString(),
                                profile,
                                isSupportedMig,
                                true);


                        if (null == containerData.getContainerDeviceList()) {
                            ContainerDeviceList containerDeviceList = new ContainerDeviceList();
                            containerData.setContainerDeviceList(containerDeviceList);
                        }
                        containerData.getContainerDeviceList().addDevice(AnalyzerConstants.DeviceType.ACCELERATOR_PARTITION, acceleratorDeviceData);
                        // TODO: Currently we consider only the first mig supported GPU
                        return true;
                    }
                }
            }
            return false;
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException |
                 JsonSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkIfModelIsKruizeSupportedMIG(String modelName) {
        if (null == modelName || modelName.isEmpty())
            return false;

        modelName = modelName.toUpperCase();

        return modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.A100)
                || modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.H100)
                || modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.H200);
    }

    public static Timestamp getNearestTimestamp(HashMap<Timestamp, IntervalResults> containerDataResults, Timestamp targetTime, int minutesRange) {
        long rangeInMillis = (long) minutesRange * 60 * 1000;
        long targetTimeMillis = targetTime.getTime();

        Timestamp nearestTimestamp = null;
        long nearestDistance = Long.MAX_VALUE;

        for (Map.Entry<Timestamp, IntervalResults> entry : containerDataResults.entrySet()) {
            Timestamp currentTimestamp = entry.getKey();
            long currentTimeMillis = currentTimestamp.getTime();
            long distance = Math.abs(targetTimeMillis - currentTimeMillis);

            if (distance <= rangeInMillis && distance < nearestDistance) {
                nearestDistance = distance;
                nearestTimestamp = currentTimestamp;
            }
        }

        return nearestTimestamp;
    }

    public static HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getMapWithOptimalProfile(
            String acceleratorModel,
            Double coreFraction,
            Double memoryFraction
    ) {
        if (null == acceleratorModel || null == coreFraction || null == memoryFraction)
            return null;

        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> returnMap = new HashMap<>();

        AcceleratorMetaDataService gpuMetaDataService = AcceleratorMetaDataService.getInstance();
        AcceleratorProfile acceleratorProfile = gpuMetaDataService.getAcceleratorProfile(acceleratorModel, coreFraction, memoryFraction);
        RecommendationConfigItem recommendationConfigItem = new RecommendationConfigItem(1.0, "cores");
        String profileName = acceleratorProfile.getProfileName().toLowerCase();

        switch (profileName) {
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_5GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_5GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_10GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_10GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_20GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_20GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_2G_10GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_10GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_2G_20GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_20GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_3G_20GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_20GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_3G_40GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_40GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_4G_20GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_20GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_4G_40GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_40GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_7G_40GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_40GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_7G_80GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_80GB, recommendationConfigItem);
            // Adding H200 Partitions to the ladder
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_1G_18GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_18GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_1G_35GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_35GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_2G_35GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_35GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_3G_71GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_71GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_4G_71GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_71GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_7G_141GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_141GB, recommendationConfigItem);
            // Adding B200 Partitions to the ladder
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_1G_23GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_23GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_1G_45GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_45GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_2G_45GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_45GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_3G_90GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_90GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_4G_90GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_90GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_7G_180GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_180GB, recommendationConfigItem);
            // Adding RTX PRO 5000 Partitions to the ladder
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_5000_PROFILE_1G_12GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_12GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_5000_PROFILE_2G_24GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_24GB_ME, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_5000_PROFILE_4G_48GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_48GB_GFX, recommendationConfigItem);
            // Adding RTX PRO 6000 Partitions to the ladder
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_6000_PROFILE_1G_24GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_24GB_GFX, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_6000_PROFILE_2G_48GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_48GB_GFX, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_6000_PROFILE_4G_96GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_96GB_GFX, recommendationConfigItem);
            // Adding H100 94 GB & 96 GB Partitions
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H100_PROFILE_1G_12GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_12GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H100_PROFILE_1G_24GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_24GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H100_PROFILE_2G_24GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_24GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H100_PROFILE_3G_47GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_47GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H100_PROFILE_3G_48GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_48GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H100_PROFILE_4G_47GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_47GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H100_PROFILE_4G_48GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_48GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H100_PROFILE_7G_94GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_94GB, recommendationConfigItem);
            case (AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H100_PROFILE_7G_96GB) ->
                returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_96GB, recommendationConfigItem);
        }
            return returnMap;
    }

    public static String getSupportedModelBasedOnModelName(String modelName) {
        if (null == modelName || modelName.isBlank())
            return null;

        modelName = modelName.toUpperCase();

        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.A100)) {
            if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_40))
                return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_40_GB;
            // NOTE: Not tested in real time, checks for predictable strings in device name
            if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_80))
                return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_80_GB;
        }
        // NOTE: Not tested in real time, checks for predictable strings in device name
        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.H100)) {
            if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_80))
                return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H100_80_GB;
            if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_94))
                return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H100_94_GB;
            if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_96))
                return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H100_96_GB;
        }
        // NOTE: Not tested in real time, checks for predictable strings in device name
        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.H200))
            return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H200_141_GB;
        // NOTE: Not tested in real time, checks for predictable strings in device name
        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.B200))
            return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.B200_180_GB;
        // NOTE: Not tested in real time, checks for predictable strings in device name
        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.RTX)
                && modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.PRO)) {
            if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.RTX_5000))
                return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.BW_RTX_PRO_5000_48_GB;
            if ( modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorNameTokens.RTX_6000))
                return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.BW_RTX_PRO_6000_96_GB;
        }
        LOGGER.info(AnalyzerConstants.AcceleratorConstants.AcceleratorLogs.UNSUPPORTED_ACCELERATOR, modelName);

        return null;
    }

    /**
     * This function converts the cpu and memory values to VPA desired format
     */
    public static String resource2str(String resource, double value) {
        if (resource.equalsIgnoreCase(AnalyzerConstants.RecommendationItem.CPU.toString())) {
            // cpu related conversions
            if (value < 1) {
                return (int) (value * 1000) + "m";
            } else {
                return String.valueOf(value);
            }
        } else {
            // memory related conversions
            if (value < 1024) {
                return (int) value + "B";
            } else if (value < 1024 * 1024) {
                return (int) (value / 1024) + "k";
            } else if (value < 1024 * 1024 * 1024) {
                return (int) (value / 1024 / 1024) + "Mi";
            } else {
                return (int) (value / 1024 / 1024 / 1024) + "Gi";
            }
        }
    }

    public static double getFrameBufferBasedOnModel(String modelName) {
        if (null == modelName || modelName.isEmpty())
            return -1;

        modelName = modelName.toUpperCase();

        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_40))
            return 40 * 1024;

        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_80))
            return 80 * 1024;

        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_141))
            return 141 * 1024;

        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_94))
            return 94 * 1024;

        if (modelName.contains(AnalyzerConstants.AcceleratorConstants.AcceleratorMemory.GB_96))
            return 96 * 1024;

        return -1;
    }

    public static int parseMajorVersion(String version) {
        if (version == null || version.isEmpty()) return 8;
        version = version.trim();
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        }
        int dotIndex = version.indexOf(".");
        return (dotIndex != -1)
                ? Integer.parseInt(version.substring(0, dotIndex))
                : Integer.parseInt(version);
    }

    /**
     * Formats tunable values for JVM environment variables (JDK_JAVA_OPTIONS / JAVA_OPTIONS).
     * Shared by Hotspot, Semeru, and any future JVM runtime layer handlers.
     *
     * @param tunableName  tunable name (e.g. MaxRAMPercentage, GCPolicy)
     * @param value       recommended value
     * @param envBuilders map of env var name to StringBuilder
     */
    public static void formatForJVMEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        if (value == null) return;

        StringBuilder jdkOpts = envBuilders.get(KruizeConstants.JSONKeys.JDK_JAVA_OPTIONS);
        StringBuilder javaOpts = envBuilders.get(KruizeConstants.JSONKeys.JAVA_OPTIONS);
        StringBuilder target = (jdkOpts != null) ? jdkOpts : javaOpts;
        if (target == null) return;

        if (AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC.equals(tunableName)) {
            target.append("-XX:MaxRAMPercentage=").append(value).append(" ");
        } else if (AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY.equals(tunableName)) {
            target.append(value).append(" ");
        }
    }

    /**
     * Extracts JVM metric metadata (runtime, version, vendor) from filteredResultsMap.
     * Looks for jvmRuntimeInfo metric in IntervalResults and returns its MetricMetadataResults.
     *
     * @param filteredResultsMap map of timestamp to IntervalResults
     * @return MetricMetadataResults containing JVM info, or null if not found
     */
    public static MetricMetadataResults getJvmMetricMetadataFromFilteredResults(Map<Timestamp, IntervalResults> filteredResultsMap) {
        if (filteredResultsMap == null) {
            return null;
        }
        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            if (intervalResults.getMetricResultsMap() == null) {
                continue;
            }
            // Try jvmInfo first, then jvmInfoTotal (both provide runtime, vendor, version)
            MetricResults metricResults = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.jvmInfo);
            if (metricResults == null) {
                metricResults = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.jvmInfoTotal);
            }
            if (metricResults == null || metricResults.getMetricMetadataResults() == null) {
                continue;
            }
            return metricResults.getMetricMetadataResults();
        }
        return null;
    }

    public static Object getTunableValue(Map<TunableSpec, Object> tunableSpecObjectMap, String layerName, String tunableName) {
        return tunableSpecObjectMap.get(new TunableSpec(layerName, tunableName));
    }

}
