package org.droidplanner.android.ruan;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Router4GFindWanIp {
    public static final String TAG = Router4GFindWanIp.class.getName();
    public static final String UNVAILD_IP = "error";
    public  String ip;
    public  Handler mWifiHandler;


    public Router4GFindWanIp(Handler h)
    {
        mWifiHandler = h;
    }
    public void doGetIp()
    {
        if( mWifiHandler == null ){
           return;
        }
        new Thread()
        {
            public void run()
            {
                getIpByClient("admin", "");
            }
        }.start();
        //getIpByClient("admin", "");
    }
    private void sendMsg(String ip)
    {
        Message message = new Message();
        Bundle bundle = new Bundle();
        message.what = RcFragment.Get4GIPHandleMsgId;
        //bundle.putString("id", TAG);
        bundle.putString("ip", ip);
        message.setData(bundle);
        mWifiHandler.sendMessage(message);
    }

    private  void getIpByClient(String name,String password){
        HttpClient client=new DefaultHttpClient();
        String path="http://192.168.1.1/api/user/login";
        HttpPost httpPost=new HttpPost(path);
        List<NameValuePair> parameters=new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("Username", name));
        parameters.add(new BasicNameValuePair("Password", "YWRtaW4="));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(parameters, "utf-8"));
            HttpResponse response= client.execute(httpPost);
            if(response.getStatusLine().getStatusCode()==200){
//			GET /api/monitoring/status HTTP/1.1
//			GET /api/monitoring/check-notifications HTTP/1.1
                HttpGet httpGet=new HttpGet("http://192.168.1.1/api/monitoring/status");
                HttpResponse response1=client.execute(httpGet);
                if(response1.getStatusLine().getStatusCode()==200){
                    InputStream is=response1.getEntity().getContent();
                    if(is!=null){
                        ByteArrayOutputStream bos=new ByteArrayOutputStream();
                        int len=0;
                        byte[] buffer=new byte[1024];
                        while((len=is.read(buffer))!=-1){
                            bos.write(buffer,0,len);
                        }
                        is.close();

                        if(mWifiHandler!=null){
                            String info=new String(bos.toByteArray());
                            String infos[]=info.split("<WanIPAddress>");
                            String infoss[]=infos[1].split("</WanIPAddress>");
                            /*
                            Message mes=mWifiHandler.obtainMessage();
                            mes.obj=infoss[0];
                            mWifiHandler.sendMessage(mes);
                            */
                            sendMsg(infoss[0]);
                        }

                    }else{
                        if(mWifiHandler!=null){
                            /*
                            Message mes=mWifiHandler.obtainMessage();
                            mes.obj="error";
                            mWifiHandler.sendMessage(mes);
                            */
                            sendMsg(UNVAILD_IP);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(mWifiHandler!=null){
                /*
                Message mes=mWifiHandler.obtainMessage();
                mes.obj="error";
                mWifiHandler.sendMessage(mes);
                */
                sendMsg(UNVAILD_IP);
            }
        }
    }
}