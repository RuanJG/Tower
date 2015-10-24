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
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.MAVLinks.MAVLinkPacket;
import com.MAVLinks.common.msg_rc_channels_override;
import com.google.android.gms.analytics.HitBuilders;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.VehicleMode;

import org.droidplanner.android.DroidPlannerApp;
import org.droidplanner.android.R;
import org.droidplanner.android.activities.helpers.SuperUI;
import org.droidplanner.android.fragments.helpers.ApiListenerFragment;
import org.droidplanner.android.utils.analytics.GAUtils;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;

import java.util.Arrays;

import org.ruan.connection.BluetoothConnection;
import org.ruan.connection.MavLinkConnection;
import org.ruan.connection.MavLinkConnectionListener;

public class RcFragment  extends ApiListenerFragment  implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = RcFragment.class.getSimpleName();
    private static final int RANGEID = 20;
    private static final String ACTION_FLIGHT_ACTION_BUTTON = "Copter flight action button";
    Switch mSwitch ;
    TextView mStatusText;
    private static boolean []keyLockRang=new boolean[8];

    private Spinner rcOutputMode ;


    private int rcChangeRange = 30;
    private static final int RcLockRangChanCount = 4;
    public int []RcLockRangValue = new int[RcLockRangChanCount];
    private int lastChanRCID = 0;


    private VlcVideoFragment mVlcVideo;
    private Button playBtn ;
    private EditText videoAddr;
    //private DroidPlannerApp dpApp;
    private DroidPlannerPrefs dpPrefs;

    private final static String CopterJostickBtName="copterJostick";
    private final static String CameraJostickBtName="cameraJostick";
    public final static String CopterJostickName="copterJ";
    public final static String CameraJostickName="cameraJ";

    public final static int BleJostickHandleMsgId =4;
    public final static int Get4GIPHandleMsgId =5;
    private BleJostick mCopterBleJostick;
    private BleJostick mCameraBleJostick;

    Router4GFindWanIp m4GIpRouter;

    public static final int O2O_ACTIVITY_ADDR_RESULT_CODE = 30;

    IRcOutputListen seekBarListen = new IRcOutputListen() {
        @Override
        public boolean doSetRcValue(int id, int value) {
            return doSetRc(id,value);
        }
    };


    private static final IntentFilter eventFilter = new IntentFilter();
    static {
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
                case AttributeEvent.STATE_CONNECTED:
                    doCopterConnect();
                    break;
                case AttributeEvent.PARAMETERS_REFRESH_COMPLETED:
                    doCopterParamRefreshed();
                    break;
                case AttributeEvent.STATE_DISCONNECTED:
                    doCopterDisconnect();
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



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rc_control, container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if( mSwitch == null )
        {
            mSwitch = (Switch) getActivity().findViewById(R.id.rcSwitch);
            if(mSwitch != null) {
                mSwitch.setChecked(false);
                mSwitch.setOnClickListener(this);
            }else{
                alertUser("Switch init Error");
            }
        }
        if(mStatusText == null)
            mStatusText = (TextView) getActivity().findViewById(R.id.statusText);

        initRcSeekBar();

        setupVlcVideo();

        setRcChangeRange(300, -1);
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

        if( rcOutputMode == null) {
            rcOutputMode = (Spinner) getActivity().findViewById(R.id.rcOutputMode);
            rcOutputMode.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "get position,id=" + position + id);
                    if( id == 0 ){
                        mRcOutput.setmMode(JgRcOutput.HARDMODE);
                    }else{
                        mRcOutput.setmMode(JgRcOutput.SOFTWAREMODE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });
        }

        Button btnble = (Button) this.getActivity().findViewById(R.id.buttonBleCopter);
        if( btnble != null )
            btnble.setOnClickListener(this);
        Button btnble2 = (Button) this.getActivity().findViewById(R.id.buttonBleCamera);
        if( btnble2 != null )
            btnble2.setOnClickListener(this);

        Button btn3 = (Button) this.getActivity().findViewById(R.id.buttonSetIp);
        if( btn3 != null )
            btn3.setOnClickListener(this);
        Button btn4 = (Button) this.getActivity().findViewById(R.id.buttonO2oIp);
        if( btn4 != null )
            btn4.setOnClickListener(this);

        m4GIpRouter = new Router4GFindWanIp(mHandler);
        if( dpPrefs == null)
            dpPrefs = new DroidPlannerPrefs(this.getContext());
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
        isApiConnect = true;
        getBroadcastManager().registerReceiver(eventReceiver, eventFilter);

        //init and resume to the last status
        doInitRcOutput();
        if( getDrone().isConnected()){
            if( mSwitch.isChecked() )
                mSwitch.setChecked(false);
        }
    }

    @Override
    public void onApiDisconnected() {
        //super.onApiDisconnected();
        isApiConnect = false;
        doStop();
        getBroadcastManager().unregisterReceiver(eventReceiver);
    }


    @Override
    public void onClick(View v) {
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(GAUtils.Category.FLIGHT);

        final Drone drone = getDrone();
        eventBuilder = null;
        switch (v.getId()) {
            case R.id.rcSwitch:
                if( mSwitch.isChecked() ){
                    doStart();
                }else{
                    doStop();
                }
                break;
            case R.id.videoPlayBtn:
                if( mVlcVideo.isPlaying()){
                    stopPlayVideo();
                }else{
                    startPlayVideo();
                }
                break;
            case R.id.buttonBleCopter:
                if( mCopterBleJostick == null){
                    findAndConnectBleJostick(CopterJostickName, CopterJostickBtName);
                }else{
                    if( mCopterBleJostick.isJostickDisconnected()){
                        mCopterBleJostick.doJostickConnect();
                    }else{
                        mCopterBleJostick.doJostickDisconnect();
                    }
                }
                break;
            case R.id.buttonSetIp:
                m4GIpRouter.doGetIp();
                break;
            case R.id.buttonO2oIp:
                Intent i;
                i = new Intent(this.getContext(),O2oActivity.class);
                startActivityForResult(i, O2O_ACTIVITY_ADDR_RESULT_CODE);
                break;
            default:
                eventBuilder = null;
                break;
        }

        if (eventBuilder != null) {
            GAUtils.sendEvent(eventBuilder);
        }
    }

    private void setupVlcVideo()
    {
        //mVlcVideo = (VlcVideoFragment) this.getActivity().fragmentManager.findFragmentById(R.id.vlcVideoView);
        if (mVlcVideo == null) {
            debugMsg("vlcvideo is null , create new");
            mVlcVideo  = new VlcVideoFragment();
            this.getActivity().getSupportFragmentManager().beginTransaction().add(R.id.vlcVideoView, mVlcVideo).commit();
        }
        if( dpPrefs == null)
        dpPrefs = new DroidPlannerPrefs(this.getContext());
        if(playBtn == null) {
            playBtn = (Button) this.getActivity().findViewById(R.id.videoPlayBtn);
            playBtn.setOnClickListener(this);
        }
        if( videoAddr == null) {
            videoAddr = (EditText) this.getActivity().findViewById(R.id.videoAddrText);
            videoAddr.setFocusable(false);
            videoAddr.setText(getIpAddr());
            videoAddr.setVisibility(View.GONE);
        }
    }

    private String getIpAddr()
    {
        final int connectionType = dpPrefs.getConnectionParameterType();
        String addr;
        String port;

        switch (connectionType) {
            case ConnectionType.TYPE_UDP:
                if(dpPrefs.isUdpPingEnabled()){
                    addr = dpPrefs.getUdpPingReceiverIp();
                    port = String.valueOf(dpPrefs.getUdpServerPort());
                }else {
                    addr = dpPrefs.getUdpPingReceiverIp();
                    port = "8554";
                }
                break;
            case ConnectionType.TYPE_TCP:
                addr = dpPrefs.getTcpServerIp();
                port = String.valueOf(dpPrefs.getTcpServerPort());
                break;
            default:
                addr = "192.168.2.1";
                port = "8554";
                break;
        }
        alertUser("connect video frome "+addr +":"+port);
        return addr;
    }

    private String getVideoAddr()
    {
        String addr;
        addr = getIpAddr();
        //addr = "rtsp://"+videoAddr.getText()+":8554";
        videoAddr.setText(addr);
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



    //********************************* rcFragment func
    private JgRcOutput mRcOutput = null;
    private boolean isApiConnect= false;
    private boolean isCopterConnect(){
        return null != getDrone() && getDrone().isConnected();
    }
    private boolean isSwitchOn = false;
    private boolean isParamUpdated = false;

    private boolean isReady(){
        return isApiConnect && isCopterConnect() && mRcOutput !=null;
    }
    public boolean isStarted(){
        return mRcOutput != null && mRcOutput.isStarted();
    }

    private boolean startRcOutput(){
        if( isStarted() )
            return true;

        boolean ret;
        if( isReady() ){
            if( mRcOutput.isReady() && mRcOutput.start() ){
                setRcSeekBarTrimValue();
                ret = true;
            }else{
                //mRcOutput = null;
                ret = false;
            }
        }else{
            alertUser("Ensure the flight is connected");
            return false;
        }
        debugMsg("Rc Outputing...");
        return ret;
    }
    private void stopRcOutput(){
        if( isStarted() ){
            mRcOutput.stop();
            //mRcOutput = null;
            debugMsg("Rc Stop");
        }
    }
    private  void doInitRcOutput(){
        mRcOutput = new JgRcOutput(this.getContext(),mHandler);
        mRcOutput.setmMode(JgRcOutput.SOFTWAREMODE);
        mRcOutput.setRate(40);
        //mRcOutput.setDrone(getDrone());
        //mRcOutput.start();
    }

    //********************************************* reseponed the ui event
    private void getFlightMode() {
        State droneState = getDrone().getAttribute(AttributeType.STATE);
        if (droneState == null)
            return;
        final VehicleMode flightMode = droneState.getVehicleMode();
        if (flightMode == null)
            return;

        switch (flightMode) {
            case COPTER_AUTO:
                break;
            case COPTER_GUIDED:
                break;
            case COPTER_RTL:
                break;
            case COPTER_LAND:
                break;
            default:
                break;
        }
       // getDrone().doGuidedTakeoff(10);
    }
    private void doCopterConnect(){
        mRcOutput.setDrone(getDrone());
        isParamUpdated =false;
        //isCopterConnect = true;
        //mSwitch.setEnabled(true);
        alertUser("DoConnect !!!");
    }
    private void doCopterDisconnect(){
        alertUser("DoDisConnect !!!");
        //isCopterConnect = false;
        if( isStarted() ) {
            stopRcOutput();
        }
        mSwitch.setChecked(false);
        //mSwitch.setEnabled(false);
        mRcOutput.setDrone(null);
        isParamUpdated =false;
    }
    private void doStartFailed(String msg){
        isSwitchOn = false;
        mSwitch.setChecked(false);
        alertUser(msg);
    }
    private void doStart(){
        if( !isCopterConnect() ){
            doStartFailed(" Connect Flight First");
            return;
        }
        if( isStarted() ){
            isSwitchOn = true;
            return;
        }
        if( mRcOutput.getmMode() == JgRcOutput.HARDMODE ){
            if( !startRcOutput() ){
                doStartFailed(" Start Rc Output Failed");
                return;
            }
        }
        if(mRcOutput.getmMode() == JgRcOutput.SOFTWAREMODE && isParamUpdated) {
            if( !startRcOutput() ){
                doStartFailed(" Start Rc Output SOFTWAREMODE Failed");
                return;
            }
        }else{
            // waiting update parameter
            //refreshParameters();
            //will be start in doCopterParamRefresh
            debugMsg("Start Rc Output after parameter received");
        }
        isSwitchOn = true;
    }
    private void doStop(){
        isSwitchOn = false;
        stopRcOutput();
    }
    private void doCopterParamRefreshed(){
        alertUser("doCopterParamRfereshed..");
        isParamUpdated =true;
        if( isSwitchOn )
            startRcOutput();
    }
    private void refreshParameters() {
        if (isCopterConnect()) {
            getDrone().refreshParameters();
            alertUser("Refreshing Parameters ... ");
        } else {
            Toast.makeText(getActivity(), R.string.msg_connect_first, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean changeRcByKey(int keyCode,boolean press, boolean lockTrimValue)
    {
        int i,id=-1;
        int rc=0;

        //if( !isStarted() )
        //   return true;
        if( press && keyCode == mRcOutput.getRcKeyById(mRcOutput.RANG_CTRL_KEY_ID,mRcOutput.KeyADDTYPE)){
            rc = RcLockRangValue[lastChanRCID];
            if( rc< 450) {
                rc += 30;
                setRcChangeRange(rc,lastChanRCID);
            }
            return true;
        }else if( press && keyCode == mRcOutput.getRcKeyById(mRcOutput.RANG_CTRL_KEY_ID,mRcOutput.KeySUBTYPE)){
            rc = RcLockRangValue[lastChanRCID];
            if( rc >=30 ) {
                rc -= 30;
                setRcChangeRange(rc,lastChanRCID);
            }
            return true;
        }


        for( i = 0 ; i<= JgRcOutput.CHN8ID; i++){
            if(keyCode == mRcOutput.getRcKeyById(i,JgRcOutput.KeyADDTYPE)){
                id = i;
                if( lockTrimValue )
                    rc = mRcOutput.getDefalutRcById(id);
                else
                    rc = mRcOutput.getRcById(id);
                if( press ) { //key down
                    //rc += rcChangeRange;
                    //rc += lockTrimValue? rcChangeRange:30; //30 step
                    rc+= id < RcLockRangChanCount ? RcLockRangValue[id]:30 ;
                }
                break;
            }
            if( keyCode == mRcOutput.getRcKeyById(i,JgRcOutput.KeySUBTYPE)){
                id = i;
                if( lockTrimValue )
                    rc = mRcOutput.getDefalutRcById(id);
                else
                    rc = mRcOutput.getRcById(id);
                if( press ) {
                    //rc -= rcChangeRange;
                    //rc -= lockTrimValue? rcChangeRange:30; //30 step
                    rc-= id < RcLockRangChanCount ? RcLockRangValue[id]:30 ;
                }
                break;
            }
        }

        //update rc
        debugMsg("updateRcSeekBar id,rc="+id+","+rc);
        if( id != -1 && rc != 0 ){
            doRcChanged(id, rc);
            lastChanRCID = id;
            setRcChangeRange(RcLockRangValue[id],id);
            return true;
        }else{
            return false;
        }
    }
    private int pressKeyCount=0;
    private int lastPressKeyCode=0;
    public boolean doKeyEven(int keyCode, KeyEvent event) {
        int id=-1;
        int rc=0;
        int i;
        boolean lockTrim=true;
        boolean lastLockTrim=true;
        /*
        if(keyCode == mRcOutput.getRcKeyById(mRcOutput.THRID,mRcOutput.KeyADDTYPE)
                || keyCode == mRcOutput.getRcKeyById(mRcOutput.THRID,mRcOutput.KeySUBTYPE) ){
            lockTrim = false;
        }*/

        if( event.ACTION_DOWN == event.getAction()) {
            mStatusText.setText("a key down:" + keyCode);
            if(pressKeyCount <= 0) {
                //first time press key
                pressKeyCount = 1;
                lastPressKeyCode = keyCode;
                return changeRcByKey(keyCode,true,lockTrim);
            }else{
                if( lastPressKeyCode == keyCode){// long press
                    if( !lockTrim ){
                        return changeRcByKey(keyCode,true,lockTrim);
                    }
                    return  true;
                }else{//no release last key , and new key press
                    /*
                    if(lastPressKeyCode == mRcOutput.getRcKeyById(mRcOutput.THRID,mRcOutput.KeyADDTYPE)
                            || lastPressKeyCode == mRcOutput.getRcKeyById(mRcOutput.THRID,mRcOutput.KeySUBTYPE) ){
                        lastLockTrim = false;
                    }*/
                    changeRcByKey(keyCode,false,lastLockTrim);
                    return changeRcByKey(keyCode,true,lockTrim);
                }
            }
        }else {
            pressKeyCount = 0;
            mStatusText.setText("a key up:" + keyCode);
            return changeRcByKey(keyCode,false,lockTrim);
        }
    }

    //mRcOutput call back use
    public void doUpdateRcUi(int id){
        updateRcSeekBar(id);
    }
    //key event or other control event use
    public void doRcChanged(int id,int value){
        if( mRcOutput.setRcById(id, (short) value) ){
            doUpdateRcUi(id);
        }
    }
    //seekbar call it to set rc
    private boolean doSetRc(int id, int value){
        return mRcOutput.setRcById(id, (short) value);
    }

    private void initRcSeekBar(){
        rcSeekbarView bar;
        Arrays.fill(keyLockRang, false);
        keyLockRang[JgRcOutput.ROLLID] = true;
        keyLockRang[JgRcOutput.YAWID] = true;
        keyLockRang[JgRcOutput.PITCHID] = true;

        for( int i=0; i<= JgRcOutput.CHN8ID; i++){
            bar = getSeekBarByRcId(i);
            bar.setId(i);
            bar.setLockValue(keyLockRang[i]);
            bar.setRcListen(seekBarListen);
            bar.setProcess(rcSeekbarView.mMin);
        }

    }

    private void updateRcChanRangeUi(int id, int val)
    {
        SeekBar bar= (SeekBar) this.getActivity().findViewById(R.id.rcRangeSeekBar);
        TextView text = (TextView) this.getActivity().findViewById(R.id.rcRangeText);
        if( bar != null )
            bar.setProgress(val);
        if( text != null )
            text.setText("Rc"+id+"("+val+")");
    }
    void setRcChangeRange(int range,int id)
    {
        if( id >= 0 && id < RcLockRangChanCount ){
            RcLockRangValue[id] = range;
            updateRcChanRangeUi(id,range);
        }else{
            //-1 all
            int i;
            for( i=0 ; i< RcLockRangChanCount; i++)
                RcLockRangValue[i] = range;
            updateRcChanRangeUi(i,range);
        }
    }
    private rcSeekbarView getSeekBarByRcId(int id){
        rcSeekbarView bar;
        switch (id){
            case JgRcOutput.ROLLID:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.rcRollSeekBar);
                break;
            case JgRcOutput.PITCHID:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.rcPitchSeekBar);
                break;
            case JgRcOutput.THRID:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.rcThrSeekBar);
                break;
            case JgRcOutput.YAWID:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.rcYawSeekBar);
                break;
            case JgRcOutput.CHN5ID:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.rcRc5SeekBar);
                break;
            case JgRcOutput.CHN6ID:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.rcRc6SeekBar);
                break;
            case JgRcOutput.CHN7ID:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.rcRc7SeekBar);
                break;
            case JgRcOutput.CHN8ID:
                bar = (rcSeekbarView) getActivity().findViewById(R.id.rcRc8SeekBar);
                break;
            default:
                bar = null;
        }
        return bar;
    }
    private void updateRcSeekBar(int id) {
        rcSeekbarView bar;
        debugMsg("updateRcSeekBar");
        bar = getSeekBarByRcId(id);
        debugMsg("try update id ="+id);
        if( bar != null ){
            bar.setProcess(mRcOutput.getRcById(id));
        }
    }
