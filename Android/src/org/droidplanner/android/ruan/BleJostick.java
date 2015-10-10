package org.droidplanner.android.ruan;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.MAVLinks.MAVLinkPacket;
import com.MAVLinks.common.msg_rc_channels_override;

import org.ruan.connection.BluetoothConnection;
import org.ruan.connection.MavLinkConnection;
import org.ruan.connection.MavLinkConnectionListener;

import java.util.Arrays;

/**
 * Created by joe on 2015/10/10.
 */
public class BleJostick {
    private BluetoothConnection mBleConnect;
    private final Handler mHandler;
    private  String mName;
    private final Context mContext;
    private String mAddress;

    private short[] mRcs = new short[8];
    public final static int MAX_CHAN_COUNT = 8;
    public final static int UNVALIABLE_RC_VALUE = 0;





    public BleJostick(Context c,Handler h, String name, String address) {
        mHandler = h;
        mContext = c;
        mName = name;
        mAddress = address;
        Arrays.fill(mRcs,(short)UNVALIABLE_RC_VALUE);
    }

    public void setAddress(String adr) { mAddress = adr;}
    public String getAddress(){ return mAddress;}
    public void setName(String name) { mName = name; }
    public String getName(){ return mName;}

    public short getRc(int chanIndex){
        if( chanIndex < MAX_CHAN_COUNT )
            return mRcs[chanIndex];
        else
            return UNVALIABLE_RC_VALUE;
    }

    public boolean isJostickDisconnected()
    {
        return mBleConnect==null || mBleConnect.getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED;
    }

    public void doJostickConnect()
    {
        if( mAddress != null && isJostickDisconnected()){
            //if( mBleConnect == null) {
                mBleConnect = new BluetoothConnection(mContext, mAddress);
                mBleConnect.addMavLinkConnectionListener(mName, mBlelistener);
            //}
            mBleConnect.connect();
        }
    }
    public void doJostickDisconnect()
    {
        if( isJostickDisconnected()) return;

        mBleConnect.removeMavLinkConnectionListener(mName);
        if (mBleConnect.getMavLinkConnectionListenersCount() == 0 && mBleConnect.getConnectionStatus() != MavLinkConnection.MAVLINK_DISCONNECTED) {
            //Timber.d("Disconnecting...");
            mBleConnect.disconnect();
        }
        //as i removelisten , onDisconnect will not recived
        Message message = new Message();
        message.what = RcFragment.BleJostickHandleMsgId;
        Bundle bundle = new Bundle();
        bundle.putLong("time",0);
        bundle.putString("id", "onDisconnect");
        bundle.putString("name",mName);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    private  MavLinkConnectionListener mBlelistener=new MavLinkConnectionListener() {
        @Override
        public void onStartingConnection() {
            Message message = new Message();
            message.what = RcFragment.BleJostickHandleMsgId;
            Bundle bundle = new Bundle();
            bundle.putString("name",mName);
            bundle.putString("id","onStartingConnection");
            message.setData(bundle);
            mHandler.sendMessage(message);
        }

        @Override
        public void onConnect(long connectionTime) {
            Message message = new Message();
            message.what = RcFragment.BleJostickHandleMsgId;
            Bundle bundle = new Bundle();
            bundle.putLong("time",connectionTime);
            bundle.putString("id", "onConnect");
            bundle.putString("name",mName);
            message.setData(bundle);
            mHandler.sendMessage(message);
        }

        @Override
        public void onReceivePacket(MAVLinkPacket packet) {
            if( packet.msgid == msg_rc_channels_override.MAVLINK_MSG_ID_RC_CHANNELS_OVERRIDE) {
                msg_rc_channels_override msg = new msg_rc_channels_override(packet);
                mRcs[0] = (short) msg.chan1_raw;
                mRcs[1] = (short) msg.chan2_raw;
                mRcs[2] = (short) msg.chan3_raw;
                mRcs[3] = (short) msg.chan4_raw;
                mRcs[4] = (short) msg.chan5_raw;
                mRcs[5] = (short) msg.chan6_raw;
                mRcs[6] = (short) msg.chan7_raw;
                mRcs[7] = (short) msg.chan8_raw;

                Message message = new Message();
                Bundle bundle = new Bundle();
                message.what = RcFragment.BleJostickHandleMsgId;
                bundle.putString("id", "onReceivePacket");
                bundle.putString("name",mName);
                message.setData(bundle);
                mHandler.sendMessage(message);
            }
        }

        @Override
        public void onDisconnect(long disconnectionTime) {
            Message message = new Message();
            message.what = RcFragment.BleJostickHandleMsgId;
            Bundle bundle = new Bundle();
            bundle.putLong("time",disconnectionTime);
            bundle.putString("id", "onDisconnect");
            bundle.putString("name",mName);
            message.setData(bundle);
            mHandler.sendMessage(message);
        }

        @Override
        public void onComError(String errMsg) {
            Message message = new Message();
            message.what = RcFragment.BleJostickHandleMsgId;
            Bundle bundle = new Bundle();
            bundle.putString("string",errMsg);
            bundle.putString("id","onComError");
            bundle.putString("name",mName);
            message.setData(bundle);
            mHandler.sendMessage(message);
        }
    };



}
