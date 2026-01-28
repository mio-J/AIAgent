package com.ts.phi.utils;

import android.util.Log;
import com.ts.phi.constants.ApiConstants;
import com.ts.phi.bean.AdjustmentResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseParser {
    private static final String TAG = ApiConstants.TAG;

    private static final Pattern CHOOSE_PATTERN = Pattern.compile(
            "\"choose\"\\s*[:=]\\s*[\"']?([A-D])[\"']?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CHINESE_PATTERN = Pattern.compile(
            "(?:答案|正确选项|选择|是)[:：]?\\s*[\"']?([A-D])[\"']?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern OPTION_PATTERN = Pattern.compile(
            "\\b(?:option|choice|answer)\\s*[:：]?\\s*([A-D])\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern STRICT_PATTERN = Pattern.compile(
            "(?:^|[\\s:：,，\"'\\{\\[])([A-D])(?:[\\s\\.。,，;；!！?？\"'\\}\\]]|$)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LOOSE_PATTERN = Pattern.compile(
            "\\b([A-D])\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RESPONSE_PATTERN = Pattern.compile(
            "'response'\\s*:\\s*\\{[^\\}]*\\}"
    );

    public static AdjustmentResult extractAdjustmentResult(String phiAnswer) {
        Map<String, String> result = new HashMap<>();

        if (phiAnswer == null || (phiAnswer = phiAnswer.trim()).isEmpty()) {
            result.put("type", "OTHER");
            result.put("result", "");
            return AdjustmentResult.fromMap(result);
        }

        // New format: RA:xx,RR:xx,CA:xx,CR:xx
        if (phiAnswer.startsWith("RA:") && phiAnswer.contains("RR:") &&
                phiAnswer.contains("CA:") && phiAnswer.contains("CR:")) {
            parseRaFormat(phiAnswer, result);
            result.put("type", "RA_FORMAT");
            return AdjustmentResult.fromMap(result);
        }

        // Single letter A-G (loose match)
        String cleaned = phiAnswer.replaceFirst("(?i)^\\s*(answer|choice|option|答案|正确答案|是)[:：=]?\\s*", "").trim();
        Matcher m = Pattern.compile("^([A-G])\\b", Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (m.find()) {
            result.put("type", "LETTER");
            result.put("result", m.group(1).toUpperCase());
            return AdjustmentResult.fromMap(result);
        }

        // Fallback
        result.put("type", "OTHER");
        result.put("result", phiAnswer);
        return AdjustmentResult.fromMap(result);
    }

    private static void parseRaFormat(String input, Map<String, String> result) {
        for (String part : input.split("\\s*,\\s*")) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String val = kv[1].trim();
                if ("null".equalsIgnoreCase(val)) val = "";
                if ("RA".equals(key) || "RR".equals(key) || "CA".equals(key) || "CR".equals(key)) {
                    result.put(key, val.replaceAll("[^0-9]", ""));
                }
            }
        }
    }

    public static String extractContent(String json) {
        try {
            JSONObject data = new JSONObject(json);
            JSONArray jsonArray = data.optJSONArray("choices");
            if (jsonArray != null && jsonArray.length() > 0) {
                JSONObject choice = jsonArray.optJSONObject(0);
                JSONObject delta = choice.optJSONObject("delta");
                if (delta != null) {
                    String content = delta.optString("content");

                    // Try to extract response part first
                    Matcher matcher = RESPONSE_PATTERN.matcher(content);
                    if (matcher.find()) {
                        String responsePart = matcher.group();
                        String jsonResponse = "{" + responsePart + "}";
                        jsonResponse = jsonResponse.replace("None", "null")
                                .replaceAll("'([A-Za-z0-9_]+)'", "\"$1\"");
                        try {
                            JSONObject obj = new JSONObject(jsonResponse);
                            JSONObject responseObj = obj.getJSONObject("response");
                            String ra = responseObj.optString("RA", "");
                            String rr = responseObj.optString("RR", "");
                            String ca = responseObj.optString("CA", "");
                            String cr = responseObj.optString("CR", "");
                            return String.format("RA:%s, RR:%s, CA:%s, CR:%s", ra, rr, ca, cr);
                        } catch (JSONException e) {
                            Log.w(TAG, "Failed to parse response part, continuing with normal flow");
                        }
                    }

                    // Try JSON parsing for choose field
                    if (content.trim().startsWith("{")) {
                        try {
                            String jsonContent = preprocessJson(content);
                            JSONObject contentJson = new JSONObject(jsonContent);
                            if (contentJson.has("choose")) {
                                String choose = contentJson.optString("choose", "").trim().toUpperCase();
                                Log.i(TAG, "Extracted choose from JSON: " + choose);
                                if (isValidOption(choose)) {
                                    return choose;
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "JSON parse failed, fallback to regex extraction");
                            String fallbackResult = extractOptionByRegex(content);
                            if (fallbackResult != null) {
                                Log.i(TAG, "Extracted choose by regex fallback: " + fallbackResult);
                                return fallbackResult;
                            }
                        }
                    }

                    Log.i(TAG, "Extracted content: " + content);
                    return content;
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, "Error extracting content: " + Log.getStackTraceString(exception));
        }
        return "";
    }

    private static String preprocessJson(String content) {
        String processed = content.replace("None", "null");
        processed = processed.replaceAll("(?<=\\{|,|\\s)'([A-Za-z0-9_]+)'(?=\\s*:)", "\"$1\"");
        processed = processed.replaceAll(":(\\s*)'([^']*)'", ":$1\"$2\"");
        return processed;
    }

    private static boolean isValidOption(String choose) {
        return choose != null && choose.matches("^[A-D]$");
    }

    public static String extractOptionByRegex(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String upperText = text.toUpperCase();

        // Strategy 1: Match choose field
        Matcher chooseMatcher = CHOOSE_PATTERN.matcher(text);
        if (chooseMatcher.find()) {
            String result = chooseMatcher.group(1).toUpperCase();
            Log.d(TAG, "Regex matched choose field: " + result);
            return result;
        }

        // Strategy 2: Match Chinese hints
        Matcher chineseMatcher = CHINESE_PATTERN.matcher(upperText);
        if (chineseMatcher.find()) {
            String result = chineseMatcher.group(1);
            Log.d(TAG, "Regex matched Chinese hint: " + result);
            return result;
        }

        // Strategy 3: Match option/choice keywords
        Matcher optionMatcher = OPTION_PATTERN.matcher(upperText);
        if (optionMatcher.find()) {
            String result = optionMatcher.group(1);
            Log.d(TAG, "Regex matched option/choice keyword: " + result);
            return result;
        }

        // Strategy 4: Strict standalone match
        Matcher strictMatcher = STRICT_PATTERN.matcher(upperText);
        java.util.List<String> candidates = new java.util.ArrayList<>();
        while (strictMatcher.find()) {
            candidates.add(strictMatcher.group(1));
        }

        if (candidates.size() == 1) {
            Log.d(TAG, "Regex matched single standalone option: " + candidates.get(0));
            return candidates.get(0);
        } else if (candidates.size() > 1) {
            Log.d(TAG, "Multiple options found, returning first: " + candidates.get(0));
            return candidates.get(0);
        }

        // Strategy 5: Loose match
        Matcher looseMatcher = LOOSE_PATTERN.matcher(upperText);
        if (looseMatcher.find()) {
            String result = looseMatcher.group(1).toUpperCase();
            Log.w(TAG, "Loose regex matched option (may be inaccurate): " + result);
            return result;
        }

        return null;
    }
}