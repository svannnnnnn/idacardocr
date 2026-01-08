package com.example.idacardocr;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.idacardocr.adapter.ResultFieldAdapter;
import com.example.idacardocr.adapter.HistoryAdapter;
import com.example.idacardocr.db.RecognitionHistoryDbHelper;
import com.example.idacardocr.model.RecognitionHistory;
import com.example.idacardocr.model.ResultField;
import com.example.idacardocr.utils.BiometricHelper;
import com.example.idacardocr.utils.ImageUtils;
import com.example.idacardocr.utils.TencentOCRClient;
import com.example.idacardocr.view.CropImageView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IdCardActivity extends AppCompatActivity implements ResultFieldAdapter.OnRevealRequestListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private MaterialCardView cardSinglePreview;
    private ImageView ivPreview;
    private TextView tvPlaceholder;
    private LinearLayout layoutSingleUpload;
    private LinearLayout layoutDoublePreview, layoutDoubleUpload;
    private ImageView ivFrontPreview, ivBackPreview;
    private TextView tvFrontPlaceholder, tvBackPlaceholder;
    private TextView tvResultHint, tvHideAll;
    private RecyclerView rvResult;
    private ResultFieldAdapter resultAdapter;
    private RadioGroup rgMode;
    private LinearLayout layoutActions;
    private MaterialButton btnRecognize, btnReupload, btnCancel;
    private TencentOCRClient ocrClient;
    private boolean isDoubleMode = false;
    private String currentCardSide = "FRONT";
    private Uri currentPhotoUri;
    private int pendingRevealPosition = -1;
    private Bitmap pendingBitmap;
    private Bitmap frontBitmap, backBitmap;
    private String frontResult, backResult;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private RecognitionHistoryDbHelper dbHelper;
    private static final String TYPE_ID_CARD = "ID_CARD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_card);
        dbHelper = RecognitionHistoryDbHelper.getInstance(this);
        initViews();
        initOCRClient();
        setupActivityResultLaunchers();
        checkPermissions();
    }

    private void initViews() {
        cardSinglePreview = findViewById(R.id.cardSinglePreview);
        ivPreview = findViewById(R.id.ivPreview);
        tvPlaceholder = findViewById(R.id.tvPlaceholder);
        layoutSingleUpload = findViewById(R.id.layoutSingleUpload);
        layoutDoublePreview = findViewById(R.id.layoutDoublePreview);
        layoutDoubleUpload = findViewById(R.id.layoutDoubleUpload);
        ivFrontPreview = findViewById(R.id.ivFrontPreview);
        ivBackPreview = findViewById(R.id.ivBackPreview);
        tvFrontPlaceholder = findViewById(R.id.tvFrontPlaceholder);
        tvBackPlaceholder = findViewById(R.id.tvBackPlaceholder);
        tvResultHint = findViewById(R.id.tvResultHint);
        tvHideAll = findViewById(R.id.tvHideAll);
        rvResult = findViewById(R.id.rvResult);
        rvResult.setLayoutManager(new LinearLayoutManager(this));
        resultAdapter = new ResultFieldAdapter(this, this);
        rvResult.setAdapter(resultAdapter);
        tvHideAll.setOnClickListener(v -> resultAdapter.hideAllFields());
        rgMode = findViewById(R.id.rgMode);
        layoutActions = findViewById(R.id.layoutActions);
        btnRecognize = findViewById(R.id.btnRecognize);
        btnReupload = findViewById(R.id.btnReupload);
        btnCancel = findViewById(R.id.btnCancel);
        findViewById(R.id.btnBackNav).setOnClickListener(v -> finishWithAnimation());
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            isDoubleMode = (checkedId == R.id.rbDouble);
            switchMode();
        });
        // 上传人像面按钮点击事件
        findViewById(R.id.btnFront).setOnClickListener(v -> {
            currentCardSide = "FRONT";
            showImagePickerDialog();
        });
        // 上传国徽面按钮点击事件
        findViewById(R.id.btnBackSide).setOnClickListener(v -> {
            currentCardSide = "BACK";
            showImagePickerDialog();
        });
        findViewById(R.id.btnUploadFront).setOnClickListener(v -> {
            currentCardSide = "FRONT";
            showImagePickerDialog();
        });
        findViewById(R.id.btnUploadBack).setOnClickListener(v -> {
            currentCardSide = "BACK";
            showImagePickerDialog();
        });
        // 开始识别按钮点击事件
        btnRecognize.setOnClickListener(v -> startRecognition());
        btnReupload.setOnClickListener(v -> {
            if (isDoubleMode) showReuploadDialog();
            else { showUploadButtons(); showImagePickerDialog(); }
        });
        btnCancel.setOnClickListener(v -> resetToInitialState());
        findViewById(R.id.btnHistory).setOnClickListener(v -> showHistoryDialog());
    }

    private void switchMode() {
        resetToInitialState();
        if (isDoubleMode) {
            cardSinglePreview.setVisibility(View.GONE);
            layoutSingleUpload.setVisibility(View.GONE);
            layoutDoublePreview.setVisibility(View.VISIBLE);
            layoutDoubleUpload.setVisibility(View.VISIBLE);
        } else {
            cardSinglePreview.setVisibility(View.VISIBLE);
            layoutSingleUpload.setVisibility(View.VISIBLE);
            layoutDoublePreview.setVisibility(View.GONE);
            layoutDoubleUpload.setVisibility(View.GONE);
        }
    }

    private void initOCRClient() {
        try { ocrClient = new TencentOCRClient(); }
        catch (Exception e) { Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
    }

    private void setupActivityResultLaunchers() {
        // 相册选择后显示裁剪对话框
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri selectedUri = result.getData().getData();
                if (selectedUri != null) {
                    showCropDialog(selectedUri);
                }
            }
        });
        
        // 拍照后显示裁剪对话框
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && currentPhotoUri != null) {
                showCropDialog(currentPhotoUri);
            }
        });
    }
    
    private void showCropDialog(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法打开图片", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = ImageUtils.getBitmapFromInputStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(this, "无法解析图片", Toast.LENGTH_SHORT).show();
                return;
            }
            // 压缩图片避免OOM
            bitmap = ImageUtils.compressBitmap(bitmap, 1600);
            
            // 创建全屏裁剪对话框
            android.app.Dialog cropDialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            cropDialog.setContentView(R.layout.dialog_crop);
            cropDialog.setCancelable(false);
            
            CropImageView cropImageView = cropDialog.findViewById(R.id.cropImageView);
            cropImageView.setImageBitmap(bitmap);
            
            final boolean currentIsDoubleMode = isDoubleMode;
            
            cropDialog.findViewById(R.id.btnCropCancel).setOnClickListener(v -> cropDialog.dismiss());
            
            cropDialog.findViewById(R.id.btnCropConfirm).setOnClickListener(v -> {
                Bitmap croppedBitmap = cropImageView.getCroppedBitmap();
                cropDialog.dismiss();
                if (croppedBitmap != null) {
                    if (currentIsDoubleMode) {
                        previewDoubleMode(croppedBitmap);
                    } else {
                        previewSingleMode(croppedBitmap);
                    }
                } else {
                    Toast.makeText(this, "裁剪失败", Toast.LENGTH_SHORT).show();
                }
            });
            
            cropDialog.show();
        } catch (OutOfMemoryError e) {
            Toast.makeText(this, "图片太大，内存不足", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "加载图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermissions() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                break;
            }
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"拍照", "从相册选择", "取消"};
        new AlertDialog.Builder(this).setTitle("选择上传方式").setItems(options, (dialog, which) -> {
            if (which == 0) openCamera();
            else if (which == 1) openGallery();
        }).show();
    }

    private void showReuploadDialog() {
        String[] options = {"重新上传人像面", "重新上传国徽面", "全部重新上传", "取消"};
        new AlertDialog.Builder(this).setTitle("选择重新上传").setItems(options, (dialog, which) -> {
            if (which == 0) { currentCardSide = "FRONT"; showImagePickerDialog(); }
            else if (which == 1) { currentCardSide = "BACK"; showImagePickerDialog(); }
            else if (which == 2) resetToInitialState();
        }).show();
    }

    private void openGallery() {// 方式1：从相册选择
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {    // 方式2：调用系统相机拍照
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先授予相机权限", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                currentPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                cameraLauncher.launch(intent);
            } catch (IOException e) { Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show(); }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("ID_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void previewImage(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "图片路径无效", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法打开图片", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = ImageUtils.getBitmapFromInputStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(this, "无法解析图片", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap compressedBitmap = ImageUtils.compressBitmap(bitmap, 1600);
            if (compressedBitmap == null) {
                Toast.makeText(this, "图片压缩失败", Toast.LENGTH_SHORT).show();
                return;
            }
            // 保存当前模式状态，避免在异步操作期间状态改变
            final boolean currentIsDoubleMode = isDoubleMode;
            if (currentIsDoubleMode) {
                previewDoubleMode(compressedBitmap);
            } else {
                previewSingleMode(compressedBitmap);
            }
        } catch (OutOfMemoryError e) {
            Toast.makeText(this, "图片太大，内存不足", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { 
            Toast.makeText(this, "加载图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show(); 
        }
    }

    private void previewSingleMode(Bitmap bitmap) {
        pendingBitmap = bitmap;
        ivPreview.setImageBitmap(bitmap);
        tvPlaceholder.setVisibility(View.GONE);
        layoutSingleUpload.setVisibility(View.GONE);
        layoutActions.setVisibility(View.VISIBLE);
        String sideText = currentCardSide.equals("FRONT") ? "人像面" : "国徽面";
        showResultHint("已上传" + sideText + "，点击\"开始\"进行识别");
    }

    private void previewDoubleMode(Bitmap bitmap) {
        if (currentCardSide.equals("FRONT")) {
            frontBitmap = bitmap;
            ivFrontPreview.setImageBitmap(bitmap);
            tvFrontPlaceholder.setVisibility(View.GONE);
        } else {
            backBitmap = bitmap;
            ivBackPreview.setImageBitmap(bitmap);
            tvBackPlaceholder.setVisibility(View.GONE);
        }
        if (frontBitmap != null && backBitmap != null) {
            layoutDoubleUpload.setVisibility(View.GONE);
            layoutActions.setVisibility(View.VISIBLE);
            showResultHint("人像面和国徽面已上传，点击\"开始\"进行识别");
        } else if (frontBitmap != null) showResultHint("已上传人像面，请继续上传国徽面");
        else showResultHint("已上传国徽面，请继续上传人像面");
    }

    private void showUploadButtons() {
        if (isDoubleMode) layoutDoubleUpload.setVisibility(View.VISIBLE);
        else layoutSingleUpload.setVisibility(View.VISIBLE);
        layoutActions.setVisibility(View.GONE);
    }

    private void startRecognition() {
        if (isDoubleMode) startDoubleRecognition();
        else startSingleRecognition();
    }

    private void startSingleRecognition() {
        if (pendingBitmap == null) { Toast.makeText(this, "请先上传图片", Toast.LENGTH_SHORT).show(); return; }
        showResultHint("正在识别...");
        btnRecognize.setEnabled(false);
        new Thread(() -> {
            try {
                String base64 = ImageUtils.bitmapToBase64(pendingBitmap);
                String result = ocrClient.recognizeIDCard(base64, currentCardSide);
                runOnUiThread(() -> { displaySingleResult(result); btnRecognize.setEnabled(true); });
            } catch (Exception e) {
                runOnUiThread(() -> { showResultHint("识别失败: " + e.getMessage()); btnRecognize.setEnabled(true); });
            }
        }).start();
    }

    private volatile boolean doubleRecognitionFailed = false;
    
    private void startDoubleRecognition() {
        if (frontBitmap == null || backBitmap == null) { 
            Toast.makeText(this, "请先上传人像面和国徽面", Toast.LENGTH_SHORT).show(); 
            return; 
        }
        showResultHint("正在识别...");
        btnRecognize.setEnabled(false);
        
        // 重置状态
        frontResult = null;
        backResult = null;
        doubleRecognitionFailed = false;
        
        // 保存bitmap引用，避免在识别过程中被清空
        final Bitmap localFrontBitmap = frontBitmap;
        final Bitmap localBackBitmap = backBitmap;
        
        if (localFrontBitmap == null || localBackBitmap == null) {
            Toast.makeText(this, "图片数据丢失，请重新上传", Toast.LENGTH_SHORT).show();
            btnRecognize.setEnabled(true);
            return;
        }
        
        new Thread(() -> {
            try {
                if (doubleRecognitionFailed) return;
                String base64 = ImageUtils.bitmapToBase64(localFrontBitmap);
                String result = ocrClient.recognizeIDCard(base64, "FRONT");
                synchronized (IdCardActivity.this) {
                    if (!doubleRecognitionFailed) {
                        frontResult = result;
                        checkDoubleRecognitionComplete();
                    }
                }
            } catch (Exception e) { 
                synchronized (IdCardActivity.this) {
                    if (!doubleRecognitionFailed) {
                        doubleRecognitionFailed = true;
                        runOnUiThread(() -> { 
                            showResultHint("人像面识别失败: " + e.getMessage()); 
                            btnRecognize.setEnabled(true); 
                        });
                    }
                }
            }
        }).start();
        
        new Thread(() -> {
            try {
                if (doubleRecognitionFailed) return;
                String base64 = ImageUtils.bitmapToBase64(localBackBitmap);
                String result = ocrClient.recognizeIDCard(base64, "BACK");
                synchronized (IdCardActivity.this) {
                    if (!doubleRecognitionFailed) {
                        backResult = result;
                        checkDoubleRecognitionComplete();
                    }
                }
            } catch (Exception e) { 
                synchronized (IdCardActivity.this) {
                    if (!doubleRecognitionFailed) {
                        doubleRecognitionFailed = true;
                        runOnUiThread(() -> { 
                            showResultHint("国徽面识别失败: " + e.getMessage()); 
                            btnRecognize.setEnabled(true); 
                        });
                    }
                }
            }
        }).start();
    }

    private void checkDoubleRecognitionComplete() {
        // 已经在synchronized块中调用，无需再次同步
        if (frontResult != null && backResult != null && !doubleRecognitionFailed) {
            runOnUiThread(() -> { 
                displayDoubleResult(); 
                btnRecognize.setEnabled(true); 
            });
        }
    }

    private void resetToInitialState() {
        pendingBitmap = null; frontBitmap = null; backBitmap = null; frontResult = null; backResult = null;
        ivPreview.setImageBitmap(null);
        tvPlaceholder.setVisibility(View.VISIBLE);
        ivFrontPreview.setImageBitmap(null);
        ivBackPreview.setImageBitmap(null);
        tvFrontPlaceholder.setVisibility(View.VISIBLE);
        tvBackPlaceholder.setVisibility(View.VISIBLE);
        showResultHint("识别结果将显示在这里");
        rvResult.setVisibility(View.GONE);
        tvHideAll.setVisibility(View.GONE);
        if (isDoubleMode) layoutDoubleUpload.setVisibility(View.VISIBLE);
        else layoutSingleUpload.setVisibility(View.VISIBLE);
        layoutActions.setVisibility(View.GONE);
    }

    private void showResultHint(String hint) {
        tvResultHint.setText(hint);
        tvResultHint.setVisibility(View.VISIBLE);
        rvResult.setVisibility(View.GONE);
        tvHideAll.setVisibility(View.GONE);
    }

    private void showResultFields(List<ResultField> fields) {
        tvResultHint.setVisibility(View.GONE);
        rvResult.setVisibility(View.VISIBLE);
        tvHideAll.setVisibility(View.VISIBLE);
        resultAdapter.setFields(fields);
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void displaySingleResult(String jsonResult) {
        try {
            JSONObject response = new JSONObject(jsonResult);
            JSONObject data = response.getJSONObject("Response");
            if (data.has("Error")) { showResultHint("错误: " + data.getJSONObject("Error").optString("Message", "")); return; }
            List<ResultField> fields = new ArrayList<>();
            String summary = "";
            if (currentCardSide.equals("FRONT")) {
                String name = data.optString("Name", "-");
                summary = name;
                fields.add(new ResultField("姓名", name, true));
                fields.add(new ResultField("性别", data.optString("Sex", "-"), false));
                fields.add(new ResultField("民族", data.optString("Nation", "-"), false));
                fields.add(new ResultField("出生日期", data.optString("Birth", "-"), false));
                fields.add(new ResultField("地址", data.optString("Address", "-"), true));
                fields.add(new ResultField("身份证号", data.optString("IdNum", "-"), true));
            } else {
                summary = data.optString("Authority", "-");
                fields.add(new ResultField("签发机关", data.optString("Authority", "-"), false));
                fields.add(new ResultField("有效期限", data.optString("ValidDate", "-"), false));
            }
            // 保存到历史记录
            dbHelper.insertHistory(TYPE_ID_CARD, currentCardSide, jsonResult, summary);
            showResultFields(fields);
        } catch (Exception e) { showResultHint("解析失败: " + e.getMessage()); }
    }

    private void displayDoubleResult() {
        List<ResultField> fields = new ArrayList<>();
        String summary = "";
        try {
            JSONObject frontResponse = new JSONObject(frontResult);
            JSONObject frontData = frontResponse.getJSONObject("Response");
            summary = frontData.optString("Name", "-");
            fields.add(new ResultField("姓名", frontData.optString("Name", "-"), true));
            fields.add(new ResultField("性别", frontData.optString("Sex", "-"), false));
            fields.add(new ResultField("民族", frontData.optString("Nation", "-"), false));
            fields.add(new ResultField("出生日期", frontData.optString("Birth", "-"), false));
            fields.add(new ResultField("地址", frontData.optString("Address", "-"), true));
            fields.add(new ResultField("身份证号", frontData.optString("IdNum", "-"), true));
            JSONObject backResponse = new JSONObject(backResult);
            JSONObject backData = backResponse.getJSONObject("Response");
            fields.add(new ResultField("签发机关", backData.optString("Authority", "-"), false));
            fields.add(new ResultField("有效期限", backData.optString("ValidDate", "-"), false));
            // 保存到历史记录（合并两个结果）
            JSONObject combined = new JSONObject();
            combined.put("front", frontResult);
            combined.put("back", backResult);
            dbHelper.insertHistory(TYPE_ID_CARD, "DOUBLE", combined.toString(), summary);
        } catch (Exception e) { showResultHint("解析失败: " + e.getMessage()); return; }
        showResultFields(fields);
    }

    private void showHistoryDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_history, null);
        dialog.setContentView(view);

        RecyclerView rvHistory = view.findViewById(R.id.rvHistory);
        TextView tvEmpty = view.findViewById(R.id.tvEmpty);
        TextView tvClearAll = view.findViewById(R.id.tvClearAll);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        HistoryAdapter adapter = new HistoryAdapter(new HistoryAdapter.OnHistoryClickListener() {
            @Override
            public void onHistoryClick(RecognitionHistory history) {
                dialog.dismiss();
                displayHistoryResult(history);
            }

            @Override
            public void onHistoryDelete(RecognitionHistory history, int position) {
                dbHelper.deleteHistory(history.getId());
                ((HistoryAdapter) rvHistory.getAdapter()).removeItem(position);
                if (rvHistory.getAdapter().getItemCount() == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }
        });
        rvHistory.setAdapter(adapter);

        List<RecognitionHistory> historyList = dbHelper.getHistoryByType(TYPE_ID_CARD, 50);
        if (historyList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            adapter.setHistoryList(historyList);
        }

        tvClearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("确认清空")
                    .setMessage("确定要清空所有历史记录吗？")
                    .setPositiveButton("确定", (d, w) -> {
                        dbHelper.clearHistoryByType(TYPE_ID_CARD);
                        adapter.setHistoryList(new ArrayList<>());
                        tvEmpty.setVisibility(View.VISIBLE);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        dialog.show();
    }

    private void displayHistoryResult(RecognitionHistory history) {
        try {
            List<ResultField> fields = new ArrayList<>();
            if ("DOUBLE".equals(history.getCardSide())) {
                JSONObject combined = new JSONObject(history.getResultJson());
                JSONObject frontResponse = new JSONObject(combined.getString("front"));
                JSONObject frontData = frontResponse.getJSONObject("Response");
                fields.add(new ResultField("姓名", frontData.optString("Name", "-"), true));
                fields.add(new ResultField("性别", frontData.optString("Sex", "-"), false));
                fields.add(new ResultField("民族", frontData.optString("Nation", "-"), false));
                fields.add(new ResultField("出生日期", frontData.optString("Birth", "-"), false));
                fields.add(new ResultField("地址", frontData.optString("Address", "-"), true));
                fields.add(new ResultField("身份证号", frontData.optString("IdNum", "-"), true));
                JSONObject backResponse = new JSONObject(combined.getString("back"));
                JSONObject backData = backResponse.getJSONObject("Response");
                fields.add(new ResultField("签发机关", backData.optString("Authority", "-"), false));
                fields.add(new ResultField("有效期限", backData.optString("ValidDate", "-"), false));
            } else {
                JSONObject response = new JSONObject(history.getResultJson());
                JSONObject data = response.getJSONObject("Response");
                if ("FRONT".equals(history.getCardSide())) {
                    fields.add(new ResultField("姓名", data.optString("Name", "-"), true));
                    fields.add(new ResultField("性别", data.optString("Sex", "-"), false));
                    fields.add(new ResultField("民族", data.optString("Nation", "-"), false));
                    fields.add(new ResultField("出生日期", data.optString("Birth", "-"), false));
                    fields.add(new ResultField("地址", data.optString("Address", "-"), true));
                    fields.add(new ResultField("身份证号", data.optString("IdNum", "-"), true));
                } else {
                    fields.add(new ResultField("签发机关", data.optString("Authority", "-"), false));
                    fields.add(new ResultField("有效期限", data.optString("ValidDate", "-"), false));
                }
            }
            showResultFields(fields);
        } catch (Exception e) {
            Toast.makeText(this, "加载历史记录失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRevealRequest(int position, ResultField field) {
        pendingRevealPosition = position;
        if (BiometricHelper.canAuthenticate(this)) {
            BiometricHelper.authenticate(this, new BiometricHelper.BiometricCallback() {
                @Override public void onSuccess() { resultAdapter.revealField(pendingRevealPosition); }
                @Override public void onError(String message) { Toast.makeText(IdCardActivity.this, "认证失败: " + message, Toast.LENGTH_SHORT).show(); }
                @Override public void onFailed() { Toast.makeText(IdCardActivity.this, "认证失败，请重试", Toast.LENGTH_SHORT).show(); }
            });
        } else { Toast.makeText(this, "设备不支持生物认证", Toast.LENGTH_SHORT).show(); }
    }
}
