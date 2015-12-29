package org.droidplanner.android.ruan;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.droidplanner.android.R;

/**
 * TODO: document your custom view class.
 */
public class RcExpoView extends View {

    private TextPaint mTextPaint;

    public static final int THR_TYPE_CURVE = 1;
    public static final int MIDDLE_TYPE_CURVE = 2;
    private final float curveHeight = 200;
    private final float curveWidth = 200;

    private int paramK = 0 ; //-100 == 0 == 100
    private  int curveType = MIDDLE_TYPE_CURVE;
    private Paint mPaint ;

    public void setParamK(int k){
        paramK = k;
        if( paramK > 100) paramK=100;
        if( paramK < -100 ) paramK = -100;
        invalidate();
    }
    public int getParamK(){return paramK;}

    public void setCurveType(int type){ curveType = type;invalidate();}
    public int getCurveType(){ return curveType;};



    public RcExpoView(Context context) {
        super(context);
        init(null, 0);
    }

    public RcExpoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public RcExpoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.RcExpoView, defStyle, 0);

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);


        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);

    }

    private void invalidateTextPaintAndMeasurements() {

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


        canvas.drawColor(Color.WHITE); //background

        // x y line
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth((float) 5.0);
        canvas.drawLine(0,contentHeight ,contentWidth,contentHeight,mPaint);
        canvas.drawLine(0,0 ,0,contentHeight,mPaint);

        // curves
        Expo expo = new Expo();
        float posX = 0;
        float posY = contentHeight;
        float xagain = 1024/curveWidth;
        float yagain = 1024/curveHeight;
        float px,py,y;
        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth((float) 5.0);
        if( curveType == MIDDLE_TYPE_CURVE){
            xagain = 2000/curveWidth;
            yagain = 2000/curveHeight;
            for( int x =-1000; x< 1000 ; x+=xagain){
                y=expo.expo(x,paramK);
                // Log.e("Ruan", "point " + x + "," + y);
                px = (x+1000)/xagain;
                py = posY - (y+1000)/yagain;
                canvas.drawPoint(px, py, mPaint);
            }
        }else{
            for( int x =0; x< 1000 ; x+=xagain){
                y=expo.expo(x,paramK);
                // Log.e("Ruan", "point " + x + "," + y);
                px = x/xagain;
                py = posY - y/yagain;
                canvas.drawPoint(px, py, mPaint);
            }
        }

        // Draw the text.
        mTextPaint.setTextSize((float) 25.0);
        mTextPaint.setColor(Color.BLACK);
        String detialText = this.getContext().getString(R.string.detial_rc_setting_expo_paramk)+" " +paramK+",   ";
        if( getParamK() == 0){
            detialText = detialText+ this.getContext().getString(R.string.rc_setting_curve_type_line);
        }else{
            if( getCurveType() == MIDDLE_TYPE_CURVE){
                detialText = detialText+ this.getContext().getString(R.string.rc_setting_curve_type_middle);
            }else{
                detialText = detialText+this.getContext().getString(R.string.rc_setting_curve_type_thr);
            }
        }
        float mTextWidth = mTextPaint.measureText(detialText);
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        float mTextHeight = fontMetrics.bottom;
        canvas.drawText(detialText,
                paddingLeft + 10,
                contentHeight - curveHeight - mTextHeight - 20,
                mTextPaint);
    }
}
