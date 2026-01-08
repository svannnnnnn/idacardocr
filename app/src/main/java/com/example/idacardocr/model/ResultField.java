package com.example.idacardocr.model;

/**
 * 识别结果字段模型
 * 包含字段名、完整值、脱敏值，支持敏感信息隐藏显示
 */
public class ResultField {
    private String fieldName;      // 字段名
    private String fullValue;      // 完整值
    private String maskedValue;    // 脱敏值
    private boolean isSensitive;   // 是否敏感字段
    private boolean isRevealed;    // 是否已解锁

    public ResultField(String fieldName, String fullValue, boolean isSensitive) {
        this.fieldName = fieldName;
        this.fullValue = fullValue;
        this.isSensitive = isSensitive;
        this.isRevealed = false;
        this.maskedValue = isSensitive ? maskValue(fullValue, fieldName) : fullValue;
    }

    /**
     * 脱敏处理
     * - 身份证号：显示前4后4
     * - 银行卡号：显示前4后4
     * - 姓名：显示姓，名用*
     * - 地址：显示前6字符
     */
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

    /**
     * 获取显示值
     * 敏感字段未解锁时返回脱敏值，否则返回完整值
     */
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
