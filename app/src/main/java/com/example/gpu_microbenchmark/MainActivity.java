package com.example.gpu_microbenchmark;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.view.KeyEvent;

public class MainActivity extends Activity {

    private GLSurfaceView mGLSurfaceView;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int verM = intent.getIntExtra("verM", 0);
        int app = intent.getIntExtra("app",0);
        int verSh = intent.getIntExtra("verSh",0);
        int fragSh = intent.getIntExtra("fragSh",0);
        int texM = intent.getIntExtra("texM",0);

        mGLSurfaceView = new GLSurfaceView(this);
        if (detectOpenGLES20()) {
            // Tell the surface view we want to create an OpenGL ES 2.0-compatible
            // context, and set an OpenGL ES 2.0-compatible renderer.
            mGLSurfaceView.setEGLContextClientVersion(2);
            mGLSurfaceView.setRenderer(new GLES20TriangleRenderer(this, verM, app, verSh, fragSh,texM));
        }

        setContentView(mGLSurfaceView);
    }

    private boolean detectOpenGLES20() {
        ActivityManager am =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return (info.reqGlEsVersion >= 0x20000);
    }

    @Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mGLSurfaceView.onPause();
    }

    //private GLSurfaceView mGLSurfaceView;
    public boolean onKeyDown(int keyCode, KeyEvent event) {
             switch(keyCode) {
                   case KeyEvent.KEYCODE_BACK:
                         new AlertDialog.Builder(this)
                                        .setTitle("exit")
                                        .setMessage("exit?")
                                        .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                finish();
                                            }
                                        })
                                        .setNegativeButton("no", null).show();
                                        return false;
                   default:
                          return false;
            }
        }

}
