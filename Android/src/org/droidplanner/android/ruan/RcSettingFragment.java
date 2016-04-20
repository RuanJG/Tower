package org.droidplanner.android.ruan;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.droidplanner.android.R;

import android.os.Handler;

public class RcSettingFragment extends Fragment {
    public RcSettingFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rc_setting, container, false);
    }



    private int mChanNum = -1;
    private int mRcMax = 2000;
    private int mRcMin = 1000;
    private int mRever = 0;
    private int mCurveType = RcExpoView.MIDDLE_TYPE_CURVE;
    private int mCurveParamk = 0; // -100~100 ; 0 is normal line
    private int mMixChan = -1;// chan_id
    private int mMixChanPoint = 1;//0:low point 1:middle Point
    private int mMixChanAddPersen = 0;//+- 100%
    private int mMixChanSubPersen = 0;//+- 100%
    private int mTrim = 0;//0 not set , range +- 100

    RcExpoView rcExpoView;
    Button paramText;
    Handler mHandler;
    //Intent rcData;

    public void doInit(Handler h, Intent rcData)
    {
        mHandler = h;

        mChanNum = rcData.getIntExtra("id", -1);
        Log.e("Ruan","id="+mChanNum+",rcData="+rcData.equals(null));
        mCurveParamk =rcData.getIntExtra("curveParamk",0);
        mCurveType = rcData.getIntExtra("curveType",RcExpoView.MIDDLE_TYPE_CURVE);
        mRever = rcData.getIntExtra("revert",0);
        mRcMin = rcData.getIntExtra("Min",0);
        mRcMax = rcData.getIntExtra("Max",0);
        Log.e("Ruan","min,max="+mRcMin+","+mRcMax);
        mMixChan = rcData.getIntExtra("mixChan",-1);
        mMixChanPoint = rcData.getIntExtra("mixChanPoint",1);
        mMixChanAddPersen = rcData.getIntExtra("mixChanAddPersen",0);
        mMixChanSubPersen = rcData.getIntExtra("mixChanSubPersen",0);
        mTrim = rcData.getIntExtra("trim",0);
        initView();

    }


    public void doInit(Handler h, RcConfigParam.baseConfig bc, RcConfigParam.mixConfig mc) {
        mHandler = h;

        mChanNum = bc.id;
        Log.e("Ruan","id="+mChanNum);
        mCurveParamk =bc.curverParamk;
        mCurveType = bc.curveType;
        mRever = bc.revert?1:0;
        mRcMin = bc.minValue;
        mRcMax = bc.maxValue;
        mTrim = bc.trim;
        Log.e("Ruan","min,max="+mRcMin+","+mRcMax);
        if(mc!=null){
            mMixChan = mc.mainChan;
            mMixChanPoint = mc.mainChanStartPoint;
            mMixChanAddPersen = mc.persenAtAdd;
            mMixChanSubPersen = mc.persenAtSub;
        }else {
            mMixChan = -1;
            mMixChanPoint = 1;
            mMixChanAddPersen =  0;
            mMixChanSubPersen =  0;
        }

        initView();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    private void initView() {
        TextView text = (TextView) this.getActivity().findViewById(R.id.rcSettingText);
        //mChanNum = rcData.getIntExtra("id",-1);
        if( mChanNum < 0 ){
            text.setText("UnKnow Channel , Error Settings");
            return;
        }else{
            text.setText("Channel " + (mChanNum + 1));
        }

        //if( rcData == null ) return;

        rcExpoView = (RcExpoView) this.getActivity().findViewById(R.id.id_rc_setting_expo_view);
        paramText = (Button) this.getActivity().findViewById(R.id.id_rc_setting_k_value_text);
        rcExpoView.setParamK(mCurveParamk);//rcData.getIntExtra("curveParamk",0));
        paramText.setText(mCurveParamk + "");//rcData.getIntExtra("curveParamk",0)+"");

        Button btn = (Button) this.getActivity().findViewById(R.id.id_rc_setting_k_add_btn);
        if( btn!=null ){
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onParamKChange(rcExpoView.getParamK() + 10);
                }
            });
        }
        btn = (Button) this.getActivity().findViewById(R.id.id_rc_setting_k_sub_btn);
        if( btn!=null ){
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onParamKChange(rcExpoView.getParamK() - 10);
                }
            });
        }
        btn = (Button) this.getActivity().findViewById(R.id.id_rc_setting_ok_btn);
        if( btn!=null ){
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doFinish();
                }
            });
        }
        btn = (Button) this.getActivity().findViewById(R.id.id_rc_setting_cancle_btn);
        if( btn!=null ){
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doQuit();
                }
            });
        }
        CheckBox cb = (CheckBox) this.getActivity().findViewById(R.id.id_rc_setting_curve_type_checkbox);
        if( cb != null ) cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    rcExpoView.setCurveType(RcExpoView.THR_TYPE_CURVE);
                else
                    rcExpoView.setCurveType(RcExpoView.MIDDLE_TYPE_CURVE);
            }
        });
        //if( rcData.getIntExtra("curveType",0) == RcExpoView.THR_TYPE_CURVE ){
        if( mCurveType == RcExpoView.THR_TYPE_CURVE ){
            rcExpoView.setCurveType(RcExpoView.THR_TYPE_CURVE);
            cb.setChecked(true);
        }else{
            rcExpoView.setCurveType(RcExpoView.MIDDLE_TYPE_CURVE);
            cb.setChecked(false);
        }

        EditText rcMin = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_rc_min_value);
        EditText rcMax = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_rc_max_value);

        //rcMin.setText(rcData.getIntExtra("Min",0)+"");
        //rcMax.setText(rcData.getIntExtra("Max", 0) + "");
        rcMin.setText(mRcMin+"");
        rcMax.setText(mRcMax + "");

        Switch revertSw = (Switch) this.getActivity().findViewById(R.id.id_rc_setting_revert_switch);
        //revertSw.setChecked( rcData.getBooleanExtra("revert",false) );
        revertSw.setChecked( mRever == 1 );

        EditText mixChanText = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_mix_main_channel);
        EditText mixAddPersen = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_mix_add_person);
        EditText mixSubPersen = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_mix_sub_person);
        CheckBox mixCheckbox = (CheckBox) this.getActivity().findViewById(R.id.id_rc_setting_mix_start_point_type_checkbox);
        /*
        if( 0 < rcData.getIntExtra("mixChan",0)){
            mixChanText.setText(rcData.getIntExtra("mixChan",0)+"");
            mixAddPersen.setText(rcData.getIntExtra("mixChanAddPersen",0)+"");
            mixSubPersen.setText(rcData.getIntExtra("mixChanSubPersen",0)+"");
            mixCheckbox.setChecked(rcData.getIntExtra("mixChanPoint",1)==0);
        }*/

        mMixChan += 1;// for the user view , channle is 1-8   , in program 0-7
        mixChanText.setText(mMixChan+"");
        mixAddPersen.setText(mMixChanAddPersen+"");
        mixSubPersen.setText(mMixChanSubPersen+"");
        mixCheckbox.setChecked(mMixChanPoint == 0);


        EditText trimText = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_trim_value);
        //trimText.setText(rcData.getIntExtra("trim",0)+"");
        trimText.setText(mTrim + "");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }



    private void doFinish()
    {
        Message message = new Message();
        Bundle bundle = new Bundle();
        message.what = boxJostickFragment.RcSettingFrameResult;

        bundle.putInt("id", mChanNum);

        Switch revertSw = (Switch) this.getActivity().findViewById(R.id.id_rc_setting_revert_switch);
        bundle.putInt("revert", revertSw.isChecked() ? 1 : 0);

        EditText rcMin = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_rc_min_value);
        EditText rcMax = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_rc_max_value);
        bundle.putInt("Min", Integer.parseInt(rcMin.getText().toString()));
        bundle.putInt("Max", Integer.parseInt(rcMax.getText().toString()));

        bundle.putInt("curveType", rcExpoView.getCurveType());
        bundle.putInt("curveParamk", rcExpoView.getParamK());

        EditText mixChanText = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_mix_main_channel);
        EditText mixAddPersen = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_mix_add_person);
        EditText mixSubPersen = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_mix_sub_person);
        CheckBox mixCheckbox = (CheckBox) this.getActivity().findViewById(R.id.id_rc_setting_mix_start_point_type_checkbox);
        int value;
        value = Integer.parseInt(mixChanText.getText().toString())-1;
        if( value >= 0 && value < 8 ) {
            bundle.putInt("mixChan", value);
        }else {
            bundle.putInt("mixChan", -1);
        }
        value = Integer.parseInt(mixAddPersen.getText().toString());
        if( value >= -100 && value <= 100 ) {
            bundle.putInt("mixChanAddPersen", value);
        }else {
            bundle.putInt("mixChanAddPersen", 0);
        }
        value = Integer.parseInt(mixSubPersen.getText().toString());
        if( value >= -100 && value <= 100 ) {
            bundle.putInt("mixChanSubPersen", value);
        }else {
            bundle.putInt("mixChanSubPersen", 0);
        }
        value = mixCheckbox.isChecked()? 0:1;
        bundle.putInt("mixChanPoint", value);


        EditText trimText = (EditText) this.getActivity().findViewById(R.id.id_rc_setting_trim_value);
        value = Integer.parseInt(trimText.getText().toString());
        if( value > -100 && value < 100 ) {
            bundle.putInt("trim", value);
        }else {
            bundle.putInt("trim", 0);
        }

        if( mHandler != null){
            message.setData(bundle);
            mHandler.sendMessage(message);
        }

        doQuit();
        //setResult(boxJostickFragment.Rc_Settings_RESULT_CODE, resultIntent);
        //finish();
    }
    private void doQuit()
    {
        this.getActivity().findViewById(R.id.box_rcSettingView).setVisibility(View.INVISIBLE);
        this.getActivity().findViewById(R.id.box_jostick_board).setVisibility(View.VISIBLE);
    }

    private void onParamKChange(int k){
        rcExpoView.setParamK(k);
        paramText.setText(rcExpoView.getParamK() + "");
    }

}
