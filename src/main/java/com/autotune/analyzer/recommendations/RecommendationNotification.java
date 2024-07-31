/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.recommendations;

import com.autotune.common.annotations.json.Exclude;

public class RecommendationNotification {
    private String type;
    private String message;
    @Exclude
    private int code;

    public RecommendationNotification(RecommendationConstants.RecommendationNotification recommendationNotification) {
        this.type = recommendationNotification.getType().getName();
        this.message = recommendationNotification.getMessage();
        this.code = recommendationNotification.getCode();
    }
    public RecommendationNotification() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "RecommendationNotification{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", code=" + code +
                '}';
    }

    public int getCode() {
        return code;
    }

    public static RecommendationNotification getNotificationForTermAvailability(String recommendationTerm) {
        RecommendationNotification recommendationNotification = null;
        if (recommendationTerm.equalsIgnoreCase(RecommendationConstants.RecommendationTerms.SHORT_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_SHORT_TERM_RECOMMENDATIONS_AVAILABLE);
        } else if (recommendationTerm.equalsIgnoreCase(RecommendationConstants.RecommendationTerms.MEDIUM_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE);
        } else if (recommendationTerm.equalsIgnoreCase(RecommendationConstants.RecommendationTerms.LONG_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_LONG_TERM_RECOMMENDATIONS_AVAILABLE);
        } else if (recommendationTerm.equalsIgnoreCase(RecommendationConstants.RecommendationTerms.FIXED_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_FIXED_TERM_RECOMMENDATIONS_AVAILABLE);
        }
        return recommendationNotification;
    }
}
