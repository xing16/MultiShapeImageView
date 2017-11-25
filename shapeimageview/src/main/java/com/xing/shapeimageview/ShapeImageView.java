package com.xing.shapeimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.text.BoringLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewDebug;

/**
 * Created by Administrator on 2017/11/18.
 */


public class ShapeImageView extends AppCompatImageView {

    private static final String TAG = "ShapeImageView";


    private static final int SHAPE_CIRCLE = 0;

    private static final int SHAPE_ROUND_RECT = 1;

    private static final int SHAPE_BEZIER = 2;

    private int shape;

    private float borderRadius;

    private Paint paint;

    private int mViewWidth;

    private int mViewHeight;

    private float circleRadius;

    private BitmapShader bitmapShader;

    private Matrix matrix;

    private int borderColor;

    private Paint borderPaint;

    private float borderWidth;

    private float halfBorderWidth;

    private RectF roundRect;

    private RectF borderRect;

    private int bezierArcHeight;
    private Path path;
    private int arcDirection;


    public ShapeImageView(Context context) {
        this(context, null);
    }

    public ShapeImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShapeImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttrs(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        matrix = new Matrix();

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(borderWidth);
        borderPaint.setStyle(Paint.Style.STROKE);

    }

    private void readAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShapeImageView);
        shape = typedArray.getInt(R.styleable.ShapeImageView_shape, 0);
        borderRadius = typedArray.getDimension(R.styleable.ShapeImageView_borderRadius, dp2Px(10));
        borderWidth = typedArray.getDimension(R.styleable.ShapeImageView_borderWidth, dp2Px(0));
        borderColor = typedArray.getColor(R.styleable.ShapeImageView_borderColor, 0xffffffff);
        bezierArcHeight = (int) typedArray.getDimension(R.styleable.ShapeImageView_arcHeight, dp2Px(0));
        arcDirection = typedArray.getInt(R.styleable.ShapeImageView_direction, 0);
        halfBorderWidth = borderWidth / 2f;
        typedArray.recycle();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (shape == SHAPE_CIRCLE) {
            mViewWidth = mViewHeight = Math.min(getMeasuredWidth(), getMeasuredHeight());
            circleRadius = mViewWidth / 2f;
        } else {
            mViewWidth = getMeasuredWidth();
            mViewHeight = getMeasuredHeight();
        }
        setMeasuredDimension(mViewWidth, mViewHeight);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        roundRect = new RectF(borderWidth, borderWidth, w - borderWidth, h - borderWidth);
        Log.d(TAG, "onSizeChanged: borderWidth = " + borderWidth);
        borderRect = new RectF(halfBorderWidth, halfBorderWidth, w - halfBorderWidth, h - halfBorderWidth);

        if (bezierArcHeight > h) {
            bezierArcHeight = h;
        }
        path = new Path();
        path.moveTo(halfBorderWidth, halfBorderWidth);
        if (arcDirection == 0) {
            path.lineTo(halfBorderWidth, mViewHeight - halfBorderWidth);
            path.quadTo(mViewWidth / 2f, mViewHeight - halfBorderWidth - bezierArcHeight, mViewWidth - halfBorderWidth, mViewHeight - halfBorderWidth);
        } else if (arcDirection == 1) {
            path.lineTo(halfBorderWidth, mViewHeight - dp2Px(bezierArcHeight) - halfBorderWidth);
            path.quadTo(mViewWidth / 2f, mViewHeight - halfBorderWidth, mViewWidth - halfBorderWidth, mViewHeight - dp2Px(bezierArcHeight) - halfBorderWidth);
        }
        path.lineTo(mViewWidth - halfBorderWidth, halfBorderWidth);
        path.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        setBitmapShader();
        if (shape == SHAPE_CIRCLE) {
            canvas.drawCircle(circleRadius, circleRadius, circleRadius - borderWidth, paint);
            // 绘制圆形边框
            canvas.drawCircle(circleRadius, circleRadius, circleRadius - halfBorderWidth, borderPaint);
        } else if (shape == SHAPE_ROUND_RECT) {
            canvas.drawRoundRect(roundRect, borderRadius, borderRadius, paint);
            // 绘制矩形边框
            if (borderWidth != 0) {
                canvas.drawRoundRect(borderRect, borderRadius, borderRadius, borderPaint);
            }
//
        } else if (shape == SHAPE_BEZIER) {
            canvas.drawPath(path, paint);
            if (borderWidth != 0) {
                canvas.drawPath(path, borderPaint);
            }
        }
    }


    /**
     * drawable 转换成 bitmap
     *
     * @return
     */
    private Bitmap getBitmapFromDrawable() {
        // 获取 ImageView 设置的 src 属性值
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        // 创建一个空的 bitmap 对象
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(30, 0, width, height);
//        canvas.translate(width / 2, (width - height) / 2);
        drawable.draw(canvas);
        return bitmap;
    }

    private void setBitmapShader() {
        setScaleType(ScaleType.CENTER_CROP);
        Bitmap bitmap = getBitmapFromDrawable();
        bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = 1.0f;
        // 如果是圆形，取 bitmap 宽高的最小值
        if (shape == SHAPE_CIRCLE) {
            int minSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
            // 根据圆形 ImageView 控件的宽高和 ImageView 设置的 bitmap 宽高计算缩放比例
            // 当 scale> 1 时，放大 bitmap; 即 ImageView 控件宽高大于 bitmap 时，需要放大 bitmap以填满整个 ImageView控件
            scale = mViewWidth * 1.0f / minSize;
            Log.d(TAG, "setBitmapShader: scale=" + scale);
        } else {
            scale = Math.max(mViewWidth * 1.0f / bitmap.getWidth(), mViewHeight * 1.0f / bitmap.getHeight());
            Log.d(TAG, "Rect: scale=" + scale);
        }
        // 矩阵设置缩放比例，用于bitmap放大还是缩小
        matrix.setScale(scale, scale);
        bitmapShader.setLocalMatrix(matrix);
        // 画笔设置shader
        paint.setShader(bitmapShader);
    }


    private int dp2Px(int dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
    }

}
