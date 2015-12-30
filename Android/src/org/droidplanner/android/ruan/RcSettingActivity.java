package org.droidplanner.android.ruan;

import org.droidplanner.android.R;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;
import org.w3c.dom.Text;

public class RcSettingActivity extends AppCompatActivity {
    DroidPlannerPrefs dpPrefs;
    RcExpoView rcExpoView;
    Button paramText;

    private int mChanNum = -1;
    private int mRcMax = 2000;
    private int mRcMin = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rc_setting);

        dpPrefs = new DroidPlannerPrefs(this.getApplicationContext());
        Intent intent=getIntent();
        TextView text = (TextView) findViewById(R.id.rcSettingText);
        mChanNum = intent.getIntExtra("id",-1);
        if( mChanNum < 0 ){
            text.setText("UnKnow Channel , Error Settings");
            return;
        }else{
            text.setText("Channel " + (mChanNum + 1));
        }
        Intent rcData=getIntent();
        if( rcData == null ) finish();

        rcExpoView = (RcExpoView) findViewById(R.id.id_rc_setting_expo_view);
        paramText = (Button) findViewById(R.id.id_rc_setting_k_value_text);
        rcExpoView.setParamK(rcData.getIntExtra("curveParamk",0));
        paramText.setText(rcData.getIntExtra("curveParamk",0)+"");

        Button btn = (Button) findViewById(R.id.id_rc_setting_k_add_btn);
        if( btn!=null ){
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onParamKChange(rcExpoView.getParamK() + 10);
                }
            });
        }
        btn = (Button) findViewById(R.id.id_rc_setting_k_sub_btn);
        if( btn!=null ){
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onParamKChange(rcExpoView.getParamK() - 10);
                }
            });
        }
        btn = (Button) findViewById(R.id.id_rc_setting_ok_btn);
        if( btn!=null ){
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doFinish();
                }
            });
        }
        btn = (Button) findViewById(R.id.id_rc_setting_cancle_btn);
        if( btn!=null ){
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        CheckBox cb = (CheckBox) findViewById(R.id.id_rc_setting_curve_type_checkbox);
        if( cb != null ) cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    rcExpoView.setCurveType(RcExpoView.THR_TYPE_CURVE);
                else
                    rcExpoView.setCurveType(RcExpoView.MIDDLE_TYPE_CURVE);
            }
        });
        if( rcData.getIntExtra("curveType",0) == RcExpoView.THR_TYPE_CURVE ){
            rcExpoView.setCurveType(RcExpoView.THR_TYPE_CURVE);
            cb.setChecked(true);
        }else{
            rcExpoView.setCurveType(RcExpoView.MIDDLE_TYPE_CURVE);
            cb.setChecked(false);
        }

        EditText rcMin = (EditText) findViewById(R.id.id_rc_setting_rc_min_value);
        EditText rcMax = (EditText) findViewById(R.id.id_rc_setting_rc_max_value);

        rcMin.setText(rcData.getIntExtra("Min",0)+"");
        rcMax.setText(rcData.getIntExtra("Max", 0) + "");

        Switch revertSw = (Switch) findViewById(R.id.id_rc_setting_revert_switch);
        revertSw.setChecked( rcData.getBooleanExtra("revert",false) );

        EditText mixChanText = (EditText) findViewById(R.id.id_rc_setting_mix_main_channel);
        EditText mixAddPersen = (EditText) findViewById(R.id.id_rc_setting_mix_add_person);
        EditText mixSubPersen = (EditText) findViewById(R.id.id_rc_setting_mix_sub_person);
        CheckBox mixCheckbox = (CheckBox) findViewById(R.id.id_rc_setting_mix_start_point_type_checkbox);
        if( 0 < rcData.getIntExtra("mixChan",0)){
            mixChanText.setText(rcData.getIntExtra("mixChan",0)+"");
            mixAddPersen.setText(rcData.getIntExtra("mixChanAddPersen",0)+"");
            mixSubPersen.setText(rcData.getIntExtra("mixChanSubPersen",0)+"");
            mixCheckbox.setChecked(rcData.getIntExtra("mixChanPoint",1)==0);
        }

        EditText trimText = (EditText) findViewById(R.id.id_rc_setting_trim_value);
        trimText.setText(rcData.getIntExtra("trim",0)+"");
    }

    private void doFinish()
    {
        Intent resultIntent = new Intent();

        resultIntent.putExtra("id",mChanNum);

        Switch revertSw = (Switch) findViewById(R.id.id_rc_setting_revert_switch);
        resultIntent.putExtra("revert",revertSw.isChecked());

        EditText rcMin = (EditText) findViewById(R.id.id_rc_setting_rc_min_value);
        EditText rcMax = (EditText) findViewById(R.id.id_rc_setting_rc_max_value);
        resultIntent.putExtra("Min", Integer.parseInt(rcMin.getText().toString()));
        resultIntent.putExtra("Max", Integer.parseInt(rcMax.getText().toString()));

        resultIntent.putExtra("curveType", rcExpoView.getCurveType());
        resultIntent.putExtra("curveParamk", rcExpoView.getParamK());

        EditText mixChanText = (EditText) findViewById(R.id.id_rc_setting_mix_main_channel);
        EditText mixAddPersen = (EditText) findViewById(R.id.id_rc_setting_mix_add_person);
        EditText mixSubPersen = (EditText) findViewById(R.id.id_rc_setting_mix_sub_person);
        CheckBox mixCheckbox = (CheckBox) findViewById(R.id.id_rc_setting_mix_start_point_type_checkbox);
        int value;
        value = Integer.parseInt(mixChanText.getText().toString());
        if( value > 0 && value < 8 ) {
            resultIntent.putExtra("mixChan", value);
        }else {
            resultIntent.putExtra("mixChan", 0);
        }
        value = Integer.parseInt(mixAddPersen.getText().toString());
        if( value > -100 && value < 100 ) {
            resultIntent.putExtra("mixChanAddPersen", value);
        }else {
            resultIntent.putExtra("mixChanAddPersen", 0);
        }
        value = Integer.parseInt(mixSubPersen.getText().toString());
        if( value > -100 && value < 100 ) {
            resultIntent.putExtra("mixChanSubPersen", value);
        }else {
            resultIntent.putExtra("mixChanSubPersen", 0);
        }
        value = mixCheckbox.isChecked()? 0:1;
        resultIntent.putExtra("mixChanPoint", value);


        EditText trimText = (EditText) findViewById(R.id.id_rc_setting_trim_value);
        value = Integer.parseInt(trimText.getText().toString());
        if( value > -100 && value < 100 ) {
            resultIntent.putExtra("trim", value);
        }else {
            resultIntent.putExtra("trim", 0);
        }

        setResult(boxJostickFragment.Rc_Settings_RESULT_CODE, resultIntent);
        finish();
    }
    private void onParamKChange(int k){
        rcExpoView.setParamK(k);
        paramText.setText(rcExpoView.getParamK()+"");
    }
}
