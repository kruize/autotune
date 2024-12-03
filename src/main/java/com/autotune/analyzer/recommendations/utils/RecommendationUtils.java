package com.autotune.analyzer.recommendations.utils;

import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricLabels;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.system.info.device.ContainerDeviceList;
import com.autotune.common.data.system.info.device.accelerator.AcceleratorDeviceData;
import com.autotune.common.data.system.info.device.accelerator.metadata.AcceleratorMetaDataService;
import com.autotune.common.data.system.info.device.accelerator.metadata.AcceleratorProfile;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.google.gson.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public static void markAcceleratorDeviceStatusToContainer(ContainerData containerData,
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
            podMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATASOURCE_ENDPOINT_WITH_QUERY_RANGE,
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

    /**
     * Generates CSV header row including aggregation functions
     * @return header row with all the metrics as columns
     */
    public static String generateCSVHeader(){

        // StringBuilder to collect all columns
        StringBuilder finalHeaderRow = new StringBuilder();

        try {
            List<String> metricsWithoutAggrFunctions = Arrays.asList("interval_end", "interval_start", "container",
                    "namespace", "pod", "owner_kind", "owner_name", "workload_type", "workload", "k8_object_type", "k8_object_name");

            List<String> metricsWithAggrFunctions = Arrays.asList("cpuRequest", "cpuLimit", "cpuUsage", "cpuThrottle",
                    "memoryRequest", "memoryLimit", "memoryUsage", "memoryRSS");

            List<String> aggrFunctions = Arrays.asList("min", "max", "avg", "sum");

            // Loop through main headers and append them to the StringBuilder
            for (String currentHeader : metricsWithoutAggrFunctions) {
                finalHeaderRow.append(currentHeader).append(",");
            }

            for (String query : metricsWithAggrFunctions) {
                for (String subHeader : aggrFunctions) {
                    String dynamicHeader = query + "_" + subHeader;
                    finalHeaderRow.append(dynamicHeader).append(",");
                }
            }

            // Remove trailing comma at the end
            if (!finalHeaderRow.isEmpty()) {
                finalHeaderRow.deleteCharAt(finalHeaderRow.length() - 1);
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        return finalHeaderRow.toString();
    }

    /**
     * Converts input query data of all the metrics to CSV file
     * @param containerDataResults
     * @param metricLabelMap
     * @param inputFilePath
     * @param header
     */
    public static void convertContainerResultsByPodMapToCSV(HashMap<Timestamp, HashMap<String, IntervalResults>> containerDataResults,
                                                            HashMap<Timestamp, HashMap<String, HashMap<String, MetricLabels>>> metricLabelMap, String inputFilePath, String header) {

        File file = new File(inputFilePath);
        LOGGER.info("Attempting to create file at: {}", file.getAbsolutePath());

        // List of expected queries
        List<String> queries = Arrays.asList(
                "cpuRequest",
                "cpuLimit",
                "cpuUsage",
                "cpuThrottle",
                "memoryRequest",
                "memoryLimit",
                "memoryUsage",
                "memoryRSS"
        );

        // A map to store existing rows, with key being the podLabel
        Map<String, String[]> csvRowMap = new HashMap<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Predefined labels for the CSV header
            //String header = generateCSVHeader();
            int totalColumns = (Arrays.asList(header.split("\\,")).size())+1;
            writer.write(header);
            writer.newLine();

            // Iterate over the timestamp entries
            for (Timestamp timestamp : metricLabelMap.keySet()) {
                HashMap<String, HashMap<String, MetricLabels>> metricMap = metricLabelMap.get(timestamp);
                // Iterate over the metric names
                for (String metricName : metricMap.keySet()) {
                    HashMap<String, MetricLabels> labelMap = metricMap.get(metricName);

                    if(labelMap == null) {
                        continue;
                    }

                    // Iterate over the labels and corresponding objects
                    for (String label : labelMap.keySet()) {
                        // Create a key combining the timestamp and metric name
                        String rowKey = timestamp.toString() + "_" + label;
                        // Use the label as the key for the csvRowMap
                        String[] dataRow = csvRowMap.getOrDefault(rowKey, new String[totalColumns]);

                        MetricLabels metricLabel = labelMap.get(label);
                        String container = metricLabel.getContainer();
                        String namespace = metricLabel.getNamespace();

                        // If this is a new row, initialize with empty values and set the initial columns
                        if (!csvRowMap.containsKey(rowKey)) {
                            Arrays.fill(dataRow, "");

                            // Add timestamp and static values at predefined positions
                            dataRow[0] = timestamp.toString();
                            dataRow[2] = container;
                            dataRow[3] = namespace;
                            dataRow[4] = label;
                        }

                        if(metricName.equals("imageOwners")) {
                            dataRow[5] = metricLabel.getOwner_kind();
                            dataRow[6] = metricLabel.getOwner_name();
                        } else if(metricName.equals("imageWorkloads")) {
                            dataRow[7] = metricLabel.getWorkload_name();
                            dataRow[8] = metricLabel.getWorkload_kind();
                        }

                        // Add placeholder values for additional columns
                        csvRowMap.put(rowKey, dataRow);
                    }
                }
            }

            for(Timestamp timestamp : containerDataResults.keySet()) {
                HashMap<String, IntervalResults> intervalResultsMap = containerDataResults.get(timestamp);

                for(String podLabel: intervalResultsMap.keySet()) {
                    // Create a key combining the timestamp and pod name
                    String rowKey = timestamp.toString() + "_" + podLabel;
                    // Use the rowKey as key for the csvRowMap
                    String[] dataRow = csvRowMap.get(rowKey);
                    IntervalResults intervalResults = intervalResultsMap.get(podLabel);
                    dataRow[1] = intervalResults.getIntervalStartTime().toString();

                    HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap = intervalResults.getMetricResultsMap();
                    // Fill in values for each query
                    for (int i = 0; i < queries.size(); i++) {
                        String query = queries.get(i);
                        MetricResults metricResults = metricResultsMap.get(AnalyzerConstants.MetricName.valueOf(query));
                        int index = i * 4 + 11;
                        if(null != metricResults) {
                            MetricAggregationInfoResults aggregationInfoResults = metricResults.getAggregationInfoResult();
                            // Append min, max, avg, sum for the current query
                            dataRow[index] = aggregationInfoResults.getMin() != null ? aggregationInfoResults.getMin().toString() : "";
                            dataRow[index + 1] = aggregationInfoResults.getMax()!= null ? aggregationInfoResults.getMax().toString() : "";
                            dataRow[index + 2] = aggregationInfoResults.getAvg() != null ? aggregationInfoResults.getAvg().toString() : "";
                            dataRow[index + 3] = aggregationInfoResults.getSum() != null ? aggregationInfoResults.getSum().toString() : "";
                        } else {
                            dataRow[index] = "";
                            dataRow[index + 1] = "";
                            dataRow[index + 2] = "";
                            dataRow[index + 3] = "";
                        }
                    }
                    csvRowMap.put(rowKey, dataRow);
                }
            }

            // After processing all data, write the rows to the CSV
            // Write all rows to the CSV file
            for (String[] dataRow : csvRowMap.values()) {

                if(null == dataRow[1] || dataRow[1].isEmpty()) {
                    continue;
                }
                // Convert the dataRow array into a comma-separated string
                StringBuilder csvRow = new StringBuilder();
                for (String value : dataRow) {
                    csvRow.append(value).append(",");
                }

                // Remove trailing comma
                csvRow.setLength(csvRow.length() - 1);

                // Write the row to the CSV
                writer.write(csvRow.toString());
                writer.newLine(); // Move to the next line
            }

            LOGGER.info("CSV file created successfully: {}", inputFilePath);
            // TODO: remove before merging
            printCsv(inputFilePath);
        } catch (IOException e) {
            LOGGER.error(String.valueOf(e));
        }
    }

    /**
     *  Calculate aggregates (avg, min, max, sum) for each workload
     */
    public static void calculateAggregateValuesForWorkload(Map<String, List<Map<String, String>>> workloadDataMap,
                                                           List<Map<String, String>> aggData, List<String> headerRow, Set<String> columnsToIgnore) {
        try {
            for (String uniqueKey : workloadDataMap.keySet()) {
                List<Map<String, String>> workloadRows = workloadDataMap.get(uniqueKey);

                Map<String, String> aggregatedRow = new HashMap<>();
                // Split the unique key to extract namespace, workload, and start_time
                String[] keyParts = uniqueKey.split("\\|");
                aggregatedRow.put("interval_start", keyParts[0]);
                aggregatedRow.put("container", keyParts[1]);
                aggregatedRow.put("k8_object_type", keyParts[2]);
                aggregatedRow.put("namespace", keyParts[3]);
                aggregatedRow.put("workload", keyParts[4]);

                for (String key : headerRow) {
                    if (columnsToIgnore.contains(key)) continue; // Skip ignored columns

                    if (key.endsWith("avg")) {
                        double avg = workloadRows.stream().mapToDouble(r -> {
                            String value = r.get(key);
                            return (null == value || value.isEmpty() || value.equals("null")) ? 0.0 : Double.parseDouble(value);
                        }).average().orElse(0);
                        aggregatedRow.put(key, String.valueOf(avg));
                    } else if (key.endsWith("min")) {
                        double min = workloadRows.stream().mapToDouble(r -> {
                            String value = r.get(key);
                            return (null == value || value.isEmpty() || value.equals("null")) ? Double.POSITIVE_INFINITY : Double.parseDouble(value);
                        }).min().orElse(Double.POSITIVE_INFINITY);
                        aggregatedRow.put(key, String.valueOf(min));
                    } else if (key.endsWith("max")) {
                        double max = workloadRows.stream().mapToDouble(r -> {
                            String value = r.get(key);
                            return (null == value || value.isEmpty() || value.equals("null")) ? Double.NEGATIVE_INFINITY : Double.parseDouble(value);
                        }).max().orElse(Double.NEGATIVE_INFINITY);
                        aggregatedRow.put(key, String.valueOf(max));
                    } else if (key.endsWith("sum")) {
                        double sum = workloadRows.stream().mapToDouble(r -> {
                            String value = r.get(key);
                            return (null == value || value.isEmpty() || value.equals("null")) ? 0.0 : Double.parseDouble(value);
                        }).sum();
                        aggregatedRow.put(key, String.valueOf(sum));
                    } else {
                        // For columns which cannot be aggregated, just use the value from the first row
                        aggregatedRow.put(key, workloadRows.get(0).get(key));
                    }
                }

                aggData.add(aggregatedRow);
            }
            for (Map<String, String> row : aggData) {
                // Remove keys from the row that are in the columnsToIgnore set
                for (String column : columnsToIgnore) {
                    row.remove(column);
                }
                // Replace "Infinity" and "-Infinity" with empty strings in the row
                for (String key : row.keySet()) {
                    String value = row.get(key);
                    if (String.valueOf(Double.POSITIVE_INFINITY).equals(value) || String.valueOf(Double.NEGATIVE_INFINITY).equals(value)) {
                        row.put(key, ""); // Set to empty string
                    } else if ((key.contains("avg") || key.contains("sum")) && value.equals("0.0")) {
                        row.put(key, "");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    // Method to read CSV file and return List of Maps
    public static List<Map<String, String>> readCsv(String filePath) {
        List<Map<String, String>> records = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (isHeader) {
                    headers.addAll(Arrays.asList(values));
                    isHeader = false;
                } else {
                    Map<String, String> rowMap = new HashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        if (i < values.length) {
                            rowMap.put(headers.get(i), values[i]);
                        } else {
                            rowMap.put(headers.get(i), null); // Handle missing columns
                        }
                    }
                    records.add(rowMap);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return records;
    }

    // Write List of Maps to a CSV file
    public static void writeCsv(String filePath, List<Map<String, String>> data) {
        if (data.isEmpty()) return;

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            // Write header
            writer.write(String.join(",", data.get(0).keySet()));
            writer.newLine();

            // Write each row
            for (Map<String, String> row : data) {
                writer.write(String.join(",", row.values()));
                writer.newLine();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    // TODO: remove below functions after code reviews
    // Print CSV file contents
    public static void printCsv(String filePath) {
        LOGGER.info("Contents of {}", filePath + ":");
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    // Write the final aggregated results to the output CSV file
    public static void writeFinalResults(String headerRow, List<Map<String, String>> aggData,
                                          String outputFile, Set<String> columnsToIgnore) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // Step 1: Write the header
            List<String> filteredHeader = new ArrayList<>();
            List<String> headerColumns = List.of(headerRow.split(","));
            for (String column : headerColumns) {
                if (!columnsToIgnore.contains(column)) {
                    filteredHeader.add(column); // Add only non-ignored columns to the filtered header
                }
            }

            // Step 2: Write the filtered header to the file
            writer.write(String.join(",", filteredHeader));
            writer.newLine();

            // Step 3: Write each row's data for only non-ignored columns
            for (Map<String, String> row : aggData) {
                List<String> filteredValues = new ArrayList<>();
                for (String column : filteredHeader) {
                    filteredValues.add(row.getOrDefault(column, "")); // Add only non-ignored column values
                }
                writer.write(String.join(",", filteredValues));
                writer.newLine();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
}

