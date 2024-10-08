package com.autotune.analyzer.recommendations.utils;

import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.system.info.device.ContainerDeviceList;
import com.autotune.common.data.system.info.device.accelerator.AcceleratorDeviceData;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.google.gson.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.autotune.common.data.system.info.device.accelerator.metadata.AcceleratorMetaDataService;
import com.autotune.common.data.system.info.device.accelerator.metadata.AcceleratorProfile;

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

    public static void markAcceleratorDeviceStatusToContainer (ContainerData containerData,
                                                               String maxDateQuery,
                                                               String namespace,
                                                               String workload,
                                                               String workload_type,
                                                               DataSourceInfo dataSourceInfo,
                                                               Map<String, Terms> termsMap,
                                                               Double measurementDurationMinutesInDouble,
                                                               String gpuDetectionQuery)
            throws IOException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, ParseException, FetchMetricsError {

        SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);
        String containerName = containerData.getContainer_name();
        String queryToEncode = null;
        long interval_end_time_epoc = 0;
        long interval_start_time_epoc = 0;

        LOGGER.debug("maxDateQuery: {}", maxDateQuery);
        queryToEncode =  maxDateQuery
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
            return;
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

        gpuDetectionQuery = gpuDetectionQuery.replace(AnalyzerConstants.NAMESPACE_VARIABLE, namespace)
                .replace(AnalyzerConstants.CONTAINER_VARIABLE, containerName)
                .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDurationMinutesInDouble.intValue()))
                .replace(AnalyzerConstants.WORKLOAD_VARIABLE, workload)
                .replace(AnalyzerConstants.WORKLOAD_TYPE_VARIABLE, workload_type);

        String podMetricsUrl;
        try {
            podMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATASOURCE_ENDPOINT_WITH_QUERY,
                    dataSourceInfo.getUrl(),
                    URLEncoder.encode(gpuDetectionQuery, CHARACTER_ENCODING),
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
                        AcceleratorDeviceData acceleratorDeviceData = new AcceleratorDeviceData(metricObject.get(KruizeConstants.JSONKeys.MODEL_NAME).getAsString(),
                                metricObject.get(KruizeConstants.JSONKeys.HOSTNAME).getAsString(),
                                metricObject.get(KruizeConstants.JSONKeys.UUID).getAsString(),
                                metricObject.get(KruizeConstants.JSONKeys.DEVICE).getAsString(),
                                isSupportedMig);


                        if (null == containerData.getContainerDeviceList()) {
                            ContainerDeviceList containerDeviceList = new ContainerDeviceList();
                            containerData.setContainerDeviceList(containerDeviceList);
                        }
                        containerData.getContainerDeviceList().addDevice(AnalyzerConstants.DeviceType.ACCELERATOR, acceleratorDeviceData);
                        // TODO: Currently we consider only the first mig supported GPU
                        return;
                    }
                }
            }
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException |
                 JsonSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkIfModelIsKruizeSupportedMIG(String modelName) {
        if (null == modelName || modelName.isEmpty())
            return false;

        modelName = modelName.toUpperCase();

        boolean A100_CHECK = (modelName.contains("A100") &&
                (modelName.contains("40GB") || modelName.contains("80GB")));

        boolean H100_CHECK = false;

        if (!A100_CHECK) {
            H100_CHECK = (modelName.contains("H100") && modelName.contains("80GB"));
        }

        return A100_CHECK || H100_CHECK;
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

        if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_5GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_5GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_10GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_10GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_20GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_20GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_2G_10GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_10GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_2G_20GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_20GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_3G_20GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_20GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_3G_40GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_40GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_4G_20GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_20GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_4G_40GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_40GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_7G_40GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_40GB, recommendationConfigItem);
        } else if (acceleratorProfile.getProfileName().equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_7G_80GB)) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_80GB, recommendationConfigItem);
        }
        return returnMap;
    }

    public static String getSupportedModelBasedOnModelName(String modelName) {
        if (null == modelName || modelName.isEmpty())
            return null;

        modelName = modelName.toUpperCase();

        if (modelName.contains("A100") && modelName.contains("40GB"))
            return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_40_GB;

        if (modelName.contains("A100") && modelName.contains("80GB"))
            return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_80_GB;

        if (modelName.contains("H100") && modelName.contains("80GB"))
            return AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H100_80_GB;

        return null;
    }
}

