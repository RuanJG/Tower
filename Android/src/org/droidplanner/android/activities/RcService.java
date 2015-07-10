package org.droidplanner.android.activities;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by joe on 2015/7/10.
 */
public class RcService extends Service {
    private static final String TAG = "RcService";

    private IBinder rcbinder=new RcService.RcBinder();
    public class RcBinder extends Binder{
        public RcService getService(){
            Log.i(TAG, "getService");
            return RcService.this;
        }
    }

    boolean mRunning = false;
    @Override
    public IBinder onBind(Intent intent) {
        return rcbinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.i(TAG, "Service onUnbind--->");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();

        mRunning =true;
        new Thread(new Runnable() {
            public void run() {
                int count= 0;
                while (mRunning) {
                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {

                    }
                    count++;
                    Log.v("CountService", "Count is" + count);
                }
            }
        }).start();
    }

    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "onStart");
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        return START_STICKY;
    }



    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mRunning = false;
    }



}
