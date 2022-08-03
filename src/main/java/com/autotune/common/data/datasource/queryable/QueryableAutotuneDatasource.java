package com.autotune.common.data.datasource.queryable;

import com.autotune.common.data.datasource.AutotuneDatasource;
import com.autotune.common.utils.CommonUtils;

public abstract class QueryableAutotuneDatasource implements AutotuneDatasource {
    protected CommonUtils.AutotuneDatasourceTypes type = CommonUtils.AutotuneDatasourceTypes.QUERYABLE;

    @Override
    public CommonUtils.AutotuneDatasourceTypes getType() {
        return this.type;
    }
}
