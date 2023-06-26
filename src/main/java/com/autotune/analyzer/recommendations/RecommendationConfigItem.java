/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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

public class RecommendationConfigItem {
    private Double amount;
    private String format;
    private String errorMsg;

    public RecommendationConfigItem(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public RecommendationConfigItem(Double amount, String format) {
        this.amount = amount;
        this.format = format;
    }

    public RecommendationConfigItem() {

    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Double getAmount() {
        return amount;
    }

    public String getFormat() {
        return format;
    }
}