private void setRcSeekBarTrimValue()
{
    rcSeekbarView bar;
    for( int i=0; i<= JgRcOutput.CHN8ID; i++){
        bar = getSeekBarByRcId(i);
        bar.setRcTrimValue(mRcOutput.getDefalutRcById(i));
        bar.setMinMax(mRcOutput.getDefalutMinRcById(i),mRcOutput.getDefalutMaxRcById(i));
        //bar.setLockValue(keyLockRang[i]);
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
                case JgRcOutput.DRONE_ERROR:
                    alertUser("Drone has something bad status, RcOutput exit");
                    break;
                case JgRcOutput.ALLID:
                    for( int i=0 ; i<= JgRcOutput.CHN8ID; i++)
                        doUpdateRcUi(i);
                    break;
                case BleJostickHandleMsgId:
                    doHandleBleMessage(msg.getData());
                    break;
                case Get4GIPHandleMsgId:
                    String ip = msg.getData().getString("ip");
                    if(ip.equals(Router4GFindWanIp.UNVAILD_IP))
                        alertUser("no avalible ip fine");
                    else
                        doSetIpToDrone(ip);
                    break;
                default:
                    alertUser("unknow msg frome rcoutput");
                    break;
            }
            //super.handleMessage(msg);
        }
    };



    //set4G ip
    private void doSetIpToDrone(String ip)
    {
        alertUser(ip);
        if( dpPrefs == null)
            dpPrefs = new DroidPlannerPrefs(this.getContext());
        //final int connectionType = dpPrefs.getConnectionParameterType();
        dpPrefs.setTcpServerIp(ip);
    }


    //########################## ble function
    //#### ui even
    private void doHandleBleMessage(Bundle data)
    {
        String id = data.getString("id");
        String name = data.getString("name");

        if( id.equals("onComError")){
            alertUser(name+" onComErr:"+data.getString("string"));
            if( name.equals(CopterJostickName)){
                 mCopterBleJostick.doJostickDisconnect();
            }else if(name.equals(CameraJostickName)){
                mCameraBleJostick.doJostickDisconnect();
            }
        }else if( id.equals("onStartingConnection") ) {
            alertUser(name+" onStartingConnection");

        }else if( id.equals("onConnect")){
            onBleConnected(name);

        } else if (id.equals("onDisconnect")){
            onBleDisconnected(name);

        }else if (id.equals("onReceivePacket")){
            if( name.equals(CopterJostickName)){
                //alertUser(name+":"+ mCopterBleJostick.getRc(0) + " ," + mCopterBleJostick.getRc(1) + " ," +mCopterBleJostick.getRc(2) + " ," + mCopterBleJostick.getRc(3));
                for( int i= 0 ; i <= JgRcOutput.CHN8ID; i++){
                    mRcOutput.setRcByIdForRealRcDevice(i, mCopterBleJostick.getRc(i));
                    doUpdateRcUi(i);
                }
            }else if(name.equals(CameraJostickName)){
                alertUser(name+":"+ mCameraBleJostick.getRc(0) + " ," + mCameraBleJostick.getRc(1) + " ," +mCameraBleJostick.getRc(2) + " ," + mCameraBleJostick.getRc(3));
            }

        }else{
            ;//no fix msg
        }
    }
    private void onBleConnected(String name){
        Button btn;
        if( name.equals(CameraJostickName) ){
            btn = (Button) this.getActivity().findViewById(R.id.buttonBleCamera);
        }else if( name.equals(CopterJostickName)){
            btn = (Button) this.getActivity().findViewById(R.id.buttonBleCopter);
        }else{
            btn = null;
        }
        if( btn != null ){
            btn.setText(name+" DisConnect");
        }
    }
    private void onBleDisconnected(String name){
        Button btn;
        if( name.equals(CameraJostickName) ){
            btn = (Button) this.getActivity().findViewById(R.id.buttonBleCamera);
        }else if( name.equals(CopterJostickName)){
            btn = (Button) this.getActivity().findViewById(R.id.buttonBleCopter);
        }else{
            btn = null;
        }
        if( btn != null ){
            btn.setText(name+" Connect");
        }
    }
    //### find devices
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FindBluetoothDevicesActivity.REQUEST_BLE_ADDR_CODE:
                if (resultCode == Activity.RESULT_CANCELED) {
                    // Bluetooth activation was denied by the user. Dismiss this activity.
                    //finish();
                    ;
                }
                if( resultCode == FindBluetoothDevicesActivity.REQUEST_BLE_ADDR_CODE){
                    if( data.getBooleanExtra("res",false) ){
                        String address;
                        address = data.getStringExtra(CameraJostickName);
                        if( address != null ){
                            if( mCameraBleJostick == null ) {
                                mCameraBleJostick = new BleJostick(this.getContext(), mHandler, CameraJostickName, address);
                                mCameraBleJostick.doJostickConnect();
                            }else if( mCameraBleJostick.isJostickDisconnected()){
                                mCameraBleJostick.setAddress(address);
                                mCameraBleJostick.doJostickConnect();
                            }else{
                                ;//has connected
                            }
                        }
                        address = data.getStringExtra(CopterJostickName);
                        if( address != null ){
                            if( mCopterBleJostick == null ) {
                                mCopterBleJostick = new BleJostick(this.getContext(), mHandler, CopterJostickName, address);
                                mCopterBleJostick.doJostickConnect();
                            }else if( mCopterBleJostick.isJostickDisconnected()){
                                mCopterBleJostick.setAddress(address);
                                mCopterBleJostick.doJostickConnect();
                            }else{
                                ;//has connected
                            }
                        }
                    }else{
                        alertUser("Can not find the ble jostick");
                    }

                }
                break;
            case O2O_ACTIVITY_ADDR_RESULT_CODE:{
                String ip = null;
                if( data != null)
                    ip = data.getStringExtra("ip");
                if( ip != null && !ip.equals(O2oActivity.UNVARLID_IP)){
                    doSetIpToDrone(ip);
                    if( ! getDrone().isConnected()){
                        ((SuperUI) getActivity()).toggleDroneConnection();
                        DroidPlannerApp dpApp =(DroidPlannerApp)this.getActivity().getApplication();
                        dpApp.connectToDrone();
                    }
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

}
