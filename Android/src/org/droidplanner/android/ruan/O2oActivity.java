package org.droidplanner.android.ruan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.droidplanner.android.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class O2oActivity extends AppCompatActivity {

    private static final String TAG = FindBluetoothDevicesActivity.class.getName();
    private static final int REQUEST_ENABLE_BT = 111;
    public static final String UNVARLID_IP = "0.0.0.0";
    private ProgressBar mProgressBar;
    private Button connectBtn;
    private Button reflashBtn;
    private Button cancleBtn;
    private TextView ipTextView ;
    private TextView statusTextView;
    private EditText userName ;
    private EditText password;
    private EditText copterId;

    private Thread tcpThread = null;
    private boolean threadQuit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_o2o2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cancleBtn = (Button) findViewById(R.id.id_cancle);
        if(cancleBtn != null )
            cancleBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doCancleEven();
                }
            });
        reflashBtn = (Button) findViewById(R.id.id_update);
        if( reflashBtn != null)
            reflashBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateCopterStatusFromO2oServicer();
                }
            });
        connectBtn = (Button) findViewById(R.id.id_connect);
        if( connectBtn != null ) {
            connectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    returnIpandDoConnect();
                }
            });
            connectBtn.setEnabled(false);
        }
        ipTextView = (TextView) findViewById(R.id.ip);
        if( ipTextView != null) ipTextView.setText(UNVARLID_IP);
        statusTextView = (TextView) findViewById(R.id.status);
        mProgressBar = (ProgressBar) findViewById(R.id.id_scan_progress_bar);
        userName = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        copterId = (EditText) findViewById(R.id.copter_id);

        //tcpThread = new Thread(tcpRunner);
        updateCopterStatusFromO2oServicer();

    }

    private void doCancleEven() {
        try {
            if( tcpThread !=null ) tcpThread.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finish();
    }

    private void returnIpandDoConnect() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("ip",ipTextView.getText().toString());
        setResult(RcFragment.O2O_ACTIVITY_ADDR_RESULT_CODE , resultIntent);
        finish();
    }

    private void updateCopterStatusFromO2oServicer() {
        if(mProgressBar!=null) mProgressBar.setVisibility(View.VISIBLE);
        if(reflashBtn != null) reflashBtn.setEnabled(false);
        tcpThread = new Thread(tcpRunner);
        tcpThread.start();
    }
    private void updateOver()
    {
        if(mProgressBar!=null) mProgressBar.setVisibility(View.INVISIBLE);
        if(reflashBtn != null) reflashBtn.setEnabled(true);
        if(! ipTextView.getText().toString().equals(UNVARLID_IP))
            if( connectBtn != null) connectBtn.setEnabled(true);

        tcpThread = null;
    }

    private void sendHttpCmd(String msg)
    {

    }
    private void getCopterStatusFromO2oServicer() {
    }
    private String getStringFromJsion(String data,String name)
    {
        String val="none";
        //{"eid":"te350_4G","ip":"100.68.73.157","mac":"11:22:33:44:55:66","iol":1,"olt":"2015-10-23 12:32:32:000","id":2}
        //if(data.length() <5) return "none";
        try {
            JSONObject a= new JSONObject(data);
            val = a.getString(name);
            if(val == null) val = "none";
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("Ruan","val="+val);

        return val;
    }
    private  boolean getStatusByHttpGet(){
        HttpClient client=new DefaultHttpClient();
        HttpGet httpGet;
        boolean res=false;
        String cmd;
        Message mesg = new Message();
        mesg.what=0;

        cmd = "http://www.o2oc.cn/UAV/"+copterId.getText().toString()+"/"+userName.getText().toString()+"/"+ password.getText().toString();
        Log.e("Ruan",cmd);

        httpGet =new HttpGet(cmd);
        try {
            HttpResponse response1=client.execute(httpGet);
            if(response1.getStatusLine().getStatusCode()==200) {
                InputStream is = response1.getEntity().getContent();
                if (is != null) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int len = 0;
                    byte[] buffer = new byte[1024];
                    Log.e("Ruan", "start reading");
                    while ( !threadQuit &&   (len = is.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    Log.e("Ruan", "start reading over");
                    is.close();
                    ClientConnectionManager cm = client.getConnectionManager();
                    if(cm != null ) cm.shutdown();
                    if( threadQuit ) return false;

                    String info = new String(bos.toByteArray());
                    //String infos[]=info.split("<WanIPAddress>");
                    // String infoss[]=infos[1].split("</WanIPAddress>");
                    Log.e("Ruan", info);
                    //{"eid":"te350_4G","ip":"100.68.73.157","mac":"11:22:33:44:55:66","iol":1,"olt":"2015-10-23 12:32:32:000","id":2}
                    //ipTextView.setText(getStringFromJsion(info,"ip"));
                    //statusTextView.setText(getStringFromJsion(info,"iol"));
                    Bundle bun = new Bundle();
                    bun.putString("data",info);
                    mesg.setData(bun);
                    res = true;

                }else{
                    Log.e("Ruan","read inputstream error");
                }
            }else{
                Log.e("Ruan","respone error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Ruan", "httpget error");
            res = false;
        }
        mHandler.sendMessage(mesg);
        return res;
    }


    private void getStatusByTcp()
    {
        Socket socket = null;
        String cmd;
        byte[] buffer=new byte[1024];
        int len;
        Message mesg = new Message();
        mesg.what=0;

        cmd = "GET /UAV/"+copterId.getText().toString()+"/"+userName.getText().toString()+"/"+ password.getText().toString()+" HTTP/1.1\r\n";
        cmd = cmd+"Host: 61.143.38.63\r\n";
        cmd = cmd +"Accept: */*\r\n";
        cmd = cmd + "Connection: close\r\n";
        cmd =cmd + "Cache-Control: no-cache\r\n";
        Log.e("Ruan", cmd);

        try {
            socket = new Socket("61.143.38.63", 80);
            socket.setSoTimeout(10000);
            if( socket != null && socket.isConnected() ) {
                Log.e("Ruan: ","+connect !!");
                OutputStream outputStream = socket.getOutputStream();
                //byte buffer[] = new byte[4 * 1024];
                outputStream.write(cmd.getBytes());
                outputStream.flush();

                InputStream inputStream = socket.getInputStream();
                while((len=inputStream.read(buffer))> 0){
                    Log.e("Ruan","read "+len+"B"+" "+buffer.toString());
                    //bos.write(buffer,0,len);
                }
                Bundle bun = new Bundle();
                bun.putString("data",buffer.toString());
                mesg.setData(bun);

                inputStream.close();
                socket.close();
            }else{
                Log.e("Ruan","connect error");
            }

        }catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e("Ruan: ", "+connect  UnknownHostException false !!");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Ruan: ", "+connect false !!");
        }
        mHandler.sendMessage(mesg);
    }

    Runnable tcpRunner = new Runnable() {
        @Override
        public void run() {
            //getStatusByTcp();
            if( threadQuit ) return ;
            getStatusByHttpGet();
            Log.e("Ruan", "tcpRunner exit");
        }
    };

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            //if( msg.what = )
            String data = msg.getData().getString("data") ;
            if(data != null){
                if(ipTextView != null ){
                    String val;
                    val = getStringFromJsion(data,"ip");
                    if( !val.equals("none"))
                        ipTextView.setText(val);
                    else
                        ipTextView.setText(UNVARLID_IP);
                }
                if(statusTextView!=null )statusTextView.setText(getStringFromJsion(data,"iol"));
            }
            updateOver();
        }
    };


}
