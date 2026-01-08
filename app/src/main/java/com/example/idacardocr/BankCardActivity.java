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

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BankCardActivity extends AppCompatActivity implements ResultFieldAdapter.OnRevealRequestListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private ImageView ivPreview;
    private TextView tvPlaceholder, tvResultHint, tvHideAll;
    private MaterialButton btnUpload, btnRecognize, btnReupload, btnCancel;
    private LinearLayout layoutActions;
    private RecyclerView rvResult;
    private ResultFieldAdapter resultAdapter;
    private TencentOCRClient ocrClient;
    private Uri currentPhotoUri;
    private Bitmap pendingBitmap;
    private int pendingRevealPosition = -1;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private RecognitionHistoryDbHelper dbHelper;
    private static final String TYPE_BANK_CARD = "BANK_CARD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_card);
        dbHelper = RecognitionHistoryDbHelper.getInstance(this);
        initViews();
        initOCRClient();
        setupActivityResultLaunchers();
        checkPermissions();
    }

    private void initViews() {
        ivPreview = findViewById(R.id.ivPreview);
        tvPlaceholder = findViewById(R.id.tvPlaceholder);
        tvResultHint = findViewById(R.id.tvResultHint);
        tvHideAll = findViewById(R.id.tvHideAll);
        btnUpload = findViewById(R.id.btnUpload);
        btnRecognize = findViewById(R.id.btnRecognize);
        btnReupload = findViewById(R.id.btnReupload);
        btnCancel = findViewById(R.id.btnCancel);
        layoutActions = findViewById(R.id.layoutActions);
        rvResult = findViewById(R.id.rvResult);
        
        rvResult.setLayoutManager(new LinearLayoutManager(this));
        resultAdapter = new ResultFieldAdapter(this, this);
        rvResult.setAdapter(resultAdapter);
        
        tvHideAll.setOnClickListener(v -> resultAdapter.hideAllFields());
        findViewById(R.id.btnBackNav).setOnClickListener(v -> finishWithAnimation());
        btnUpload.setOnClickListener(v -> showImagePickerDialog());
        btnRecognize.setOnClickListener(v -> startRecognition());
        btnReupload.setOnClickListener(v -> showImagePickerDialog());
        btnCancel.setOnClickListener(v -> resetToInitialState());
        findViewById(R.id.btnHistory).setOnClickListener(v -> showHistoryDialog());
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
            
            cropDialog.findViewById(R.id.btnCropCancel).setOnClickListener(v -> cropDialog.dismiss());
            
            cropDialog.findViewById(R.id.btnCropConfirm).setOnClickListener(v -> {
                Bitmap croppedBitmap = cropImageView.getCroppedBitmap();
                cropDialog.dismiss();
                if (croppedBitmap != null) {
                    displayCroppedImage(croppedBitmap);
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
    
    private void displayCroppedImage(Bitmap bitmap) {
        pendingBitmap = bitmap;
        ivPreview.setImageBitmap(bitmap);
        tvPlaceholder.setVisibility(View.GONE);
        btnUpload.setVisibility(View.GONE);
        layoutActions.setVisibility(View.VISIBLE);
        showResultHint("图片已裁剪，点击\"开始识别\"进行识别");
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
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
        return File.createTempFile("BANK_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void previewImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = ImageUtils.getBitmapFromInputStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(this, "无法解析图片", Toast.LENGTH_SHORT).show();
                return;
            }
            bitmap = ImageUtils.compressBitmap(bitmap, 1600);
            pendingBitmap = bitmap;
            ivPreview.setImageBitmap(bitmap);
            tvPlaceholder.setVisibility(View.GONE);
            btnUpload.setVisibility(View.GONE);
            layoutActions.setVisibility(View.VISIBLE);
            showResultHint("图片已上传，点击\"开始识别\"进行识别");
        } catch (Exception e) { Toast.makeText(this, "加载图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
    }

    private void startRecognition() {
        if (pendingBitmap == null) { Toast.makeText(this, "请先上传图片", Toast.LENGTH_SHORT).show(); return; }
        showResultHint("正在识别...");
        btnRecognize.setEnabled(false);
        new Thread(() -> {
            try {
                String base64 = ImageUtils.bitmapToBase64(pendingBitmap);
                String result = ocrClient.recognizeBankCard(base64);
                runOnUiThread(() -> { displayResult(result); btnRecognize.setEnabled(true); });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showResultHint("识别失败: " + e.getMessage());
                    btnRecognize.setEnabled(true);
                    Toast.makeText(this, "识别失败", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void resetToInitialState() {
        pendingBitmap = null;
        ivPreview.setImageBitmap(null);
        tvPlaceholder.setVisibility(View.VISIBLE);
        showResultHint("识别结果将显示在这里");
        rvResult.setVisibility(View.GONE);
        tvHideAll.setVisibility(View.GONE);
        btnUpload.setVisibility(View.VISIBLE);
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

    private void displayResult(String jsonResult) {
        try {
            JSONObject response = new JSONObject(jsonResult);
            JSONObject data = response.getJSONObject("Response");
            if (data.has("Error")) { showResultHint("错误: " + data.getJSONObject("Error").optString("Message", "")); return; }
            List<ResultField> fields = new ArrayList<>();
            String cardNo = data.optString("CardNo", "-");
            String summary = cardNo.length() > 4 ? "**** " + cardNo.substring(cardNo.length() - 4) : cardNo;
            fields.add(new ResultField("卡号", cardNo, true));
            fields.add(new ResultField("银行", data.optString("BankInfo", "-"), false));
            fields.add(new ResultField("卡类型", data.optString("CardType", "-"), false));
            fields.add(new ResultField("卡名称", data.optString("CardName", "-"), false));
            fields.add(new ResultField("有效期", data.optString("ValidDate", "-"), false));
            // 保存到历史记录
            dbHelper.insertHistory(TYPE_BANK_CARD, null, jsonResult, summary);
            showResultFields(fields);
        } catch (Exception e) { showResultHint("解析失败: " + e.getMessage()); }
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

        List<RecognitionHistory> historyList = dbHelper.getHistoryByType(TYPE_BANK_CARD, 50);
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
                        dbHelper.clearHistoryByType(TYPE_BANK_CARD);
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
            JSONObject response = new JSONObject(history.getResultJson());
            JSONObject data = response.getJSONObject("Response");
            List<ResultField> fields = new ArrayList<>();
            fields.add(new ResultField("卡号", data.optString("CardNo", "-"), true));
            fields.add(new ResultField("银行", data.optString("BankInfo", "-"), false));
            fields.add(new ResultField("卡类型", data.optString("CardType", "-"), false));
            fields.add(new ResultField("卡名称", data.optString("CardName", "-"), false));
            fields.add(new ResultField("有效期", data.optString("ValidDate", "-"), false));
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
                @Override public void onError(String message) { Toast.makeText(BankCardActivity.this, "认证失败: " + message, Toast.LENGTH_SHORT).show(); }
                @Override public void onFailed() { Toast.makeText(BankCardActivity.this, "认证失败，请重试", Toast.LENGTH_SHORT).show(); }
            });
        } else { Toast.makeText(this, "设备不支持生物认证", Toast.LENGTH_SHORT).show(); }
    }
}
