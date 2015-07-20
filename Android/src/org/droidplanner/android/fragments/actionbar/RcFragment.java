package org.droidplanner.android.fragments.actionbar;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
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


public class RcFragment  extends ApiListenerFragment  implements View.OnClickListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = RcFragment.class.getSimpleName();
    private static final String ACTION_FLIGHT_ACTION_BUTTON = "Copter flight action button";
    Switch mSwitch ;
    TextView mStatusText;



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
        Toast.makeText(this.getActivity().getApplicationContext(), TAG+":"+message, Toast.LENGTH_SHORT).show();
        debugMsg(message);
    }
    private  void debugMsg(String msg){
        Log.d(TAG, msg);
        mStatusText.setText(msg);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rc_control, container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwitch = (Switch) getActivity().findViewById(R.id.rcSwitch);
        if(mSwitch != null) {
            mSwitch.setChecked(false);
            mSwitch.setOnClickListener(this);
        }else{
            alertUser("Switch init Error");
        }

        mStatusText = (TextView) getActivity().findViewById(R.id.statusText);

        doInitRcOutput();
    }

    @Override
    public void onApiConnected() {
        //super.onApiConnected();
        alertUser("onApiConnected");
        isApiConnect = true;
        getBroadcastManager().registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onApiDisconnected() {
        //super.onApiDisconnected();
        alertUser("onApiDisconnected");
        isApiConnect = false;
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
            default:
                eventBuilder = null;
                break;
        }

        if (eventBuilder != null) {
            GAUtils.sendEvent(eventBuilder);
        }
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
                ret = true;
            }else{
                //mRcOutput = null;
                ret = false;
            }
        }else{
            alertUser("Ensure the flight is connected");
            return false;
        }
        return ret;
    }
    private void stopRcOutput(){
        if( isStarted() ){
            mRcOutput.stop();
            //mRcOutput = null;
        }
    }
    private  void doInitRcOutput(){
        mRcOutput = new JgRcOutput(this.getContext(),mHandler);
        mRcOutput.setmMode(JgRcOutput.SOFTWAREMODE);
        mRcOutput.setRate(50);
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
            debugMsg("Refreshing Parameters ... ");
        } else {
            Toast.makeText(getActivity(), R.string.msg_connect_first, Toast.LENGTH_SHORT).show();
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
                    break;
                default:
                    alertUser("unknow msg frome rcoutput");
                    break;
            }
            super.handleMessage(msg);
        }
    };



}
