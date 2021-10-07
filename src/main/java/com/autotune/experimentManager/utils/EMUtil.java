package com.autotune.experimentManager.utils;

import com.autotune.experimentManager.exceptions.EMInvalidTimeDuarationException;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EMUtil {
    public enum EMExpStages {
        INIT(0, 1, false, null),
        CREATE_CONFIG(1, 1, false, EMConstants.TransitionClasses.CREATE_CONFIG),
        DEPLOY_CONFIG(2, 1, false, EMConstants.TransitionClasses.DEPLOY_CONFIG),
        INITIATE_TRAIL_RUN_PHASE(3, 1, false, EMConstants.TransitionClasses.INITIATE_TRAIL_RUN_PHASE),
        INITIAL_LOAD_CHECK(3, 2, false, EMConstants.TransitionClasses.INITIAL_LOAD_CHECK),
        LOAD_CONSISTENCY_CHECK(3, 3, true, EMConstants.TransitionClasses.LOAD_CONSISTENCY_CHECK),
        INITIATE_METRICS_COLLECTION_PHASE(4, 1, false, EMConstants.TransitionClasses.INITIATE_METRICS_COLLECTION_PHASE),
        COLLECT_METRICS(4, 1, true, EMConstants.TransitionClasses.COLLECT_METRICS),
        CREATE_RESULT_DATA(5, 1, false, EMConstants.TransitionClasses.CREATE_RESULT_DATA),
        SEND_RESULT_DATA(5, 2, false, EMConstants.TransitionClasses.SEND_RESULT_DATA),
        CLEAN_OR_ROLLBACK_DEPLOYMENT(6, 1, true, EMConstants.TransitionClasses.CLEAN_OR_ROLLBACK_DEPLOYMENT),
        EXIT(7, 1, false, null)
        ;

        private int stage;
        private int intermediate_stage;
        private boolean isScheduled;
        private String className;
        private static final EMExpStages values[] = values();

        private EMExpStages(final int stage, final int intermediate_stage, final boolean isScheduled, final String className) {
            this.stage = stage;
            this.intermediate_stage = intermediate_stage;
            this.isScheduled = isScheduled;
            this.className = className;
        }

        public int getStage() {
            return stage;
        }

        public int getIntermediate_stage() {
            return intermediate_stage;
        }

        public boolean isScheduled() {
            return isScheduled;
        }

        public String getClassName() {
            return className;
        }

        public static EMExpStages get(int ordinal) { return values[ordinal]; }

        public static int getSize() {
            return values().length;
        }

    }

    public enum EMExpStatus {
        CREATED,
        WAIT,
        IN_PROGRESS,
        WAITING_FOR_LOAD,
        APPLYING_LOAD,
        COLLECTING_METRICS,
        COMPLETED
    }

    public static String createUUID() {
        return UUID.randomUUID().toString();
    }

    public static String formatNSDKey(String namespace, String deploymentName) {
        return (new StringBuilder())
                .append(namespace)
                .append(":")
                .append(deploymentName)
                .toString();
    }

    public static int convertMinsToSeconds(int minutes) {
        return minutes * EMConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE;
    }

    public static int convertHoursToMinutes(int hours) {
        return hours * EMConstants.TimeConv.NO_OF_MINUTES_PER_HOUR;
    }

    public static int convertHoursToSeconds(int hours) {
        return convertMinsToSeconds(convertHoursToMinutes(hours));
    }

    public static TimeUnit extractTimeUnit(String durationString) throws EMInvalidTimeDuarationException {
        durationString = durationString.replaceAll("\\s", "").toLowerCase();
        Pattern pattern = Pattern.compile("([0-9]+)([a-z]+)");
        Matcher match = pattern.matcher(durationString);
        if (!match.find()) {
            throw new EMInvalidTimeDuarationException();
        }
        String extractedTimeUnit = match.group(2);
        if (    extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.SECOND_LC_SINGULAR)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.SECOND_LC_PLURAL)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.SECOND_SHORT_LC_SINGULAR)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.SECOND_SHORT_LC_PLURAL)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.SECOND_SINGLE_LC)
        ) {
            return TimeUnit.SECONDS;
        } else if(
                extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.MINUTE_LC_SINGULAR)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.MINUTE_LC_PLURAL)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.MINUTE_SHORT_LC_SINGULAR)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.MINUTE_SHORT_LC_PLURAL)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.MINUTE_SINGLE_LC)
        ) {
            return TimeUnit.MINUTES;
        } else if(
                extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.HOUR_LC_SINGULAR)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.HOUR_LC_PLURAL)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.HOUR_SHORT_LC_SINGULAR)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.HOUR_SHORT_LC_PLURAL)
                || extractedTimeUnit.equalsIgnoreCase(EMConstants.TimeUnitsExt.HOUR_SINGLE_LC)
        ) {
            return TimeUnit.HOURS;
        }
        return null;
    }

    public static int extractTimeQuantity(String durationString) throws EMInvalidTimeDuarationException {
        durationString = durationString.replaceAll("\\s", "").toLowerCase();
        Pattern pattern = Pattern.compile("([0-9]+)([a-z]+)");
        Matcher match = pattern.matcher(durationString);
        if (!match.find()) {
            throw new EMInvalidTimeDuarationException();
        }
        if (!isInteger(match.group(1))){
            return -1;
        }
        return Integer.parseInt(match.group(1));
    }

    public static boolean isInteger(String intStr) {
        if (null == intStr) {
            return false;
        }
        try {
            Integer.parseInt(intStr);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        return true;
    }

}
