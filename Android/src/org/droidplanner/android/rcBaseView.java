package org.droidplanner.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;


/**
 * TODO: document your custom view class.
 */
public class rcBaseView extends View implements View.OnTouchListener, View.OnKeyListener {
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;
    private Paint pointPaint;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;
    private  int pointT = 0;
    private  int pointB = 0;
    private  int pointR = 0;
    private  int pointL = 0;
    private float pointX = 0;
    private float pointY = 0;
    private int pointRadius = 20;


    private static final String TAG = rcBaseView.class.getSimpleName();

    public rcBaseView(Context context) {
        super(context);
        init(null, 0);
    }

    public rcBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public rcBaseView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.rcBaseView, defStyle, 0);

        mExampleString = a.getString(
                R.styleable.rcBaseView_exampleString);
        mExampleColor = a.getColor(
                R.styleable.rcBaseView_exampleColor,
                mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.rcBaseView_exampleDimension,
                mExampleDimension);

        if (a.hasValue(R.styleable.rcBaseView_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                    R.styleable.rcBaseView_exampleDrawable);
            mExampleDrawable.setCallback(this);
        }

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(R.dimen.abc_text_size_small_material);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setColor(Color.BLACK);
        pointX=getWidth()/2;
        pointY = getHeight()/2;


        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
        this.setOnTouchListener(this);
        this.setOnKeyListener(this);
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        int pointH = 60;
        int pointW = 60;
        if( pointB==0 && pointT == 0){
            //pointT = paddingTop+(contentHeight-pointH) /2;
            //pointB = paddingBottom+(contentHeight-pointH) /2;
            //pointL = paddingLeft + (contentWidth-pointW) /2;
            //pointR = paddingRight + (contentWidth-pointW) /2;

            pointT = (getHeight()-pointH) /2;
            pointB = (getHeight()-pointH) /2;
           pointL = (getWidth()-pointW) /2;
            pointR = (getWidth()-pointW) /2;
        }

        // Draw the text.
        //canvas.drawText(mExampleString, paddingLeft,paddingTop+mTextHeight,mTextPaint);

        // Draw the example drawable on top of the text.
        if (mExampleDrawable != null) {
            mExampleDrawable.setBounds(pointL, pointT, pointR,pointB);
            mExampleDrawable.draw(canvas);
        }
        canvas.drawCircle(pointX,pointY,pointRadius,pointPaint);
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.i(TAG, "down in x,y=" +event.getRawX()+","+ event.getRawY() );
                Log.i(TAG,"content top="+ this.getTop());
                break;
            case MotionEvent.ACTION_MOVE:
                Log.i(TAG, "move to x,y=" + event.getRawX() + "," + event.getRawY());
                pointX = event.getX();
                pointY = event.getY();
                postInvalidate();
                break;
            case MotionEvent.ACTION_UP:
                Log.i(TAG, "up in x,y=" +event.getRawX()+","+ event.getRawY() );
                break;
        }
        return false;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }
}
