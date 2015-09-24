package org.droidplanner.android.fragments.actionbar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
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

import com.google.android.gms.analytics.HitBuilders;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.follow.FollowState;
import com.o3dr.services.android.lib.gcs.follow.FollowType;

import org.droidplanner.android.DroidPlannerApp;
import org.droidplanner.android.JgRcOutput;
import org.droidplanner.android.R;
import org.droidplanner.android.activities.FlightActivity;
import org.droidplanner.android.activities.helpers.SuperUI;
import org.droidplanner.android.dialogs.SlideToUnlockDialog;
import org.droidplanner.android.dialogs.YesNoDialog;
import org.droidplanner.android.dialogs.YesNoWithPrefsDialog;
import org.droidplanner.android.fragments.control.BaseFlightControlFragment;
import org.droidplanner.android.fragments.helpers.ApiListenerFragment;
import org.droidplanner.android.proxy.mission.MissionProxy;
import org.droidplanner.android.utils.analytics.GAUtils;
import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;
import org.droidplanner.android.widgets.rcSeekbarView;

import java.util.Arrays;


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
    private int pressKeyCount=0;
    private VlcVideoFragment mVlcVideo;
    private Button playBtn ;
    private EditText videoAddr;
    //private DroidPlannerApp dpApp;
    private DroidPlannerPrefs dpPrefs;

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
        Log.d(TAG, message);
        //debugMsg(message);
    }
    private  void debugMsg(String msg){
        Log.d(TAG, msg);
        //mStatusText.setText(msg);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        alertUser("onCreateView");
        return inflater.inflate(R.layout.fragment_rc_control, container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        alertUser("onViewCreated");

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

        setRcChangeRange(rcChangeRange);
        SeekBar rcRangBar = (SeekBar)this.getActivity().findViewById(R.id.rcRangeSeekBar);
        rcRangBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setRcChangeRange(progress);
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
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        alertUser("Rc Fragment onAttach");
    }
    @Override
    public void onDetach() {
        super.onDetach();
        alertUser("Rc Fragment onDetach");
        //mListener = null;
    }
    @Override
    public void onApiConnected() {
        //super.onApiConnected();
        alertUser("onApiConnected");
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
        alertUser("onApiDisconnected");
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
        mRcOutput.setRate(2);
        //mRcOutput.setDrone(getDrone());
        //mRcOutput.start();
    }

    //********************************************* reseponed the ui event
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

    public boolean doKeyEven(int keyCode, KeyEvent event) {
        int id=-1;
        int rc=0;
        int i;
        boolean press;

        if( event.ACTION_DOWN == event.getAction()) {
            pressKeyCount = pressKeyCount>1?pressKeyCount:1+pressKeyCount;
            press = true;
            debugMsg("a key down:" + keyCode);
            mStatusText.setText("a key down:" + keyCode);
        }else {
            pressKeyCount = 0;
            press = false;
            debugMsg("a key up" + keyCode);
            mStatusText.setText("a key up:" + keyCode);
        }

        if( pressKeyCount >1 )
                //&& keyCode != mRcOutput.getRcKeyById(JgRcOutput.THRID,JgRcOutput.KeyADDTYPE)
                //&& keyCode != mRcOutput.getRcKeyById(JgRcOutput.THRID,JgRcOutput.KeySUBTYPE)  )
        { //ignore the long press event
            //if( id != JgRcOutput.THRID ){
            return true;
            //}
        }

        //if( !isStarted() )
         //   return true;
        for( i = 0 ; i<= JgRcOutput.CHN8ID; i++){
            if(keyCode == mRcOutput.getRcKeyById(i,JgRcOutput.KeyADDTYPE)){
                id = i;
                if( press ) { //key down
                    rc = rcChangeRange;
                }else {        //key up
                    if( keyLockRang[i] )//key lock a range
                        rc = rcChangeRange * (-1);
                    else
                        rc = 0;
                }
                break;
            }
            if( keyCode == mRcOutput.getRcKeyById(i,JgRcOutput.KeySUBTYPE)){
                id = i;
                if( press ) {
                    rc = rcChangeRange * (-1);
                }else {
                    if(keyLockRang[i] )
                        rc = rcChangeRange;
                    else
                        rc = 0;
                }
                break;
            }
        }

        //update rc
        debugMsg("updateRcSeekBar id,rc="+id+","+rc);
        if( id != -1 ){
            doRcChanged(id, mRcOutput.getRcById(id)+rc );
            return true;
        }else{
            return false;
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
        }

    }

    void setRcChangeRange(int range)
    {
        SeekBar bar= (SeekBar) this.getActivity().findViewById(R.id.rcRangeSeekBar);
        TextView text = (TextView) this.getActivity().findViewById(R.id.rcRangeText);
        if( pressKeyCount ==0 ) {
            rcChangeRange = range;
            debugMsg("rcChange to "+rcChangeRange);

        }
        if( bar != null )
            bar.setProgress(rcChangeRange);
        if( text != null )
            text.setText("diff("+rcChangeRange+")");
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
            if( mRcOutput == null) {
                super.handleMessage(msg);
                return;
            }
            switch (msg.what) {
                case JgRcOutput.DRONE_ERROR:
                    alertUser("Drone has something bad status, RcOutput exit");
                case JgRcOutput.ALLID:
                    for( int i=0 ; i<= JgRcOutput.CHN8ID; i++)
                        doUpdateRcUi(i);
                    break;
                default:
                    alertUser("unknow msg frome rcoutput");
                    break;
            }
            super.handleMessage(msg);
        }
    };



}
