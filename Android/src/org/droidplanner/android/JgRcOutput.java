package org.droidplanner.android;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.MAVLink.common.msg_rc_channels_override;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.drone.ExperimentalApi;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.property.Parameter;
import com.o3dr.services.android.lib.drone.property.Parameters;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by joe on 2015/7/13.
 */
public class JgRcOutput{
    public static final short INVALID_RC_VALUE = 0;
    public static final short MAX_RC_VALUE = 2000;
    public static final short MIN_RC_VALUE = 1000;

    public static final int ROLLID = 0;
    public static final int THRID = 2;
    public static final int PITCHID=1;
    public static final int YAWID=3;
    public static final int CHN5ID=4;
    public static final int CHN6ID=5;
    public static final int CHN7ID=6;
    public static final int CHN8ID=7;
    public  static final int ALLID = 8;

    //error msg id
    public  static final  int DRONE_ERROR = -1;

    public static  final String TAG = "JgRcOutput";

    private  short[] rcOutputs = new short[8];
    private short [][] rcParamValue = new short[8][3];;
    private msg_rc_channels_override rcMsg =new msg_rc_channels_override() ;
    private MavlinkMessageWrapper rcMw = new MavlinkMessageWrapper(rcMsg);

    Context mContext=null;
    Drone mDrone=null;

    private Handler mClientRcUpdateHandler = null;

    private int mRcStatus; // 0 is not start, 1 rc set default ok, 2 the rc is changed
    private ScheduledExecutorService mTask;
    private  int mRate = 50;//  1000/50=20 , 20times per 1S

    private Parameters mParams;

    public static  final  int HARDMODE = 0; // ignore parameter rc value
    public static  final  int SOFTWAREMODE = 1; //use parameter rc value first
    private int mMode = SOFTWAREMODE;

    public static  final int KeyADDTYPE = 0;
    public static  final int KeySUBTYPE = 1;
    private int [][] mKeyMap = new int[8][2]; //[id][KeyAddTYPE]

    private int mRcMask=0;

    public  JgRcOutput(Drone drone, Context context, Handler handler){
        mContext = context;
        mDrone = drone;
        mClientRcUpdateHandler = handler;
        initRcOutput();
        initKeyMap();
    }
    public  JgRcOutput(Context context, Handler handler){
        mContext = context;
        mDrone = null;
        mClientRcUpdateHandler = handler;
        initRcOutput();
        initKeyMap();
    }
    public boolean setDrone(Drone drone){
        if( isStarted() ){
            stop();
            mDrone = drone;
        }else{
            mDrone = drone;
        }
        return true;
    }

    //*************************** Status ,, check is ready for run
    private  boolean isUserReady(){
        if( mContext == null || mDrone == null || mClientRcUpdateHandler == null)
            return false;
        return true;
    }
    public   boolean isReady(){
        return isUserReady() && mDrone.isConnected();
    }
    public boolean isStarted(){
        return mTask != null ;
    }
    private boolean isRcChanged(){
        return mRcStatus == 2;
    }
    private void setRcStatusChanged(boolean changed){
        int val = changed?2:1;
        if( mRcStatus>0)
            setRcStatus(val);
    }
    private void setRcStatus(int val){
        mRcStatus = val;
    }

    public int getmMode() {
        return mMode;
    }

    public void setmMode(int mMode) {
        this.mMode = mMode;
    }

