package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.performanceProfiles.MetricProfileCollection;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.plots.PlotManager;
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
import com.autotune.common.data.dataSourceQueries.PromQLDataSourceQueries;
import com.autotune.common.data.metrics.AggregationFunctions;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.exceptions.DataSourceNotExist;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.metrics.KruizeNotificationCollectionRegistry;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.micrometer.core.instrument.Timer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.*;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_EXPERIMENT_NAME;

public class RecommendationEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationEngine.class);
    private final String intervalEndTimeStr;
    private final String intervalStartTimeStr; // TODO: to be used in future
    List<RecommendationModel> recommendationModels;
    private String performanceProfile;
    private String experimentName;
    private Map<String, Terms> terms;
    private KruizeObject kruizeObject;
    private Timestamp interval_end_time;


    public RecommendationEngine(String experimentName, String intervalEndTimeStr, String intervalStartTimeStr) {
        this.experimentName = experimentName;
        this.intervalEndTimeStr = intervalEndTimeStr;
        this.intervalStartTimeStr = intervalStartTimeStr;
        this.init();
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
     * Populates the given map with Prometheus Query Language (PromQL) queries for various metrics.
     *
     * @param promQls The map to be populated with PromQL queries.
     */
    private static void getPromQls(Map<AnalyzerConstants.MetricName, String> promQls) {
        promQls.put(AnalyzerConstants.MetricName.cpuUsage, PromQLDataSourceQueries.CPU_USAGE);
        promQls.put(AnalyzerConstants.MetricName.cpuThrottle, PromQLDataSourceQueries.CPU_THROTTLE);
        promQls.put(AnalyzerConstants.MetricName.cpuLimit, PromQLDataSourceQueries.CPU_LIMIT);
        promQls.put(AnalyzerConstants.MetricName.cpuRequest, PromQLDataSourceQueries.CPU_REQUEST);
        promQls.put(AnalyzerConstants.MetricName.memoryUsage, PromQLDataSourceQueries.MEMORY_USAGE);
        promQls.put(AnalyzerConstants.MetricName.memoryRSS, PromQLDataSourceQueries.MEMORY_RSS);
        promQls.put(AnalyzerConstants.MetricName.memoryLimit, PromQLDataSourceQueries.MEMORY_LIMIT);
        promQls.put(AnalyzerConstants.MetricName.memoryRequest, PromQLDataSourceQueries.MEMORY_REQUEST);
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

    public Timestamp getInterval_end_time() {
        return interval_end_time;
    }

    public void setInterval_end_time(Timestamp interval_end_time) {
        this.interval_end_time = interval_end_time;
    }

    public Map<String, Terms> getTerms() {
        return terms;
    }

    public void setTerms(Map<String, Terms> terms) {
        this.terms = terms;
    }

    public KruizeObject getKruizeObject() {
        return kruizeObject;
    }

    public void setKruizeObject(KruizeObject kruizeObject) {
        this.kruizeObject = kruizeObject;
    }

    private KruizeObject createKruizeObject() {
        Map<String, KruizeObject> mainKruizeExperimentMAP = new ConcurrentHashMap<>();
        KruizeObject kruizeObject = new KruizeObject();
        try {
            new ExperimentDBService().loadExperimentFromDBByName(mainKruizeExperimentMAP, experimentName);
            if (null != mainKruizeExperimentMAP.get(experimentName)) {
                kruizeObject = mainKruizeExperimentMAP.get(experimentName);
                kruizeObject.setValidation_data(new ValidationOutputData(true, null, null));
            } else {
                kruizeObject.setValidation_data(new ValidationOutputData(false, String.format("%s%s",
                        MISSING_EXPERIMENT_NAME, experimentName), HttpServletResponse.SC_BAD_REQUEST));
            }
        } catch (Exception e) {
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.LOAD_EXPERIMENT_FAILURE, e.getMessage()));
            kruizeObject.setValidation_data(new ValidationOutputData(false, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
        return kruizeObject;
    }

    public String validate() {

        String validationFailureMsg = "";
        // Check if experiment_name is provided
        if (experimentName == null || experimentName.isEmpty()) {
            validationFailureMsg += AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.EXPERIMENT_NAME_MANDATORY + ", ";
        }

        // Check if interval_end_time is provided
        if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
            validationFailureMsg += AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INTERVAL_END_TIME_MANDATORY;
        } else if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr)) {
            validationFailureMsg += String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, intervalEndTimeStr);
        }

        // Check if interval_start_time is provided
        // TODO: to be considered in future

        return validationFailureMsg;
    }

    public String validate_local() {            //TODO Instead of relying on the 'local=true' check everywhere, aim to avoid this complexity by introducing a higher-level abstraction in the code.

        String validationFailureMsg = "";
        // Check if experiment_name is provided
        if (experimentName == null || experimentName.isEmpty()) {
            validationFailureMsg += AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.EXPERIMENT_NAME_MANDATORY + ", ";
        }

        // Check if interval_end_time is provided
        if (intervalEndTimeStr != null) {
            if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr)) {
                validationFailureMsg += String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, intervalEndTimeStr);
            }
        }

        // Check if interval_start_time is provided
        // TODO: to be considered in future

        return validationFailureMsg;
    }

    /**
     * Prepares recommendations based on the input params received in the previous step.
     *
     * @param calCount The count of incoming requests.
     * @return The KruizeObject containing the prepared recommendations.
     */
    public KruizeObject prepareRecommendations(int calCount) {
        Map<String, KruizeObject> mainKruizeExperimentMAP = new ConcurrentHashMap<>();
        Map<String, Terms> terms = new HashMap<>();
        ValidationOutputData validationOutputData;
        Timestamp interval_start_time = null;
        if (intervalEndTimeStr != null) {       //TODO remove this check and avoid same if across this flow
            interval_end_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT,
                    intervalEndTimeStr);
            setInterval_end_time(interval_end_time);
        }
        KruizeObject kruizeObject = createKruizeObject();
        if (!kruizeObject.getValidation_data().isSuccess())
            return kruizeObject;
        setKruizeObject(kruizeObject);
        mainKruizeExperimentMAP.put(kruizeObject.getExperimentName(), kruizeObject);
        // continue to generate recommendation when kruizeObject is successfully created
        try {
            // set the default terms if the terms aren't provided by the user
            if (kruizeObject.getTerms() == null)
                KruizeObject.setDefaultTerms(terms, kruizeObject);
            // set the performance profile
            setPerformanceProfile(kruizeObject.getPerformanceProfile());
            // get the datasource
            // TODO: If no data source given use KruizeDeploymentInfo.monitoring_agent / default datasource
            String dataSource = kruizeObject.getDataSource();
            LOGGER.debug(String.format(KruizeConstants.APIMessages.EXPERIMENT_DATASOURCE, kruizeObject.getExperimentName(), dataSource));

            int maxDay = Terms.getMaxDays(terms);
            if (intervalEndTimeStr != null) {  //TODO remove this check and avoid same if across this flow
                interval_start_time = Timestamp.valueOf(Objects.requireNonNull(getInterval_end_time()).toLocalDateTime().minusDays(maxDay));
            }
            // update the KruizeObject to have the results data from the available datasource
            try {
                String errorMsg = getResults(mainKruizeExperimentMAP, kruizeObject, experimentName, interval_start_time, dataSource);
                if (!errorMsg.isEmpty()) {
                    throw new Exception(errorMsg);
                }
            } catch (Exception e) {
                LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_FAILED_COUNT, calCount));
                kruizeObject = new KruizeObject();
                kruizeObject.setValidation_data(new ValidationOutputData(false, e.getMessage(), HttpServletResponse.SC_BAD_REQUEST));
                return kruizeObject;
            }

            // generate recommendation
            try {
                generateRecommendations(kruizeObject);
                // store the recommendations in the DB
                validationOutputData = addRecommendationsToDB(mainKruizeExperimentMAP, kruizeObject);
                if (!validationOutputData.isSuccess()) {
                    LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_FAILED_COUNT, calCount));
                    validationOutputData = new ValidationOutputData(false, validationOutputData.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } else {
                    LOGGER.debug(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_SUCCESS_COUNT, calCount));
                }
                kruizeObject.setValidation_data(validationOutputData);
            } catch (Exception e) {
                LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_FAILED_COUNT, calCount));
                LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.RECOMMENDATION_ERROR,
                        experimentName, interval_start_time, interval_end_time));
                kruizeObject.setValidation_data(new ValidationOutputData(false, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            }
        } catch (Exception e) {
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.RECOMMENDATION_EXCEPTION,
                    experimentName, interval_end_time, e.getMessage()));
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_FAILED_COUNT, calCount));
            kruizeObject.setValidation_data(new ValidationOutputData(false, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
        return kruizeObject;
    }

    public void generateRecommendations(KruizeObject kruizeObject) {

        for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
            for (String containerName : k8sObject.getContainerDataMap().keySet()) {
                ContainerData containerData = k8sObject.getContainerDataMap().get(containerName);

                if (containerData.getResults() == null || containerData.getResults().isEmpty()) {
                    continue;
                }

                // generate recommendations based on each container
                generateRecommendationsBasedOnContainer(containerData, kruizeObject);
                // TODO: generate recommendations based on namespace, kubernetes_object name and type
                // todo The process of data validation and notification generation is currently tightly coupled and needs to be separated. By doing so, we can avoid additional iterations at kruizeNotificationCollectionRegistry.logNotification. This should be included as part of the code refactor.
                KruizeNotificationCollectionRegistry kruizeNotificationCollectionRegistry = new KruizeNotificationCollectionRegistry(kruizeObject.getExperimentName(), getInterval_end_time(), containerData.getContainer_name());
                kruizeNotificationCollectionRegistry.logNotification(containerData);

            }
        }
    }

    private void generateRecommendationsBasedOnContainer(ContainerData containerData, KruizeObject kruizeObject) {

        // Get the monitoringEndTime from ResultData's ContainerData. Should have only one element
        Timestamp monitoringEndTime = containerData.getResults().keySet().stream().max(Timestamp::compareTo).get();

        ContainerRecommendations containerRecommendations = containerData.getContainerRecommendations();
        // Just to make sure the container recommendations object is not empty
        if (null == containerRecommendations) {
            containerRecommendations = new ContainerRecommendations();
        }

        HashMap<Integer, RecommendationNotification> recommendationLevelNM = containerRecommendations.getNotificationMap();
        if (null == recommendationLevelNM) {
            recommendationLevelNM = new HashMap<>();
        }

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

        timestampRecommendation.setMonitoringEndTime(monitoringEndTime);

        // get the current config data
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>> currentConfig = getCurrentConfigData(containerData, monitoringEndTime,
                timestampRecommendation);
        timestampRecommendation.setCurrentConfig(currentConfig);

        // get recommendations based on terms
        boolean recommendationAvailable = generateRecommendationsBasedOnTerms(containerData, kruizeObject, monitoringEndTime, currentConfig, timestampRecommendation);

        RecommendationNotification recommendationsLevelNotifications;
        if (recommendationAvailable) {
            // put recommendations tagging to timestamp
            timestampBasedRecommendationMap.put(monitoringEndTime, timestampRecommendation);
            // set the Recommendations object level notifications
            recommendationsLevelNotifications = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_RECOMMENDATIONS_AVAILABLE);
        } else {
            recommendationsLevelNotifications = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
            timestampBasedRecommendationMap = new HashMap<>();
        }

        recommendationLevelNM.put(recommendationsLevelNotifications.getCode(), recommendationsLevelNotifications);
        containerRecommendations.setNotificationMap(recommendationLevelNM);
        // set the data object to map
        containerRecommendations.setData(timestampBasedRecommendationMap);


        // set the container recommendations in container object
        containerData.setContainerRecommendations(containerRecommendations);
    }

    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
            RecommendationConfigItem>> getCurrentConfigData(ContainerData containerData, Timestamp monitoringEndTime,
                                                            MappedRecommendationForTimestamp timestampRecommendation) {

        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
                RecommendationConfigItem>> currentConfig = new HashMap<>();

        ArrayList<RecommendationConstants.RecommendationNotification> notifications = new ArrayList<>();

        // Create Current Requests Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentRequestsMap = new HashMap<>();

        // Create Current Limits Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentLimitsMap = new HashMap<>();

        for (AnalyzerConstants.ResourceSetting resourceSetting : AnalyzerConstants.ResourceSetting.values()) {
            for (AnalyzerConstants.RecommendationItem recommendationItem : AnalyzerConstants.RecommendationItem.values()) {
                RecommendationConfigItem configItem = RecommendationUtils.getCurrentValue(containerData.getResults(),
                        monitoringEndTime,
                        resourceSetting,
                        recommendationItem,
                        notifications);

                if (null == configItem)
                    continue;
                if (null == configItem.getAmount()) {
                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_CPU_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_CPU_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                        experimentName, interval_end_time)));
                    } else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory))) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_MEMORY_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_MEMORY_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                        experimentName, interval_end_time)));
                    }
                    continue;
                }
                if (null == configItem.getFormat()) {
                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_CPU_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                        experimentName, interval_end_time)));
                    } else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory))) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                        experimentName, interval_end_time)));
                    }
                    continue;
                }
                if (configItem.getAmount() <= 0.0) {
                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                        experimentName, interval_end_time)));
                    } else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory))) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                        experimentName, interval_end_time)));
                    }
                    continue;
                }
                if (configItem.getFormat().isEmpty() || configItem.getFormat().isBlank()) {
                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                        experimentName, interval_end_time)));
                    } else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory))) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                        experimentName, interval_end_time)));
                    }
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
        return currentConfig;
    }

    private boolean generateRecommendationsBasedOnTerms(ContainerData containerData, KruizeObject kruizeObject,
                                                        Timestamp monitoringEndTime,
                                                        HashMap<AnalyzerConstants.ResourceSetting,
                                                                HashMap<AnalyzerConstants.RecommendationItem,
                                                                        RecommendationConfigItem>> currentConfig,
                                                        MappedRecommendationForTimestamp timestampRecommendation) {

        boolean recommendationAvailable = false;
        double measurementDuration = kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble();
        for (Map.Entry<String, Terms> termsEntry : kruizeObject.getTerms().entrySet()) {
            String recommendationTerm = termsEntry.getKey();
            Terms terms = termsEntry.getValue();
            LOGGER.debug(String.format(KruizeConstants.APIMessages.RECOMMENDATION_TERM, recommendationTerm));
            int duration = termsEntry.getValue().getDays();
            Timestamp monitoringStartTime = Terms.getMonitoringStartTime(monitoringEndTime, duration);
            LOGGER.debug(String.format(KruizeConstants.APIMessages.MONITORING_START_TIME, monitoringStartTime));

            TermRecommendations mappedRecommendationForTerm = new TermRecommendations();
            // Check if there is min data available for the term
            if (!Terms.checkIfMinDataAvailableForTerm(containerData, terms, monitoringEndTime, measurementDuration)) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                mappedRecommendationForTerm.addNotification(recommendationNotification);
            } else {
                ArrayList<RecommendationNotification> termLevelNotifications = new ArrayList<>();
                for (RecommendationModel model : getModels()) {
                    boolean isCostModel = false;
                    boolean isPerfModel = false;

                    if (model.getModelName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.ModelNames.COST)) {
                        isCostModel = true;
                    }
                    if (model.getModelName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.ModelNames.PERFORMANCE)) {
                        isPerfModel = true;
                    }

                    // Now generate a new recommendation for the new data corresponding to the monitoringEndTime
                    MappedRecommendationForModel mappedRecommendationForModel = generateRecommendationBasedOnModel(
                            monitoringStartTime,
                            model,
                            containerData,
                            monitoringEndTime,
                            kruizeObject.getRecommendation_settings(),
                            currentConfig,
                            termsEntry);

                    if (null == mappedRecommendationForModel) {
                        continue;
                    }

                    // Adding the term level recommendation availability after confirming the recommendation exists
                    RecommendationNotification rn = RecommendationNotification.getNotificationForTermAvailability(recommendationTerm);
                    if (null != rn) {
                        timestampRecommendation.addNotification(rn);
                    }

                    RecommendationNotification recommendationNotification = null;
                    if (isCostModel) {
                        // Setting it as at least one recommendation available
                        recommendationAvailable = true;
                        recommendationNotification = new RecommendationNotification(
                                RecommendationConstants.RecommendationNotification.INFO_COST_RECOMMENDATIONS_AVAILABLE
                        );
                    }

                    if (isPerfModel) {
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
                // generate plots when minimum data is available for the term
                if (KruizeDeploymentInfo.plots) {
                    if (null != monitoringStartTime) {
                        Timer.Sample timerBoxPlots = null;
                        String status = KruizeConstants.APIMessages.SUCCESS;   // TODO avoid this constant at multiple place
                        try {
                            timerBoxPlots = Timer.start(MetricsConfig.meterRegistry());
                            mappedRecommendationForTerm.setPlots(new PlotManager(containerData.getResults(), terms, monitoringStartTime, monitoringEndTime).generatePlots());
                        } catch (Exception e) {
                            status = String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.BOX_PLOTS_FAILURE, e.getMessage());
                        } finally {
                            if (timerBoxPlots != null) {
                                MetricsConfig.timerBoxPlots = MetricsConfig.timerBBoxPlots.tag(KruizeConstants.DataSourceConstants
                                        .DataSourceQueryJSONKeys.STATUS, status).register(MetricsConfig.meterRegistry());
                                timerBoxPlots.stop(MetricsConfig.timerBoxPlots);
                            }
                        }
                    }
                }
            }
            Terms.setDurationBasedOnTerm(containerData, mappedRecommendationForTerm, recommendationTerm);
            timestampRecommendation.setRecommendationForTermHashMap(recommendationTerm, mappedRecommendationForTerm);

        }
        return recommendationAvailable;

    }

    private MappedRecommendationForModel generateRecommendationBasedOnModel(Timestamp monitoringStartTime, RecommendationModel model, ContainerData containerData,
                                                                            Timestamp monitoringEndTime,
                                                                            RecommendationSettings recommendationSettings,
                                                                            HashMap<AnalyzerConstants.ResourceSetting,
                                                                                    HashMap<AnalyzerConstants.RecommendationItem,
                                                                                            RecommendationConfigItem>> currentConfigMap,
                                                                            Map.Entry<String, Terms> termEntry) {

        MappedRecommendationForModel mappedRecommendationForModel = new MappedRecommendationForModel();
        // Set CPU threshold to default
        double cpuThreshold = DEFAULT_CPU_THRESHOLD;
        // Set Memory threshold to default
        double memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
        if (null != recommendationSettings) {
            Double threshold = recommendationSettings.getThreshold();
            if (null == threshold) {
                LOGGER.info(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.THRESHOLD_NOT_SET,
                        DEFAULT_CPU_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
            } else if (threshold <= 0.0) {
                LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INVALID_THRESHOLD,
                        DEFAULT_CPU_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
            } else {
                cpuThreshold = threshold;
                memoryThreshold = threshold;
            }
        } else {
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.NULL_RECOMMENDATION_SETTINGS,
                    DEFAULT_CPU_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
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
            Map<Timestamp, IntervalResults> filteredResultsMap = containerData.getResults().entrySet().stream()
                    .filter((x -> ((x.getKey().compareTo(finalMonitoringStartTime) >= 0)
                            && (x.getKey().compareTo(monitoringEndTime) <= 0))))
                    .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

            // Set number of pods
            int numPods = getNumPods(filteredResultsMap);

            mappedRecommendationForModel.setPodsCount(numPods);

            // Pass Notification object to all callers to update the notifications required
            ArrayList<RecommendationNotification> notifications = new ArrayList<>();

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
            HashMap<String, RecommendationConfigItem> internalMapToPopulate = new HashMap<>();
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
                    termEntry,
                    mappedRecommendationForModel,
                    notifications,
                    internalMapToPopulate,
                    numPods,
                    cpuThreshold,
                    memoryThreshold
            );
        } else {
            RecommendationNotification notification = new RecommendationNotification(
                    RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
            mappedRecommendationForModel.addNotification(notification);
        }
        return mappedRecommendationForModel;
    }

    /**
     * This method handles validating the data and populating to the recommendation object
     * <p>
     * DO NOT EDIT THIS METHOD UNLESS THERE ARE ANY CHANGES TO BE ADDED IN VALIDATION OR POPULATION MECHANISM
     * EDITING THIS METHOD MIGHT LEAD TO UNEXPECTED OUTCOMES IN RECOMMENDATIONS, PLEASE PROCEED WITH CAUTION
     *
     * @param termEntry The entry containing a term key and its associated {@link Terms} object.
     * @param recommendationModel The model used to map recommendations.
     * @param notifications A list to which recommendation notifications will be added.
     * @param internalMapToPopulate The internal map to populate with recommendation configuration items.
     * @param numPods The number of pods to consider for the recommendation.
     * @param cpuThreshold The CPU usage threshold for the recommendation.
     * @param memoryThreshold The memory usage threshold for the recommendation.
     * @return {@code true} if the internal map was successfully populated; {@code false} otherwise.
     */
    private boolean populateRecommendation(Map.Entry<String, Terms> termEntry,
                                           MappedRecommendationForModel recommendationModel,
                                           ArrayList<RecommendationNotification> notifications,
                                           HashMap<String, RecommendationConfigItem> internalMapToPopulate,
                                           int numPods, double cpuThreshold, double memoryThreshold) {
        // Check for cpu & memory Thresholds (Duplicate check if the caller is generateRecommendations)
        String recommendationTerm = termEntry.getKey();
        double hours = termEntry.getValue().getDays() * KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY * KruizeConstants.TimeConv.
                NO_OF_MINUTES_PER_HOUR;
        if (cpuThreshold <= 0.0) {
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INVALID_CPU_THRESHOLD, DEFAULT_CPU_THRESHOLD));
            cpuThreshold = DEFAULT_CPU_THRESHOLD;
        }
        if (memoryThreshold <= 0.0) {
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INVALID_MEMORY_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
            memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
        }
        // Check for null
        if (null == recommendationTerm) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.NULL_RECOMMENDATION_TERM);
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
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INVALID_RECOMMENDATION_TERM, recommendationTerm));
            return false;
        }

        // Check if recommendation is null
        if (null == recommendationModel) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.NULL_RECOMMENDATIONS);
            return false;
        }

        // Check if notification is null (Do not check for empty as notifications might not have been populated)
        if (null == notifications) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.NULL_NOTIFICATIONS);
            return false;
        }

        // Check if the map is populated with at least one data point
        if (null == internalMapToPopulate || internalMapToPopulate.isEmpty()) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INTERNAL_MAP_EMPTY);
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
            LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.HOURS_CANNOT_BE_ZERO.concat(
                    String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName,
                            interval_end_time)));
            isSuccess = false;
        } else if (hours < 0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_HOURS_CANNOT_BE_NEGATIVE);
            notifications.add(recommendationNotification);
            LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.HOURS_CANNOT_BE_NEGATIVE.concat(
                    String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName,
                            interval_end_time)));
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
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
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
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
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
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
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
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
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
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
            } else if (null == currentCpuRequest.getFormat() || currentCpuRequest.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
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
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
            } else if (null == currentMemRequest.getFormat() || currentMemRequest.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
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
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
            } else if (null == currentCpuLimit.getFormat() || currentCpuLimit.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
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
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
            } else if (null == currentMemLimit.getFormat() || currentMemLimit.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, interval_end_time)));
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
            LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.NUM_PODS_CANNOT_BE_ZERO
                    .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                            experimentName, interval_end_time)));
            isSuccess = false;
        } else if (numPods < 0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_NUM_PODS_CANNOT_BE_NEGATIVE);
            engineNotifications.add(recommendationNotification);
            LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.NUM_PODS_CANNOT_BE_NEGATIVE
                    .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                            experimentName, interval_end_time)));
            isSuccess = false;
        } else {
            recommendationModel.setPodsCount(numPods);
        }

        // Check for thresholds
        if (isRecommendedCPURequestAvailable) {
            if (isCurrentCPURequestAvailable && currentCpuRequestValue > 0.0 && null != generatedCpuRequest) {
                double diffCpuRequestPercentage = CommonUtils.getPercentage(generatedCpuRequest, currentCpuRequestValue);
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
                double diffCPULimitPercentage = CommonUtils.getPercentage(generatedCpuLimit, currentCpuLimitValue);
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
                double diffMemRequestPercentage = CommonUtils.getPercentage(generatedMemRequest, currentMemRequestValue);
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
                double diffMemLimitPercentage = CommonUtils.getPercentage(generatedMemLimit, currentMemLimitValue);
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

        //Set all existing notifications
        for (RecommendationNotification recommendationNotification : notifications) {
            recommendationModel.addNotification(recommendationNotification);
        }

        // set the engine level notifications here
        for (RecommendationNotification recommendationNotification : engineNotifications) {
            recommendationModel.addNotification(recommendationNotification);
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
            recommendationModel.setConfig(config);
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
            recommendationModel.setVariation(variation);
        }

        return isSuccess;
    }

    private ValidationOutputData addRecommendationsToDB(Map<String, KruizeObject> mainKruizeExperimentMAP, KruizeObject kruizeObject) {
        ValidationOutputData validationOutputData;
        try {
            validationOutputData = new ExperimentDBService().addRecommendationToDB(mainKruizeExperimentMAP, kruizeObject, interval_end_time);
        } catch (Exception e) {
            LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.ADDING_RECOMMENDATIONS_TO_DB_FAILED
                    .concat(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME)
                    .concat(" : " + e.getMessage()));
            validationOutputData = new ValidationOutputData(false, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return validationOutputData;
    }

    /**
     * Retrieves results for the specified experiment and stores them in the main Kruize experiment map.
     *
     * @param mainKruizeExperimentMAP The map containing KruizeObject and experiment name.
     * @param kruizeObject            The KruizeObject representing the experiment.
     * @param experimentName          The name of the experiment.
     * @param intervalStartTime       The start time of the interval for fetching metrics.
     * @param dataSource              The data source used for monitoring.
     * @throws Exception if an error occurs during the process of fetching and storing results.
     */
    private String getResults(Map<String, KruizeObject> mainKruizeExperimentMAP, KruizeObject kruizeObject,
                              String experimentName, Timestamp intervalStartTime, String dataSource) throws Exception {
        String errorMsg = "";

        mainKruizeExperimentMAP.put(experimentName, kruizeObject);
        // get data from the DB in case of remote monitoring
        if (kruizeObject.getExperiment_usecase_type().isRemote_monitoring()) {
            try {
                boolean resultsAvailable = new ExperimentDBService().loadResultsFromDBByName(mainKruizeExperimentMAP, experimentName, intervalStartTime, interval_end_time);
                if (!resultsAvailable) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT);
                    errorMsg = String.format(AnalyzerErrorConstants.AutotuneObjectErrors.NO_METRICS_AVAILABLE,
                            dateFormat.format(intervalStartTime), dateFormat.format(interval_end_time));
                    LOGGER.error(errorMsg);
                    return errorMsg;
                }
            } catch (Exception e) {
                LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.FETCHING_RESULTS_FAILED, e.getMessage()));
            }
        } else if (kruizeObject.getExperiment_usecase_type().isLocal_monitoring()) {
            // get data from the provided datasource in case of local monitoring
            DataSourceInfo dataSourceInfo = new ExperimentDBService().loadDataSourceFromDBByName(dataSource);
            if (dataSourceInfo == null) {
                throw new DataSourceNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            // Fetch metrics from the Metric Profile based on the datasource
            fetchMetricsBasedOnProfileAndDatasource(kruizeObject, interval_end_time, intervalStartTime, dataSourceInfo);
        }
        return errorMsg;
    }

    /**
     * Fetches metrics based on the specified datasource for the given time interval.
     *
     * @param kruizeObject        The KruizeObject containing the experiment data.
     * @param interval_end_time   The end time of the interval for fetching metrics.
     * @param interval_start_time The start time of the interval for fetching metrics.
     * @param dataSourceInfo      The datasource object to fetch metrics from.
     * @throws Exception if an error occurs during the fetching process.
     *                                                                                                                                                                                                                                                                               TODO: Need to add right abstractions for this
     */
    public void fetchMetricsBasedOnDatasource(KruizeObject kruizeObject, Timestamp interval_end_time, Timestamp interval_start_time, DataSourceInfo dataSourceInfo) throws Exception {
        try {
            long interval_end_time_epoc = 0;
            long interval_start_time_epoc = 0;
            SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);

            // Get MetricsProfile name and list of promQL to fetch
            Map<AnalyzerConstants.MetricName, String> promQls = new HashMap<>();
            getPromQls(promQls);
            List<String> aggregationMethods = Arrays.asList(KruizeConstants.JSONKeys.SUM, KruizeConstants.JSONKeys.AVG,
                    KruizeConstants.JSONKeys.MAX, KruizeConstants.JSONKeys.MIN);
            Double measurementDurationMinutesInDouble = kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble();
            List<K8sObject> kubernetes_objects = kruizeObject.getKubernetes_objects();

            // Iterate over Kubernetes objects
            for (K8sObject k8sObject : kubernetes_objects) {
                String namespace = k8sObject.getNamespace();
                HashMap<String, ContainerData> containerDataMap = k8sObject.getContainerDataMap();
                // Iterate over containers
                for (Map.Entry<String, ContainerData> entry : containerDataMap.entrySet()) {
                    ContainerData containerData = entry.getValue();
                    String containerName = containerData.getContainer_name();
                    if (null == interval_end_time) {
                        LOGGER.info(KruizeConstants.APIMessages.CONTAINER_USAGE_INFO);
                        String dateMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATE_ENDPOINT_WITH_QUERY,
                                dataSourceInfo.getUrl(),
                                URLEncoder.encode(String.format(PromQLDataSourceQueries.MAX_DATE, containerName, namespace), CHARACTER_ENCODING)
                        );
                        LOGGER.info(dateMetricsUrl);
                        JSONObject genericJsonObject = new GenericRestApiClient(dateMetricsUrl).fetchMetricsJson(KruizeConstants.APIMessages.GET, "");
                        JsonObject jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
                        JsonArray resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);
                        // Process fetched metrics
                        if (null != resultArray && !resultArray.isEmpty()) {
                            resultArray = resultArray.get(0)
                                    .getAsJsonObject().getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.VALUE);
                            long epochTime = resultArray.get(0).getAsLong();
                            String timestamp = sdf.format(new Date(epochTime * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC));
                            Date date = sdf.parse(timestamp);
                            Timestamp dateTS = new Timestamp(date.getTime());
                            interval_end_time_epoc = dateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                                    - ((long) dateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
                            int maxDay = Terms.getMaxDays(kruizeObject.getTerms());
                            LOGGER.info(KruizeConstants.APIMessages.MAX_DAY, maxDay);
                            Timestamp startDateTS = Timestamp.valueOf(Objects.requireNonNull(dateTS).toLocalDateTime().minusDays(maxDay));
                            interval_start_time_epoc = startDateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                                    - ((long) startDateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);
                        }
                    } else {
                        // Convert timestamps to epoch time
                        interval_end_time_epoc = interval_end_time.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                                - ((long) interval_end_time.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
                        interval_start_time_epoc = interval_start_time.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                                - ((long) interval_start_time.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);
                    }
                    HashMap<Timestamp, IntervalResults> containerDataResults = new HashMap<>();
                    IntervalResults intervalResults;
                    HashMap<AnalyzerConstants.MetricName, MetricResults> resMap;
                    MetricResults metricResults;
                    MetricAggregationInfoResults metricAggregationInfoResults;
                    // Iterate over metrics and aggregation methods
                    for (Map.Entry<AnalyzerConstants.MetricName, String> metricEntry : promQls.entrySet()) {
                        for (String methodName : aggregationMethods) {
                            String promQL = null;
                            String format = null;
                            // Determine promQL and format based on metric type
                            if (metricEntry.getKey() == AnalyzerConstants.MetricName.cpuUsage) {
                                String secondMethodName = methodName;
                                if (secondMethodName.equals(KruizeConstants.JSONKeys.SUM))
                                    secondMethodName = KruizeConstants.JSONKeys.AVG;
                                promQL = String.format(metricEntry.getValue(), methodName, secondMethodName, namespace, containerName, measurementDurationMinutesInDouble.intValue());
                                format = KruizeConstants.JSONKeys.CORES;
                            } else if (metricEntry.getKey() == AnalyzerConstants.MetricName.cpuThrottle) {
                                promQL = String.format(metricEntry.getValue(), methodName, namespace, containerName, measurementDurationMinutesInDouble.intValue());
                                format = KruizeConstants.JSONKeys.CORES;
                            } else if (metricEntry.getKey() == AnalyzerConstants.MetricName.cpuLimit || metricEntry.getKey() == AnalyzerConstants.MetricName.cpuRequest) {
                                promQL = String.format(metricEntry.getValue(), methodName, namespace, containerName);
                                format = KruizeConstants.JSONKeys.CORES;
                            } else if (metricEntry.getKey() == AnalyzerConstants.MetricName.memoryUsage || metricEntry.getKey() == AnalyzerConstants.MetricName.memoryRSS) {
                                String secondMethodName = methodName;
                                if (secondMethodName.equals(KruizeConstants.JSONKeys.SUM))
                                    secondMethodName = KruizeConstants.JSONKeys.AVG;
                                promQL = String.format(metricEntry.getValue(), methodName, secondMethodName, namespace, containerName, measurementDurationMinutesInDouble.intValue());
                                format = KruizeConstants.JSONKeys.BYTES;
                            } else if (metricEntry.getKey() == AnalyzerConstants.MetricName.memoryLimit || metricEntry.getKey() == AnalyzerConstants.MetricName.memoryRequest) {
                                promQL = String.format(metricEntry.getValue(), methodName, namespace, containerName);
                                format = KruizeConstants.JSONKeys.BYTES;
                            }
                            // If promQL is determined, fetch metrics from the datasource
                            if (promQL != null) {
                                LOGGER.info(promQL);
                                String podMetricsUrl;
                                try {
                                    podMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATASOURCE_ENDPOINT_WITH_QUERY,
                                            dataSourceInfo.getUrl(),
                                            URLEncoder.encode(promQL, CHARACTER_ENCODING),
                                            interval_start_time_epoc,
                                            interval_end_time_epoc,
                                            measurementDurationMinutesInDouble.intValue() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
//                                    LOGGER.info(podMetricsUrl);
                                    JSONObject genericJsonObject = new GenericRestApiClient(podMetricsUrl).fetchMetricsJson(KruizeConstants.APIMessages.GET, "");
                                    JsonObject jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
                                    JsonArray resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);
                                    // Process fetched metrics
                                    if (null != resultArray && !resultArray.isEmpty()) {
                                        resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(
                                                        KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT).get(0)
                                                .getAsJsonObject().getAsJsonArray(KruizeConstants.DataSourceConstants
                                                        .DataSourceQueryJSONKeys.VALUES);
                                        sdf.setTimeZone(TimeZone.getTimeZone(KruizeConstants.TimeUnitsExt.TimeZones.UTC));

                                        // Iterate over fetched metrics
                                        Timestamp sTime = new Timestamp(interval_start_time_epoc);
                                        for (JsonElement element : resultArray) {
                                            JsonArray valueArray = element.getAsJsonArray();
                                            long epochTime = valueArray.get(0).getAsLong();
                                            double value = valueArray.get(1).getAsDouble();
                                            String timestamp = sdf.format(new Date(epochTime * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC));
                                            Date date = sdf.parse(timestamp);
                                            Timestamp eTime = new Timestamp(date.getTime());

                                            // Prepare interval results
                                            if (containerDataResults.containsKey(eTime)) {
                                                intervalResults = containerDataResults.get(eTime);
                                                resMap = intervalResults.getMetricResultsMap();
                                            } else {
                                                intervalResults = new IntervalResults();
                                                resMap = new HashMap<>();
                                            }
                                            if (resMap.containsKey(metricEntry.getKey())) {
                                                metricResults = resMap.get(metricEntry.getKey());
                                                metricAggregationInfoResults = metricResults.getAggregationInfoResult();
                                            } else {
                                                metricResults = new MetricResults();
                                                metricAggregationInfoResults = new MetricAggregationInfoResults();
                                            }
                                            Method method = MetricAggregationInfoResults.class.getDeclaredMethod(KruizeConstants.APIMessages.SET + methodName.substring(0, 1).toUpperCase() + methodName.substring(1), Double.class);
                                            method.invoke(metricAggregationInfoResults, value);
                                            metricAggregationInfoResults.setFormat(format);
                                            metricResults.setAggregationInfoResult(metricAggregationInfoResults);
                                            metricResults.setName(String.valueOf(metricEntry.getKey()));
                                            metricResults.setFormat(format);
                                            resMap.put(metricEntry.getKey(), metricResults);
                                            intervalResults.setMetricResultsMap(resMap);
                                            intervalResults.setIntervalStartTime(sTime);  //Todo this will change
                                            intervalResults.setIntervalEndTime(eTime);
                                            intervalResults.setDurationInMinutes((double) ((eTime.getTime() - sTime.getTime())
                                                    / ((long) KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE
                                                    * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC)));
                                            containerDataResults.put(eTime, intervalResults);
                                            sTime = eTime;
                                        }
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                    containerData.setResults(containerDataResults);
                    if (!containerDataResults.isEmpty())
                        setInterval_end_time(Collections.max(containerDataResults.keySet()));    //TODO Temp fix invalid date is set if experiment having two container with different last seen date
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.METRIC_EXCEPTION + e.getMessage());
        }
    }

    /**
     * Fetches metrics based on the specified datasource using queries from the metricProfile for the given time interval.
     *
     * @param kruizeObject
     * @param interval_end_time
     * @param interval_start_time
     * @param dataSourceInfo
     * @throws Exception
     */
    public void fetchMetricsBasedOnProfileAndDatasource(KruizeObject kruizeObject, Timestamp interval_end_time, Timestamp interval_start_time, DataSourceInfo dataSourceInfo) throws Exception {
        try {
            long interval_end_time_epoc = 0;
            long interval_start_time_epoc = 0;
            SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);

            String metricProfileName = kruizeObject.getPerformanceProfile();
            PerformanceProfile metricProfile = MetricProfileCollection.getInstance().getMetricProfileCollection().get(metricProfileName);
            if (null == metricProfile) {
                LOGGER.error("MetricProfile does not exist or is not valid: {}", metricProfileName);
                return;
            }

            Double measurementDurationMinutesInDouble = kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble();
            List<K8sObject> kubernetes_objects = kruizeObject.getKubernetes_objects();

            // Iterate over Kubernetes objects
            for (K8sObject k8sObject : kubernetes_objects) {
                String namespace = k8sObject.getNamespace();
                HashMap<String, ContainerData> containerDataMap = k8sObject.getContainerDataMap();
                // Iterate over containers
                for (Map.Entry<String, ContainerData> entry : containerDataMap.entrySet()) {
                    ContainerData containerData = entry.getValue();
                    String containerName = containerData.getContainer_name();
                    if (null == interval_end_time) {
                        LOGGER.info(KruizeConstants.APIMessages.CONTAINER_USAGE_INFO);
                        String dateMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATE_ENDPOINT_WITH_QUERY,
                                dataSourceInfo.getUrl(),
                                URLEncoder.encode(String.format(PromQLDataSourceQueries.MAX_DATE, containerName, namespace), CHARACTER_ENCODING)
                        );
                        LOGGER.info(dateMetricsUrl);
                        JSONObject genericJsonObject = new GenericRestApiClient(dateMetricsUrl).fetchMetricsJson(KruizeConstants.APIMessages.GET, "");
                        JsonObject jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
                        JsonArray resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);
                        // Process fetched metrics
                        if (null != resultArray && !resultArray.isEmpty()) {
                            resultArray = resultArray.get(0)
                                    .getAsJsonObject().getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.VALUE);
                            long epochTime = resultArray.get(0).getAsLong();
                            String timestamp = sdf.format(new Date(epochTime * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC));
                            Date date = sdf.parse(timestamp);
                            Timestamp dateTS = new Timestamp(date.getTime());
                            interval_end_time_epoc = dateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                                    - ((long) dateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
                            int maxDay = Terms.getMaxDays(kruizeObject.getTerms());
                            LOGGER.info(KruizeConstants.APIMessages.MAX_DAY, maxDay);
                            Timestamp startDateTS = Timestamp.valueOf(Objects.requireNonNull(dateTS).toLocalDateTime().minusDays(maxDay));
                            interval_start_time_epoc = startDateTS.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                                    - ((long) startDateTS.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);
                        }
                    } else {
                        // Convert timestamps to epoch time
                        interval_end_time_epoc = interval_end_time.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                                - ((long) interval_end_time.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
                        interval_start_time_epoc = interval_start_time.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                                - ((long) interval_start_time.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);
                    }
                    HashMap<Timestamp, IntervalResults> containerDataResults = new HashMap<>();
                    IntervalResults intervalResults;
                    HashMap<AnalyzerConstants.MetricName, MetricResults> resMap;
                    HashMap<String, MetricResults> resultMap;
                    MetricResults metricResults;
                    MetricAggregationInfoResults metricAggregationInfoResults;

                    List<Metric> metricList = metricProfile.getSloInfo().getFunctionVariables();

                    // Iterate over metrics and aggregation functions
                    for (Metric metricEntry : metricList) {
                        HashMap<String, AggregationFunctions> aggregationFunctions = metricEntry.getAggregationFunctionsMap();
                        for (Map.Entry<String, AggregationFunctions> aggregationFunctionsEntry: aggregationFunctions.entrySet()) {
                            String metricQuery = aggregationFunctionsEntry.getValue().getQuery();
                            String promQL = metricQuery;
                            String format = null;

                            // Determine format based on metric type - Todo move this metric profile
                            List<String> cpuFunction = Arrays.asList(AnalyzerConstants.MetricName.cpuUsage.toString(), AnalyzerConstants.MetricName.cpuThrottle.toString(), AnalyzerConstants.MetricName.cpuLimit.toString(), AnalyzerConstants.MetricName.cpuRequest.toString());
                            List<String> memFunction = Arrays.asList(AnalyzerConstants.MetricName.memoryLimit.toString(), AnalyzerConstants.MetricName.memoryRequest.toString(), AnalyzerConstants.MetricName.memoryRSS.toString(), AnalyzerConstants.MetricName.memoryUsage.toString());
                            if (cpuFunction.contains(metricEntry.getName())) {
                                format = KruizeConstants.JSONKeys.CORES;
                            } else if (memFunction.contains(metricEntry.getName())) {
                                format = KruizeConstants.JSONKeys.BYTES;
                            }

                            promQL = promQL
                                    .replace(AnalyzerConstants.NAMESPACE_VARIABLE, namespace)
                                    .replace(AnalyzerConstants.CONTAINER_VARIABLE, containerName)
                                    .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDurationMinutesInDouble.intValue()));

                            // If promQL is determined, fetch metrics from the datasource
                            if (promQL != null) {
                                LOGGER.info(promQL);
                                String podMetricsUrl;
                                try {
                                    podMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATASOURCE_ENDPOINT_WITH_QUERY,
                                            dataSourceInfo.getUrl(),
                                            URLEncoder.encode(promQL, CHARACTER_ENCODING),
                                            interval_start_time_epoc,
                                            interval_end_time_epoc,
                                            measurementDurationMinutesInDouble.intValue() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
                                    JSONObject genericJsonObject = new GenericRestApiClient(podMetricsUrl).fetchMetricsJson(KruizeConstants.APIMessages.GET, "");
                                    JsonObject jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
                                    JsonArray resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);
                                    // Process fetched metrics
                                    if (null != resultArray && !resultArray.isEmpty()) {
                                        resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(
                                                        KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT).get(0)
                                                .getAsJsonObject().getAsJsonArray(KruizeConstants.DataSourceConstants
                                                        .DataSourceQueryJSONKeys.VALUES);
                                        sdf.setTimeZone(TimeZone.getTimeZone(KruizeConstants.TimeUnitsExt.TimeZones.UTC));

                                        // Iterate over fetched metrics
                                        Timestamp sTime = new Timestamp(interval_start_time_epoc);
                                        for (JsonElement element : resultArray) {
                                            JsonArray valueArray = element.getAsJsonArray();
                                            long epochTime = valueArray.get(0).getAsLong();
                                            double value = valueArray.get(1).getAsDouble();
                                            String timestamp = sdf.format(new Date(epochTime * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC));
                                            Date date = sdf.parse(timestamp);
                                            Timestamp eTime = new Timestamp(date.getTime());

                                            // Prepare interval results
                                            if (containerDataResults.containsKey(eTime)) {
                                                intervalResults = containerDataResults.get(eTime);
                                                resMap = intervalResults.getMetricResultsMap();
                                            } else {
                                                intervalResults = new IntervalResults();
                                                resMap = new HashMap<>();
                                            }
                                            AnalyzerConstants.MetricName metricName = AnalyzerConstants.MetricName.valueOf(metricEntry.getName());
                                            if (resMap.containsKey(metricName)) {
                                                metricResults = resMap.get(metricName);
                                                metricAggregationInfoResults = metricResults.getAggregationInfoResult();
                                            } else {
                                                metricResults = new MetricResults();
                                                metricAggregationInfoResults = new MetricAggregationInfoResults();
                                            }

                                            Method method = MetricAggregationInfoResults.class.getDeclaredMethod(KruizeConstants.APIMessages.SET + aggregationFunctionsEntry.getKey().substring(0, 1).toUpperCase() + aggregationFunctionsEntry.getKey().substring(1), Double.class);
                                            method.invoke(metricAggregationInfoResults, value);
                                            metricAggregationInfoResults.setFormat(format);
                                            metricResults.setAggregationInfoResult(metricAggregationInfoResults);
                                            metricResults.setName(metricEntry.getName());
                                            metricResults.setFormat(format);
                                            resMap.put(metricName, metricResults);
                                            intervalResults.setMetricResultsMap(resMap);
                                            intervalResults.setIntervalStartTime(sTime);  //Todo this will change
                                            intervalResults.setIntervalEndTime(eTime);
                                            intervalResults.setDurationInMinutes((double) ((eTime.getTime() - sTime.getTime())
                                                    / ((long) KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE
                                                    * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC)));
                                            containerDataResults.put(eTime, intervalResults);
                                            sTime = eTime;
                                        }
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }

                    containerData.setResults(containerDataResults);
                    if (!containerDataResults.isEmpty())
                        setInterval_end_time(Collections.max(containerDataResults.keySet()));    //TODO Temp fix invalid date is set if experiment having two container with different last seen date

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.METRIC_EXCEPTION + e.getMessage());
        }
    }
}
