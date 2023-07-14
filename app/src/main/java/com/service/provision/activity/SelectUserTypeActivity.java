package com.service.provision.activity;

import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;

public class SelectUserTypeActivity extends PermisoActivity {

    public static final int REQUEST_CODE_SET_DEFAULT_DIALER = 100;
    static public Context context;
    LinearLayout realtime, linechart;

    public static ImageView live_menu, recorded_menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user_type);

        context = SelectUserTypeActivity.this;

        realtime = findViewById(R.id.realtime);
        linechart = findViewById(R.id.linechart);
    }
}
