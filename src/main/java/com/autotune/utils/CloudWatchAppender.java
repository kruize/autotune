package com.autotune.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static com.autotune.operator.KruizeDeploymentInfo.*;

public class CloudWatchAppender extends AbstractAppender {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudWatchAppender.class);
    private final String logGroupName;
    private final String logStreamName;
    private final CloudWatchLogsAsyncClient cloudWatchLogsClient;
    private String sequenceToken = null;

    public CloudWatchAppender(String name, Filter filter, Layout<?> layout, String logGroupName, String logStreamName, String region, String awsAccessKeyId, String awsSecretKey) {
        super(name, filter, layout, false, null);
        this.logGroupName = logGroupName;
        this.logStreamName = logStreamName;
        this.cloudWatchLogsClient = CloudWatchLogsAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKeyId, awsSecretKey)))
                .region(Region.of(region))
                .build();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void append(LogEvent event) {
        String message = getLayout().toSerializable(event).toString();
        List<InputLogEvent> logEvents = new ArrayList<>();
        logEvents.add(InputLogEvent.builder()
                .timestamp(event.getTimeMillis())
                .message(message)
                .build());

        PutLogEventsRequest request = PutLogEventsRequest.builder()
                .logGroupName(logGroupName)
                .logStreamName(logStreamName)
                .logEvents(logEvents)
                .sequenceToken(sequenceToken)
                .build();

        CompletableFuture<PutLogEventsResponse> futureResponse = cloudWatchLogsClient.putLogEvents(request);
        futureResponse.whenComplete((response, error) -> {
            if (error != null) {
                error.printStackTrace();
            } else {
                sequenceToken = response.nextSequenceToken();
            }
        });
    }

    public static void configureLoggerForCloudWatchLog() {
        if (cloudwatch_logs_access_key_id != null && !cloudwatch_logs_access_key_id.isEmpty() && cloudwatch_logs_secret_access_key != null && !cloudwatch_logs_secret_access_key.isEmpty() && cloudwatch_logs_region != null && !cloudwatch_logs_region.isEmpty()) {
            try {
                // Define default values for attributes if they are empty or null
                String cw_logs_log_group = cloudwatch_logs_log_group == null || cloudwatch_logs_log_group.isEmpty() ? "kruize-logs" : cloudwatch_logs_log_group;
                String cw_logs_log_stream = cloudwatch_logs_log_stream == null || cloudwatch_logs_log_stream.isEmpty() ? "kruize-stream" : cloudwatch_logs_log_stream;
                String cw_logs_log_level = cloudwatch_logs_log_level == null || cloudwatch_logs_log_level.isEmpty() ? "INFO" : cloudwatch_logs_log_level;
                String cw_logs_log_level_uc = cw_logs_log_level.toUpperCase();


                CloudWatchLogsClient logsClient = CloudWatchLogsClient.builder()
                        .region(Region.of(cloudwatch_logs_region))
                        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(cloudwatch_logs_access_key_id, cloudwatch_logs_secret_access_key)))
                        .build();

                if (!logGroupExists(logsClient, cw_logs_log_group)) {
                    createLogGroup(logsClient, cw_logs_log_group);
                }

                if (!logStreamExists(logsClient, cw_logs_log_group, cw_logs_log_stream)) {
                    createLogStream(logsClient, cw_logs_log_group, cw_logs_log_stream);
                }

                LoggerContext context = (LoggerContext) LogManager.getContext(false);
                Configuration config = context.getConfiguration();

                Level level = Level.getLevel(cw_logs_log_level_uc);
                Filter filter = new LogFilter(level);
                Layout<?> layout = PatternLayout.newBuilder().withPattern(KruizeConstants.Patterns.CLOUDWATCH_LOG_PATTERN).build();
                CloudWatchAppender appender = new CloudWatchAppender("cloudwatchRootAppender", filter, layout, cw_logs_log_group, cw_logs_log_stream, cloudwatch_logs_region,cloudwatch_logs_access_key_id,cloudwatch_logs_secret_access_key);

                appender.start();
                config.addAppender(appender);
                // Adding CloudWatch Appender to "com.autotune" logger
                LoggerConfig loggerConfig = config.getLoggerConfig("com.autotune");
                loggerConfig.addAppender(appender, level, filter);
                context.updateLoggers(config);
                LOGGER.debug("Enabled CloudWatch logs");

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        } else {
            LOGGER.info("AWS access details are not provided. Skipping sending logs to CloudWatch.");
        }
    }
    private static boolean logGroupExists(CloudWatchLogsClient logsClient, String logGroupName) {
        DescribeLogGroupsRequest request = DescribeLogGroupsRequest.builder()
                .logGroupNamePrefix(logGroupName)
                .build();
        DescribeLogGroupsResponse response = logsClient.describeLogGroups(request);
        List<LogGroup> logGroups = response.logGroups();
        return logGroups.stream().anyMatch(group -> group.logGroupName().equals(logGroupName));
    }

    private static boolean logStreamExists(CloudWatchLogsClient logsClient, String logGroupName, String logStreamName) {
        DescribeLogStreamsRequest request = DescribeLogStreamsRequest.builder()
                .logGroupName(logGroupName)
                .logStreamNamePrefix(logStreamName)
                .build();
        DescribeLogStreamsResponse response = logsClient.describeLogStreams(request);
        List<LogStream> logStreams = response.logStreams();
        return logStreams.stream().anyMatch(stream -> stream.logStreamName().equals(logStreamName));
    }

    private static void createLogGroup(CloudWatchLogsClient logsClient, String logGroupName) {
        CreateLogGroupRequest request = CreateLogGroupRequest.builder()
                .logGroupName(logGroupName)
                .build();
        logsClient.createLogGroup(request);
        LOGGER.info("Created log group: {}", logGroupName);
    }

    private static void createLogStream(CloudWatchLogsClient logsClient, String logGroupName, String logStreamName) {
        CreateLogStreamRequest request = CreateLogStreamRequest.builder()
                .logGroupName(logGroupName)
                .logStreamName(logStreamName)
                .build();
        logsClient.createLogStream(request);
        LOGGER.info("Created log stream: {} in log group: {}", logStreamName, logGroupName);
    }


    private static class LogFilter extends AbstractFilter {
        private final Level level;

        protected LogFilter(Level level) {
            super(Result.ACCEPT, Result.DENY);
            this.level = level;
        }

        @Override
        public Result filter(LogEvent event) {
            return event.getLevel().isMoreSpecificThan(level) ? onMatch : onMismatch;
        }

    }
}
