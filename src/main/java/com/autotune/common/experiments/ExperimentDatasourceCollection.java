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
        if (experimentDatasourceMap.containsKey(autotuneDatasource.getName()))
            throw new AutotuneDatasourceAlreadyExists();
        switch (autotuneDatasource.isReachable()) {
            case NOT_REACHABLE:
                return CommonUtils.AddDataSourceStatus.DATASOURCE_NOT_REACHABLE;
            case SOURCE_NOT_SET:
                return CommonUtils.AddDataSourceStatus.INVALID_DATASOURCE;
            case REACHABLE:
                experimentDatasourceMap.put(autotuneDatasource.getName(), autotuneDatasource);
                return CommonUtils.AddDataSourceStatus.SUCCESS;
        }
        return CommonUtils.AddDataSourceStatus.FAILURE;
    }

    @Override
    public AutotuneDatasource getDatasource(String datasourceName) throws AutotuneDatasourceDoesNotExist {
        if (experimentDatasourceMap.containsKey(datasourceName))
            return experimentDatasourceMap.get(datasourceName);
        throw new AutotuneDatasourceDoesNotExist();
    }

    @Override
    public Map<String, AutotuneDatasource> getDatasourceMap() {
        return this.experimentDatasourceMap;
    }

    @Override
    public void removeDataSource(String datasourceName) throws AutotuneDatasourceDoesNotExist {
        if (experimentDatasourceMap.containsKey(datasourceName))
            experimentDatasourceMap.remove(datasourceName);
        throw new AutotuneDatasourceDoesNotExist();
    }
}
