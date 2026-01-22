package com.ts.phi.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 自定义View，AI圆角view
 */
public class TextCircleView extends View {
    public static final int DEFAUT_TEXT_SIZE = 60;
    public static final int STROKE_WIDTH = 4;
    private String mText = "AI";
    private int mColor = Color.parseColor("#FF6929F1");
    private Paint mPaint;

    public TextCircleView(Context context) {
        this(context, null);
    }

    public TextCircleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextCircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(STROKE_WIDTH);
        canvas.drawCircle(getWidth() / 2,
                getHeight() / 2,
                getWidth() / 2 - STROKE_WIDTH,
                mPaint);

        if (!TextUtils.isEmpty(mText)) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mColor);
            mPaint.setTextSize(DEFAUT_TEXT_SIZE);
            mPaint.setAntiAlias(true);
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
            float baseline = centerY - (fontMetrics.top + fontMetrics.bottom) / 2f;
            Rect textBounds = new Rect();
            mPaint.getTextBounds(mText, 0, mText.length(), textBounds);
            int xPosition = (int) (centerX - textBounds.width() / 2);
            canvas.drawText(mText, xPosition, baseline, mPaint);
        }
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
        invalidate();
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        this.mColor = color;
        invalidate();
    }
}
