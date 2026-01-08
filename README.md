# 证件识别助手

基于腾讯云 OCR API 的 Android 身份证和银行卡识别应用。

## 功能特性

### 核心功能
- 身份证人像面识别（姓名、性别、民族、出生日期、住址、身份证号）
- 身份证国徽面识别（签发机关、有效期限）
- 身份证双面识别模式（同时识别人像面和国徽面）
- 银行卡识别（卡号、银行信息、卡类型、卡名称、有效期）

### 图片上传
- 支持拍照上传
- 支持从相册选择图片
- 内置图片裁剪功能（自定义 CropImageView，可拖动裁剪框和调整四角大小）
- 自动图片压缩优化

### 隐私保护
- 敏感信息默认脱敏显示（身份证号、姓名、地址、银行卡号）
- 生物认证（指纹/面容/设备密码）解锁查看完整信息
- 一键复制识别结果
- 全部隐藏功能

### 历史记录
- 自动保存识别历史到本地 SQLite 数据库
- 查看历史识别记录
- 删除单条或清空全部历史

### 交互体验
- Material Design UI 设计
- 页面切换滑动动画
- 卡片点击波纹效果

## 配置说明

### 1. 获取腾讯云 API 密钥

1. 访问 [腾讯云控制台](https://console.cloud.tencent.com/)
2. 进入 [API密钥管理](https://console.cloud.tencent.com/capi)
3. 创建密钥，获取 `SecretId` 和 `SecretKey`

### 2. 配置 API 密钥

在项目根目录创建 `.env` 文件：

```env
TENCENT_SECRET_ID=你的SecretId
TENCENT_SECRET_KEY=你的SecretKey
```

> `.env` 文件已添加到 `.gitignore`，不会被提交到版本控制。

## 项目结构

```
app/src/main/java/com/example/idacardocr/
├── MainActivity.java              # 主页面（卡片式导航）
├── IdCardActivity.java            # 身份证识别页面
├── BankCardActivity.java          # 银行卡识别页面
├── adapter/
│   ├── ResultFieldAdapter.java    # 识别结果列表适配器
│   └── HistoryAdapter.java        # 历史记录列表适配器
├── db/
│   └── RecognitionHistoryDbHelper.java  # SQLite 数据库帮助类
├── model/
│   ├── ResultField.java           # 识别结果字段模型（含脱敏逻辑）
│   └── RecognitionHistory.java    # 历史记录模型
├── view/
│   └── CropImageView.java         # 自定义图片裁剪视图
└── utils/
    ├── TencentOCRClient.java      # 腾讯云 OCR API 客户端
    ├── TencentCloudSigner.java    # TC3-HMAC-SHA256 签名算法
    ├── ImageUtils.java            # 图片处理工具
    └── BiometricHelper.java       # 生物认证工具

app/src/main/res/
├── layout/                        # 布局文件
├── anim/                          # 页面切换动画
├── drawable/                      # 图标资源
└── values/                        # 颜色、字符串资源
```

## 技术栈

| 类别 | 技术 |
|------|------|
| 网络请求 | OkHttp 4.12.0 |
| JSON 解析 | Gson 2.10.1 |
| UI 组件 | Material Components |
| 生物认证 | AndroidX Biometric 1.1.0 |
| 数据存储 | SQLite |
| API 签名 | TC3-HMAC-SHA256 |

## 使用说明

### 身份证识别
1. 主页点击"身份证识别"
2. 选择识别模式：单面或双面
3. 点击上传按钮，拍照或从相册选择
4. 裁剪图片后点击"确定"
5. 点击"开始"进行识别
6. 敏感信息需生物认证后查看

### 银行卡识别
1. 主页点击"银行卡识别"
2. 上传银行卡照片并裁剪
3. 点击"开始识别"
4. 查看识别结果

## 权限说明

| 权限 | 用途 |
|------|------|
| INTERNET | 调用腾讯云 OCR API |
| READ_EXTERNAL_STORAGE | 读取相册图片 |
| CAMERA | 拍照上传 |
| USE_BIOMETRIC | 生物认证 |

## 注意事项

1. **图片要求**：支持 PNG、JPG、JPEG、BMP 格式，建议使用裁剪功能选取卡片区域
2. **API 限制**：身份证识别 20次/秒，银行卡识别 10次/秒
3. **安全建议**：不要将 `.env` 文件提交到版本控制

## License

MIT License
