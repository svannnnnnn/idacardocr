package com.example.idacardocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.idacardocr.utils.TencentOCRClient;
import com.example.idacardocr.utils.ImageUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private ImageView ivIdCard, ivBankCard;
    private TextView tvIdCardResult, tvBankCardResult;
    private Button btnSelectIdCardFront, btnSelectIdCardBack, btnSelectBankCard;
    private Button btnCameraIdCardFront, btnCameraIdCardBack, btnCameraBankCard;
    
    private TencentOCRClient ocrClient;
    private String currentIdCardSide = "FRONT"; // FRONT or BACK
    private Uri currentPhotoUri;
    private int currentCameraType = 0; // 0: 身份证, 1: 银行卡
    
    // Activity Result Launchers
    private ActivityResultLauncher<Intent> idCardLauncher;
    private ActivityResultLauncher<Intent> bankCardLauncher;
    private ActivityResultLauncher<Intent> cameraIdCardLauncher;
    private ActivityResultLauncher<Intent> cameraBankCardLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        initViews();
        initOCRClient();
        setupActivityResultLaunchers();
        checkPermissions();
    }

    private void initViews() {
        ivIdCard = findViewById(R.id.ivIdCard);
        ivBankCard = findViewById(R.id.ivBankCard);
        tvIdCardResult = findViewById(R.id.tvIdCardResult);
        tvBankCardResult = findViewById(R.id.tvBankCardResult);
        btnSelectIdCardFront = findViewById(R.id.btnSelectIdCardFront);
        btnSelectIdCardBack = findViewById(R.id.btnSelectIdCardBack);
        btnSelectBankCard = findViewById(R.id.btnSelectBankCard);
        btnCameraIdCardFront = findViewById(R.id.btnCameraIdCardFront);
        btnCameraIdCardBack = findViewById(R.id.btnCameraIdCardBack);
        btnCameraBankCard = findViewById(R.id.btnCameraBankCard);
        
        // 选择图片按钮
        btnSelectIdCardFront.setOnClickListener(v -> {
            currentIdCardSide = "FRONT";
            openImagePicker(idCardLauncher);
        });
        
        btnSelectIdCardBack.setOnClickListener(v -> {
            currentIdCardSide = "BACK";
            openImagePicker(idCardLauncher);
        });
        
        btnSelectBankCard.setOnClickListener(v -> openImagePicker(bankCardLauncher));
        
        // 拍照按钮
        btnCameraIdCardFront.setOnClickListener(v -> {
            currentIdCardSide = "FRONT";
            openCamera(cameraIdCardLauncher);
        });
        
        btnCameraIdCardBack.setOnClickListener(v -> {
            currentIdCardSide = "BACK";
            openCamera(cameraIdCardLauncher);
        });
        
        btnCameraBankCard.setOnClickListener(v -> openCamera(cameraBankCardLauncher));
    }

    private void initOCRClient() {
        try {
            ocrClient = new TencentOCRClient();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "初始化OCR客户端失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupActivityResultLaunchers() {
        // 身份证图片选择器
        idCardLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        processIdCardImage(imageUri);
                    }
                }
        );
        
        // 银行卡图片选择器
        bankCardLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        processBankCardImage(imageUri);
                    }
                }
        );
        
        // 身份证拍照
        cameraIdCardLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentPhotoUri != null) {
                        processIdCardImage(currentPhotoUri);
                    }
                }
        );
        
        // 银行卡拍照
        cameraBankCardLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentPhotoUri != null) {
                        processBankCardImage(currentPhotoUri);
                    }
                }
        );
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        };
        
        boolean needRequest = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }
        
        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void openImagePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcher.launch(intent);
    }

    private void openCamera(ActivityResultLauncher<Intent> launcher) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先授予相机权限", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            return;
        }
        
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                launcher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "OCR_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void processIdCardImage(Uri imageUri) {
        tvIdCardResult.setText("正在识别...");
        
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = ImageUtils.getBitmapFromInputStream(inputStream);
                bitmap = ImageUtils.compressBitmap(bitmap, 1600);
                
                String base64Image = ImageUtils.bitmapToBase64(bitmap);
                String result = ocrClient.recognizeIDCard(base64Image, currentIdCardSide);
                
                Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> {
                    ivIdCard.setImageBitmap(finalBitmap);
                    displayIdCardResult(result);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvIdCardResult.setText("识别失败: " + e.getMessage());
                    Toast.makeText(this, "识别失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void processBankCardImage(Uri imageUri) {
        tvBankCardResult.setText("正在识别...");
        
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = ImageUtils.getBitmapFromInputStream(inputStream);
                bitmap = ImageUtils.compressBitmap(bitmap, 1600);
                
                String base64Image = ImageUtils.bitmapToBase64(bitmap);
                String result = ocrClient.recognizeBankCard(base64Image);
                
                Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> {
                    ivBankCard.setImageBitmap(finalBitmap);
                    displayBankCardResult(result);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvBankCardResult.setText("识别失败: " + e.getMessage());
                    Toast.makeText(this, "识别失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void displayIdCardResult(String jsonResult) {
        try {
            JSONObject response = new JSONObject(jsonResult);
            JSONObject data = response.getJSONObject("Response");
            
            StringBuilder result = new StringBuilder();
            
            if (currentIdCardSide.equals("FRONT")) {
                // 人像面信息
                result.append("姓名: ").append(data.optString("Name", "")).append("\n");
                result.append("性别: ").append(data.optString("Sex", "")).append("\n");
                result.append("民族: ").append(data.optString("Nation", "")).append("\n");
                result.append("出生日期: ").append(data.optString("Birth", "")).append("\n");
                result.append("地址: ").append(data.optString("Address", "")).append("\n");
                result.append("身份证号: ").append(data.optString("IdNum", "")).append("\n");
            } else {
                // 国徽面信息
                result.append("签发机关: ").append(data.optString("Authority", "")).append("\n");
                result.append("有效期限: ").append(data.optString("ValidDate", "")).append("\n");
            }
            
            // 检查是否有错误
            if (data.has("Error")) {
                JSONObject error = data.getJSONObject("Error");
                result.append("\n错误: ").append(error.optString("Message", ""));
            }
            
            tvIdCardResult.setText(result.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            tvIdCardResult.setText("解析结果失败: " + e.getMessage() + "\n\n原始结果:\n" + jsonResult);
        }
    }

    private void displayBankCardResult(String jsonResult) {
        try {
            JSONObject response = new JSONObject(jsonResult);
            JSONObject data = response.getJSONObject("Response");
            
            StringBuilder result = new StringBuilder();
            result.append("卡号: ").append(data.optString("CardNo", "")).append("\n");
            result.append("银行信息: ").append(data.optString("BankInfo", "")).append("\n");
            result.append("卡类型: ").append(data.optString("CardType", "")).append("\n");
            result.append("卡名称: ").append(data.optString("CardName", "")).append("\n");
            result.append("有效期: ").append(data.optString("ValidDate", "")).append("\n");
            
            // 检查是否有错误
            if (data.has("Error")) {
                JSONObject error = data.getJSONObject("Error");
                result.append("\n错误: ").append(error.optString("Message", ""));
            }
            
            tvBankCardResult.setText(result.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            tvBankCardResult.setText("解析结果失败: " + e.getMessage() + "\n\n原始结果:\n" + jsonResult);
        }
    }
}
