package com.autotune.common.experiments;

import com.autotune.common.data.datasource.AutotuneDatasource;
import com.autotune.common.data.datasource.AutotuneDatasourceCollection;
import com.autotune.common.exceptions.AutotuneDatasourceAlreadyExists;
import com.autotune.common.exceptions.AutotuneDatasourceDoesNotExist;
import com.autotune.common.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class ExperimentDatasourceCollection implements AutotuneDatasourceCollection {
    HashMap<String, AutotuneDatasource> experimentDatasourceMap;

    public ExperimentDatasourceCollection() {
        experimentDatasourceMap = new HashMap<String, AutotuneDatasource>();
    }

    @Override
    public CommonUtils.AddDataSourceStatus addDataSource(AutotuneDatasource autotuneDatasource) throws AutotuneDatasourceAlreadyExists {
        return null;
    }

    @Override
    public AutotuneDatasource getDatasource(String datasourceName) {
        return null;
    }

    @Override
    public Map<String, AutotuneDatasource> getDatasourceMap() {
        return null;
    }

    @Override
    public void removeDataSource(String datasourceName) throws AutotuneDatasourceDoesNotExist {

    }
}
