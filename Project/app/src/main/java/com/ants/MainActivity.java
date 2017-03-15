package com.ants;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import com.ants.engine.AntsEngine;

public class MainActivity extends AppCompatActivity implements NEInterface {
    private static AntsEngine antsEngine = null;
    private AntsSurface surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.makeFullScreen(this);

        setContentView(R.layout.activity_main);

        Utils.setLastActionTime();

        findViewById(R.id.game_home).setOnTouchListener(Utils.onTouchListener);
        if (antsEngine == null) {
            antsEngine = new AntsEngine(this);
        }

        surface = (AntsSurface) findViewById(R.id.game_surface);
        surface.setInstances(this, antsEngine);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            onHome(null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onHome(View view) {
        antsEngine = null;
        finish();
    }

    @Override
    public void exit() {
        onHome(null);
    }
}
