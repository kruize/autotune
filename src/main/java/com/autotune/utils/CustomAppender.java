package com.autotune.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Plugin(name = "CustomAppender", category = "Core", elementType = Appender.ELEMENT_TYPE)
public class CustomAppender extends AbstractAppender {

    private final String awsAccessKeyId;
    private final String logGroup;
    private final String logStream;
    private final String awsSecretKey;
    private final String region;

    private String sequenceToken = null;
    private final CloudWatchLogsAsyncClient logsClient;
    private final Filter filter;

    protected CustomAppender(String name, Filter filter, Layout layout, String awsAccessKeyId, String logGroup, String logStream, String awsSecretKey, String region) {
        super(name,filter, layout, false, null);
        this.awsAccessKeyId = awsAccessKeyId;
        this.logGroup = logGroup;
        this.logStream = logStream;
        this.awsSecretKey = awsSecretKey;
        this.region = region;
        this.filter = filter;
        this.logsClient = CloudWatchLogsAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKeyId, awsSecretKey)))
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
                .logGroupName(logGroup)
                .logStreamName(logStream)
                .logEvents(logEvents)
                .sequenceToken(sequenceToken)
                .build();

        CompletableFuture<PutLogEventsResponse> futureResponse = logsClient.putLogEvents(request);
        futureResponse.whenComplete((response, error) -> {
            if (error != null) {
                error.printStackTrace();
            } else {
                sequenceToken = response.nextSequenceToken();
            }
        });
    }

    @PluginFactory
    public static CustomAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("logGroup") String logGroup,
            @PluginAttribute("logStream") String logStream,
            @PluginAttribute("awsAccessKeyId") String awsAccessKeyId,
            @PluginAttribute("awsSecretKey") String awsSecretKey,
            @PluginAttribute("region") String region,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginAttribute("logLevel") String logLevel) {
        Level level = Level.getLevel(logLevel);
        Filter filter = new LogFilter(level);
        return new CustomAppender(name, filter, layout, awsAccessKeyId, logGroup, logStream, awsSecretKey, region);
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