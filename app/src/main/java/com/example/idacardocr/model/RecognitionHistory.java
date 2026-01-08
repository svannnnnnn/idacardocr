package com.example.idacardocr.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecognitionHistory {
    private long id;
    private String type; // ID_CARD 或 BANK_CARD
    private String cardSide; // FRONT, BACK, DOUBLE
    private String resultJson;
    private String summary;
    private long timestamp;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCardSide() { return cardSide; }
    public void setCardSide(String cardSide) { this.cardSide = cardSide; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getCardSideDisplay() {
        if (cardSide == null) return "";
        switch (cardSide) {
            case "FRONT": return "人像面";
            case "BACK": return "国徽面";
            case "DOUBLE": return "双面";
            default: return "";
        }
    }
}
