package com.autotune.analyzer.data;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class AutotuneOperatorData {

    private AutotuneMetaData metadata;
    private AutotuneSpecificationData spec;
    private String version;
    private String apiVersion;
    private String kind;

    public AutotuneOperatorData(AutotuneMetaData metadata, AutotuneSpecificationData spec, String version) {
        this.metadata = metadata;
        this.spec = spec;
        this.version = version;
        this.apiVersion = "recommender.com/v1";
        this.kind = "Autotune";
    }

    public static void main(String[] args) {
        AutotuneMetaData metaData = new AutotuneMetaData("229ac511-4c23-4aca-823e-f61c609dcabd:tfb-sample", "default");
        QueryData queryData = new QueryData("request_sum", "rate(http_server_requests_seconds_sum{method=\\\"GET\\\",outcome=\\\"SUCCESS\\\",status=\\\"200\\\",uri=\\\"/db\\\",}[1m])", "prometheus", "double");
        QueryData queryData2 = new QueryData("request_count", "rate(http_server_requests_seconds_count{method=\\\"GET\\\",outcome=\\\"SUCCESS\\\",status=\\\"200\\\",uri=\\\"/db\\\",}[1m])", "prometheus", "double");
        List<QueryData> function_var = new ArrayList<>();
        function_var.add(queryData);
        function_var.add(queryData2);
        SLOData sloData = new SLOData("Minimize resource utilization", "response_time", "minimize", function_var);
        SelectorData selectorData = new SelectorData("app.kubernetes.io/name", "tfb-qrh-deployment", "", "", "");
        AutotuneSpecificationData specificationData = new AutotuneSpecificationData(sloData, AnalyzerConstants.autotuneOperatorMode.monitoring, selectorData);
        AutotuneOperatorData autotuneOperatorData = new AutotuneOperatorData(metaData, specificationData, "V1.0.0");
        System.out.println(new Gson().toJson(autotuneOperatorData));
    }

    public AutotuneMetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(AutotuneMetaData metadata) {
        this.metadata = metadata;
    }

    public AutotuneSpecificationData getSpec() {
        return spec;
    }

    public void setSpec(AutotuneSpecificationData spec) {
        this.spec = spec;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
