package com.example.idacardocr.utils;

import com.example.idacardocr.BuildConfig;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 腾讯云OCR API客户端
 * 封装身份证识别和银行卡识别接口
 */
public class TencentOCRClient {
    
    private static final String HOST = "ocr.tencentcloudapi.com";
    private static final String SERVICE = "ocr";
    private static final String VERSION = "2018-11-19";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final TencentCloudSigner signer;
    private final OkHttpClient httpClient;

    public TencentOCRClient() {
        // 从BuildConfig读取API密钥（由gradle从.env文件注入）
        String secretId = BuildConfig.TENCENT_SECRET_ID;
        String secretKey = BuildConfig.TENCENT_SECRET_KEY;
        this.signer = new TencentCloudSigner(secretId, secretKey);
        this.httpClient = new OkHttpClient();
    }

    /**
     * 身份证识别
     * @param imageBase64 图片Base64编码
     * @param cardSide FRONT-人像面, BACK-国徽面
     */
    public String recognizeIDCard(String imageBase64, String cardSide) throws Exception {
        String action = "IDCardOCR";
        JSONObject payload = new JSONObject();
        payload.put("ImageBase64", imageBase64);
        payload.put("CardSide", cardSide);
        
        return callAPI(action, payload.toString());
    }

    /**
     * 银行卡识别
     * @param imageBase64 图片Base64编码
     */
    public String recognizeBankCard(String imageBase64) throws Exception {
        String action = "BankCardOCR";
        JSONObject payload = new JSONObject();
        payload.put("ImageBase64", imageBase64);
        
        return callAPI(action, payload.toString());
    }

    /**
     * 调用腾讯云API
     * 1. 生成签名 2. 发送HTTP请求
     */
    private String callAPI(String action, String payload) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        
        // 生成签名
        TencentCloudSigner.SignResult signResult = signer.sign(HOST, SERVICE, action, payload, timestamp);
        
        // 发送HTTP请求
        return post(action, payload, signResult.authorization, signResult.timestamp);
    }
    
    /**
     * 发送POST请求
     */
    private String post(String action, String payload, String authorization, String timestamp) throws Exception {
        RequestBody body = RequestBody.create(payload, JSON);
        
        Request request = new Request.Builder()
                .url("https://" + HOST)
                .post(body)
                .addHeader("Authorization", authorization)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Host", HOST)
                .addHeader("X-TC-Action", action)
                .addHeader("X-TC-Timestamp", timestamp)
                .addHeader("X-TC-Version", VERSION)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            throw new Exception("Response body is null");
        }
    }
}
