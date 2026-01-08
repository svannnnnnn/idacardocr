package com.example.idacardocr.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * 简单的裁剪ImageView
 * 支持拖动和缩放裁剪框
 */
public class CropImageView extends AppCompatImageView {

    private Paint borderPaint;
    private Paint overlayPaint;
    private RectF cropRect;
    private float minCropSize = 100f;
    
    // 触摸相关
    private int touchMode = TOUCH_NONE;
    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_DRAG = 1;
    private static final int TOUCH_RESIZE = 2;
    
    private float lastX, lastY;
    private int resizeCorner = 0; // 1=左上, 2=右上, 3=左下, 4=右下
    private float cornerTouchRadius = 60f;

    public CropImageView(Context context) {
        super(context);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        
        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#80000000"));
        overlayPaint.setStyle(Paint.Style.FILL);
        
        setScaleType(ScaleType.FIT_CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initCropRect();
    }

    private void initCropRect() {
        int w = getWidth();
        int h = getHeight();
        if (w > 0 && h > 0) {
            // 默认裁剪框为视图中心80%区域
            float margin = Math.min(w, h) * 0.1f;
            cropRect = new RectF(margin, margin, w - margin, h - margin);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (cropRect == null) return;
        
        int w = getWidth();
        int h = getHeight();
        
        // 绘制半透明遮罩（裁剪框外部）
        canvas.drawRect(0, 0, w, cropRect.top, overlayPaint);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint);
        canvas.drawRect(cropRect.right, cropRect.top, w, cropRect.bottom, overlayPaint);
        canvas.drawRect(0, cropRect.bottom, w, h, overlayPaint);
        
        // 绘制裁剪框边框
        canvas.drawRect(cropRect, borderPaint);
        
        // 绘制四个角的拖动点
        float cornerSize = 30f;
        borderPaint.setStrokeWidth(6f);
        // 左上
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + cornerSize, cropRect.top, borderPaint);
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + cornerSize, borderPaint);
        // 右上
        canvas.drawLine(cropRect.right - cornerSize, cropRect.top, cropRect.right, cropRect.top, borderPaint);
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + cornerSize, borderPaint);
        // 左下
        canvas.drawLine(cropRect.left, cropRect.bottom - cornerSize, cropRect.left, cropRect.bottom, borderPaint);
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + cornerSize, cropRect.bottom, borderPaint);
        // 右下
        canvas.drawLine(cropRect.right - cornerSize, cropRect.bottom, cropRect.right, cropRect.bottom, borderPaint);
        canvas.drawLine(cropRect.right, cropRect.bottom - cornerSize, cropRect.right, cropRect.bottom, borderPaint);
        borderPaint.setStrokeWidth(4f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                resizeCorner = getTouchedCorner(x, y);
                if (resizeCorner > 0) {
                    touchMode = TOUCH_RESIZE;
                } else if (cropRect != null && cropRect.contains(x, y)) {
                    touchMode = TOUCH_DRAG;
                } else {
                    touchMode = TOUCH_NONE;
                }
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (cropRect == null) return true;
                
                float dx = x - lastX;
                float dy = y - lastY;
                
                if (touchMode == TOUCH_DRAG) {
                    moveCropRect(dx, dy);
                } else if (touchMode == TOUCH_RESIZE) {
                    resizeCropRect(resizeCorner, dx, dy);
                }
                
                lastX = x;
                lastY = y;
                invalidate();
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchMode = TOUCH_NONE;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int getTouchedCorner(float x, float y) {
        if (cropRect == null) return 0;
        
        if (distance(x, y, cropRect.left, cropRect.top) < cornerTouchRadius) return 1;
        if (distance(x, y, cropRect.right, cropRect.top) < cornerTouchRadius) return 2;
        if (distance(x, y, cropRect.left, cropRect.bottom) < cornerTouchRadius) return 3;
        if (distance(x, y, cropRect.right, cropRect.bottom) < cornerTouchRadius) return 4;
        return 0;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    private void moveCropRect(float dx, float dy) {
        float newLeft = cropRect.left + dx;
        float newTop = cropRect.top + dy;
        float newRight = cropRect.right + dx;
        float newBottom = cropRect.bottom + dy;
        
        // 边界检查
        if (newLeft < 0) { newRight -= newLeft; newLeft = 0; }
        if (newTop < 0) { newBottom -= newTop; newTop = 0; }
        if (newRight > getWidth()) { newLeft -= (newRight - getWidth()); newRight = getWidth(); }
        if (newBottom > getHeight()) { newTop -= (newBottom - getHeight()); newBottom = getHeight(); }
        
        cropRect.set(newLeft, newTop, newRight, newBottom);
    }

    private void resizeCropRect(int corner, float dx, float dy) {
        float newLeft = cropRect.left;
        float newTop = cropRect.top;
        float newRight = cropRect.right;
        float newBottom = cropRect.bottom;
        
        switch (corner) {
            case 1: // 左上
                newLeft += dx;
                newTop += dy;
                break;
            case 2: // 右上
                newRight += dx;
                newTop += dy;
                break;
            case 3: // 左下
                newLeft += dx;
                newBottom += dy;
                break;
            case 4: // 右下
                newRight += dx;
                newBottom += dy;
                break;
        }
        
        // 最小尺寸和边界检查
        if (newRight - newLeft >= minCropSize && newBottom - newTop >= minCropSize) {
            newLeft = Math.max(0, newLeft);
            newTop = Math.max(0, newTop);
            newRight = Math.min(getWidth(), newRight);
            newBottom = Math.min(getHeight(), newBottom);
            
            if (newRight - newLeft >= minCropSize && newBottom - newTop >= minCropSize) {
                cropRect.set(newLeft, newTop, newRight, newBottom);
            }
        }
    }

    /**
     * 获取裁剪后的Bitmap
     */
    public Bitmap getCroppedBitmap() {
        if (cropRect == null || getDrawable() == null) return null;
        
        // 启用绘图缓存
        setDrawingCacheEnabled(true);
        buildDrawingCache();
        Bitmap viewBitmap = getDrawingCache();
        
        if (viewBitmap == null) return null;
        
        // 计算裁剪区域
        int left = (int) Math.max(0, cropRect.left);
        int top = (int) Math.max(0, cropRect.top);
        int width = (int) Math.min(cropRect.width(), viewBitmap.getWidth() - left);
        int height = (int) Math.min(cropRect.height(), viewBitmap.getHeight() - top);
        
        if (width <= 0 || height <= 0) {
            setDrawingCacheEnabled(false);
            return null;
        }
        
        Bitmap croppedBitmap = Bitmap.createBitmap(viewBitmap, left, top, width, height);
        setDrawingCacheEnabled(false);
        
        return croppedBitmap;
    }

    /**
     * 重置裁剪框
     */
    public void resetCropRect() {
        initCropRect();
        invalidate();
    }
}
