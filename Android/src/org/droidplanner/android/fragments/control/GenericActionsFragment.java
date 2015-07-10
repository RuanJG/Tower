package org.droidplanner.android.fragments.control;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.o3dr.android.client.Drone;

import org.droidplanner.android.R;
import org.droidplanner.android.activities.RcService;
import org.droidplanner.android.activities.helpers.SuperUI;
import org.droidplanner.android.fragments.control.FlightControlManagerFragment;

/**
 * Provides action buttons functionality for generic drone type.
 */
public class GenericActionsFragment extends BaseFlightControlFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_generic_mission_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        Button connectBtn = (Button) view.findViewById(R.id.mc_connectBtn);
        connectBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.mc_connectBtn:
                ((SuperUI) getActivity()).toggleDroneConnection();
                toggleBinderService();
                break;
        }
    }

    @Override
    public boolean isSlidingUpPanelEnabled(Drone drone) {
        return false;
    }

    RcService mRcService=null;
    ServiceConnection mConn = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            Log.i("RUAN","Service connect");
            mRcService = ((RcService.RcBinder) binder).getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i("RUAN","Service disconnect");
            mRcService = null;
        }
    };
    private void startBinderService(){
        Intent intent=new Intent(getActivity(),RcService.class);
        boolean ret;
        ret = getActivity().bindService(intent,mConn,this.getContext().BIND_AUTO_CREATE);
        if( ret ){
            Log.i("RUAN","bindService ok");
        }else
            Log.i("RUAN","bindService false");

    }
    private void stopBinderService() {
        //Intent intent=new Intent(this,RcService.class);
        Log.i("RUAN","stopbindService ");
        getActivity().unbindService(mConn);
    }
    private  void startService(){
        Intent intent = new Intent(this.getActivity(),
                RcService.class);
        this.getContext().startService(intent);
        Log.i("RUAN", "startService ");
    }
    private  void stopService()
    {
        Intent intent = new Intent(this.getActivity(),
                RcService.class);
        this.getContext().stopService(intent);
        Log.i("RUAN", "stopService ");

    }
    public void toggleBinderService(){
        Log.i("RUAN", "toggleBinderService");
        if( mRcService != null){
            stopBinderService();
            //stopService();
        }else{
            startBinderService();
            //startService();
        }
    }
}
