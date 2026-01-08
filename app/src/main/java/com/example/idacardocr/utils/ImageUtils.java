package com.example.idacardocr.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图片处理工具类
 */
public class ImageUtils {

    /**
     * 将Bitmap转换为Base64字符串
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    /**
     * 从InputStream读取Bitmap，带有采样率优化以避免OOM
     */
    public static Bitmap getBitmapFromInputStream(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            // 直接解码，如果图片太大可能会OOM
            // 调用方应该捕获OutOfMemoryError
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            return bitmap;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 压缩图片到指定大小以下
     * @param bitmap 原始图片
     * @param maxSize 最大尺寸（宽或高）
     * @return 压缩后的图片
     */
    public static Bitmap compressBitmap(Bitmap bitmap, int maxSize) {
        if (bitmap == null) {
            return null;
        }
        
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            if (width <= maxSize && height <= maxSize) {
                return bitmap;
            }
            
            float scale;
            if (width > height) {
                scale = (float) maxSize / width;
            } else {
                scale = (float) maxSize / height;
            }
            
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            
            Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            // 如果创建了新的bitmap，可以考虑回收原始bitmap（但要小心，调用方可能还在使用）
            return result;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return bitmap; // 返回原始bitmap，让调用方决定如何处理
        }
    }

    /**
     * 根据EXIF信息旋转图片
     */
    public static Bitmap rotateImageIfRequired(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            int rotate = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
