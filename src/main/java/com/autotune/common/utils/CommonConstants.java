package com.autotune.common.utils;

public class CommonConstants {
    private CommonConstants() {

    }

    public static class AutotuneDatasource {
        private AutotuneDatasource() {

        }

        public static class Prometheus {
            private Prometheus() {

            }
            public static String DEFAULT_NAME = "prometheus";
            public static String READINESS_ENDPOINT = "ready";
        }
    }

    public static class Http {
        private Http() {

        }

        public static class MethodTypes {
            private MethodTypes() {

            }

            public static String GET = "GET";
            public static String POST = "POST";
            public static String PUT = "PUT";
            public static String DELETE = "DELETE";
        }
    }
}
