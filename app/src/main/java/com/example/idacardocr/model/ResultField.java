package com.example.idacardocr.model;

public class ResultField {
    private String fieldName;
    private String fullValue;
    private String maskedValue;
    private boolean isSensitive;
    private boolean isRevealed;

    public ResultField(String fieldName, String fullValue, boolean isSensitive) {
        this.fieldName = fieldName;
        this.fullValue = fullValue;
        this.isSensitive = isSensitive;
        this.isRevealed = false;
        this.maskedValue = isSensitive ? maskValue(fullValue, fieldName) : fullValue;
    }

    private String maskValue(String value, String fieldName) {
        if (value == null || value.equals("-") || value.isEmpty()) {
            return value;
        }
        
        // 身份证号脱敏：显示前4后4
        if (fieldName.contains("身份证") || fieldName.contains("证件号")) {
            if (value.length() > 8) {
                return value.substring(0, 4) + "**********" + value.substring(value.length() - 4);
            }
        }
        
        // 银行卡号脱敏：显示前4后4
        if (fieldName.contains("卡号")) {
            if (value.length() > 8) {
                return value.substring(0, 4) + " **** **** " + value.substring(value.length() - 4);
            }
        }
        
        // 姓名脱敏：显示姓，名用*
        if (fieldName.contains("姓名")) {
            if (value.length() >= 2) {
                StringBuilder masked = new StringBuilder();
                masked.append(value.charAt(0));
                for (int i = 1; i < value.length(); i++) {
                    masked.append("*");
                }
                return masked.toString();
            }
        }
        
        // 地址脱敏：显示前6个字符
        if (fieldName.contains("地址")) {
            if (value.length() > 6) {
                return value.substring(0, 6) + "****";
            }
        }
        
        // 默认脱敏：中间用*
        if (value.length() > 4) {
            return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        }
        return "****";
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFullValue() {
        return fullValue;
    }

    public String getMaskedValue() {
        return maskedValue;
    }

    public String getDisplayValue() {
        return (isSensitive && !isRevealed) ? maskedValue : fullValue;
    }

    public boolean isSensitive() {
        return isSensitive;
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    public void setRevealed(boolean revealed) {
        isRevealed = revealed;
    }
}