    //***************************  Start Stop
    private  int times=0;
    public boolean start(){
        if( isStarted() )
            return true;

        if ( isReady() ) {
            initRcOutput();
            sendRcMsg();
            sendRcMsg();
            sendRcMsg();
            mRcMask = 0xffff;
            setDefaultRc();
            mTask = Executors.newScheduledThreadPool(5);
            mTask.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    if( isReady() ) {
                        if( false && isRcChanged()) {
                            sendRcMsg();
                            onRcChanged(ALLID);
                            setRcStatusChanged(false);
                            times = 0;
                        }else {
                            times++;
                            if( times *getDelayMs() >= 500) {
                                sendRcMsg();
                                times = 0;
                            }
                        }
                    }else{
                        sendErrorMessageToClient(DRONE_ERROR);
                    }
                }
            }, 0, getDelayMs(), TimeUnit.MILLISECONDS);

        }else{
            alertUser("Start RcOutput failed, no Ready");
            return false;
        }
        return true;
    }
    public  boolean stop(){
        //initRcOutput();
        Arrays.fill(rcOutputs, INVALID_RC_VALUE);
        setRcStatus(0);
        if( isStarted() ) {
            mTask.shutdownNow();
            mTask = null;
            updateRcMsg();
            sendRc() ;
            sendRc() ;
        }
        onRcChanged(ALLID);
        return true;
    }



    //******************** loop speed
    private  int getRate(){ return mRate; }
    public boolean setRate(int rate){
        if( rate >=1 && rate < 500) {
            mRate = rate;
            return true;
        }else{
            return false;
        }
    }
    private int getDelayMs() {
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parrentContext);
        //int rate = Integer.parseInt(prefs.getString("pref_mavlink_stream_rate_RC_override", "0"));
        int rate = getRate();
        if ((rate >= 1) & (rate < 500)) {
            return 1000 / rate;
        } else {
            return 20;
        }
    }

    //***************************  Send rc Message
    private  void updateRcMsg(){
        if( (mRcMask & 1<<0) != 0 )
            rcMsg.chan1_raw = rcOutputs[0];
        if( (mRcMask & 1<<1) != 0 )
            rcMsg.chan2_raw = rcOutputs[1];
        if( (mRcMask & 1<<2) != 0 )
            rcMsg.chan3_raw = rcOutputs[2];
        if( (mRcMask & 1<<3) != 0 )
            rcMsg.chan4_raw = rcOutputs[3];
        if( (mRcMask & 1<<4) != 0 )
            rcMsg.chan5_raw = rcOutputs[4];
        if( (mRcMask & 1<<5) != 0 )
            rcMsg.chan6_raw = rcOutputs[5];
        if( (mRcMask & 1<<6) != 0 )
            rcMsg.chan7_raw = rcOutputs[6];
        if( (mRcMask & 1<<7) != 0 )
            rcMsg.chan8_raw = rcOutputs[7];
        //rcMsg.target_system =
    }
    private boolean sendRc(){
        rcMw.setMavLinkMessage(rcMsg);
        ExperimentalApi.sendMavlinkMessage(mDrone, rcMw);
        return true;
    }
    private boolean sendRcMsg(){
        if( isReady() ) {
            updateRcMsg();
            return sendRc() ;
        }else
            debugMsg("sendRcMsg false , no Ready");
        debugMsg("sendRcMsg .....");
        return false;
    }


    //***************************  Rc callback update ui
    private void initRcOutput(){
        Arrays.fill(rcOutputs, INVALID_RC_VALUE);
        onRcChanged(ALLID);
        setRcStatus(0);
        //clear or set the default value
        for ( int i =0 ; i< 8 ; i++){
            rcParamValue[i][0]=MIN_RC_VALUE;
            rcParamValue[i][2]=MAX_RC_VALUE;
            rcParamValue[i][1]=MIN_RC_VALUE+(MAX_RC_VALUE-MIN_RC_VALUE)/2;
        }
        debugMsg("initRcOutput !!");
    }
    private void initKeyMap(){
        /* the key for long jostick
        mKeyMap[ROLLID][KeyADDTYPE] = KeyEvent.KEYCODE_3;
        mKeyMap[ROLLID][KeySUBTYPE] = KeyEvent.KEYCODE_1;

        mKeyMap[PITCHID][KeyADDTYPE] = KeyEvent.KEYCODE_4;
        mKeyMap[PITCHID][KeySUBTYPE] = KeyEvent.KEYCODE_2;

        mKeyMap[THRID][KeyADDTYPE] = KeyEvent.KEYCODE_W;
        mKeyMap[THRID][KeySUBTYPE] = KeyEvent.KEYCODE_S;

        mKeyMap[YAWID][KeyADDTYPE] = KeyEvent.KEYCODE_D;
        mKeyMap[YAWID][KeySUBTYPE] = KeyEvent.KEYCODE_A;

        mKeyMap[CHN5ID][KeyADDTYPE] = KeyEvent.KEYCODE_6;
        mKeyMap[CHN5ID][KeySUBTYPE] = KeyEvent.KEYCODE_5;

        mKeyMap[CHN6ID][KeyADDTYPE] = KeyEvent.KEYCODE_8;
        mKeyMap[CHN6ID][KeySUBTYPE] = KeyEvent.KEYCODE_7;

        mKeyMap[CHN7ID][KeyADDTYPE] = KeyEvent.KEYCODE_9;
        mKeyMap[CHN7ID][KeySUBTYPE] = KeyEvent.KEYCODE_0;

        mKeyMap[CHN8ID][KeyADDTYPE] = KeyEvent.KEYCODE_M;
        mKeyMap[CHN8ID][KeySUBTYPE] = KeyEvent.KEYCODE_N;
        */

        // jodisk for usb
        mKeyMap[ROLLID][KeyADDTYPE] = KeyEvent.KEYCODE_BUTTON_1;
        mKeyMap[ROLLID][KeySUBTYPE] = KeyEvent.KEYCODE_BUTTON_3;

        mKeyMap[PITCHID][KeyADDTYPE] = KeyEvent.KEYCODE_BUTTON_2;
        mKeyMap[PITCHID][KeySUBTYPE] = KeyEvent.KEYCODE_BUTTON_4;

        mKeyMap[THRID][KeyADDTYPE] = KeyEvent.KEYCODE_DPAD_UP;
        mKeyMap[THRID][KeySUBTYPE] = KeyEvent.KEYCODE_DPAD_DOWN;

        mKeyMap[YAWID][KeyADDTYPE] = KeyEvent.KEYCODE_DPAD_RIGHT;
        mKeyMap[YAWID][KeySUBTYPE] = KeyEvent.KEYCODE_DPAD_LEFT;

        mKeyMap[CHN5ID][KeyADDTYPE] = KeyEvent.KEYCODE_6;
        mKeyMap[CHN5ID][KeySUBTYPE] = KeyEvent.KEYCODE_5;

        mKeyMap[CHN6ID][KeyADDTYPE] = KeyEvent.KEYCODE_8;
        mKeyMap[CHN6ID][KeySUBTYPE] = KeyEvent.KEYCODE_7;

        mKeyMap[CHN7ID][KeyADDTYPE] = KeyEvent.KEYCODE_9;
        mKeyMap[CHN7ID][KeySUBTYPE] = KeyEvent.KEYCODE_0;

        mKeyMap[CHN8ID][KeyADDTYPE] = KeyEvent.KEYCODE_M;
        mKeyMap[CHN8ID][KeySUBTYPE] = KeyEvent.KEYCODE_N;
        //
    }
    private void updateParamRc(){
        int i;
        Parameters droneParams = mDrone.getAttribute(AttributeType.PARAMETERS);
        if (droneParams != null) {
            //override the default value
            Parameter rcTrim;
            Parameter rcMax;
            Parameter rcMin;
            for( i=1; i<= 8; i++  ){
                rcTrim = droneParams.getParameter("RC"+i+"_TRIM");
                rcMin = droneParams.getParameter("RC"+i+"_MIN");
                rcMax = droneParams.getParameter("RC"+i+"_MAX");

                if( rcTrim != null ){
                    rcParamValue[i-1][1] = (short) rcTrim.getValue();
                }else{
                    //use the default value
                    continue;
                }
                if( rcMin != null && rcMax != null){
                    rcParamValue[i-1][0] = (short) rcMin.getValue();
                    rcParamValue[i-1][2] = (short) rcMax.getValue();
                }else if( rcMax!=null && rcMin == null){
                    rcParamValue[i-1][2] = (short) rcMax.getValue();
                    rcParamValue[i-1][0] = (short)( 2*rcParamValue[i-1][1] - rcParamValue[i-1][2] );
                }else if( rcMax == null && rcMin != null){
                    rcParamValue[i-1][0] = (short) rcMin.getValue();
                    rcParamValue[i-1][2] = (short)( 2*rcParamValue[i-1][1] - rcParamValue[i-1][0] );
                }else{
                    // min and max is null
                    alertUser("Parameter RC"+i+"MAX/MIN not found for Rc, use default Value");
                }
            }

        }else {
            //use the rcParamValue from init
            alertUser("Parameter is not found for Rc, use default Value");
        }
    }
    private void setDefaultRc(){
        int i;

        if( getmMode() == SOFTWAREMODE) {
            updateParamRc();
        }
// default is a hardware Jostick, use the 1000-2000 rc value
        //thr , yaw , roll, pitch
        for( i = 0; i<= YAWID; i++) {
            rcOutputs[i] = rcParamValue[i][1];
        }
        rcOutputs[THRID]= rcParamValue[THRID][0]; // thr use the min value

        // rc 5 ~ rc 8
        for ( i = CHN5ID; i<= CHN8ID; i++ ){
            rcOutputs[i] = rcParamValue[i][0];
        }

        onRcChanged(ALLID);
        setRcStatus(1);
    }
    private void onRcChanged(int id){
        if( isUserReady()) {
            Message message = new Message();
            message.what = id;
            mClientRcUpdateHandler.sendMessage(message);
        }
    }

    private void sendErrorMessageToClient(int msgid){
        if(mClientRcUpdateHandler != null) {
            Message message = new Message();
            message.what = msgid;
            mClientRcUpdateHandler.sendMessage(message);
        }
    }

    //***************************  get set Rc
    public  String getRcByIdToString(int id){
        return String.valueOf(rcOutputs[id]);
    }
    public short getRcById(int id){
        if( id >= ALLID) return 0;
        return rcOutputs[id];
    }
    public short getDefalutRcById(int id){
        if( id >= ALLID) return 0;
        return rcParamValue[id][1];
    }
    public short getDefalutMaxRcById(int id){
        if( id >= ALLID) return 0;
        return rcParamValue[id][2];
    }
    public short getDefalutMinRcById(int id){
        if( id >= ALLID) return 0;
        return rcParamValue[id][0];
    }
    public boolean setRcById(int id, short rc){
        if( id >= ALLID) return false;

        if( rc <= rcParamValue[id][2] && rc >= rcParamValue[id][0]){
            rcOutputs[id] = rc;
        }else{
            //alertUser("Rc"+id+": set by a bad value="+rc);
            if( rc > rcParamValue[id][2] ) rcOutputs[id] = rcParamValue[id][2];
            if( rc < rcParamValue[id][0] ) rcOutputs[id] = rcParamValue[id][0];
            //return false;
        }
        setRcStatusChanged(true);
        mRcMask |= 1<<id;
        sendRcMsg();
        //onRcChanged(ALLID);
        return true;
    }

    public int getRcKeyById(int id,int type){
        return mKeyMap[id][type];
    }
    public void setRcKeyById(int id,int type,int key){
        mKeyMap[id][type] = key;
    }

    //debug
    protected void alertUser(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }
    private  void debugMsg(String msg){
        Log.d(TAG, msg);
    }


}

