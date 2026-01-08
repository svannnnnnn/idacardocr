# 身份证和银行卡OCR识别应用

基于腾讯云OCR API实现的Android身份证和银行卡识别应用。

## 功能特性

### 核心功能
- ✅ 身份证人像面识别（姓名、性别、民族、出生日期、住址、身份证号）
- ✅ 身份证国徽面识别（签发机关、有效期限）
- ✅ 身份证双面识别模式（同时识别人像面和国徽面）
- ✅ 银行卡识别（卡号、银行信息、卡类型、卡名称、有效期）

### 图片处理
- ✅ 支持拍照上传
- ✅ 支持从相册选择图片
- ✅ 内置图片裁剪功能（自定义裁剪视图，兼容所有设备）
- ✅ 可拖动裁剪框和调整四角大小
- ✅ 自动图片压缩和优化，防止内存溢出

### 隐私保护
- ✅ 敏感信息默认隐藏显示
- ✅ 生物认证（指纹/面容）解锁查看敏感信息
- ✅ 一键复制识别结果
- ✅ 全部隐藏功能

### 历史记录
- ✅ 自动保存识别历史到本地数据库
- ✅ 查看历史识别记录
- ✅ 点击历史记录快速查看详情
- ✅ 删除单条或清空全部历史

### 其他
- ✅ 通过BuildConfig安全管理API密钥
- ✅ Material Design UI设计
- ✅ 流畅的页面切换动画

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

**注意：** `.env` 文件已添加到 `.gitignore`，不会被提交到版本控制系统。

## 项目结构

```
app/src/main/
├── java/com/example/idacardocr/
│   ├── MainActivity.java              # 主页面
│   ├── IdCardActivity.java            # 身份证识别页面
│   ├── BankCardActivity.java          # 银行卡识别页面
│   ├── adapter/
│   │   ├── ResultFieldAdapter.java    # 识别结果列表适配器
│   │   └── HistoryAdapter.java        # 历史记录列表适配器
│   ├── db/
│   │   └── RecognitionHistoryDbHelper.java  # 历史记录数据库
│   ├── model/
│   │   ├── ResultField.java           # 识别结果字段模型
│   │   └── RecognitionHistory.java    # 历史记录模型
│   ├── view/
│   │   └── CropImageView.java         # 自定义裁剪视图
│   └── utils/
│       ├── TencentOCRClient.java      # 腾讯云OCR API客户端
│       ├── TencentCloudSigner.java    # API签名工具
│       ├── ImageUtils.java            # 图片处理工具
│       └── BiometricHelper.java       # 生物认证工具
└── res/
    ├── layout/
    │   ├── activity_main.xml          # 主页面布局
    │   ├── activity_id_card.xml       # 身份证识别布局
    │   ├── activity_bank_card.xml     # 银行卡识别布局
    │   ├── dialog_crop.xml            # 裁剪对话框布局
    │   ├── dialog_history.xml         # 历史记录对话框布局
    │   ├── item_result_field.xml      # 结果字段项布局
    │   └── item_history.xml           # 历史记录项布局
    ├── drawable/                       # 图标资源
    └── anim/                           # 动画资源

.env                                   # API密钥配置文件（根目录）
```

## 依赖库

- OkHttp 4.12.0 - HTTP网络请求
- Gson 2.10.1 - JSON解析
- Material Components - UI组件
- AndroidX Biometric - 生物认证

## 使用说明

### 身份证识别
1. 在主页点击"身份证识别"进入识别页面
2. 选择识别模式：单面识别或双面识别
3. 点击上传按钮，选择拍照或从相册选择
4. 在裁剪界面调整裁剪框，选取卡片区域后点击"确定"
5. 点击"开始"进行识别
6. 查看识别结果，敏感信息需要生物认证后查看
7. 点击右上角历史图标可查看历史记录

### 银行卡识别
1. 在主页点击"银行卡识别"进入识别页面
2. 点击上传按钮，选择拍照或从相册选择
3. 在裁剪界面调整裁剪框，选取卡片区域后点击"确定"
4. 点击"开始识别"进行识别
5. 查看识别结果
6. 点击右上角历史图标可查看历史记录

### 裁剪功能
- 拖动裁剪框中间区域可移动位置
- 拖动四个角可调整裁剪区域大小
- 裁剪框外部显示半透明遮罩

## 权限说明

- `INTERNET` - 网络访问
- `READ_EXTERNAL_STORAGE` - 读取相册图片
- `CAMERA` - 相机拍照
- `USE_BIOMETRIC` - 生物认证

## 注意事项

1. 图片要求：
   - 格式：PNG、JPG、JPEG、BMP
   - 建议使用裁剪功能选取卡片区域，提高识别准确率

2. API调用限制：
   - 身份证识别：20次/秒
   - 银行卡识别：10次/秒

3. 安全建议：
   - 不要将API密钥硬编码在代码中
   - 不要将包含真实密钥的.env文件提交到版本控制

4. 隐私保护：
   - 敏感信息（姓名、身份证号、地址、卡号）默认隐藏
   - 需要通过生物认证才能查看
   - 历史记录存储在本地SQLite数据库

## License

MIT License
