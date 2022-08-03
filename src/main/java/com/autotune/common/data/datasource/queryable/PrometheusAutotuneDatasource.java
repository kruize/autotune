package com.autotune.common.data.datasource.queryable;

import com.autotune.common.utils.CommonConstants;

public class PrometheusAutotuneDatasource extends QueryableAutotuneDatasource {
    private String name = CommonConstants.AutotuneDatasource.Prometheus.DEFAULT_NAME;
    private String source = null;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        if((null != name) && (0 != name.trim().length()))
            this.name = name;
    }

    /**
     * Needs a check for null when used
     * @return
     */
    @Override
    public String getSource() {
        return this.source;
    }

    @Override
    public void setSource(String source) {
        if((null != source) && (0 != source.trim().length()))
            this.source = source;
    }
}
