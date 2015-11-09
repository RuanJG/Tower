package org.droidplanner.android.ruan;

import org.droidplanner.android.R;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.droidplanner.android.utils.prefs.DroidPlannerPrefs;

public class RcSettingActivity extends AppCompatActivity {
    DroidPlannerPrefs dpPrefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rc_setting);

        dpPrefs = new DroidPlannerPrefs(this.getApplicationContext());
        Intent intent=getIntent();
        TextView text = (TextView) findViewById(R.id.rcSettingText);
        text.setText("id="+intent.getIntExtra("id",0));
    }
}
