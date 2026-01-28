package com.ts.phi.constants;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class ApiConstants {
    public static final String TAG = "AIAgentDebug";
    public static final String TAG_TEST = "AIAgentTest";
    public static final String TAG_KEY = "AIAgent";

    public static final String BASE_URL = "http://0.0.0.0:8080/v1/mllm/completions";
    public static final String FLAG_STREAM_DATA = "data:";
    public static final String MODEL_NAME = "phi35vl";

    public static final long CONNECT_TIMEOUT = 200;
    public static final long READ_TIMEOUT = 200;
    public static final long WRITE_TIMEOUT = 200;
    public static final long TIMEOUT_DURATION_MS = 900000; // 15 minutes

    private static volatile OkHttpClient client;

    public static OkHttpClient getOkHttpClient() {
        if (client == null) {
            synchronized (ApiConstants.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return client;
    }
}