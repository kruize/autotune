package com.autotune.common.datasource;

import com.autotune.common.datasource.prometheus.PrometheusDataOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import org.slf4j.LoggerFactory;

public class DataSourceOperatorImpl implements DataSourceOperator{

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DataSourceOperatorImpl.class);
    private static DataSourceOperatorImpl dataSourceOperator = null;
    protected DataSourceOperatorImpl() {
    }

    public static DataSourceOperatorImpl getInstance() {
        if (null == dataSourceOperator) {
            dataSourceOperator = new DataSourceOperatorImpl();
        }
        return dataSourceOperator;
    }

    @Override
    public DataSourceOperatorImpl getOperator(String provider) {
        if (provider.equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
            return PrometheusDataOperatorImpl.getInstance();
        }
        return null;
    }

    @Override
    public String getDefaultServicePortForProvider(){
        return "";
    }

    @Override
    public CommonUtils.DatasourceReachabilityStatus isServiceable(String dataSourceUrl){
        return null;
    }

    @Override
    public Object getValueForQuery(String url, String query){
        return null;
    }

}
