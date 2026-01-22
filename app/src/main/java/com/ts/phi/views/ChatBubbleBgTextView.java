package com.ts.phi.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.ts.phi.R;

/**
 * 自定义TextView，聊天气泡背景
 */
public class ChatBubbleBgTextView extends AppCompatTextView {
    private static final int DEFAULT_ARROW_POSITION = 80;
    private Paint mPaint;
    private Path mBubblePath;
    private int mBubbleColor = Color.parseColor("#FFE1E2E4");
    private float mCornerRadius = 10f;
    private int mArrowDirection = 0; // 0:左 1:右
    private float mArrowWidth = 20f;
    private float mArrowHeight = 30f;
    private float mArrowPosition; // 箭头位置比例 0-1

    private boolean mHasBubbleBg = true;

    public ChatBubbleBgTextView(Context context) {
        this(context, null);
    }

    public ChatBubbleBgTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatBubbleBgTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBubblePath = new Path();
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs,
                    R.styleable.ChatBubbleBgTextView);
            mHasBubbleBg = typedArray.getBoolean(R.styleable.ChatBubbleBgTextView_hasBubbleBg,
                    mHasBubbleBg);
            mBubbleColor = typedArray.getColor(R.styleable.ChatBubbleBgTextView_bubbleColor,
                    mBubbleColor);
            mCornerRadius = typedArray.getDimension(R.styleable.ChatBubbleBgTextView_cornerRadius,
                    mCornerRadius);
            mArrowDirection = typedArray.getInt(R.styleable.ChatBubbleBgTextView_arrowDirection,
                    mArrowDirection);
            mArrowWidth = typedArray.getDimension(R.styleable.ChatBubbleBgTextView_arrowWidth,
                    mArrowWidth);
            mArrowHeight = typedArray.getDimension(R.styleable.ChatBubbleBgTextView_arrowHeight,
                    mArrowHeight);
            mArrowPosition = typedArray.getFloat(R.styleable.ChatBubbleBgTextView_arrowPosition,
                    mArrowPosition);
            typedArray.recycle();
        }
        mPaint.setColor(mBubbleColor);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mHasBubbleBg) {
            mBubblePath.reset();
            int width = getWidth();
            int height = getHeight();
            float arrowX, arrowY;
            if (mArrowDirection == 0) {
                arrowX = 0;
                arrowY = mArrowPosition != 0 ? height * mArrowPosition : DEFAULT_ARROW_POSITION;
                drawLeftArrowBubble(width, height, arrowX, arrowY);
            } else {
                arrowX = width;
                arrowY = mArrowPosition != 0 ? height * mArrowPosition : DEFAULT_ARROW_POSITION;
                drawRightArrowBubble(width, height, arrowX, arrowY);
            }
            canvas.drawPath(mBubblePath, mPaint);
        }
        super.onDraw(canvas);
    }

    private void drawLeftArrowBubble(int width, int height, float arrowX, float arrowY) {
        mBubblePath.moveTo(arrowX, arrowY);
        mBubblePath.lineTo(arrowX + mArrowWidth, arrowY - mArrowHeight / 2);
        mBubblePath.lineTo(arrowX + mArrowWidth, arrowY + mArrowHeight / 2);
        mBubblePath.close();
        RectF rect = new RectF(mArrowWidth, 0, width, height);
        mBubblePath.addRoundRect(rect, mCornerRadius, mCornerRadius, Path.Direction.CW);
    }

    private void drawRightArrowBubble(int width, int height, float arrowX, float arrowY) {
        RectF rect = new RectF(0, 0, width - mArrowWidth, height);
        mBubblePath.addRoundRect(rect, mCornerRadius, mCornerRadius, Path.Direction.CW);
        mBubblePath.moveTo(arrowX, arrowY);
        mBubblePath.lineTo(arrowX - mArrowWidth, arrowY - mArrowHeight / 2);
        mBubblePath.lineTo(arrowX - mArrowWidth, arrowY + mArrowHeight / 2);
        mBubblePath.close();
    }

    /**
     * 设置气泡颜色.
     *
     * @param color color
     */
    public void setBubbleColor(int color) {
        this.mBubbleColor = color;
        mPaint.setColor(color);
        invalidate();
    }

    /**
     * 设置箭头方向.
     *
     * @param direction 0:left, 1:right
     */
    public void setArrowDirection(int direction) {
        this.mArrowDirection = direction;
        invalidate();
    }

    /**
     * 设置圆角半径.
     *
     * @param radius radius
     */
    public void setCornerRadius(float radius) {
        this.mCornerRadius = radius;
        invalidate();
    }

    /**
     * 设置箭头大小.
     *
     * @param width  width
     * @param height height
     */
    public void setArrowSize(float width, float height) {
        this.mArrowWidth = width;
        this.mArrowHeight = height;
        invalidate();
    }

    /**
     * 设置箭头位置.
     *
     * @param position 0~1百分比
     */
    public void setArrowPosition(float position) {
        this.mArrowPosition = position;
        invalidate();
    }
}
