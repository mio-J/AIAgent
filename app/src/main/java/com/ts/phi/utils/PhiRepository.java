package com.ts.phi.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import com.ts.phi.constants.ApiConstants;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class PhiRepository {
    private static final String TAG = ApiConstants.TAG;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface PhiCallback {
        void onSuccess(String result);
        void onFailure(String error);
    }

    public void sendRequest(String prompt, boolean isStream, PhiCallback callback) {
        try {
            JSONObject jsonBody = buildRequestBody(prompt, isStream);
            RequestBody requestBody = RequestBody.create(JSON, jsonBody.toString());

            Request request = new Request.Builder()
                    .post(requestBody)
                    .url(ApiConstants.BASE_URL)
                    .build();

            Call call = ApiConstants.getOkHttpClient().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.d(TAG, "requestSubmit fail: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    handleResponse(response, callback);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error building request: " + Log.getStackTraceString(e));
            callback.onFailure(e.getMessage());
        }
    }

    private JSONObject buildRequestBody(String prompt, boolean isStream) throws JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", ApiConstants.MODEL_NAME);
        jsonBody.put("temperature", 0.0);
        jsonBody.put("stream", isStream);
        jsonBody.put("reset", true);

        // Meta
        JSONObject meta = new JSONObject();
        JSONArray tupleData = new JSONArray();
        tupleData.put(1).put(2).put(3);
        meta.put("tuple_data", tupleData);
        meta.put("extra_info", "test");
        meta.put("image_id", "123");
        jsonBody.put("meta", meta);

        // Messages
        JSONArray messages = new JSONArray();
        JSONObject roleUser = new JSONObject();
        roleUser.put("role", "user");

        JSONArray content = new JSONArray();
        JSONObject textObj = new JSONObject();
        textObj.put("type", "text");
        textObj.put("text", prompt);
        content.put(textObj);

        roleUser.put("content", content);
        messages.put(roleUser);
        jsonBody.put("messages", messages);

        return jsonBody;
    }

    private void handleResponse(Response response, PhiCallback callback) {
        if (!response.isSuccessful()) {
            callback.onFailure("Response not successful: " + response.code());
            return;
        }

        try (ResponseBody responseBody = response.body()) {
            if (responseBody != null) {
                String fullResponse = responseBody.string();
                Log.d(TAG, "Full response: " + fullResponse);

                StringBuilder sb = new StringBuilder();

                if (fullResponse.trim().startsWith("{") && fullResponse.trim().endsWith("}")) {
                    sb.append(ResponseParser.extractContent(fullResponse));
                } else {
                    String[] lines = fullResponse.split("\n");
                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;

                        String data = line;
                        if (line.startsWith(ApiConstants.FLAG_STREAM_DATA)) {
                            data = line.substring(ApiConstants.FLAG_STREAM_DATA.length()).trim();
                        }

                        if (data.startsWith("{")) {
                            sb.append(ResponseParser.extractContent(data));
                            Log.d(TAG, "data--   " + data);
                        }
                    }
                }

                Log.i(TAG, "phi result:" + sb.toString());
                callback.onSuccess(sb.toString());
            } else {
                callback.onFailure("Empty response body");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing response: " + e.getMessage());
            callback.onFailure(e.getMessage());
        }
    }
}