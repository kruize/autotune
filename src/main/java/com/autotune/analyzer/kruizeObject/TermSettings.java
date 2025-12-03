/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.kruizeObject;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;

/**
 * Container for term configurations in recommendation settings.
 * Contains a list of term names and their detailed definitions for custom terms.
 */
public class TermSettings {
    @SerializedName("terms")
    private List<String> terms;

    @SerializedName("terms_definition")
    private HashMap<String, TermDefinition> termsDefinition;

    public TermSettings() {}

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public HashMap<String, TermDefinition> getTermsDefinition() {
        return termsDefinition;
    }

    public void setTermsDefinition(HashMap<String, TermDefinition> termsDefinition) {
        this.termsDefinition = termsDefinition;
    }

    @Override
    public String toString() {
        return "TermSettings{" +
                "terms=" + terms +
                ", termsDefinition=" + termsDefinition +
                '}';
    }
}
