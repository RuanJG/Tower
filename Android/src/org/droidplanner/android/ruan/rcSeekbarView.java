package org.droidplanner.android.ruan;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.droidplanner.android.R;

/**
 * TODO: document your custom view class.
 */
public class rcSeekbarView extends View implements View.OnTouchListener{
    private static final String TAG = rcSeekbarView.class.getSimpleName();

    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private Paint mPaint ;
    private TextPaint mPersonPaint;
    private float mPersonPaintWidth,mPersonPaintHeight;

    private double person = 0.5;
    private int mMax=2000,mMin=1000;
    private int process = 1500;

    private int padding = 1;
    private int mRcId;
    private boolean lockValue=false;
    private int mRcTrimValue=1500;

    IRcOutputListen rcListen;

    public rcSeekbarView(Context context) {
        super(context);
        init(null, 0);
    }

    public rcSeekbarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public rcSeekbarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.rcSeekbarView, defStyle, 0);

        mExampleString = a.getString(
                R.styleable.rcSeekbarView_rcString);
        mExampleColor = a.getColor(
                R.styleable.rcSeekbarView_rcColor,
                mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(R.styleable.rcSeekbarView_rcTextDimension,mExampleDimension);
        mMax = a.getInteger(R.styleable.rcSeekbarView_Max, 2000);
        mMin = a.getInteger(R.styleable.rcSeekbarView_Min,1000);

        mRcId = a.getInteger(R.styleable.rcSeekbarView_Id,0);

        a.recycle();

        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(mExampleColor);

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(Color.BLACK);

        // Update TextPaint and text measurements from attributes
        //invalidateTextPaintAndMeasurements();


        mPersonPaint = new TextPaint();
        mPersonPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPersonPaint.setTextAlign(Paint.Align.LEFT);
        mPersonPaint.setFakeBoldText(true);
        mPersonPaint.setColor(Color.WHITE);
        mPersonPaint.setTextSize(mExampleDimension);


        this.setOnTouchListener(this);

    }

    private void invalidateTextPaintAndMeasurements() {
        mTextWidth = mTextPaint.measureText(mExampleString);
        mTextHeight = mTextPaint.getFontMetrics().bottom;
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

        if( process < mMin) process = mMin;
        if( process > mMax) process = mMax;
        person = (double)(process-mMin)/(mMax-mMin);
        String ps = person*100+"%";
        if(ps.length()>5){
            String tmp = ps.substring(0,4);
            ps = tmp+"%";
        }
        mPersonPaintWidth = mPersonPaint.measureText(ps);
        mPersonPaintHeight = mPersonPaint.getFontMetrics().bottom;

        //Draw
        canvas.drawRect(getLeft(), getTop()+padding, (float) (getRight() * person), getBottom()-padding, mPaint);
        // Draw the name text.
        canvas.drawText(mExampleString,
                getLeft() + 4,
                getTop() + (contentHeight + mTextHeight) / 2,
                mTextPaint);
        //Draw the person
        canvas.drawText(ps,
                getLeft()+(contentWidth - mPersonPaintWidth )/2 ,
                getTop()+(contentHeight + mPersonPaintHeight) / 2,
                mPersonPaint);

    }

    public String getExampleString() {
        return mExampleString;
    }
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }


    public int getExampleColor() {
        return mExampleColor;
    }
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    public float getExampleDimension() {
        return mExampleDimension;
    }
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    public void setProcess (int p){
        if( p < mMin )
            p=mMin;
        if( p > mMax )
            p=mMax;
        process = p;
        invalidate();
    }
    public int getProcess() {
        return process;
    }

    public void setMinMax(int min,int max){
        if( min > max ) return;
        if( min < 0 ) return;

        mMax = max;
        mMin = min;
        invalidate();
    }
    public void setRcTrimValue(short val)
    {
        mRcTrimValue = val;
    }
    public  void setLockValue(boolean lock)
    {
        lockValue = lock;
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int[] position = new int[2];
        v.getLocationOnScreen(position);
        int p;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //Log.e(TAG, "down in x,y=" + event.getRawX() + "," + event.getRawY());
                //Log.e(TAG, "content top=" + this.getTop());
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.e(TAG, "move to x,y,top,btom=" + event.getRawX() + "," + event.getRawY() + ","+getLeft()+"," + getTop() + ","+getRight()+"," + getBottom());
                p = (int)(mMin + (mMax-mMin) * (event.getRawX()-position[0])/(getWidth()));
                //Log.e(TAG, "process=" + p);
                doNotifyRcChanged(mRcId, p);
                break;
            case MotionEvent.ACTION_UP:
                if( lockValue )
                    doNotifyRcChanged(mRcId,mRcTrimValue);
                //Log.i(TAG, "up in x,y=" +event.getRawX()+","+ event.getRawY() );
                //Log.i(TAG, "screen in x,y=" +position[0]+","+ position[1] );
                //Log.e(TAG, "content with=" + this.getWidth());
                break;
            default:
                //Log.e(TAG,"on TOuch ");
        }
        return false;

    }

    public IRcOutputListen getRcListen(){
        return rcListen;
    }
    public void setRcListen(IRcOutputListen l){
        rcListen = l;
    }
    private void doNotifyRcChanged(int id, int value){
        if( value != process && rcListen != null && rcListen.doSetRcValue(id, value)) {
            setProcess(value);
        }
    }
    public void setId(int id){
        mRcId = id;
    }
    public int getId (){
        return mRcId;
    }

}
