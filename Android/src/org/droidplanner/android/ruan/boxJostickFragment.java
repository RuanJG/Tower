package org.droidplanner.android.ruan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class boxJostickFragment  extends ApiListenerFragment  implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = boxJostickFragment.class.getSimpleName();

    TextView mStatusText;
    private VlcVideoFragment mVlcVideo;
    private Button playBtn ;
    private RadioButton mRadioButton;
    private CheckBox m4gCheckBox;
    private TextView mDistanceText;
    private SeekBar mDistanceBar;
    private final int MAX_RADIO_DISTANCE = 2000;
    private int mDistanceFor4G=MAX_RADIO_DISTANCE;
    private int mDistanceNow = 0;
    private boolean m4gConnectStatus = false;
    private boolean mRadioConnectStatus = false;
    private TextView mConnectingTypeText ;

    //private DroidPlannerApp dpApp;
    private DroidPlannerPrefs dpPrefs;
    Router4GFindWanIp m4GIpRouter;
    public static final int O2O_ACTIVITY_ADDR_RESULT_CODE = 30;
    public static final int Rc_Settings_RESULT_CODE = 40;
    private static final int MAX_RC_COUNT =8;



    public final static int BleJostickHandleMsgId =4;
    public final static int Get4GIPHandleMsgId =5;
    public final static int JostickHandleMsgId = 6;
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

    private final int MEGA2560_SYS_ID =254;
    private final int    MEGA2560_BOARD_CMD_WIFI_CONNECT_TCP=1;
    private final int    MEGA2560_BOARD_CMD_WIFI_SET_CONNECT_IP =2;
    private final int    MEGA2560_BOARD_CMD_WIFI_DISCONNECT_TCP=3;
    private final int    MEGA2560_BOARD_CMD_WIFI_MAX_ID=4;
    private final int    MEGA2560_BOARD_CMD_SWITCH_CONNECT = 5;
    private final int    MEGA2560_BOARD_CMD_MAX_ID=6;
    private final int    GCS_CMD_REPORT_STATUS =7;
    private final int    GCS_CMD_MAX_ID=8;

    private  msg_rc_channels_override mRcOverridePacket;

    private final int mModeForConnect = GCS_ID;//GCS_ID local send, TELEM_ID telem, WIFI_ID wifi

    IRcOutputListen seekBarListen = new IRcOutputListen() {
        @Override
        public boolean doSetRcValue(int id, int value) {
            return doSetRc(id,value);
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
        eventFilter.addAction(AttributeEvent.FOLLOW_START);
        eventFilter.addAction(AttributeEvent.FOLLOW_STOP);
        eventFilter.addAction(AttributeEvent.FOLLOW_UPDATE);
        eventFilter.addAction(AttributeEvent.MISSION_DRONIE_CREATED);
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
                case AttributeEvent.MISSION_DRONIE_CREATED:
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
        mRadioButton = (RadioButton) this.getActivity().findViewById(R.id.box_radioButton);
        if(mRadioButton!=null){
            ;//mRadioButton.setOnClickListener(this);
            mRadioButton.setClickable(false);
        }
        m4gCheckBox = (CheckBox) this.getActivity().findViewById(R.id.box_4g_checkBox);
        if( m4gCheckBox != null ) {
            m4gCheckBox.setOnClickListener(this);
            /*
            m4gCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    //doTriggle4gConnect(isChecked);
                }
            });*/
        }


        mDistanceBar = (SeekBar) this.getActivity().findViewById(R.id.box_distance_Bar);
        if(mDistanceBar != null) {
            mDistanceBar.setMax(MAX_RADIO_DISTANCE);
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


        if( isJostickDisconnected() ) doJostickConnect();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
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
        stopPlayVideo();
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
                startActivityForResult(i, O2O_ACTIVITY_ADDR_RESULT_CODE);
                break;
            case R.id.box_4g_checkBox:
                doTriggle4gConnect();
                break;
            default:
                eventBuilder = null;
                break;
        }

        if (eventBuilder != null) {
            GAUtils.sendEvent(eventBuilder);
        }
    }
    /*
    int mConnectMode
    private final int  modeLocal4G
    private boolean isUsingLocal4GConnect()
    {

    }*/
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
    private void doTriggle4gConnect(boolean callConnect)
    {
        if( callConnect ){
            mega2560WifiSetServer(dpPrefs.getTcpServerIp(),dpPrefs.getTcpServerPort());
            mega2560WifiConnect();
        }else{
            mega2560WifiDisconnect();
        }/*
        if( m4gConnectStatus ){
            m4gCheckBox.setChecked(false);
            mega2560WifiDisconnect();
            m4gConnectStatus = false;
            //m4gConnectStatus = false; //update this status by
        }else{
            mega2560WifiSetServer(dpPrefs.getTcpServerIp(),dpPrefs.getTcpServerPort());
            mega2560WifiConnect();
            m4gConnectStatus = true;
        }*/
    }

    private void onConnectStatusChanged()
    {
        if( mConnectingTypeText != null){
            if(isWifiConnecting()) mConnectingTypeText.setText("Using 4G");
            if(isTelemConnecting()) mConnectingTypeText.setText("Using Radio");
            if( mModeForConnect == GCS_ID ) mConnectingTypeText.setText("Using Local 4G");
        }
    }
    private void onActivityPathChanged()
    {
        m4gCheckBox.setChecked(isWifiActivity());
        mRadioButton.setChecked(isTelemActivity());

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
        debugMsg("play "+getVideoAddr());
        playBtn.setText("Stop");
    }
    private void stopPlayVideo()
    {
        mVlcVideo.stopPlay();
        playBtn.setText("Start");
    }




    //mRcOutput call back use
    public void onRcChanged(int id,int value) {
        updateRcSeekBar(id, value);
    }

    //seekbar call it to set rc
    private boolean doSetRc(int id, int value){
        return true;
    }


    private void doRcSeekBarTouch( int id )
    {
        Intent i;
        i = new Intent(this.getContext(),RcSettingActivity.class);
        //i.putExtra(CameraJostickName, CameraJostickBtName);
        i.putExtra("id", id);
        startActivityForResult(i, Rc_Settings_RESULT_CODE);
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

    private Handler mHandler = new Handler(){
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
                default:
                    break;
            }
            //super.handleMessage(msg);
        }
    };



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
            case O2O_ACTIVITY_ADDR_RESULT_CODE:{
                String ip = null;
                if( data != null)
                    ip = data.getStringExtra("ip");
                if( ip != null && !ip.equals(O2oActivity.UNVARLID_IP)){
                    doSetIpToDrone(ip);
                    if( mModeForConnect == GCS_ID) {
                        dpPrefs.setConnectionParameterType(ConnectionType.TYPE_TCP);
                        final int connectionType = dpPrefs.getConnectionParameterType();
                        if ((connectionType == ConnectionType.TYPE_TCP || connectionType == ConnectionType.TYPE_UDP)
                                && dpPrefs.getTcpServerIp().equals(ip)
                                && !getDrone().isConnected()) {
                            ((SuperUI) getActivity()).toggleDroneConnection();
                            DroidPlannerApp dpApp = (DroidPlannerApp) this.getActivity().getApplication();
                            dpApp.connectToDrone();
                        }
                    }else {
                        mega2560WifiSetServer(ip, dpPrefs.getTcpServerPort());
                        mega2560WifiConnect();
                    }
                }
                break;
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
        com.MAVLink.common.msg_rc_channels_override rcMsg =new com.MAVLink.common.msg_rc_channels_override() ;
        rcMsg.chan1_raw = (short)getSeekBarByRcId(0).getProcess();
        rcMsg.chan2_raw = (short)getSeekBarByRcId(1).getProcess();
        rcMsg.chan3_raw = (short)getSeekBarByRcId(2).getProcess();
        rcMsg.chan4_raw = (short)getSeekBarByRcId(3).getProcess();
        rcMsg.chan5_raw = (short)getSeekBarByRcId(4).getProcess();
        rcMsg.chan6_raw = (short)getSeekBarByRcId(5).getProcess();
        rcMsg.chan7_raw = (short)getSeekBarByRcId(6).getProcess();

        MavlinkMessageWrapper rcMw = new MavlinkMessageWrapper(rcMsg);
        rcMw.setMavLinkMessage(rcMsg);
        ExperimentalApi.sendMavlinkMessage(getDrone(), rcMw);
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
        sendMavlinkMsg(rcMsg.pack());
    }
    private void mega2560WifiConnect(){
        com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
        rcMsg.chan1_raw= MEGA2560_SYS_ID;
        rcMsg.chan2_raw = MEGA2560_BOARD_CMD_WIFI_CONNECT_TCP;
        sendMavlinkMsg(rcMsg.pack());
    }
    private void mega2560WifiDisconnect(){
        com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
        rcMsg.chan1_raw= MEGA2560_SYS_ID;
        rcMsg.chan2_raw = MEGA2560_BOARD_CMD_WIFI_DISCONNECT_TCP;
        sendMavlinkMsg(rcMsg.pack());
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
        sendMavlinkMsg(rcMsg.pack());
    }

    private void mega2560Ask()
    {
        com.MAVLinks.common.msg_rc_channels_override rcMsg=new com.MAVLinks.common.msg_rc_channels_override() ;
        rcMsg.chan1_raw= MEGA2560_SYS_ID;
        rcMsg.chan2_raw = MEGA2560_BOARD_CMD_MAX_ID;
        sendMavlinkMsg(rcMsg.pack());
    }

    private void sendMavlinkMsg(com.MAVLinks.MAVLinkPacket pack)
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
            msg_rc_channels_override msg = new msg_rc_channels_override(packet);
            if( msg.chan1_raw == MEGA2560_SYS_ID ){
                if( msg.chan2_raw == GCS_CMD_REPORT_STATUS){
                    mega2560ConnectStatus = (short) msg.chan3_raw;
                    mega2560ActivityPath = (short) msg.chan4_raw;
                }
                mRcOverridePacket = null;
            }else{
                mRcOverridePacket = msg;//this msg is for rc
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
            if( mRcOverridePacket != null){
                onRcChanged(0,mRcOverridePacket.chan1_raw);
                onRcChanged(1,mRcOverridePacket.chan2_raw);
                onRcChanged(2,mRcOverridePacket.chan3_raw);
                onRcChanged(3,mRcOverridePacket.chan4_raw);
                onRcChanged(4,mRcOverridePacket.chan5_raw);
                onRcChanged(5,mRcOverridePacket.chan6_raw);
                onRcChanged(6,mRcOverridePacket.chan7_raw);
                onRcChanged(7,mRcOverridePacket.chan8_raw);
            }
            onActivityPathChanged();
            onConnectStatusChanged();
            if( mModeForConnect == GCS_ID){
                doSendRcOverrideByLocal();
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

/*
    //###########################################################   BluetoothConnection
    public boolean isJostickDisconnected()
    {
        return mBleConnect==null || mBleConnect.getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED;
    }
    public void doJostickConnect()
    {
        if( mAddress != null && isJostickDisconnected()){
            //if( mBleConnect == null) {
            mBleConnect = new BluetoothConnection(this.getContext(), mAddress);
            mBleConnect.addMavLinkConnectionListener(mUartName, mUartlistener);
            //}
            mBleConnect.connect();
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

*/

    //############################################# uart

    public boolean isJostickDisconnected()
    {
        return mUartConnect==null || mUartConnect.getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED;
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
        mUartConnect.sendMavPacket(pack);
    }


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
                alertUser("home distance::"+update);
                //mDistanceNow = Integer.parseInt(update);
            }
        }
    }

}
