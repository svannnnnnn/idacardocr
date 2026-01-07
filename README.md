# 身份证和银行卡OCR识别应用

基于腾讯云OCR API实现的Android身份证和银行卡识别应用。

## 功能特性

- ✅ 身份证人像面识别（姓名、性别、民族、出生日期、住址、身份证号）
- ✅ 身份证国徽面识别（签发机关、有效期限）
- ✅ 银行卡识别（卡号、银行信息、卡类型、卡名称、有效期）
- ✅ 支持从相册选择图片
- ✅ 自动图片压缩和优化
- ✅ 通过BuildConfig安全管理API密钥

## 配置说明

### 1. 获取腾讯云API密钥

1. 访问 [腾讯云控制台](https://console.cloud.tencent.com/)
2. 进入 [API密钥管理](https://console.cloud.tencent.com/capi)
3. 创建密钥，获取 `SecretId` 和 `SecretKey`

### 2. 配置API密钥

编辑项目根目录下的 `.env` 文件：

```env
TENCENT_SECRET_ID=你的SecretId
TENCENT_SECRET_KEY=你的SecretKey
```

**工作原理：** 

- Gradle在编译时读取根目录的 `.env` 文件
- 将密钥注入到 `BuildConfig` 类中
- 应用运行时通过 `BuildConfig.TENCENT_SECRET_ID` 和 `BuildConfig.TENCENT_SECRET_KEY` 获取密钥

**注意：** `.env` 文件已添加到 `.gitignore`，不会被提交到版本控制系统。

## 项目结构

```
app/src/main/
├── java/com/example/idacardocr/
│   ├── MainActivity.java              # 主Activity
│   ├── api/
│   │   └── TencentOCRClient.java     # 腾讯云OCR API客户端
│   └── utils/
│       └── ImageUtils.java           # 图片处理工具
└── res/
    └── layout/
        └── activity_main.xml         # 主界面布局

.env                                   # API密钥配置文件（根目录）
```

## 依赖库

- OkHttp 4.12.0 - HTTP网络请求
- Gson 2.10.1 - JSON解析
- Material Components - UI组件

## 使用说明

1. 启动应用
2. 点击"选择人像面"或"选择国徽面"按钮选择身份证图片
3. 点击"选择银行卡图片"按钮选择银行卡图片
4. 等待识别完成，结果会显示在对应的文本框中

## API说明

### 身份证识别 (IDCardOCR)

- **接口**: `ocr.tencentcloudapi.com`
- **Action**: `IDCardOCR`
- **参数**:
  - `ImageBase64`: 图片的Base64编码
  - `CardSide`: `FRONT`(人像面) 或 `BACK`(国徽面)

### 银行卡识别 (BankCardOCR)

- **接口**: `ocr.tencentcloudapi.com`
- **Action**: `BankCardOCR`
- **参数**:
  - `ImageBase64`: 图片的Base64编码

## 签名方法

本项目使用腾讯云API签名方法v3 (TC3-HMAC-SHA256)，签名过程包括：

1. 拼接规范请求串
2. 拼接待签名字符串
3. 计算签名
4. 拼接Authorization头部

详细说明请参考：`cankao/签名方法 v3.md`

## 权限说明

应用需要以下权限：

- `INTERNET` - 网络访问
- `READ_EXTERNAL_STORAGE` - 读取相册图片
- `CAMERA` - 相机拍照（可选）

## 注意事项

1. 图片要求：
   - 格式：PNG、JPG、JPEG、BMP
   - 大小：Base64编码后不超过10M
   - 分辨率：建议500*800以上
   - 建议卡片部分占据图片2/3以上

2. API调用限制：
   - 身份证识别：20次/秒
   - 银行卡识别：10次/秒

3. 安全建议：
   - 不要将API密钥硬编码在代码中
   - 不要将包含真实密钥的.env文件提交到版本控制
   - 生产环境建议使用临时密钥或后端代理



## License

MIT License
