package com.autotune.analyzer.model;

import com.autotune.analyzer.utils.AnalyzerConstants;

public class AutotuneSpecificationData {
    private SLOData slo;
    private AnalyzerConstants.autotuneOperatorMode mode;
    private SelectorData selector;

    public AutotuneSpecificationData(SLOData slo, AnalyzerConstants.autotuneOperatorMode mode, SelectorData selector) {
        this.slo = slo;
        this.mode = mode;
        this.selector = selector;
    }

    public SLOData getSlo() {
        return slo;
    }

    public void setSlo(SLOData slo) {
        this.slo = slo;
    }

    public AnalyzerConstants.autotuneOperatorMode getMode() {
        return mode;
    }

    public void setMode(AnalyzerConstants.autotuneOperatorMode mode) {
        this.mode = mode;
    }

    public SelectorData getSelector() {
        return selector;
    }

    public void setSelector(SelectorData selector) {
        this.selector = selector;
    }
}
