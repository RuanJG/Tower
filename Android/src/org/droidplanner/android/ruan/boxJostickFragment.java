package org.droidplanner.android.ruan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_log_data;
import com.MAVLinks.MAVLinkPacket;
import com.MAVLinks.common.msg_rc_channels_override;
import com.google.android.gms.analytics.HitBuilders;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.drone.ExperimentalApi;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.util.MathUtils;

import org.beyene.sius.unit.length.LengthUnit;
import org.droidplanner.android.DroidPlannerApp;
import org.droidplanner.android.R;
import org.droidplanner.android.activities.helpers.BluetoothDevicesActivity;
import org.droidplanner.android.activities.helpers.SuperUI;
import org.droidplanner.android.fragments.helpers.ApiListenerFragment;
import org.droidplanner.android.utils.analytics.GAUtils;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ruan.connection.BluetoothConnection;
import org.ruan.connection.MavLinkConnection;
import org.ruan.connection.MavLinkConnectionListener;
import org.ruan.connection.UsbConnection;
import org.w3c.dom.Text;

import static org.droidplanner.android.ruan.RcConfigParam.*;

public class boxJostickFragment  extends ApiListenerFragment  implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = boxJostickFragment.class.getSimpleName();

    TextView mStatusText;
    private VlcVideoFragment mVlcVideo;
    private Button playBtn ;
    private CheckBox mRadioButton;
    private CheckBox m4gCheckBox;
    private CheckBox m2gCheckBox;
    private TextView mDistanceText;
    private SeekBar mDistanceBar;
    private final int MAX_RADIO_DISTANCE = 2000;
    private int mDistanceFor4G=MAX_RADIO_DISTANCE;
    private int mDistanceNow = 0;
    private TextView mConnectingTypeText ;

    //private DroidPlannerApp dpApp;
    private DroidPlannerPrefs dpPrefs;

    public static final int Rc_Settings_REQUEST_CODE = 40;
    public static final int Rc_Settings_RESULT_CODE = 41;
    private static final int MAX_RC_COUNT =8;



    public final static int BleJostickHandleMsgId =4;
    public final static int Get4GIPHandleMsgId =5;
    public final static int JostickHandleMsgId = 6;
    public final static int SendRcThreadStatus = 7;
    public final static int RcSettingFrameResult =8;
    private BleJostick mCopterBleJostick;
    //private BleJostick mCameraBleJostick;

    private BluetoothConnection mBleConnect;
    private String mAddress;
    UsbConnection mUartConnect;
    String mUartName = "boxUartName";

    private short mega2560ConnectStatus=0;
    private short mega2560ActivityPath = 0;
    private short GCS_ID= 0;
    private short WIFI_ID= 1;
    private short TELEM_ID= 2;
    private short DTMF_2G_ID = 3;
    private short NONE_ID = 7;

    private final int MEGA2560_SYS_ID =254;
    private final int    MEGA2560_BOARD_CMD_WIFI_CONNECT_TCP=1;
    private final int    MEGA2560_BOARD_CMD_WIFI_SET_CONNECT_IP =2;
    private final int    MEGA2560_BOARD_CMD_WIFI_DISCONNECT_TCP=3;
    private final int    MEGA2560_BOARD_CMD_WIFI_MAX_ID=4;
    private final int    MEGA2560_BOARD_CMD_SWITCH_CONNECT = 5;
    private final int    MEGA2560_BOARD_CMD_MAX_ID=6;
    private final int    GCS_CMD_REPORT_STATUS =7;
    private final int    MEGA2560_BOARD_CMD_2G_CONNECT = 8;
    private final int    MEGA2560_BOARD_CMD_2G_DISCONNECT = 9;
    private final int    MEGA2560_BOARD_CMD_2G_SEND_DTMF=10;
    private final int    MEGA2560_BOARD_CMD_2G_SEND_LOG = 11;
    private final int    GCS_CMD_MAX_ID=12;

    private  msg_rc_channels_override mRcOverridePacket;
    private String log2G;
    private  int mKeyRcSpeed = 200;

    private int mModeForConnect = 1<<GCS_ID;//GCS_ID local send, TELEM_ID telem, WIFI_ID wifi

    public static final int ROLLID = 0;
    public static final int THRID = 2;
    public static final int PITCHID=1;
    public static final int YAWID=3;
    public static final int CHN5ID=4;
    public static final int CHN6ID=5;
    public static final int CHN7ID=6;
    public static final int CHN8ID=7;

    RcSettingFragment rcSettingFragment ;
    RcConfigParam mRcConfigParam;


    IRcOutputListen seekBarListen = new IRcOutputListen() {
        @Override
        public boolean doSetRcValue(int id, int value) {
            return true;
        }
    };
    private static final IntentFilter eventFilter = new IntentFilter();
    static {
        eventFilter.addAction(AttributeEvent.GPS_POSITION);
        eventFilter.addAction(AttributeEvent.HOME_UPDATED);
        eventFilter.addAction(AttributeEvent.STATE_ARMING);
        eventFilter.addAction(AttributeEvent.STATE_CONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_DISCONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_UPDATED);
        eventFilter.addAction(AttributeEvent.STATE_VEHICLE_MODE);
        eventFilter.addAction(AttributeEvent.ALTITUDE_UPDATED);
        eventFilter.addAction(AttributeEvent.PARAMETERS_REFRESH_COMPLETED);
    }
    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case AttributeEvent.GPS_POSITION:
                case AttributeEvent.HOME_UPDATED:
                    updateHomeDistance();
                    break;
                case AttributeEvent.STATE_CONNECTED:
                    break;
                case AttributeEvent.PARAMETERS_REFRESH_COMPLETED:
                    break;
                case AttributeEvent.STATE_DISCONNECTED:
                    break;
                case AttributeEvent.STATE_VEHICLE_MODE:
                    break;
                case AttributeEvent.STATE_UPDATED:
                case AttributeEvent.STATE_ARMING:
                case AttributeEvent.FOLLOW_START:
                case AttributeEvent.FOLLOW_STOP:
                case AttributeEvent.FOLLOW_UPDATE:
                    break;
                case AttributeEvent.ALTITUDE_UPDATED:
                    break;
                default:
                    break;
            }
        }
    };
    protected void alertUser(String message) {
        //Toast.makeText(this.getActivity().getApplicationContext(), TAG+":"+message, Toast.LENGTH_SHORT).show();
        //debugMsg(message);
        mStatusText.setText(message);
    }
    private  void debugMsg(String msg){
        Log.d(TAG, msg);
        //mStatusText.setText(msg);
        ;
    }
    private void showUser(String message){
        Toast.makeText(this.getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_box_jostick, container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(mStatusText == null)
            mStatusText = (TextView) getActivity().findViewById(R.id.box_statusText);
        if( dpPrefs == null)
            dpPrefs = new DroidPlannerPrefs(this.getContext());
        initRcSeekBar();
        setupVlcVideo();

        /*
        SeekBar rcRangBar = (SeekBar)this.getActivity().findViewById(R.id.rcRangeSeekBar);
        rcRangBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setRcChangeRange(progress, lastChanRCID);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
*/

        Button btn4 = (Button) this.getActivity().findViewById(R.id.box_buttonO2oIp);
        if( btn4 != null )
            btn4.setOnClickListener(this);


        mRadioButton = (CheckBox) this.getActivity().findViewById(R.id.box_radioButton);
        if(mRadioButton!=null){
            mRadioButton.setChecked( isThisMode(TELEM_ID) );
            //mRadioButton.setOnClickListener(this);
            mRadioButton.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    doTriggleRadioConnect();
                }
            });
            //mRadioButton.setClickable(false);
        }
        m2gCheckBox = (CheckBox) this.getActivity().findViewById(R.id.box_2g_checkBox);
        if(m2gCheckBox != null) {
            m2gCheckBox.setChecked(isThisMode(DTMF_2G_ID));
            //m2gCheckBox.setOnClickListener(this);
            m2gCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    doTriggle2gConnect();
                }
            });
        }
        m4gCheckBox = (CheckBox) this.getActivity().findViewById(R.id.box_4g_checkBox);
        if( m4gCheckBox != null ) {
            m4gCheckBox.setChecked(isThisMode(GCS_ID));
            //m4gCheckBox.setOnClickListener(this);

            m4gCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    doTriggle4gConnect();
                }
            });
        }
        onModeConnectChange();
        doTriggle4gConnect();


        mDistanceBar = (SeekBar) this.getActivity().findViewById(R.id.box_distance_Bar);
        if(mDistanceBar != null) {
            mDistanceBar.setMax(MAX_RADIO_DISTANCE);
            mDistanceNow=0;
            mDistanceBar.setProgress(mDistanceNow);
            mDistanceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    //setRcChangeRange(progress, lastChanRCID);
                    set4gDistance(progress);
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }
        mConnectingTypeText = (TextView) this.getActivity().findViewById(R.id.box_connecting_type_text);
        mDistanceText = (TextView) this.getActivity().findViewById(R.id.box_distan_text);
        set4gDistance(MAX_RADIO_DISTANCE);

        /*
        m4GIpRouter = new Router4GFindWanIp(mHandler);
        Button btn3 = (Button) this.getActivity().findViewById(R.id.buttonSetIp);
        if( btn3 != null )
            btn3.setOnClickListener(this);
            */

        setup2GJostickButtons();

        Button btn1 = (Button) this.getActivity().findViewById(R.id.box_connect_hardware_button);
        if( btn1 != null ) btn1.setOnClickListener(this);
        //if( isJostickDisconnected() ) doJostickConnect();

        if( rcSettingFragment == null){
            rcSettingFragment = new RcSettingFragment();
            this.getActivity().getSupportFragmentManager().beginTransaction().add(R.id.box_rcSettingView, rcSettingFragment).commit();
        }
        this.getActivity().findViewById(R.id.box_rcSettingView).setVisibility(View.INVISIBLE);

        mRcConfigParam = new RcConfigParam(MAX_RC_COUNT,this.getContext());
        RcConfigParam.baseConfig bc= new RcConfigParam.baseConfig();
        bc.revert = false;
        bc.minValue = 1000;
        bc.maxValue = 2000;
        bc.trim = 0;
        bc.curverParamk = 0;
        bc.curveType = RcExpoView.MIDDLE_TYPE_CURVE;
        bc.valiable = true;
        for( int i=0 ;i < MAX_RC_COUNT; i++){
            bc.id = (short) i;
            //mRcConfigParam.storeBaseConfig(bc);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
        stopPlayVideo();
        stopSendRcThread();
    }
    @Override
    public void onApiConnected() {
        //super.onApiConnected();
        getBroadcastManager().registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onApiDisconnected() {
        //super.onApiDisconnected();
        getBroadcastManager().unregisterReceiver(eventReceiver);
    }

    private void set4gDistance(int m){
        mDistanceFor4G = m;
        if(mDistanceText != null) mDistanceText.setText("auto switch 4G when farter ("+mDistanceFor4G+")m");
    }

    @Override
    public void onClick(View v) {
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(GAUtils.Category.FLIGHT);
        final Drone drone = getDrone();
        eventBuilder = null;
        switch (v.getId()) {
            case R.id.box_videoPlayBtn:
                if( mVlcVideo.isPlaying()){
                    stopPlayVideo();
                }else{
                    startPlayVideo();
                }
                break;
            case R.id.box_buttonO2oIp:
                Intent i;
                i = new Intent(this.getContext(),O2oActivity.class);
                startActivityForResult(i, O2oActivity.O2O_ACTIVITY_ADDR_RESULT_CODE);
                break;
            case R.id.box_4g_checkBox:
                doTriggle4gConnect();
                break;
            case R.id.box_2g_checkBox:
                doTriggle2gConnect();
                break;
            case R.id.box_radioButton:
                doTriggleRadioConnect();
                break;
            case R.id.box_connect_hardware_button:
                triggleJostickConnect();
                break;
            case R.id.id_jostick_arm_btn:
            case R.id.id_jostick_takeoff_btn:
            case R.id.id_jostick_stop_btn:
            case R.id.id_jostick_call_btn:
            case R.id.id_jostick_thr_down_btn:
            case R.id.id_jostick_thr_up_btn:
            case R.id.id_jostick_yaw_left_btn:
            case R.id.id_jostick_yaw_right_btn:
            case R.id.id_jostick_speed_add_btn:
            case R.id.id_jostick_speed_sub_btn:
            case R.id.id_jostick_roll_left_btn:
            case R.id.id_jostick_roll_right_btn:
            case R.id.id_jostick_pitch_down_btn:
            case R.id.id_jostick_pitch_up_btn:
                handleJostickButtons(v.getId());
                break;
            default:
                eventBuilder = null;
                break;
        }

        if (eventBuilder != null) {
            GAUtils.sendEvent(eventBuilder);
        }
    }

    private boolean isThisMode(short mode){
        return ((mModeForConnect & (1<<mode) ) != 0 ) ;
    }
    private void onModeConnectChange() {
        mega2560SwitchConnectWay(mModeForConnect);
        /*
        if( !isThisMode(DTMF_2G_ID)) m2gCheckBox.setChecked(false);
        if( !isThisMode(GCS_ID)) m4gCheckBox.setChecked(false);
        if( !isThisMode(TELEM_ID)) mRadioButton.setChecked(false);
        */
        m2gCheckBox.setChecked(isThisMode(DTMF_2G_ID));
        m4gCheckBox.setChecked(isThisMode(GCS_ID));
        mRadioButton.setChecked(isThisMode(TELEM_ID));
    }
    private void doTriggleRadioConnect() {
        if( mRadioButton.isChecked()) {
            mModeForConnect = 1 << TELEM_ID;
        }else{
            mModeForConnect &= ~(1<<TELEM_ID);
        }
        mega2560SwitchConnectWay(mModeForConnect);
        onModeConnectChange();
    }
    private void doTriggle2gConnect() {
        if( m2gCheckBox.isChecked()){
            mModeForConnect = 1<<DTMF_2G_ID;
        }else{
            mModeForConnect &= ~(1<<DTMF_2G_ID);
            alertUser("dis use 2g");
        }
        onModeConnectChange();
    }
    private void doTriggle4gConnect()
    {
        if( m4gCheckBox.isChecked() ) {
            mModeForConnect = 1<<GCS_ID;
            starSendRcThread();
        }else{
            alertUser("dis use 4g");
            stopSendRcThread();
            mModeForConnect &= ~(1<<GCS_ID);
        }
        onModeConnectChange();
    }

    /* wifi call connect
        private void doTriggle4gConnect()
    {
        if( m4gCheckBox.isChecked() ){
            mega2560WifiSetServer(dpPrefs.getTcpServerIp(),dpPrefs.getTcpServerPort());
            mega2560WifiConnect();
        }else{
            alertUser("4g is no checked");
            mega2560WifiDisconnect();
        }
    }
     */


    private void onConnectStatusChanged()
    {
        if( mega2560ConnectStatus != (mModeForConnect) ){
            mega2560SwitchConnectWay(mModeForConnect);
        }
        /*
        if( mConnectingTypeText != null){
            if(isWifiConnecting()) mConnectingTypeText.setText("Using Wifi");
            if(isTelemConnecting()) mConnectingTypeText.setText("Using Radio");
            if( mModeForConnect == GCS_ID ) mConnectingTypeText.setText("Using Local 4G");
        }
        */
    }
    private void onActivityPathChanged()
    {
        //m4gCheckBox.setChecked(isWifiActivity());
        //mRadioButton.setChecked(isTelemActivity());
    }




    private void setupVlcVideo()
    {
        //mVlcVideo = (VlcVideoFragment) this.getActivity().fragmentManager.findFragmentById(R.id.vlcVideoView);
        if (mVlcVideo == null) {
            debugMsg("vlcvideo is null , create new");
            mVlcVideo  = new VlcVideoFragment();
            this.getActivity().getSupportFragmentManager().beginTransaction().add(R.id.box_vlcVideoView, mVlcVideo).commit();
        }
        playBtn = (Button) this.getActivity().findViewById(R.id.box_videoPlayBtn);
        if(playBtn != null) {
            playBtn.setOnClickListener(this);
        }
    }

    private String getIpAddr()
    {
        final int connectionType = dpPrefs.getConnectionParameterType();
        String addr;
        String port;

        addr = dpPrefs.getTcpServerIp();
        port = String.valueOf(dpPrefs.getTcpServerPort());

        alertUser("connect video frome "+addr +":"+port);
        return addr;
    }

    private String getVideoAddr()
    {
        String addr;
        addr = getIpAddr();
        return "rtsp://"+addr+":8554";
    }

    private  void startPlayVideo()
    {
        mVlcVideo.startPlay(getVideoAddr());
        alertUser("play " + getVideoAddr());
        playBtn.setText(this.getContext().getString(R.string.vlc_video_btn_text_stop));
    }
    private void stopPlayVideo()
    {
        mVlcVideo.stopPlay();
        playBtn.setText(this.getContext().getString(R.string.vlc_video_btn_text_start));
    }




    //mRcOutput call back use
    public void onRcChanged(int id,int value) {
        updateRcSeekBar(id, value);
    }
    private void doRcSeekBarTouch( int id )
    {

        Intent i=new Intent();
        RcConfigParam.mixConfig mc = mRcConfigParam.getMixConfigBySlavchan(id);
        RcConfigParam.baseConfig bc = mRcConfigParam.getBaseConfig(id);
        //i = new Intent(this.getContext(),RcSettingActivity.class);
        //i.putExtra(CameraJostickName, CameraJostickBtName);
       // if( bc == null || !bc.valiable){
        //    showUser("No rc Config for this channel");
        //    return;
       // }else{
            /*
            i.putExtra("id", id);
            i.putExtra("revert",bc.revert);
            i.putExtra("Min", bc.minValue);
            i.putExtra("Max", bc.maxValue);
            i.putExtra("curveType", bc.curveType);
            i.putExtra("curveParamk", bc.curverParamk);
            i.putExtra("trim", bc.trim);//0 not set , range +- 100
            if( mc != null && mc.valiable) {
                i.putExtra("mixChan", mc.mainChan);// chan_id + 1 , 0 = no set
                i.putExtra("mixChanPoint", mc.mainChanStartPoint);//0:low point 1:middle Point
                i.putExtra("mixChanAddPersen", mc.persenAtAdd);//+- 100%
                i.putExtra("mixChanSubPersen", mc.persenAtSub);//+- 100%
            }else{
                i.putExtra("mixChan", -1);// chan_id + 1 , 0 = no set
            }
            */
          //  rcSettingFragment.doInit(mHandler, bc,mc);
         //   this.getActivity().findViewById(R.id.box_rcSettingView).setVisibility(View.VISIBLE);
          //  this.getActivity().findViewById(R.id.box_jostick_board).setVisibility(View.INVISIBLE);
            //startActivityForResult(i, Rc_Settings_REQUEST_CODE);
       // }
        if( bc == null || !bc.valiable){
                bc = new RcConfigParam.baseConfig(id);//get a default setting
        }
        rcSettingFragment.doInit(mHandler, bc,mc);
        this.getActivity().findViewById(R.id.box_rcSettingView).setVisibility(View.VISIBLE);
        this.getActivity().findViewById(R.id.box_jostick_board).setVisibility(View.GONE);

    }
    private void initRcSeekBar(){
        rcSeekbarView bar;

        for( int i=0; i< MAX_RC_COUNT; i++){
            bar = getSeekBarByRcId(i);
            bar.setId(i);
            bar.setLockValue(false);
            bar.setRcListen(seekBarListen);
            bar.setProcess(rcSeekbarView.mMin);
            bar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //alertUser("rc seekbar click");
                    rcSeekbarView bar = (rcSeekbarView) v;
                    doRcSeekBarTouch(bar.getId());
                }
            });
        }
    }

    private rcSeekbarView getSeekBarByRcId(int id){
        rcSeekbarView bar;
        switch (id){
            case 0:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.box_rcRollSeekBar);
                break;
            case 1:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.box_rcPitchSeekBar);
                break;
            case 2:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.box_rcThrSeekBar);
                break;
            case 3:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.box_rcYawSeekBar);
                break;
            case 4:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.box_rcRc5SeekBar);
                break;
            case 5:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.box_rcRc6SeekBar);
                break;
            case 6:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.box_rcRc7SeekBar);
                break;
            case 7:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.box_rcRc8SeekBar);
                break;
            default:
                bar = null;
        }
        return bar;
    }
    private void updateRcSeekBar(int id,int value) {
        rcSeekbarView bar;
        bar = getSeekBarByRcId(id);
        if( bar != null ){
            bar.setProcess(value);
        }
    }

    protected Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            /*
            if( mRcOutput == null) {
                super.handleMessage(msg);
                return;
            }*/
            switch (msg.what) {
                case JostickHandleMsgId:
                    doHandleJostickMessage(msg);
                    break;
                case Get4GIPHandleMsgId:
                    String ip = msg.getData().getString("ip");
                    if(ip.equals(Router4GFindWanIp.UNVAILD_IP))
                        alertUser("no avalible ip fine");
                    else
                        doSetIpToDrone(ip);
                    break;
                case SendRcThreadStatus:
                    if( msg.getData().getString("status").equals("false")){
                        alertUser("stoped send rc");
                    }else {
                        alertUser("start sending rc");
                    }
                    break;
                case RcSettingFrameResult:
                    alertUser("rc:"+msg.getData().getInt("id", -1)+","+msg.getData().getInt("revert", 0)+","+
                                    msg.getData().getInt("Min", 0)+","+msg.getData().getInt("Max", 0)+","+
                                    msg.getData().getInt("curveType", -1)+","+msg.getData().getInt("curveParamk",0)+"mix:"+
                                    msg.getData().getInt("mixChan", -1)+","+
                                    msg.getData().getInt("mixChanAddPersen", 0)+","+
                                    msg.getData().getInt("mixChanSubPersen", 0)+","+
                                    msg.getData().getInt("mixChanPoint", 1)+","+
                                    msg.getData().getInt("trim",0)+","
                    );
                    RcConfigParam.baseConfig bc = new RcConfigParam.baseConfig();
                    bc.id = (short) msg.getData().getInt("id", -1);
                    bc.revert = msg.getData().getInt("revert", 0)==1;
                    bc.minValue = (short) msg.getData().getInt("Min", 0);
                    bc.maxValue = (short) msg.getData().getInt("Max",0);
                    bc.trim = (short) msg.getData().getInt("trim",0);
                    bc.curverParamk = (short) msg.getData().getInt("curveParamk",0);
                    bc.curveType = (short) msg.getData().getInt("curveType", -1);
                    bc.valiable = true;
                    mRcConfigParam.storeBaseConfig(bc);
                    doSyncRcBaseConfigWithJostick(bc);

                    RcConfigParam.mixConfig mc = new RcConfigParam.mixConfig();
                    mc.slaveChan = msg.getData().getInt("id", -1);
                    mc.mainChan = msg.getData().getInt("mixChan", -1);
                    mc.persenAtAdd = msg.getData().getInt("mixChanAddPersen", 0);
                    mc.persenAtSub = msg.getData().getInt("mixChanSubPersen", 0);
                    mc.mainChanStartPoint = msg.getData().getInt("mixChanPoint", 1);
                    mc.valiable = true;
                    mRcConfigParam.storeMixConfig(mc);
                    doSyncRcMixConfigWithJostick(mc);

                    break;
                default:
                    break;
            }
            //super.handleMessage(msg);
        }
    };

    private static final int BASE_CONFIG_ID =0;
    private static final int MIX_CONFIG_ID =1;
    private void doSyncRcMixConfigWithJostick(RcConfigParam.mixConfig mc)
    {
        int MAVLINK_MSG_ID_RC_CHANNELS = 65;
        int MAVLINK_MSG_LENGTH = 42;
        long serialVersionUID = MAVLINK_MSG_ID_RC_CHANNELS;
        com.MAVLinks.MAVLinkPacket packet = new com.MAVLinks.MAVLinkPacket();
        packet.len = MAVLINK_MSG_LENGTH;
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_RC_CHANNELS;

        if(!mc.isValiable()){
            alertUser("bad mixConfig for sending");
            return ;
        }
        packet.payload.putUnsignedInt(MIX_CONFIG_ID);
        packet.payload.putShort((short) mc.mainChan);
        packet.payload.putShort((short) mc.slaveChan);
        packet.payload.putShort((short) mc.mainChanStartPoint);
        packet.payload.putShort((short) mc.persenAtAdd);
        packet.payload.putShort((short) mc.persenAtSub);
        packet.payload.putShort((short) 0);
        packet.payload.putShort((short) 0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedByte((short) 0);
        packet.payload.putUnsignedByte((short) 0);

        doJostickSendMavlink(packet);
    }
    private void doSyncRcBaseConfigWithJostick(RcConfigParam.baseConfig bc)
    {
        int MAVLINK_MSG_ID_RC_CHANNELS = 65;
        int MAVLINK_MSG_LENGTH = 42;
        long serialVersionUID = MAVLINK_MSG_ID_RC_CHANNELS;
        com.MAVLinks.MAVLinkPacket packet = new com.MAVLinks.MAVLinkPacket();
        packet.len = MAVLINK_MSG_LENGTH;
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_RC_CHANNELS;

        if(!bc.isValiable()){
            alertUser("bad baseConfig for sending,id="+bc.id);
            return ;
        }
        packet.payload.putUnsignedInt(BASE_CONFIG_ID);
        packet.payload.putShort(bc.id);
        packet.payload.putShort(bc.curverParamk);
        packet.payload.putShort(bc.curveType);
        packet.payload.putShort(bc.maxValue);
        packet.payload.putShort(bc.minValue);
        packet.payload.putShort(bc.trim);
        packet.payload.putShort((short) (bc.revert?1:0));
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedShort(0);
        packet.payload.putUnsignedByte((short) 0);
        packet.payload.putUnsignedByte((short) 0);

        doJostickSendMavlink(packet);
    }
    //set4G ip
    private void doSetIpToDrone(String ip)
    {
        if( dpPrefs == null)
            dpPrefs = new DroidPlannerPrefs(this.getContext());
        dpPrefs.setTcpServerIp(ip);
    }


    //### find devices
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case O2oActivity.O2O_ACTIVITY_ADDR_RESULT_CODE:{
                if( resultCode == O2oActivity.O2O_ACTIVITY_ADDR_RESULT_CODE ) {
                    String ip = null;
                    if( data != null)
                        ip = data.getStringExtra("ip");
                    if( ip != null && !ip.equals(O2oActivity.UNVARLID_IP)){
                        doSetIpToDrone(ip);
                        //if( mModeForConnect == GCS_ID) {
                            dpPrefs.setConnectionParameterType(ConnectionType.TYPE_TCP);
                            final int connectionType = dpPrefs.getConnectionParameterType();
                            if ((connectionType == ConnectionType.TYPE_TCP || connectionType == ConnectionType.TYPE_UDP)
                                    && dpPrefs.getTcpServerIp().equals(ip)
                                    && !getDrone().isConnected()) {
                                ((SuperUI) getActivity()).toggleDroneConnection();
                                //DroidPlannerApp dpApp = (DroidPlannerApp) this.getActivity().getApplication();
                                //dpApp.connectToDrone();
                            }
                        /*}else {
                            mega2560WifiSetServer(ip, dpPrefs.getTcpServerPort());
                            mega2560WifiConnect();
                        }*/
                    }
                    break;

                }else if(resultCode == O2oActivity.O2O_ACTIVITY_NUMBER_RESULT_CODE ){
                    String num = null;
                    byte[] numArry = new byte[11];
                    if( data != null)
                        num = data.getStringExtra("number");
                    if( num != null && num.length()==11) {
                        setMega2560CallNumber(num);
                    }
                    break;
                }
            }

            case Rc_Settings_REQUEST_CODE:{
                if( resultCode == Rc_Settings_RESULT_CODE){
                    /*
                    i.putExtra("id", id);
                    i.putExtra("revert",false);
                    i.putExtra("Min", 1100);
                    i.putExtra("Max", 2010);
                    i.putExtra("curveType", RcExpoView.MIDDLE_TYPE_CURVE);
                    i.putExtra("curveParamk", 10);
                    */
                    alertUser("rc:"+data.getIntExtra("id",-1)+","+data.getBooleanExtra("revert", false)+","+
                            data.getIntExtra("Min", 0)+","+data.getIntExtra("Max",0)+","+
                            data.getIntExtra("curveType",-1)+","+data.getIntExtra("curveParamk",0)+"mix:"+
                            data.getIntExtra("mixChan",0)+","+
                                    data.getIntExtra("mixChanAddPersen",0)+","+
                                    data.getIntExtra("mixChanSubPersen",0)+","+
                                    data.getIntExtra("mixChanPoint",1)+","+
                                    data.getIntExtra("trim",0)+","
                    );
                }
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void findAndConnectBleJostick(String jostickName, String jostickBtName)
    {//use this function when two jostick has their names
        Intent i;
        i = new Intent(this.getContext(),FindBluetoothDevicesActivity.class);
        //i.putExtra(CameraJostickName, CameraJostickBtName);
        i.putExtra(jostickName, jostickBtName);
        startActivityForResult(i, FindBluetoothDevicesActivity.REQUEST_BLE_ADDR_CODE);
    }

    private  void doSendRcOverrideByLocal()
    {
        final State droneState = getDrone().getAttribute(AttributeType.STATE);
        if( isThisMode(GCS_ID) && getDrone().isConnected()  && droneState!=null ){//&& droneState.isArmed() ) {
            com.MAVLink.common.msg_rc_channels_override rcMsg = new com.MAVLink.common.msg_rc_channels_override();
            rcMsg.chan1_raw = (short) getSeekBarByRcId(0).getProcess();
            rcMsg.chan2_raw = (short) getSeekBarByRcId(1).getProcess();
            rcMsg.chan3_raw = (short) getSeekBarByRcId(2).getProcess();
            rcMsg.chan4_raw = (short) getSeekBarByRcId(3).getProcess();
            rcMsg.chan5_raw = (short) getSeekBarByRcId(4).getProcess();
            rcMsg.chan6_raw = (short) getSeekBarByRcId(5).getProcess();
            rcMsg.chan7_raw = (short) getSeekBarByRcId(6).getProcess();
            rcMsg.chan8_raw = (short) getSeekBarByRcId(7).getProcess();

            MavlinkMessageWrapper rcMw = new MavlinkMessageWrapper(rcMsg);
            rcMw.setMavLinkMessage(rcMsg);
            ExperimentalApi.sendMavlinkMessage(getDrone(), rcMw);
        }
    }


//#########################################################  box mega2560  jostick
    /*
//sync with tower ,esp8266 and mega2560{
#define MEGA2560_SYS_ID 254
enum _GCS_CMDS {
    MEGA2560_BOARD_CMD_WIFI_CONNECT_TCP =  1,
    MEGA2560_BOARD_CMD_WIFI_SET_CONNECT_IP,
    MEGA2560_BOARD_CMD_WIFI_DISCONNECT_TCP,
    MEGA2560_BOARD_CMD_WIFI_MAX_ID,

    //MEGA2560_BOARD_CMD_CHANGE_PATH
    //....
    MEGA2560_BOARD_CMD_MAX_ID
};
struct param_ip_data{
    uint8_t ip[4];
    uint8_t port[2] ;
};
//}
    */


    public static String binaryArray2Ipv4Address(byte[]addr){
        String ip="";
        for(int i=0;i<addr.length;i++){
            ip+=(addr[i]&0xFF)+".";
        }
        return ip.substring(0, ip.length()-1);
    }

    public static short[] ipv4Address2BinaryArray(String ipAdd){
        short[] binIP = new short[4];
        String[] strs = ipAdd.split("\\.");
        for(int i=0;i<strs.length;i++){
            binIP[i] = (short) Integer.parseInt(strs[i]);
        }
        return binIP;
    }
    private void mega2560WifiSetServer(String ipAddr,int port)
    {
        com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
        boolean isIpValid = true;
 /*        try {
            byte []tmp = InetAddress.getByName(ipAddr).getAddress();
            for(int i=0;i<4;i++) ip[i]=(short)tmp[i];
            alertUser("ip:"+","+ip[0]+","+ip[1]+","+ip[2]+","+ip[3]);
        } catch (Exception e) {
            isIpValid = false;
            alertUser("Ip is invalid");
        }*/
        short []ip = ipv4Address2BinaryArray(ipAddr);
        if( ip.length != 4){
            alertUser("Ip is invalid");
            return;
        }
        rcMsg.chan1_raw= MEGA2560_SYS_ID;
        rcMsg.chan2_raw = MEGA2560_BOARD_CMD_WIFI_SET_CONNECT_IP;
        rcMsg.chan3_raw =  ip[0];
        rcMsg.chan4_raw =  ip[1];
        rcMsg.chan5_raw =  ip[2];
        rcMsg.chan6_raw =  ip[3];
        rcMsg.chan7_raw = (short) port;
        sendJostickMavlinkMsg(rcMsg.pack());
    }
    private void mega2560WifiConnect(){
        com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
        rcMsg.chan1_raw= MEGA2560_SYS_ID;
        rcMsg.chan2_raw = MEGA2560_BOARD_CMD_WIFI_CONNECT_TCP;
        sendJostickMavlinkMsg(rcMsg.pack());
    }
    private void mega2560WifiDisconnect(){
        com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
        rcMsg.chan1_raw= MEGA2560_SYS_ID;
        rcMsg.chan2_raw = MEGA2560_BOARD_CMD_WIFI_DISCONNECT_TCP;
        sendJostickMavlinkMsg(rcMsg.pack());
    }

    /*
    #define GCS_ID 0
            #define WIFI_ID 1
            #define TELEM_ID 2
    uint8_t connectStatus = 1<<TELEM_ID ; //1<<TELEM_ID = telem 1<<GCS_ID = phone 4G just one use for connect
    */
    private void mega2560SwitchConnectWay(int way)
    {//uint8_t connectStatus = 1 ; //1= telem ; 2=wifi; 4= 4G , just one use for connect
        com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
        rcMsg.chan1_raw= MEGA2560_SYS_ID;
        rcMsg.chan2_raw = MEGA2560_BOARD_CMD_SWITCH_CONNECT;
        rcMsg.chan3_raw = (short)way;
        sendJostickMavlinkMsg(rcMsg.pack());
    }

    private void setMega2560CallNumber(String number)
    {
        byte[] numArry = new byte[11];
        if( number != null && number.length()==11) {
            alertUser("call number " + number);
            numArry = number.getBytes();

            com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
            rcMsg.chan1_raw= MEGA2560_SYS_ID;
            rcMsg.chan2_raw = MEGA2560_BOARD_CMD_2G_CONNECT;
            rcMsg.chan3_raw =  (numArry[0] | (numArry[1]<<8) );
            rcMsg.chan4_raw =  (numArry[2] | (numArry[3]<<8) );
            rcMsg.chan5_raw =  (numArry[4] | (numArry[5]<<8) );
            rcMsg.chan6_raw =  (numArry[6] | (numArry[7]<<8) );
            rcMsg.chan7_raw = (numArry[8] | (numArry[9]<<8) );
            rcMsg.chan8_raw = (numArry[10]);
            sendJostickMavlinkMsg(rcMsg.pack());
        }
    }
    private void setMega2560Disconnect2G()
    {
            com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
            rcMsg.chan1_raw= MEGA2560_SYS_ID;
            rcMsg.chan2_raw = MEGA2560_BOARD_CMD_2G_DISCONNECT;
            sendJostickMavlinkMsg(rcMsg.pack());
    }
    private void setMega2560Send2GDTMF(char dtmf)
    {
        com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
        rcMsg.chan1_raw= MEGA2560_SYS_ID;
        rcMsg.chan2_raw = MEGA2560_BOARD_CMD_2G_SEND_DTMF;
        rcMsg.chan3_raw = dtmf & 0xff;
        sendJostickMavlinkMsg(rcMsg.pack());
    }
    private void mega2560Ask()
    {
        com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
        rcMsg.chan1_raw= MEGA2560_SYS_ID;
        rcMsg.chan2_raw = MEGA2560_BOARD_CMD_MAX_ID;
        sendJostickMavlinkMsg(rcMsg.pack());
    }

    private void sendJostickMavlinkMsg(com.MAVLinks.MAVLinkPacket pack)
    {
        /*
        MavlinkMessageWrapper rcMw = new MavlinkMessageWrapper(Msg);
        rcMw.setMavLinkMessage(Msg);
        ExperimentalApi.sendMavlinkMessage(getDrone(), rcMw);
        */
        if( !isJostickDisconnected() )
            doJostickSendMavlink(pack);
    }

    private boolean isWifiConnecting(){ return (mega2560ConnectStatus & (1<<WIFI_ID)) != 0 ? true:false;}
    private boolean isTelemConnecting(){ return (mega2560ConnectStatus & (1<<TELEM_ID)) != 0 ? true:false;}
    private boolean isWifiActivity(){ return (mega2560ActivityPath & (1<<WIFI_ID)) != 0 ? true:false;}
    private boolean isTelemActivity(){ return (mega2560ActivityPath & (1<<TELEM_ID)) != 0 ? true:false;}
    private void Mega2560MavlinkHandler(MAVLinkPacket packet)
    {
        if( packet.msgid == msg_rc_channels_override.MAVLINK_MSG_ID_RC_CHANNELS_OVERRIDE) {
            mRcOverridePacket = new msg_rc_channels_override(packet);
            if( mRcOverridePacket.chan1_raw == MEGA2560_SYS_ID ){
                if( mRcOverridePacket.chan2_raw == GCS_CMD_REPORT_STATUS){
                    mega2560ConnectStatus = (short) mRcOverridePacket.chan3_raw;
                    mega2560ActivityPath = (short) mRcOverridePacket.chan4_raw;
                }
                checkAndHandle2GCmdMessage(mRcOverridePacket);
            }else{
                ;//mRcOverridePacket = msg;//this msg is for rc
            }
        }
    }



    //######################################  connection control
    private ScheduledExecutorService mTask;
    private void startConnectUartThread()
    {
        if( mTask != null) return;
        mTask = Executors.newScheduledThreadPool(5);
        mTask.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if( isJostickDisconnected())
                    doJostickConnect();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }
    private void stopConnectUartThread()
    {
        if( mTask!=null ){
            mTask.shutdownNow();
            mTask = null;
        }
    }
    private void doHandleJostickMessage(Message msg)
    {
        if( msg.getData().getString("id").equals("onReceivePacket")){
            if( mRcOverridePacket != null && mRcOverridePacket.chan1_raw != MEGA2560_SYS_ID ){
                onRcChanged(0,mRcOverridePacket.chan1_raw);
                onRcChanged(1,mRcOverridePacket.chan2_raw);
                onRcChanged(2,mRcOverridePacket.chan3_raw);
                onRcChanged(3,mRcOverridePacket.chan4_raw);
                onRcChanged(4,mRcOverridePacket.chan5_raw);
                onRcChanged(5,mRcOverridePacket.chan6_raw);
                onRcChanged(6,mRcOverridePacket.chan7_raw);
                onRcChanged(7, mRcOverridePacket.chan8_raw);
            }else {
                onActivityPathChanged();
                onConnectStatusChanged();
                on2GLogChange();
            }
        }else if( msg.getData().getString("id").equals("onConnect") ){
            alertUser("Uart connected");
        }else if( msg.getData().getString("id").equals("onComError") ){
            alertUser("Uart onComError");
        }
    }


    private  MavLinkConnectionListener mUartlistener=new MavLinkConnectionListener() {
        @Override
        public void onStartingConnection() {
        }
        @Override
        public void onConnect(long connectionTime) {
            stopConnectUartThread();
            mega2560Ask();
            Message message = new Message();
            Bundle bundle = new Bundle();
            message.what = JostickHandleMsgId;
            bundle.putString("id", "onConnect");
            message.setData(bundle);
            mHandler.sendMessage(message);
        }
        @Override
        public void onReceivePacket(MAVLinkPacket packet) {
            Mega2560MavlinkHandler(packet);//update the global data
            // update ui
            Message message = new Message();
            Bundle bundle = new Bundle();
            message.what = JostickHandleMsgId;
            bundle.putString("id", "onReceivePacket");
            //bundle.putString("name",mName);
            message.setData(bundle);
            mHandler.sendMessage(message);
        }
        @Override
        public void onDisconnect(long disconnectionTime) {
            stopConnectUartThread();
            Message message = new Message();
            Bundle bundle = new Bundle();
            message.what = JostickHandleMsgId;
            bundle.putString("id", "onDisconnect");
            message.setData(bundle);
            mHandler.sendMessage(message);
        }
        @Override
        public void onComError(String errMsg) {
            doJostickDisconnect();
            Message message = new Message();
            Bundle bundle = new Bundle();
            message.what = JostickHandleMsgId;
            bundle.putString("id", "onComError");
            message.setData(bundle);
            mHandler.sendMessage(message);
            startConnectUartThread();
        }
    };


    //###########################################################   BluetoothConnection
    public boolean isJostickDisconnected()
    {
        return mBleConnect==null || mBleConnect.getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED;
    }
    public void triggleJostickConnect()
    {
        if( isJostickDisconnected() )
            doJostickConnect();
        else
            doJostickDisconnect();
    }
    public void doJostickConnect()
    {
        if( isJostickDisconnected()){
        //if( mAddress != null && isJostickDisconnected()){
            //if( mBleConnect == null) {
            if( mBleConnect != null){
                mBleConnect.removeMavLinkConnectionListener(mUartName);
                mBleConnect.disconnect();
            }

            final Context context = this.getContext();//getApplicationContext();
            if (true ){// TextUtils.isEmpty(addr)) {
                startActivity(new Intent(context,
                        BluetoothDevicesActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

            }

            DroidPlannerPrefs mAppPrefs = new DroidPlannerPrefs(context);
            String addr = mAppPrefs.getBluetoothDeviceAddress();

            if(TextUtils.isEmpty(addr)) {
                //mBleConnect = new BluetoothConnection(this.getContext(),addr);// mAddress);
                alertUser("no set ble addr ");
                return;
                //mBleConnect = new BluetoothConnection(this.getContext(), "98:d3:31:70:5f:dd");// mAddress);
            }else{
               // if( addr.equals("98:d3:31:70:5f:dd") ){
                    mBleConnect = new BluetoothConnection(this.getContext(), addr);//"98:d3:31:60:05:aa");// mAddress);
            }

            //mBleConnect = new BluetoothConnection(this.getContext(), "98:d3:31:70:5f:dd");// mAddress);
            if(mBleConnect != null ) {
                mBleConnect.addMavLinkConnectionListener(mUartName, mUartlistener);
                //}
                mBleConnect.connect();
            }
        }
    }
    public void doJostickDisconnect()
    {
        if( isJostickDisconnected()) return;

        mBleConnect.removeMavLinkConnectionListener(mUartName);
        if (mBleConnect.getMavLinkConnectionListenersCount() == 0 && mBleConnect.getConnectionStatus() != MavLinkConnection.MAVLINK_DISCONNECTED) {
            //Timber.d("Disconnecting...");
            mBleConnect.disconnect();
        }
    }
    private void doJostickSendMavlink(com.MAVLinks.MAVLinkPacket pack){
        if( ! isJostickDisconnected())
            mBleConnect.sendMavPacket(pack);
    }


    //############################################# uart
/*
    public boolean isJostickDisconnected()
    {
        return mUartConnect==null || mUartConnect.getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED;
    }
    public void triggleJostickConnect()
    {
        if( isJostickDisconnected() )
            doJostickConnect();
        else
            doJostickDisconnect();
    }
    public void doJostickConnect()
    {
        if( isJostickDisconnected() ){
            mUartConnect = new UsbConnection(this.getContext(),57600);
            mUartConnect.addMavLinkConnectionListener(mUartName,mUartlistener);
            mUartConnect.connect();
        }
    }
    public void doJostickDisconnect()
    {
        if( isJostickDisconnected()) return;

        mUartConnect.removeMavLinkConnectionListener(mUartName);
        if (mUartConnect.getMavLinkConnectionListenersCount() == 0 && mUartConnect.getConnectionStatus() != MavLinkConnection.MAVLINK_DISCONNECTED) {
            //Timber.d("Disconnecting...");
            mUartConnect.disconnect();
        }
    }
    private void doJostickSendMavlink(com.MAVLinks.MAVLinkPacket pack){
        if( ! isJostickDisconnected())
            mUartConnect.sendMavPacket(pack);
    }

*/
    //################################################## distance function
    private void updateHomeDistance() {
        final Context context = getActivity().getApplicationContext();
        final Drone drone = getDrone();

        String update ;
        if (drone.isConnected()) {
            final Gps droneGps = drone.getAttribute(AttributeType.GPS);
            final Home droneHome = drone.getAttribute(AttributeType.HOME);
            if (droneGps.isValid() && droneHome.isValid()) {
                LengthUnit distanceToHome = getLengthUnitProvider().boxBaseValueToTarget
                        (MathUtils.getDistance(droneHome.getCoordinate(), droneGps.getPosition()));
                update = String.format("%s", distanceToHome);
                String infos[]=update.split(" ");
                Log.e("Ruan","home distance::"+infos[0]);
                mDistanceNow = (int) Float.parseFloat(infos[0]);
                mDistanceBar.setProgress(mDistanceNow);
            }
        }
    }

//######################################################################  2G jostick function
    private boolean callConnectStatus = false;
    private char ROLL_LEFT_DTMF = '0';
    private char ROLL_RIGHT_DTMF = '1';
    private char PITCH_UP_DTMF = '2';
    private char PITCH_DOWN_DTMF = '3';
    private char THR_UP_DTMF = '4';
    private char THR_DOWN_DTMF = '5';
    private char YAW_LEFT_DTMF = '6';
    private char YAW_RIGHT_DTMF = '7';
    private char STOP_DTMF = '8';
    private char ARM_DTMF = '9';
    private char LAND_DTMF = '9';
    private char TAKEOFF_DTMF = '*';
    private char SPEED_ADD_DTMF = 'A';
    private char SPEED_SUB_DTMF = 'B';

    private void setup2GJostickButtons()
    {
        Button btn;
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_arm_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_takeoff_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_stop_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_call_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_thr_down_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_thr_up_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_yaw_left_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_yaw_right_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_pitch_down_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_pitch_up_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_roll_left_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_roll_right_btn);
        if(btn != null ) btn.setOnClickListener(this);

        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_speed_add_btn);
        if(btn != null ) btn.setOnClickListener(this);
        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_speed_sub_btn);
        if(btn != null ) btn.setOnClickListener(this);

        btn = (Button) this.getActivity().findViewById(R.id.id_jostick_speed_text);
        if(btn != null) btn.setText(mKeyRcSpeed+"");

    }

    private static final String ACTION_FLIGHT_ACTION_BUTTON = "Copter flight action button";

    public boolean isSafeToRcControlInLoiter() {
        final State droneState = getDrone().getAttribute(AttributeType.STATE);
        Gps droneGps = getDrone().getAttribute(AttributeType.GPS);

        if( droneState.isArmed() && droneState.isFlying() && droneGps.isValid() && droneGps.getFixStatus().equals(Gps.LOCK_3D) )
            return true;
        else
            return false;
    }
    private void handleJostickButtons(int viewId)
    {
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(GAUtils.Category.FLIGHT);
        final State droneState = getDrone().getAttribute(AttributeType.STATE);
        switch ( viewId ) {
            case R.id.id_jostick_arm_btn:
                getDrone().changeVehicleMode(VehicleMode.COPTER_GUIDED);
                if (droneState.getVehicleMode() == VehicleMode.COPTER_GUIDED) {
                    if (droneState != null && droneState.isConnected()) {
                        if (droneState.isArmed()) {
                            if (!droneState.isFlying()) {
                                getDrone().arm(false);
                                eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel("Disarm");
                                GAUtils.sendEvent(eventBuilder);
                            }
                        } else {
                            getDrone().arm(true);
                            eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel("Arm");
                            GAUtils.sendEvent(eventBuilder);
                        }
                    }
                }
                break;
            case R.id.id_jostick_land_btn:
                if( isThisMode(DTMF_2G_ID)) {
                    setMega2560Send2GDTMF(LAND_DTMF);
                }else {
                    getDrone().changeVehicleMode(VehicleMode.COPTER_LAND);
                }
                break;
            case R.id.id_jostick_takeoff_btn:
                if( isThisMode(DTMF_2G_ID) ) {
                    triggle2gTakeoff();
                }else{
                    if (droneState != null && droneState.isConnected()) {
                        /*
                        if( droneState.getVehicleMode() != VehicleMode.COPTER_GUIDED){
                            getDrone().changeVehicleMode(VehicleMode.COPTER_GUIDED);
                       }
                        if (droneState.isArmed()) {
                            getDrone().doGuidedTakeoff(getAppPrefs().getDefaultAltitude());
                        }else{
                        getDrone().arm(true);
                        eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel("Arm");
                        GAUtils.sendEvent(eventBuilder);
                        getDrone().doGuidedTakeoff(getAppPrefs().getDefaultAltitude());
                        }*/
                        startGuideTakeOffTask();
                    }
                }
                break;
            case R.id.id_jostick_stop_btn:
                if( isThisMode(DTMF_2G_ID) ) {
                    triggle2gHoldOn();
                }else{
                    //if( droneState.getVehicleMode() == VehicleMode.COPTER_STABILIZE ){
                    //}
                    //getDrone().changeVehicleMode(VehicleMode.COPTER_GUIDED);
                    if( !isJostickDisconnected()){
                        if( getSeekBarByRcId(THRID).getProcess() < 1300 ) {
                            showUser("set thr channel up to middle");
                            break;
                        }
                    }else{
                        initRcPrepareForControl();
                        if( isSafeToRcControlInLoiter() ) {
                            getDrone().changeVehicleMode(VehicleMode.COPTER_LOITER);
                            alertUser("start control by rc");
                        }else{
                            alertUser("no safe to change loiter!!!");
                        }
                    }
                }
                break;
            case R.id.id_jostick_call_btn:
                if( isThisMode(DTMF_2G_ID) )
                    triggle2gCall();
                break;
            case R.id.id_jostick_speed_sub_btn:
                mKeyRcSpeed -= 100;
                onKeyRcSpeedChange();
                triggle2gSpeedSub();
                break;
            case R.id.id_jostick_speed_add_btn:
                mKeyRcSpeed += 100;
                onKeyRcSpeedChange();
                triggle2gSpeedAdd();
                break;
            case R.id.id_jostick_thr_up_btn:
                caliRcValueInKeyMode(THRID,true);
                setMega2560Send2GDTMF(THR_UP_DTMF);
                break;
            case R.id.id_jostick_thr_down_btn:
                caliRcValueInKeyMode(THRID,false);
                setMega2560Send2GDTMF(THR_DOWN_DTMF);
                break;
            case R.id.id_jostick_yaw_left_btn:
                caliRcValueInKeyMode(YAWID,false);
                setMega2560Send2GDTMF(YAW_LEFT_DTMF);
                break;
            case R.id.id_jostick_yaw_right_btn:
                caliRcValueInKeyMode(YAWID,true);
                setMega2560Send2GDTMF(YAW_RIGHT_DTMF);
                break;
            case R.id.id_jostick_roll_left_btn:
                caliRcValueInKeyMode(ROLLID,false);
                setMega2560Send2GDTMF(ROLL_LEFT_DTMF);
                break;
            case R.id.id_jostick_roll_right_btn:
                caliRcValueInKeyMode(ROLLID,true);
                setMega2560Send2GDTMF(ROLL_RIGHT_DTMF);
                break;

            case R.id.id_jostick_pitch_up_btn:
                caliRcValueInKeyMode(PITCHID,true);
                setMega2560Send2GDTMF(PITCH_UP_DTMF);
                break;
            case R.id.id_jostick_pitch_down_btn:
                caliRcValueInKeyMode(PITCHID,false);
                setMega2560Send2GDTMF(PITCH_DOWN_DTMF);
                break;
        }
    }


    private void initRcPrepareForControl() {
        onRcChanged(THRID,1500);
        onRcChanged(ROLLID,1500);
        onRcChanged(PITCHID,1500);
        onRcChanged(YAWID,1500);
    }

    private void onKeyRcSpeedChange()
    {
        if( mKeyRcSpeed < 0 ) mKeyRcSpeed = 0;
        if( mKeyRcSpeed > 500) mKeyRcSpeed = 500;
        Button btn = (Button) this.getActivity().findViewById(R.id.id_jostick_speed_text);
        if(btn != null) btn.setText(mKeyRcSpeed+"");
    }

    private void caliRcValueInKeyMode(int id, boolean add)
    {
        int rc = getSeekBarByRcId(id).getProcess();
        if( add ) {
            if (rc < 1500) {
                onRcChanged(id, 1500);
            } else {
                onRcChanged(id,1500+mKeyRcSpeed);
            }
        }else{
            if (rc > 1500) {
                onRcChanged(id, 1500);
            } else {
                onRcChanged(id,1500-mKeyRcSpeed);
            }
        }
    }
    private void triggle2gSpeedAdd() {
        setMega2560Send2GDTMF(SPEED_ADD_DTMF);
    }

    private void triggle2gSpeedSub() {
        setMega2560Send2GDTMF(SPEED_SUB_DTMF);
    }

    private void triggle2gCall() {
        EditText et = (EditText)this.getActivity().findViewById(R.id.id_jostick_phone_number_text);
        String num = et.getText().toString();
        if( num != null && num.length()==11) {
            if( callConnectStatus ) {
                setMega2560Disconnect2G();
                callConnectStatus = false;
            }else {
                setMega2560CallNumber(num);
                callConnectStatus = true;
            }
        }else{
            showUser("wrong number ,fill number or check it ");
        }
    }

    private void triggle2gHoldOn() {
        setMega2560Send2GDTMF(STOP_DTMF);
    }

    private void triggle2gTakeoff() {
        setMega2560Send2GDTMF(TAKEOFF_DTMF);
    }

    private void triggle2gArm() {
        setMega2560Send2GDTMF(ARM_DTMF);
    }


    private void checkAndHandle2GCmdMessage(msg_rc_channels_override msg) {
        if( msg.chan1_raw == MEGA2560_SYS_ID ){
            if( msg.chan2_raw == MEGA2560_BOARD_CMD_2G_SEND_DTMF) {

            }else if(msg.chan2_raw == MEGA2560_BOARD_CMD_2G_SEND_LOG ){
                display2gLog(msg);
            }
        }
    }

    private void display2gLog(msg_rc_channels_override msg) {
        char[] logs = new char[12];
        int len;
        len = msg.chan3_raw & 0xff;
        logs[0]= (char) ((msg.chan3_raw >> 8) & 0xff);
        logs[1]= (char) ((msg.chan4_raw) & 0xff);
        logs[2]= (char) ((msg.chan4_raw >> 8) & 0xff);
        logs[3]= (char) ((msg.chan5_raw) & 0xff);
        logs[4]= (char) ((msg.chan5_raw >> 8) & 0xff);
        logs[5]= (char) ((msg.chan6_raw ) & 0xff);
        logs[6]= (char) ((msg.chan6_raw >> 8) & 0xff);
        logs[7]= (char) ((msg.chan7_raw) & 0xff);
        logs[8]= (char) ((msg.chan7_raw >> 8) & 0xff);
        logs[9]= (char) ((msg.chan8_raw ) & 0xff);
        logs[10]= (char) ((msg.chan8_raw >> 8) & 0xff);

        log2G="";
        for( int i =0 ; i< len; i++) log2G+=logs[i];
        log2G+=len;
        //log2G+='\n';
    }
    private void on2GLogChange() {
        if( log2G != null){
            EditText cns = (EditText) this.getActivity().findViewById(R.id.id_jostick_console);
            if( cns != null ){
                cns.append(log2G);
            }
        }
        log2G = null;
    }


    //######################################################################### send rc task

    private Handler sendrcHandler;
    private Runnable sendrcRunner;
    private  int sendrcMs = 18;
    private void startSendRcByGcsTask() {
        if( sendrcHandler == null){
            sendrcHandler= new Handler(Looper.getMainLooper());
        }
        if( sendrcRunner == null) {
            sendrcRunner = new Runnable() {
                @Override
                public void run() {
                    doSendRcOverrideByLocal();
                    sendrcHandler.postDelayed(this, sendrcMs);
                }
            };
            //start
            sendrcHandler.postDelayed(sendrcRunner, 100);
        }
    }
    private void stopSendRcByGcsTask(){
        if( sendrcHandler != null && sendrcRunner != null){
            sendrcHandler.removeCallbacks(sendrcRunner);
            sendrcRunner = null;
        }
    }




    Thread sendRcThread ;
    boolean sendrcThreadQuit = true;
    private void starSendRcThread() {
        if( sendRcThread == null || !sendRcThread.isAlive()) {
            sendRcThread = new Thread(new Runnable(){
                @Override
                public void run() {
                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    message.what = SendRcThreadStatus;
                    bundle.putString("status", "true");
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                    while( !sendrcThreadQuit ){
                        //doSendRcOverrideByLocal();
                        try{
                            Thread.sleep(50);
                            doSendRcOverrideByLocal();
                        }catch(InterruptedException e){
                            return;
                        }
                    }
                    bundle = new Bundle();
                    message = new Message();
                    message.what = SendRcThreadStatus;
                    bundle.putString("status", "false");
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                }
            });
            sendrcThreadQuit = false;
            sendRcThread.start();
        }
    }
    private void stopSendRcThread()
    {
        sendrcThreadQuit = true;
    }

    Thread guiedTakeoffThread ;
    Runnable takeOffRunner =new Runnable()
    {
        @Override
        public void run()
        {
            boolean quit = false;
            int count=0;
            State droneState = getDrone().getAttribute(AttributeType.STATE);

            Log.e("Ruan","start set guied mode");
            if( !droneState.isConnected()) return;
            try{
                for( count = 0 ; count < 5; count++){
                    droneState = getDrone().getAttribute(AttributeType.STATE);
                    if (droneState.getVehicleMode() == VehicleMode.COPTER_GUIDED) {
                        break;
                    }
                    getDrone().changeVehicleMode(VehicleMode.COPTER_GUIDED);
                    Thread.sleep(200);
                }
                droneState = getDrone().getAttribute(AttributeType.STATE);
                if (droneState.getVehicleMode() != VehicleMode.COPTER_GUIDED) {
                    return;
                }
            }catch(InterruptedException e){
                return;
            }
            Log.e("Ruan","set guied mode ok");

            Log.e("Ruan","start set arm");
            try{
                for( count = 0 ; count < 5; count++){
                    droneState = getDrone().getAttribute(AttributeType.STATE);
                    if (droneState.isArmed()) {
                        break;
                    }
                    getDrone().arm(true);
                    Thread.sleep(200);
                }
                droneState = getDrone().getAttribute(AttributeType.STATE);
                if( !droneState.isArmed() ){
                    getDrone().arm(false);
                    return;
                }
            }catch(InterruptedException e){
                return;
            }
            Log.e("Ruan","start set arm ok");

            getDrone().doGuidedTakeoff(getAppPrefs().getDefaultAltitude());
        }
    };
    private void startGuideTakeOffTask() {
        if( guiedTakeoffThread == null || !guiedTakeoffThread.isAlive()) {
            guiedTakeoffThread = new Thread(takeOffRunner);
            guiedTakeoffThread.start();
        }else{
            alertUser("can not start new task "+guiedTakeoffThread.isAlive()+","+guiedTakeoffThread.isInterrupted()+","+guiedTakeoffThread.isDaemon());
        }
    }

}
