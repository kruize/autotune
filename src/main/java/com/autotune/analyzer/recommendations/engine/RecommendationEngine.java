package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.model.CostBasedRecommendationModel;
import com.autotune.analyzer.recommendations.model.PerformanceBasedRecommendationModel;
import com.autotune.analyzer.recommendations.model.RecommendationModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.*;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.MEM_ZERO;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_EXPERIMENT_NAME;

public class RecommendationEngine {
    private String performanceProfile;
    private String experimentName;
    private Timestamp intervalEndTime;
    private Map<String, Terms> terms;
    List<RecommendationModel> recommendationModels;

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationEngine.class);


    public RecommendationEngine(String performanceProfile, String experimentName, Timestamp intervalEndTime, Map<String, Terms> terms) {
        this.performanceProfile = performanceProfile;
        this.experimentName = experimentName;
        this.intervalEndTime = intervalEndTime;
        this.terms = terms;
        this.init();
    }

    public RecommendationEngine() {

    }

    private void init() {
        // Add new models
        recommendationModels = new ArrayList<>();
        // Create Cost based model
        CostBasedRecommendationModel costBasedRecommendationModel = new CostBasedRecommendationModel();
        // TODO: Create profile based model
        registerModel(costBasedRecommendationModel);
        // Create Performance based model
        PerformanceBasedRecommendationModel performanceBasedRecommendationModel = new PerformanceBasedRecommendationModel();
        registerModel(performanceBasedRecommendationModel);
        // TODO: Add profile based once recommendation algos are available
    }

    private void registerModel(RecommendationModel recommendationModel) {
        if (null == recommendationModel) {
            return;
        }
        for (RecommendationModel model : getModels()) {
            if (model.getModelName().equalsIgnoreCase(recommendationModel.getModelName()))
                return;
        }
        // Add models
        getModels().add(recommendationModel);
    }

    public List<RecommendationModel> getModels() {
        return this.recommendationModels;
    }

    public String getPerformanceProfile() {
        return performanceProfile;
    }

    public void setPerformanceProfile(String performanceProfile) {
        this.performanceProfile = performanceProfile;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public Timestamp getIntervalEndTime() {
        return intervalEndTime;
    }

    public void setIntervalEndTime(Timestamp intervalEndTime) {
        this.intervalEndTime = intervalEndTime;
    }

    public Map<String, Terms> getTerms() {
        return terms;
    }

    public void setTerms(Map<String, Terms> terms) {
        this.terms = terms;
    }

    public KruizeObject validateAndGenerateRecommendation(String experiment_name, Timestamp interval_end_time,
                                                                  Timestamp interval_start_time, String intervalEndTimeStr,
                                                                  String intervalStartTimeStr, int calCount) {
        KruizeObject kruizeObject = new KruizeObject();
        ValidationOutputData validationOutputData;
        String validationMessage = validate(experiment_name, interval_end_time, interval_start_time, intervalEndTimeStr, intervalStartTimeStr);
        if (!validationMessage.isEmpty()) {
            LOGGER.debug("UpdateRecommendations API request count: {} failed", calCount);
            kruizeObject.setValidation_data(new ValidationOutputData(false, validationMessage, HttpServletResponse.SC_BAD_REQUEST));
            return kruizeObject;
        }
        interval_end_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT,
                intervalEndTimeStr);
        LOGGER.debug("UpdateRecommendations API request count: {} experiment_name : {} and interval_start_time : {} and interval_end_time : {} ", calCount, experiment_name, intervalStartTimeStr, intervalEndTimeStr);
        LOGGER.debug("experiment_name : {} and interval_start_time : {} and interval_end_time : {} ", experiment_name, intervalStartTimeStr, intervalEndTimeStr);

        Map<String, KruizeObject> mainKruizeExperimentMAP = new ConcurrentHashMap<>();
        Map<String, Terms> terms = new HashMap<>();
        try {
            new ExperimentDBService().loadExperimentFromDBByName(mainKruizeExperimentMAP, experiment_name);
            if (null != mainKruizeExperimentMAP.get(experiment_name)) {
                kruizeObject = mainKruizeExperimentMAP.get(experiment_name);
            }
            if (null != kruizeObject) {
                // set the default terms if the terms object is empty
                if (kruizeObject.getTerms() == null)
                    setDefaultTerms(terms);
                // get the datasource
                String dataSource = kruizeObject.getDataSource(); // TODO: This will be required for future use
                int maxDay = Terms.getMaxDays(terms);
                interval_start_time = Timestamp.valueOf(Objects.requireNonNull(interval_end_time).toLocalDateTime().minusDays(maxDay));

                mainKruizeExperimentMAP.put(experiment_name, kruizeObject);
                new ExperimentDBService().loadResultsFromDBByName(mainKruizeExperimentMAP, experiment_name,
                        interval_start_time, interval_end_time);

                // generate recommendation
                try {
                    generateRecommendations(kruizeObject);
                    validationOutputData = new ExperimentDBService().addRecommendationToDB(mainKruizeExperimentMAP,
                            kruizeObject, interval_end_time);
                    if (!validationOutputData.isSuccess()) {
                        LOGGER.debug("UpdateRecommendations API request count: {} failed", calCount);
                        validationOutputData = new ValidationOutputData(false, validationOutputData.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } else {
                        LOGGER.debug("UpdateRecommendations API request count: {} success", calCount);
                    }
                    kruizeObject.setValidation_data(validationOutputData);
                } catch (Exception e) {
                    LOGGER.debug("UpdateRecommendations API request count: {} failed", calCount);
                    e.printStackTrace();
                    LOGGER.error("Failed to create recommendation for experiment: {} and interval_start_time: {} and interval_end_time: {}",
                            experiment_name,
                            interval_start_time,
                            interval_end_time);
                    kruizeObject.setValidation_data(new ValidationOutputData(false, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
                }
            } else {
                LOGGER.debug("UpdateRecommendations API request count: {} failed", calCount);
                kruizeObject = new KruizeObject();
                kruizeObject.setValidation_data(new ValidationOutputData(false, String.format("%s%s", MISSING_EXPERIMENT_NAME, experiment_name),
                        HttpServletResponse.SC_BAD_REQUEST));
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.debug("UpdateRecommendations API request count: {} failed", calCount);
            kruizeObject = new KruizeObject();
            kruizeObject.setValidation_data(new ValidationOutputData(false, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
        return kruizeObject;
    }

    public String validate(String experiment_name, Timestamp interval_end_time, Timestamp interval_start_time,
                                  String intervalEndTimeStr, String intervalStartTimeStr) {

        String validationFailureMsg = "";
        // Check if experiment_name is provided
        if (experiment_name == null || experiment_name.isEmpty()) {
            validationFailureMsg += AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.EXPERIMENT_NAME_MANDATORY;
        }

        // Check if interval_end_time is provided
        if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
            validationFailureMsg += AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INTERVAL_END_TIME_MANDATORY;
        }
        if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr)) {
            validationFailureMsg += AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG;
        }

        // Check if interval_start_time is provided
        if (intervalStartTimeStr != null) {
            if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalStartTimeStr)) {
                validationFailureMsg += AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG;
            } else {
                interval_start_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT,
                        intervalStartTimeStr);
                if (interval_start_time != null) {
                    int comparisonResult = interval_start_time.compareTo(interval_end_time);
                    if (comparisonResult >= 0) {
                        // interval_start_time is after interval_end_time
                        validationFailureMsg += AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.TIME_COMPARE;
                    } else {
                        // calculate difference between two dates
                        if (interval_end_time != null) {
                            long differenceInMillis = interval_end_time.getTime() - interval_start_time.getTime();
                            long differenceInDays = TimeUnit.MILLISECONDS.toDays(differenceInMillis);
                            if (differenceInDays > KruizeDeploymentInfo.generate_recommendations_date_range_limit_in_days) {
                                validationFailureMsg += AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.TIME_GAP_LIMIT;
                            }
                        }
                    }
                }
            }
        }
        return validationFailureMsg;
    }


    public void generateRecommendations(KruizeObject kruizeObject) {

        //TODO: iterate using terms instead of results, check for min data and other notifications at terms level

        RecommendationSettings recommendationSettings = kruizeObject.getRecommendation_settings();
        for (K8sObject k8sObjectResultData : kruizeObject.getKubernetes_objects()) {
            // We only support one K8sObject currently
            K8sObject k8sObjectKruizeObject = kruizeObject.getKubernetes_objects().get(0);
            for (String cName : k8sObjectResultData.getContainerDataMap().keySet()) {
                ContainerData containerDataResultData = k8sObjectResultData.getContainerDataMap().get(cName);
                if (null == containerDataResultData.getResults() || containerDataResultData.getResults().isEmpty())
                    continue;

                // Get the ContainerData from the KruizeObject and not from ResultData
                ContainerData containerDataKruizeObject = k8sObjectKruizeObject.getContainerDataMap().get(cName);

                // Get the monitoringEndTime from ResultData's ContainerData. Should have only one element
                Timestamp monitoringEndTime = containerDataKruizeObject.getResults().keySet().stream().max(Timestamp::compareTo).get();

                ContainerRecommendations containerRecommendations = containerDataKruizeObject.getContainerRecommendations();
                // Just to make sure the container recommendations object is not empty
                if (null == containerRecommendations) {
                    containerRecommendations = new ContainerRecommendations();
                }

                HashMap<Integer, RecommendationNotification> recommendationLevelNM = containerRecommendations.getNotificationMap();
                if (null == recommendationLevelNM) {
                    recommendationLevelNM = new HashMap<>();
                }

                // Check for min data before setting notifications
                // Check for at least short term
                if (!Terms.checkIfMinDataAvailableForTerm(containerDataKruizeObject, RecommendationConstants.RecommendationTerms.SHORT_TERM)) {
                    RecommendationNotification recommendationNotification = new RecommendationNotification(
                            RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA
                    );
                    recommendationLevelNM.put(recommendationNotification.getCode(), recommendationNotification);
                    continue;
                }

                boolean recommendationAvailable = false;

                // Get the engine recommendation map for a time stamp if it exists else create one
                HashMap<Timestamp, MappedRecommendationForTimestamp> timestampBasedRecommendationMap
                        = containerRecommendations.getData();

                if (null == timestampBasedRecommendationMap) {
                    timestampBasedRecommendationMap = new HashMap<>();
                }
                // check if engines map exists else create one
                MappedRecommendationForTimestamp timestampRecommendation;
                if (timestampBasedRecommendationMap.containsKey(monitoringEndTime)) {
                    timestampRecommendation = timestampBasedRecommendationMap.get(monitoringEndTime);
                } else {
                    timestampRecommendation = new MappedRecommendationForTimestamp();
                }

                HashMap<Timestamp, IntervalResults> intervalResultsHashMap = containerDataResultData.getResults();
                timestampRecommendation.setMonitoringEndTime(monitoringEndTime);
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig = new HashMap<>();

                ArrayList<RecommendationConstants.RecommendationNotification> notifications = new ArrayList<>();

                // Create Current Requests Map
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentRequestsMap = new HashMap<>();

                // Create Current Limits Map
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentLimitsMap = new HashMap<>();

                for (AnalyzerConstants.ResourceSetting resourceSetting : AnalyzerConstants.ResourceSetting.values()) {
                    for (AnalyzerConstants.RecommendationItem recommendationItem : AnalyzerConstants.RecommendationItem.values()) {
                        RecommendationConfigItem configItem = RecommendationUtils.getCurrentValue(intervalResultsHashMap,
                                monitoringEndTime,
                                resourceSetting,
                                recommendationItem,
                                notifications);

                        if (null == configItem)
                            continue;
                        if (null == configItem.getAmount()) {
                            if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu))
                                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_CPU_SECTION);
                            else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory)))
                                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_MEMORY_SECTION);
                            continue;
                        }
                        if (null == configItem.getFormat()) {
                            if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu))
                                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_CPU_SECTION);
                            else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory)))
                                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);
                            continue;
                        }
                        if (configItem.getAmount() <= 0.0) {
                            if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu))
                                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
                            else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory)))
                                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
                            continue;
                        }
                        if (configItem.getFormat().isEmpty() || configItem.getFormat().isBlank()) {
                            if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu))
                                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                            else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory)))
                                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                            continue;
                        }

                        if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                            currentRequestsMap.put(recommendationItem, configItem);
                        }
                        if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                            currentLimitsMap.put(recommendationItem, configItem);
                        }
                    }
                }

                // Iterate over notifications and set to recommendations
                for (RecommendationConstants.RecommendationNotification recommendationNotification : notifications) {
                    timestampRecommendation.addNotification(new RecommendationNotification(recommendationNotification));
                }

                // Check if map is not empty and set requests map to current config
                if (!currentRequestsMap.isEmpty()) {
                    currentConfig.put(AnalyzerConstants.ResourceSetting.requests, currentRequestsMap);
                }

                // Check if map is not empty and set limits map to current config
                if (!currentLimitsMap.isEmpty()) {
                    currentConfig.put(AnalyzerConstants.ResourceSetting.limits, currentLimitsMap);
                }

                timestampRecommendation.setCurrentConfig(currentConfig);

                boolean termLevelRecommendationExist = false;
                for (RecommendationConstants.RecommendationTerms recommendationTerm : RecommendationConstants.RecommendationTerms.values()) {
                    String term = recommendationTerm.getValue();
                    LOGGER.debug("term = {}", term);
                    double duration = recommendationTerm.getDuration();

                    // TODO: Add check for min data

                    TermRecommendations mappedRecommendationForTerm = new TermRecommendations();

                    ArrayList<RecommendationNotification> termLevelNotifications = new ArrayList<>();

                    // Check if there is min data available for the term
                    if (!RecommendationUtils.checkIfMinDataAvailableForTerm(containerDataKruizeObject, recommendationTerm)) {
                        RecommendationNotification recommendationNotification = new RecommendationNotification(
                                RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                        mappedRecommendationForTerm.addNotification(recommendationNotification);
                    } else {
                        Timestamp monitoringStartTime = null;
                        for (RecommendationModel model : getModels()) {
                            boolean isCostEngine = false;
                            boolean isPerfEngine = false;

                            if (model.getModelName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.EngineNames.COST)) {
                                isCostEngine = true;
                            }
                            if (model.getModelName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.EngineNames.PERFORMANCE)) {
                                isPerfEngine = true;
                            }

                            // Get the results
                            HashMap<Timestamp, IntervalResults> resultsMap = containerDataKruizeObject.getResults();
                            monitoringStartTime = RecommendationUtils.getMonitoringStartTime(resultsMap,
                                    monitoringEndTime,
                                    Double.valueOf(String.valueOf(duration)));

                            // Now generate a new recommendation for the new data corresponding to the monitoringEndTime
                            MappedRecommendationForModel mappedRecommendationForModel = generateRecommendationBasedOnModel(
                                    model,
                                    monitoringStartTime,
                                    containerDataKruizeObject,
                                    monitoringEndTime,
                                    term,
                                    recommendationSettings,
                                    currentConfig,
                                    Double.valueOf(String.valueOf(duration)));

                            if (null == mappedRecommendationForModel) {
                                continue;
                            }

                            // Set term level notification available
                            termLevelRecommendationExist = true;

                            // Adding the term level recommendation availability after confirming the recommendation exists
                            RecommendationNotification rn = RecommendationUtils.getNotificationForTermAvailability(recommendationTerm);
                            if (null != rn) {
                                timestampRecommendation.addNotification(rn);
                            }

                            RecommendationNotification recommendationNotification = null;
                            if (isCostEngine) {
                                // Setting it as at least one recommendation available
                                recommendationAvailable = true;
                                recommendationNotification = new RecommendationNotification(
                                        RecommendationConstants.RecommendationNotification.INFO_COST_RECOMMENDATIONS_AVAILABLE
                                );
                            }

                            if (isPerfEngine) {
                                // Setting it as at least one recommendation available
                                recommendationAvailable = true;
                                recommendationNotification = new RecommendationNotification(
                                        RecommendationConstants.RecommendationNotification.INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE
                                );
                            }

                            if (null != recommendationNotification) {
                                termLevelNotifications.add(recommendationNotification);
                            } else {
                                recommendationNotification = new RecommendationNotification(
                                        RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA
                                );
                                termLevelNotifications.add(recommendationNotification);
                            }
                            mappedRecommendationForTerm.setRecommendationForEngineHashMap(model.getModelName(), mappedRecommendationForModel);
                        }

                        for (RecommendationNotification recommendationNotification : termLevelNotifications) {
                            mappedRecommendationForTerm.addNotification(recommendationNotification);
                        }
                        mappedRecommendationForTerm.setMonitoringStartTime(monitoringStartTime);
                    }
                    Terms.setDurationBasedOnTerm(containerDataKruizeObject, mappedRecommendationForTerm, recommendationTerm);
                    timestampRecommendation.setRecommendationForTermHashMap(term, mappedRecommendationForTerm);
                }
                if (!termLevelRecommendationExist) {
                    RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                    timestampRecommendation.addNotification(recommendationNotification);
                }

                // put recommendations tagging to timestamp
                timestampBasedRecommendationMap.put(monitoringEndTime, timestampRecommendation);

                if (recommendationAvailable) {
                    // set the Recommendations object level notifications
                    RecommendationNotification rn = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_RECOMMENDATIONS_AVAILABLE);
                    recommendationLevelNM.put(rn.getCode(), rn);
                }

                containerRecommendations.setNotificationMap(recommendationLevelNM);
                // set the data object to map
                containerRecommendations.setData(timestampBasedRecommendationMap);
                // set the container recommendations in container object
                containerDataKruizeObject.setContainerRecommendations(containerRecommendations);
            }
        }

    }

    private MappedRecommendationForModel generateRecommendationBasedOnModel(RecommendationModel model, Timestamp monitoringStartTime, ContainerData
            containerData, Timestamp monitoringEndTime, String recPeriod, RecommendationSettings recommendationSettings,
                                                                            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                    RecommendationConfigItem>> currentConfigMap, Double durationInHrs) {
        MappedRecommendationForModel mappedRecommendationForModel = new MappedRecommendationForModel();
        // Set CPU threshold to default
        double cpuThreshold = DEFAULT_CPU_THRESHOLD;
        // Set Memory threshold to default
        double memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
        if (null != recommendationSettings) {
            Double threshold = recommendationSettings.getThreshold();
            if (null == threshold) {
                LOGGER.info("Threshold is not set, setting Default CPU Threshold : " + DEFAULT_CPU_THRESHOLD + " and Memory Threshold : " + DEFAULT_MEMORY_THRESHOLD);
            } else if (threshold.doubleValue() <= 0.0) {
                LOGGER.error("Given Threshold is invalid, setting Default CPU Threshold : " + DEFAULT_CPU_THRESHOLD + " and Memory Threshold : " + DEFAULT_MEMORY_THRESHOLD);
            } else {
                cpuThreshold = threshold.doubleValue();
                memoryThreshold = threshold.doubleValue();
            }
        } else {
            LOGGER.error("Recommendation Settings are null, setting Default CPU Threshold : " + DEFAULT_CPU_THRESHOLD + " and Memory Threshold : " + DEFAULT_MEMORY_THRESHOLD);
        }

        RecommendationConfigItem currentCPURequest = null;
        RecommendationConfigItem currentCPULimit = null;
        RecommendationConfigItem currentMemRequest = null;
        RecommendationConfigItem currentMemLimit = null;

        if (currentConfigMap.containsKey(AnalyzerConstants.ResourceSetting.requests) && null != currentConfigMap.get(AnalyzerConstants.ResourceSetting.requests)) {
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap = currentConfigMap.get(AnalyzerConstants.ResourceSetting.requests);
            if (requestsMap.containsKey(AnalyzerConstants.RecommendationItem.cpu) && null != requestsMap.get(AnalyzerConstants.RecommendationItem.cpu)) {
                currentCPURequest = requestsMap.get(AnalyzerConstants.RecommendationItem.cpu);
            }
            if (requestsMap.containsKey(AnalyzerConstants.RecommendationItem.memory) && null != requestsMap.get(AnalyzerConstants.RecommendationItem.memory)) {
                currentMemRequest = requestsMap.get(AnalyzerConstants.RecommendationItem.memory);
            }
        }
        if (currentConfigMap.containsKey(AnalyzerConstants.ResourceSetting.limits) && null != currentConfigMap.get(AnalyzerConstants.ResourceSetting.limits)) {
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMap = currentConfigMap.get(AnalyzerConstants.ResourceSetting.limits);
            if (limitsMap.containsKey(AnalyzerConstants.RecommendationItem.cpu) && null != limitsMap.get(AnalyzerConstants.RecommendationItem.cpu)) {
                currentCPULimit = limitsMap.get(AnalyzerConstants.RecommendationItem.cpu);
            }
            if (limitsMap.containsKey(AnalyzerConstants.RecommendationItem.memory) && null != limitsMap.get(AnalyzerConstants.RecommendationItem.memory)) {
                currentMemLimit = limitsMap.get(AnalyzerConstants.RecommendationItem.memory);
            }
        }
        if (null != monitoringStartTime) {
            Timestamp finalMonitoringStartTime = monitoringStartTime;
            // Set the timestamp to extract
            Timestamp timestampToExtract = monitoringEndTime;

            Map<Timestamp, IntervalResults> filteredResultsMap = containerData.getResults().entrySet().stream()
                    .filter((x -> ((x.getKey().compareTo(finalMonitoringStartTime) >= 0)
                            && (x.getKey().compareTo(monitoringEndTime) <= 0))))
                    .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

            // Set number of pods
            int numPods = getNumPods(filteredResultsMap);

            mappedRecommendationForModel.setPodsCount(numPods);

            // Pass Notification object to all callers to update the notifications required
            ArrayList<RecommendationNotification> notifications = new ArrayList<RecommendationNotification>();

            // Get the Recommendation Items
            RecommendationConfigItem recommendationCpuRequest = model.getCPURequestRecommendation(filteredResultsMap, notifications);
            RecommendationConfigItem recommendationMemRequest = model.getMemoryRequestRecommendation(filteredResultsMap, notifications);

            // Get the Recommendation Items
            // Calling requests on limits as we are maintaining limits and requests as same
            // Maintaining different flow for both of them even though if they are same as in future we might have
            // a different implementation for both and this avoids confusion
            RecommendationConfigItem recommendationCpuLimits = recommendationCpuRequest;
            RecommendationConfigItem recommendationMemLimits = recommendationMemRequest;

            // Create an internal map to send data to populate
            HashMap<String, RecommendationConfigItem> internalMapToPopulate = new HashMap<String, RecommendationConfigItem>();
            // Add current values
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_REQUEST, currentCPURequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_LIMIT, currentCPULimit);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_REQUEST, currentMemRequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_LIMIT, currentMemLimit);
            // Add recommended values
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_REQUEST, recommendationCpuRequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_LIMIT, recommendationCpuLimits);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_REQUEST, recommendationMemRequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_LIMIT, recommendationMemLimits);

            // Call the populate method to validate and populate the recommendation object
            boolean isSuccess = populateRecommendation(
                    recPeriod,
                    mappedRecommendationForModel,
                    notifications,
                    internalMapToPopulate,
                    numPods,
                    durationInHrs,
                    cpuThreshold,
                    memoryThreshold
            );
        }  else {
            RecommendationNotification notification = new RecommendationNotification(
                    RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
            mappedRecommendationForModel.addNotification(notification);
        }
        return mappedRecommendationForModel;
    }

    private static int getNumPods(Map<Timestamp, IntervalResults> filteredResultsMap) {
        Double max_pods_cpu = filteredResultsMap.values()
                .stream()
                .map(e -> {
                    Optional<MetricResults> cpuUsageResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage));
                    double cpuUsageSum = cpuUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
                    double cpuUsageAvg = cpuUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
                    double numPods = 0;

                    if (0 != cpuUsageAvg) {
                        numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
                    }
                    return numPods;
                })
                .max(Double::compareTo).get();

        return (int) Math.ceil(max_pods_cpu);
    }

    /**
     * This method handles validating the data and populating to the recommendation object
     * <p>
     * DO NOT EDIT THIS METHOD UNLESS THERE ARE ANY CHANGES TO BE ADDED IN VALIDATION OR POPULATION MECHANISM
     * EDITING THIS METHOD MIGHT LEAD TO UNEXPECTED OUTCOMES IN RECOMMENDATIONS, PLEASE PROCEED WITH CAUTION
     *
     * @param recommendationTerm
     * @param recommendation
     * @param notifications
     * @param internalMapToPopulate
     */
    private boolean populateRecommendation(String recommendationTerm,
                                           MappedRecommendationForModel recommendation,
                                           ArrayList<RecommendationNotification> notifications,
                                           HashMap<String, RecommendationConfigItem> internalMapToPopulate,
                                           int numPods, double hours, double cpuThreshold, double memoryThreshold) {
        // Check for cpu & memory Thresholds (Duplicate check if the caller is generate recommendations)
        if (cpuThreshold <= 0.0) {
            LOGGER.error("Given CPU Threshold is invalid, setting Default CPU Threshold : " + DEFAULT_CPU_THRESHOLD);
            cpuThreshold = DEFAULT_CPU_THRESHOLD;
        }
        if (memoryThreshold <= 0.0) {
            LOGGER.error("Given Memory Threshold is invalid, setting Default Memory Threshold : " + DEFAULT_MEMORY_THRESHOLD);
            memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
        }
        // Check for null
        if (null == recommendationTerm) {
            LOGGER.error("Recommendation term cannot be null");
            return false;
        }
        // Remove whitespaces
        recommendationTerm = recommendationTerm.trim();

        // Check if term is not empty and also must be one of short, medium or long term
        if (recommendationTerm.isEmpty() ||
                (
                        !recommendationTerm.equalsIgnoreCase(KruizeConstants.JSONKeys.SHORT_TERM) &&
                                !recommendationTerm.equalsIgnoreCase(KruizeConstants.JSONKeys.MEDIUM_TERM) &&
                                !recommendationTerm.equalsIgnoreCase(KruizeConstants.JSONKeys.LONG_TERM)
                )
        ) {
            LOGGER.error("Invalid Recommendation Term");
            return false;
        }

        // Check if recommendation is null
        if (null == recommendation) {
            LOGGER.error("Recommendation cannot be null");
            return false;
        }

        // Check if notification is null (Do not check for empty as notifications might not have been populated)
        if (null == notifications) {
            LOGGER.error("Notifications cannot be null");
            return false;
        }

        // Check if the map is populated with atleast one data point
        if (null == internalMapToPopulate || internalMapToPopulate.isEmpty()) {
            LOGGER.error("Internal map sent to populate method cannot be null or empty");
            return false;
        }

        boolean isSuccess = true;

        // CPU flags
        //      Current Request and Limits flags
        boolean isCurrentCPURequestAvailable = false;
        boolean isCurrentCPULimitAvailable = false;

        //      Recommended Request and Limits flags
        boolean isRecommendedCPURequestAvailable = false;
        boolean isRecommendedCPULimitAvailable = false;

        //      Variation Requests and Limits flags
        boolean isVariationCPURequestAvailable = false;
        boolean isVariationCPULimitAvailable = false;

        // Memory flags
        //      Current Request and Limits flags
        boolean isCurrentMemoryRequestAvailable = false;
        boolean isCurrentMemoryLimitAvailable = false;

        //      Recommended Request and Limits flags
        boolean isRecommendedMemoryRequestAvailable = false;
        boolean isRecommendedMemoryLimitAvailable = false;

        //      Variation Requests and Limits flags
        boolean isVariationMemoryRequestAvailable = false;
        boolean isVariationMemoryLimitAvailable = false;


        // Set Hours
        if (hours == 0.0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_HOURS_CANNOT_BE_ZERO);
            notifications.add(recommendationNotification);
            LOGGER.debug("Duration hours cannot be zero");
            isSuccess = false;
        } else if (hours < 0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_HOURS_CANNOT_BE_NEGATIVE);
            notifications.add(recommendationNotification);
            LOGGER.debug("Duration hours cannot be negative");
            isSuccess = false;
        }

        RecommendationConfigItem recommendationCpuRequest = null;
        RecommendationConfigItem recommendationMemRequest = null;
        RecommendationConfigItem recommendationCpuLimits = null;
        RecommendationConfigItem recommendationMemLimits = null;

        RecommendationConfigItem currentCpuRequest = null;
        RecommendationConfigItem currentMemRequest = null;
        RecommendationConfigItem currentCpuLimit = null;
        RecommendationConfigItem currentMemLimit = null;

        RecommendationConfigItem variationCpuRequest = null;
        RecommendationConfigItem variationMemRequest = null;
        RecommendationConfigItem variationCpuLimit = null;
        RecommendationConfigItem variationMemLimit = null;

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_REQUEST))
            recommendationCpuRequest = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_REQUEST);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_REQUEST))
            recommendationMemRequest = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_REQUEST);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_LIMIT))
            recommendationCpuLimits = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_LIMIT);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_LIMIT))
            recommendationMemLimits = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_LIMIT);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_REQUEST))
            currentCpuRequest = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_REQUEST);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_REQUEST))
            currentMemRequest = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_REQUEST);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_LIMIT))
            currentCpuLimit = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_LIMIT);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_LIMIT))
            currentMemLimit = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_LIMIT);


        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config = new HashMap<>();
        // Create Request Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap = new HashMap<>();
        // Recommendation Item checks
        boolean isCpuRequestValid = true;
        boolean isMemoryRequestValid = true;

        if (null == recommendationCpuRequest || null == recommendationCpuRequest.getAmount() || recommendationCpuRequest.getAmount() <= 0) {
            isCpuRequestValid = false;
        }
        if (null == recommendationMemRequest || null == recommendationMemRequest.getAmount() || recommendationMemRequest.getAmount() <= 0) {
            isMemoryRequestValid = false;
        }

        // Initiate generated value holders with min values constants to compare later
        Double generatedCpuRequest = null;
        String generatedCpuRequestFormat = null;
        Double generatedMemRequest = null;
        String generatedMemRequestFormat = null;

        // Check for null
        if (null != recommendationCpuRequest && isCpuRequestValid) {
            generatedCpuRequest = recommendationCpuRequest.getAmount();
            generatedCpuRequestFormat = recommendationCpuRequest.getFormat();
            if (null != generatedCpuRequestFormat && !generatedCpuRequestFormat.isEmpty()) {
                isRecommendedCPURequestAvailable = true;
                requestsMap.put(AnalyzerConstants.RecommendationItem.cpu, recommendationCpuRequest);
            } else {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION);
            }
        }

        // Check for null
        if (null != recommendationMemRequest && isMemoryRequestValid) {
            generatedMemRequest = recommendationMemRequest.getAmount();
            generatedMemRequestFormat = recommendationMemRequest.getFormat();
            if (null != generatedMemRequestFormat && !generatedMemRequestFormat.isEmpty()) {
                isRecommendedMemoryRequestAvailable = true;
                requestsMap.put(AnalyzerConstants.RecommendationItem.memory, recommendationMemRequest);
            } else {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION);
            }
        }

        // Create Limits Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMap = new HashMap<>();
        // Recommendation Item checks (adding additional check for limits even though they are same as limits to maintain code to be flexible to add limits in future)
        boolean isCpuLimitValid = true;
        boolean isMemoryLimitValid = true;


        if (null == recommendationCpuLimits || null == recommendationCpuLimits.getAmount() || recommendationCpuLimits.getAmount() <= 0) {
            isCpuLimitValid = false;
        }
        if (null == recommendationMemLimits || null == recommendationMemLimits.getAmount() || recommendationMemLimits.getAmount() <= 0) {
            isMemoryLimitValid = false;
        }

        // Initiate generated value holders with min values constants to compare later
        Double generatedCpuLimit = null;
        String generatedCpuLimitFormat = null;
        Double generatedMemLimit = null;
        String generatedMemLimitFormat = null;

        // Check for null
        if (null != recommendationCpuLimits && isCpuLimitValid) {
            generatedCpuLimit = recommendationCpuLimits.getAmount();
            generatedCpuLimitFormat = recommendationCpuLimits.getFormat();
            if (null != generatedCpuLimitFormat && !generatedCpuLimitFormat.isEmpty()) {
                isRecommendedCPULimitAvailable = true;
                limitsMap.put(AnalyzerConstants.RecommendationItem.cpu, recommendationCpuLimits);
            } else {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION);
            }
        }

        // Check for null
        if (null != recommendationMemLimits && isMemoryLimitValid) {
            generatedMemLimit = recommendationMemLimits.getAmount();
            generatedMemLimitFormat = recommendationMemLimits.getFormat();
            if (null != generatedMemLimitFormat && !generatedMemLimitFormat.isEmpty()) {
                isRecommendedMemoryLimitAvailable = true;
                limitsMap.put(AnalyzerConstants.RecommendationItem.memory, recommendationMemLimits);
            } else {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION);
            }
        }

        // Create Current Map
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig = new HashMap<>();

        // Create Current Requests Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentRequestsMap = new HashMap<>();

        // Check if Current CPU Requests Exists
        if (null != currentCpuRequest && null != currentCpuRequest.getAmount()) {
            if (currentCpuRequest.getAmount() <= 0.0) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION);
            } else if (null == currentCpuRequest.getFormat() || currentCpuRequest.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION);
            } else {
                isCurrentCPURequestAvailable = true;
                currentRequestsMap.put(AnalyzerConstants.RecommendationItem.cpu, currentCpuRequest);
            }
        }

        // Check if Current Memory Requests Exists
        if (null != currentMemRequest && null != currentMemRequest.getAmount()) {
            if (currentMemRequest.getAmount() <= 0) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION);
            } else if (null == currentMemRequest.getFormat() || currentMemRequest.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION);
            } else {
                isCurrentMemoryRequestAvailable = true;
                currentRequestsMap.put(AnalyzerConstants.RecommendationItem.memory, currentMemRequest);
            }
        }

        // Create Current Limits Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentLimitsMap = new HashMap<>();

        // Check if Current CPU Limits Exists
        if (null != currentCpuLimit && null != currentCpuLimit.getAmount()) {
            if (currentCpuLimit.getAmount() <= 0.0) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION);
            } else if (null == currentCpuLimit.getFormat() || currentCpuLimit.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION);
            } else {
                isCurrentCPULimitAvailable = true;
                currentLimitsMap.put(AnalyzerConstants.RecommendationItem.cpu, currentCpuLimit);
            }
        }

        // Check if Current Memory Limits Exists
        if (null != currentMemLimit && null != currentMemLimit.getAmount()) {
            if (currentMemLimit.getAmount() <= 0.0) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION);
            } else if (null == currentMemLimit.getFormat() || currentMemLimit.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION);
            } else {
                isCurrentMemoryLimitAvailable = true;
                currentLimitsMap.put(AnalyzerConstants.RecommendationItem.memory, currentMemLimit);
            }
        }

        // Create variation map
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variation = new HashMap<>();
        // Create a new map for storing variation in requests
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsVariationMap = new HashMap<>();

        double currentCpuRequestValue = 0.0;
        if (null != currentCpuRequest && null != currentCpuRequest.getAmount() && currentCpuRequest.getAmount() > 0.0) {
            currentCpuRequestValue = currentCpuRequest.getAmount();
        }
        if (null != generatedCpuRequest && null != generatedCpuRequestFormat) {
            double diff = generatedCpuRequest - currentCpuRequestValue;
            // TODO: If difference is positive it can be considered as under-provisioning, Need to handle it better
            isVariationCPURequestAvailable = true;
            variationCpuRequest = new RecommendationConfigItem(diff, generatedCpuRequestFormat);
            requestsVariationMap.put(AnalyzerConstants.RecommendationItem.cpu, variationCpuRequest);
        }

        double currentMemRequestValue = 0.0;
        if (null != currentMemRequest && null != currentMemRequest.getAmount() && currentMemRequest.getAmount() > 0.0) {
            currentMemRequestValue = currentMemRequest.getAmount();
        }
        if (null != generatedMemRequest && null != generatedMemRequestFormat) {
            double diff = generatedMemRequest - currentMemRequestValue;
            // TODO: If difference is positive it can be considered as under-provisioning, Need to handle it better
            isVariationMemoryRequestAvailable = true;
            variationMemRequest = new RecommendationConfigItem(diff, generatedMemRequestFormat);
            requestsVariationMap.put(AnalyzerConstants.RecommendationItem.memory, variationMemRequest);
        }

        // Create a new map for storing variation in limits
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsVariationMap = new HashMap<>();

        // No notification if CPU limit not set
        // Check if currentCpuLimit is not null and

        double currentCpuLimitValue = 0.0;
        if (null != currentCpuLimit && null != currentCpuLimit.getAmount() && currentCpuLimit.getAmount() > 0.0) {
            currentCpuLimitValue = currentCpuLimit.getAmount();
        }
        if (null != generatedCpuLimit && null != generatedCpuLimitFormat) {
            double diff = generatedCpuLimit - currentCpuLimitValue;
            isVariationCPULimitAvailable = true;
            variationCpuLimit = new RecommendationConfigItem(diff, generatedCpuLimitFormat);
            limitsVariationMap.put(AnalyzerConstants.RecommendationItem.cpu, variationCpuLimit);
        }

        double currentMemLimitValue = 0.0;
        if (null != currentMemLimit && null != currentMemLimit.getAmount() && currentMemLimit.getAmount() > 0.0) {
            currentMemLimitValue = currentMemLimit.getAmount();
        }
        if (null != generatedMemLimit && null != generatedMemLimitFormat) {
            double diff = generatedMemLimit - currentMemLimitValue;
            isVariationMemoryLimitAvailable = true;
            variationMemLimit = new RecommendationConfigItem(diff, generatedMemLimitFormat);
            limitsVariationMap.put(AnalyzerConstants.RecommendationItem.memory, variationMemLimit);
        }

        // build the engine level notifications here
        ArrayList<RecommendationNotification> engineNotifications = new ArrayList<>();
        if (numPods == 0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_NUM_PODS_CANNOT_BE_ZERO);
            engineNotifications.add(recommendationNotification);
            LOGGER.debug("Number of pods cannot be zero");
            isSuccess = false;
        } else if (numPods < 0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_NUM_PODS_CANNOT_BE_NEGATIVE);
            engineNotifications.add(recommendationNotification);
            LOGGER.debug("Number of pods cannot be negative");
            isSuccess = false;
        } else {
            recommendation.setPodsCount(numPods);
        }

        // Check for thresholds
        if (isRecommendedCPURequestAvailable) {
            if (isCurrentCPURequestAvailable && currentCpuRequestValue > 0.0 && null != generatedCpuRequest) {
                double diffCpuRequestPercentage = CommonUtils.getPercentage(generatedCpuRequest.doubleValue(), currentCpuRequestValue);
                // Check if variation percentage is negative
                if (diffCpuRequestPercentage < 0.0) {
                    // Convert to positive to check with threshold
                    diffCpuRequestPercentage = diffCpuRequestPercentage * (-1);
                }
                if (diffCpuRequestPercentage <= cpuThreshold) {
                    // Remove from Config (Uncomment next line and comment the alternative if you don't want to display recommendation if threshold is not met)
                    // requestsMap.remove(AnalyzerConstants.RecommendationItem.cpu);

                    // Remove from Variation (Uncomment next line and comment the alternative if you don't want to display recommendation if threshold is not met)
                    // requestsVariationMap.remove(AnalyzerConstants.RecommendationItem.cpu);

                    // Alternative - CPU REQUEST VALUE
                    // Accessing existing recommendation item
                    RecommendationConfigItem tempAccessedRecCPURequest = requestsMap.get(AnalyzerConstants.RecommendationItem.cpu);
                    if (null != tempAccessedRecCPURequest) {
                        // Updating it with desired value
                        tempAccessedRecCPURequest.setAmount(currentCpuRequestValue);
                    }
                    // Replace the updated object (Step not needed as we are updating existing object, but just to make sure it's updated)
                    requestsMap.put(AnalyzerConstants.RecommendationItem.cpu, tempAccessedRecCPURequest);

                    // Alternative - CPU REQUEST VARIATION VALUE
                    // Accessing existing recommendation item
                    RecommendationConfigItem tempAccessedRecCPURequestVariation = requestsVariationMap.get(AnalyzerConstants.RecommendationItem.cpu);
                    if (null != tempAccessedRecCPURequestVariation) {
                        // Updating it with desired value (as we are setting to current variation would be 0)
                        tempAccessedRecCPURequestVariation.setAmount(CPU_ZERO);
                    }
                    // Replace the updated object (Step not needed as we are updating existing object, but just to make sure it's updated)
                    requestsVariationMap.put(AnalyzerConstants.RecommendationItem.cpu, tempAccessedRecCPURequestVariation);

                    RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.NOTICE_CPU_REQUESTS_OPTIMISED);
                    engineNotifications.add(recommendationNotification);
                }
            }
        }

        if (isRecommendedCPULimitAvailable) {
            if (isCurrentCPULimitAvailable && currentCpuLimitValue > 0.0 && null != generatedCpuLimit) {
                double diffCPULimitPercentage = CommonUtils.getPercentage(generatedCpuLimit.doubleValue(), currentCpuLimitValue);
                // Check if variation percentage is negative
                if (diffCPULimitPercentage < 0.0) {
                    // Convert to positive to check with threshold
                    diffCPULimitPercentage = diffCPULimitPercentage * (-1);
                }
                if (diffCPULimitPercentage <= cpuThreshold) {
                    // Remove from Config (Uncomment next line and comment the alternative if you don't want to display recommendation if threshold is not met)
                    // limitsMap.remove(AnalyzerConstants.RecommendationItem.cpu);
                    // Remove from Variation (Uncomment next line and comment the alternative if you don't want to display recommendation if threshold is not met)
                    // limitsVariationMap.remove(AnalyzerConstants.RecommendationItem.cpu);

                    // Alternative - CPU LIMIT VALUE
                    // Accessing existing recommendation item
                    RecommendationConfigItem tempAccessedRecCPULimit = limitsMap.get(AnalyzerConstants.RecommendationItem.cpu);
                    if (null != tempAccessedRecCPULimit) {
                        // Updating it with desired value
                        tempAccessedRecCPULimit.setAmount(currentCpuLimitValue);
                    }
                    // Replace the updated object (Step not needed as we are updating existing object, but just to make sure it's updated)
                    limitsMap.put(AnalyzerConstants.RecommendationItem.cpu, tempAccessedRecCPULimit);

                    // Alternative - CPU LIMIT VARIATION VALUE
                    // Accessing existing recommendation item
                    RecommendationConfigItem tempAccessedRecCPULimitVariation = limitsVariationMap.get(AnalyzerConstants.RecommendationItem.cpu);
                    if (null != tempAccessedRecCPULimitVariation) {
                        // Updating it with desired value (as we are setting to current variation would be 0)
                        tempAccessedRecCPULimitVariation.setAmount(CPU_ZERO);
                    }
                    // Replace the updated object (Step not needed as we are updating existing object, but just to make sure it's updated)
                    limitsVariationMap.put(AnalyzerConstants.RecommendationItem.cpu, tempAccessedRecCPULimitVariation);

                    RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.NOTICE_CPU_LIMITS_OPTIMISED);
                    engineNotifications.add(recommendationNotification);
                }
            }
        }

        if (isRecommendedMemoryRequestAvailable) {
            if (isCurrentMemoryRequestAvailable && currentMemRequestValue > 0.0 && null != generatedMemRequest) {
                double diffMemRequestPercentage = CommonUtils.getPercentage(generatedMemRequest.doubleValue(), currentMemRequestValue);
                // Check if variation percentage is negative
                if (diffMemRequestPercentage < 0.0) {
                    // Convert to positive to check with threshold
                    diffMemRequestPercentage = diffMemRequestPercentage * (-1);
                }
                if (diffMemRequestPercentage <= memoryThreshold) {
                    // Remove from Config (Uncomment next line and comment the alternative if you don't want to display recommendation if threshold is not met)
                    // requestsMap.remove(AnalyzerConstants.RecommendationItem.memory);
                    // Remove from Variation (Uncomment next line and comment the alternative if you don't want to display recommendation if threshold is not met)
                    // requestsVariationMap.remove(AnalyzerConstants.RecommendationItem.memory);

                    // Alternative - MEMORY REQUEST VALUE
                    // Accessing existing recommendation item
                    RecommendationConfigItem tempAccessedRecMemoryRequest = requestsMap.get(AnalyzerConstants.RecommendationItem.memory);
                    if (null != tempAccessedRecMemoryRequest) {
                        // Updating it with desired value
                        tempAccessedRecMemoryRequest.setAmount(currentMemRequestValue);
                    }
                    // Replace the updated object (Step not needed as we are updating existing object, but just to make sure it's updated)
                    requestsMap.put(AnalyzerConstants.RecommendationItem.memory, tempAccessedRecMemoryRequest);

                    // Alternative - MEMORY REQUEST VARIATION VALUE
                    // Accessing existing recommendation item
                    RecommendationConfigItem tempAccessedRecMemoryRequestVariation = requestsVariationMap.get(AnalyzerConstants.RecommendationItem.memory);
                    if (null != tempAccessedRecMemoryRequestVariation) {
                        // Updating it with desired value (as we are setting to current variation would be 0)
                        tempAccessedRecMemoryRequestVariation.setAmount(MEM_ZERO);
                    }
                    // Replace the updated object (Step not needed as we are updating existing object, but just to make sure it's updated)
                    requestsVariationMap.put(AnalyzerConstants.RecommendationItem.memory, tempAccessedRecMemoryRequestVariation);

                    RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.NOTICE_MEMORY_REQUESTS_OPTIMISED);
                    engineNotifications.add(recommendationNotification);
                }
            }
        }

        if (isRecommendedMemoryLimitAvailable) {
            if (isCurrentMemoryLimitAvailable && currentMemLimitValue > 0.0 && null != generatedMemLimit) {
                double diffMemLimitPercentage = CommonUtils.getPercentage(generatedMemLimit.doubleValue(), currentMemLimitValue);
                // Check if variation percentage is negative
                if (diffMemLimitPercentage < 0.0) {
                    // Convert to positive to check with threshold
                    diffMemLimitPercentage = diffMemLimitPercentage * (-1);
                }
                if (diffMemLimitPercentage <= memoryThreshold) {
                    // Remove from Config (Uncomment next line and comment the alternative if you don't want to display recommendation if threshold is not met)
                    // limitsMap.remove(AnalyzerConstants.RecommendationItem.memory);
                    // Remove from Variation (Uncomment next line and comment the alternative if you don't want to display recommendation if threshold is not met)
                    // limitsVariationMap.remove(AnalyzerConstants.RecommendationItem.memory);

                    // Alternative - MEMORY LIMIT VALUE
                    // Accessing existing recommendation item
                    RecommendationConfigItem tempAccessedRecMemoryLimit = limitsMap.get(AnalyzerConstants.RecommendationItem.memory);
                    if (null != tempAccessedRecMemoryLimit) {
                        // Updating it with desired value
                        tempAccessedRecMemoryLimit.setAmount(currentMemLimitValue);
                    }
                    // Replace the updated object (Step not needed as we are updating existing object, but just to make sure it's updated)
                    limitsMap.put(AnalyzerConstants.RecommendationItem.memory, tempAccessedRecMemoryLimit);

                    // Alternative - MEMORY LIMIT VARIATION VALUE
                    // Accessing existing recommendation item
                    RecommendationConfigItem tempAccessedRecMemoryLimitVariation = limitsVariationMap.get(AnalyzerConstants.RecommendationItem.memory);
                    if (null != tempAccessedRecMemoryLimitVariation) {
                        // Updating it with desired value (as we are setting to current variation would be 0)
                        tempAccessedRecMemoryLimitVariation.setAmount(MEM_ZERO);
                    }
                    // Replace the updated object (Step not needed as we are updating existing object, but just to make sure it's updated)
                    limitsVariationMap.put(AnalyzerConstants.RecommendationItem.memory, tempAccessedRecMemoryLimitVariation);

                    RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.NOTICE_MEMORY_LIMITS_OPTIMISED);
                    engineNotifications.add(recommendationNotification);
                }
            }
        }

        // set the engine level notifications here
        for (RecommendationNotification recommendationNotification : engineNotifications) {
            recommendation.addNotification(recommendationNotification);
        }

        // Set Request Map
        if (!requestsMap.isEmpty()) {
            config.put(AnalyzerConstants.ResourceSetting.requests, requestsMap);
        }

        // Set Limits Map
        if (!limitsMap.isEmpty()) {
            config.put(AnalyzerConstants.ResourceSetting.limits, limitsMap);
        }

        // Set Config
        if (!config.isEmpty()) {
            recommendation.setConfig(config);
        }

        // Check if map is not empty and set requests map to current config
        if (!currentRequestsMap.isEmpty()) {
            currentConfig.put(AnalyzerConstants.ResourceSetting.requests, currentRequestsMap);
        }

        // Check if map is not empty and set limits map to current config
        if (!currentLimitsMap.isEmpty()) {
            currentConfig.put(AnalyzerConstants.ResourceSetting.limits, currentLimitsMap);
        }

        // Set Request variation map
        if (!requestsVariationMap.isEmpty()) {
            variation.put(AnalyzerConstants.ResourceSetting.requests, requestsVariationMap);
        }

        // Set Limits variation map
        if (!limitsVariationMap.isEmpty()) {
            variation.put(AnalyzerConstants.ResourceSetting.limits, limitsVariationMap);
        }

        // Set Variation Map
        if (!variation.isEmpty()) {
            recommendation.setVariation(variation);
        }

        return isSuccess;
    }

    public static void setDefaultTerms(Map<String, Terms> terms) {
        terms.put(KruizeConstants.JSONKeys.SHORT_TERM, new Terms(KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.SHORT_TERM_DURATION_DAYS, KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.SHORT_TERM_DURATION_DAYS_THRESHOLD));
        terms.put(KruizeConstants.JSONKeys.MEDIUM_TERM, new Terms(KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.MEDIUM_TERM_DURATION_DAYS, KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.MEDIUM_TERM_DURATION_DAYS_THRESHOLD));
        terms.put(KruizeConstants.JSONKeys.LONG_TERM, new Terms(KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.LONG_TERM_DURATION_DAYS, KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.LONG_TERM_DURATION_DAYS_THRESHOLD));
    }
}
