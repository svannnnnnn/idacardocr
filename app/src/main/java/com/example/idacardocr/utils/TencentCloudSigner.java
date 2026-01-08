package com.example.idacardocr.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 腾讯云API签名工具类
 * 
 * 这个类是整个项目最头疼的部分，腾讯云的 TC3-HMAC-SHA256 签名算法搞了我好久
 * 官方文档写得不太清楚，踩了不少坑：
 * 1. 时间戳必须用 UTC 时区，不然签名会失败
 * 2. canonicalHeaders 里的 action 要转小写，这个坑藏得很深
 * 3. 签名用的是多层 HMAC，每一层的 key 都是上一层的结果
 * 
 * 参考了官方 SDK 源码才搞定：https://github.com/TencentCloud/tencentcloud-sdk-java
 */
public class TencentCloudSigner {

    private static final String ALGORITHM = "TC3-HMAC-SHA256";

    private final String secretId;
    private final String secretKey;

    public TencentCloudSigner(String secretId, String secretKey) {
        this.secretId = secretId;
        this.secretKey = secretKey;
    }

    /**
     * 生成签名信息
     * 
     * 签名流程分4步，每一步都不能出错：
     * 1. 拼接规范请求串 CanonicalRequest
     * 2. 拼接待签名字符串 StringToSign  
     * 3. 用 SecretKey 派生签名密钥，然后计算签名
     * 4. 把签名塞到 Authorization 头里
     */
    public SignResult sign(String host, String service, String action, String payload, long timestamp) throws Exception {
        String timestampStr = String.valueOf(timestamp);

        // 这里必须用 UTC 时区！！！被这个坑了好久，本地时间会导致签名失败
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(timestamp * 1000));

        // 步骤1：拼接规范请求串
        String canonicalRequest = buildCanonicalRequest(host, action, payload);

        // 步骤2：拼接待签名字符串
        String credentialScope = date + "/" + service + "/tc3_request";
        String stringToSign = buildStringToSign(timestampStr, credentialScope, canonicalRequest);

        // 步骤3：计算签名
        String signature = calculateSignature(date, service, stringToSign);

        // 步骤4：拼接Authorization
        String authorization = buildAuthorization(credentialScope, signature);

        return new SignResult(authorization, timestampStr);
    }

    /**
     * 拼接规范请求串
     * 注意：x-tc-action 必须转小写，官方文档没写清楚这点，debug 了半天才发现
     */
    private String buildCanonicalRequest(String host, String action, String payload) throws Exception {
        String httpRequestMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        // action.toLowerCase() 是关键！不转小写签名会失败
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                + "host:" + host + "\n"
                + "x-tc-action:" + action.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String hashedRequestPayload = sha256Hex(payload);

        return httpRequestMethod + "\n"
                + canonicalUri + "\n"
                + canonicalQueryString + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + hashedRequestPayload;
    }

    private String buildStringToSign(String timestamp, String credentialScope, String canonicalRequest) throws Exception {
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        return ALGORITHM + "\n"
                + timestamp + "\n"
                + credentialScope + "\n"
                + hashedCanonicalRequest;
    }

    /**
     * 计算签名
     * 这里用了多层 HMAC，每一层的输出作为下一层的 key
     * TC3 + secretKey -> date -> service -> tc3_request -> 最终签名
     * 感觉腾讯是故意搞这么复杂的...
     */
    private String calculateSignature(String date, String service, String stringToSign) throws Exception {
        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, service);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        return bytesToHex(hmac256(secretSigning, stringToSign));
    }

    private String buildAuthorization(String credentialScope, String signature) {
        String signedHeaders = "content-type;host;x-tc-action";
        return ALGORITHM + " "
                + "Credential=" + secretId + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;
    }

    private byte[] hmac256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(d);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 签名结果
     */
    public static class SignResult {
        public final String authorization;
        public final String timestamp;

        public SignResult(String authorization, String timestamp) {
            this.authorization = authorization;
            this.timestamp = timestamp;
        }
    }
}
