package com.ts.phi.bean;

import androidx.annotation.NonNull;

import java.util.Map;

public class AdjustmentResult {
    private final String ra; // Responsiveness Absolute
    private final String rr; // Responsiveness Relative
    private final String ca; // Convergence Absolute
    private final String cr; // Convergence Relative
    private final String type;
    private final String result;


    public AdjustmentResult(String ra, String rr, String ca, String cr, String type,String result) {
        this.ra = ra;
        this.rr = rr;
        this.ca = ca;
        this.cr = cr;
        this.type = type;
        this.result = result;
    }

    public static AdjustmentResult fromMap(Map<String, String> map) {
        if (map == null) {
            return new AdjustmentResult(null, null, null, null, "OTHER","");
        }
        return new AdjustmentResult(
                map.get("RA"),
                map.get("RR"),
                map.get("CA"),
                map.get("CR"),
                map.getOrDefault("type", "OTHER"),
                map.get("result")
        );
    }

    public boolean isRaFormat() {
        return "RA_FORMAT".equals(type);
    }

    public boolean isLetter() {
        return "LETTER".equals(type);
    }

    public String getResult() {
//        Map<String, String> result = new HashMap<>();
//        result.put("type", type);
//        if (ra != null) result.put("RA", ra);
//        if (rr != null) result.put("RR", rr);
//        if (ca != null) result.put("CA", ca);
//        if (cr != null) result.put("CR", cr);
//        return result.toString();
        return result;
    }

    // Getters
    public String getRa() { return ra; }
    public String getRr() { return rr; }
    public String getCa() { return ca; }
    public String getCr() { return cr; }
    public String getType() { return type; }

    public int getRaAsInt() {
        return parseToInt(ra);
    }

    public int getRrAsInt() {
        return parseToInt(rr);
    }

    public int getCaAsInt() {
        return parseToInt(ca);
    }

    public int getCrAsInt() {
        return parseToInt(cr);
    }

    private int parseToInt(String value) {
        if (value == null || value.isEmpty() || "None".equals(value) || "null".equalsIgnoreCase(value)) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("RA:%s, RR:%s, CA:%s, CR:%s",
                safeString(ra), safeString(rr), safeString(ca), safeString(cr));
    }

    private String safeString(String s) {
        return s == null || s.isEmpty() ? "None" : s;
    }
}