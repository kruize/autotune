package com.autotune.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.util.List;

import static com.autotune.operator.KruizeDeploymentInfo.*;

public class CloudWatchLogSender {
    private static final Logger LOGGER = LogManager.getLogger(CloudWatchLogSender.class);

    private CloudWatchLogSender() {
    }
    public static void configureCloudWatchLog() {

        // Check if access details are provided
        if (cloudwatch_logs_access_key_id != null && !cloudwatch_logs_access_key_id.isEmpty() && cloudwatch_logs_secret_access_key != null && !cloudwatch_logs_secret_access_key.isEmpty() && cloudwatch_logs_region != null && !cloudwatch_logs_region.isEmpty()) {
            try {
                // Define default values for attributes if they are empty or null
                String cw_logs_log_group = cloudwatch_logs_log_group == null || cloudwatch_logs_log_group.isEmpty() ? "kruize-logs" : cloudwatch_logs_log_group;
                String cw_logs_log_stream = cloudwatch_logs_log_stream == null || cloudwatch_logs_log_stream.isEmpty() ? "kruize-stream" : cloudwatch_logs_log_stream;
                String cw_logs_log_level = cloudwatch_logs_log_level == null || cloudwatch_logs_log_level.isEmpty() ? "INFO" : cloudwatch_logs_log_level;

                // Create a CloudWatchLogsClient with provided credentials and region
                CloudWatchLogsClient logsClient = CloudWatchLogsClient.builder()
                        .region(Region.of(cloudwatch_logs_region))
                        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(cloudwatch_logs_access_key_id, cloudwatch_logs_secret_access_key)))
                        .build();

                // Check if log group exists, if not, create it
                if (!logGroupExists(logsClient, cw_logs_log_group)) {
                    createLogGroup(logsClient, cw_logs_log_group);
                }

                // Check if log stream exists, if not, create it
                if (!logStreamExists(logsClient, cw_logs_log_group, cw_logs_log_stream)) {
                    createLogStream(logsClient, cw_logs_log_group, cw_logs_log_stream);
                }

                // Create a configuration builder
                ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
                LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

                // Define the console appender
                AppenderComponentBuilder consoleAppenderBuilder = builder.newAppender("consoleLogger", "Console")
                        .addAttribute("target", "SYSTEM_OUT")
                        .add(builder.newLayout("PatternLayout")
                                .addAttribute("pattern", "%d{yyyy-MM-ddHH:mm:ss.SSS} %level [%t][%F(%L)]-%msg%n"));
                builder.add(consoleAppenderBuilder);

                // Add a custom appender
                AppenderComponentBuilder cloudWatchAppenderBuilder = builder.newAppender("CloudWatchAppender", "CustomAppender")
                        .addAttribute("logGroup", cw_logs_log_group)
                        .addAttribute("logStream", cw_logs_log_stream)
                        .addAttribute("region", cloudwatch_logs_region)
                        .addAttribute("awsAccessKeyId", cloudwatch_logs_access_key_id)
                        .addAttribute("awsSecretKey", cloudwatch_logs_secret_access_key)
                        .addAttribute("logLevel", cw_logs_log_level)
                        .add(builder.newLayout("PatternLayout")
                                .addAttribute("pattern", "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"));;
                builder.add(cloudWatchAppenderBuilder);
                // Configure the root logger to use both console appender and the new CloudWatch appender
                RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.INFO)
                        .add(builder.newAppenderRef("consoleLogger"))
                        .add(builder.newAppenderRef("CloudWatchAppender"));
                builder.add(rootLogger);
                Configuration config = builder.build();
                // Update the current context with the new configuration
                ctx.updateLoggers(config);
                for (Appender appender : config.getAppenders().values()) {
                    appender.start();
                }
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
}