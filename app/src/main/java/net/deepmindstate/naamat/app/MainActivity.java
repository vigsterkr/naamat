package net.deepmindstate.naamat.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

public class MainActivity extends Activity {
    private static final String DEBUG_TAG = "Naamat";

    public static String START_ACTION = "net.deepmindstate.naamat.app.startSlideshow";
    public static String STOP_ACTION = "net.deepmindstate.naamat.app.stopSlideshow";

    Networking networkHandler;
    SlideShowControlReceiver ssControlReceiver;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        networkHandler = new Networking(MainActivity.this);
        ssControlReceiver = new SlideShowControlReceiver();
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(START_ACTION);
        myIntentFilter.addAction(STOP_ACTION);
        registerReceiver(ssControlReceiver, myIntentFilter);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(DEBUG_TAG, "onPause");
        if(networkHandler.nwChangeReceiver != null)
            unregisterReceiver(networkHandler.nwChangeReceiver);
        unregisterReceiver(ssControlReceiver);
        // TODO pause/reset slideshow as well
    }

    public class SlideShowControlReceiver extends BroadcastReceiver {
        boolean launched = false;
        public void onReceive(Context c, Intent intent) {
            String action  = intent.getAction();
            if(action.equals(START_ACTION)) {
                Log.d(DEBUG_TAG, "onReceive");
                if (!launched) {
                    Log.d(DEBUG_TAG, "Start slideshow");
                    new ChangeImageTask(MainActivity.this).execute();
                    launched = true;
                }
            }
        }
    }

    public class ChangeImageTask extends AsyncTask<Void, Bitmap, Void> {
        Activity activity;
        File f;

        public ChangeImageTask(Activity a) {
            activity = a;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(DEBUG_TAG, "doInBackground");
            int i = 0;
            while (i<1000) {
                f = new File(Environment.getExternalStorageDirectory()+"/naamat/"+i+".jpg");
                if (f.exists()) {
                    Log.d(DEBUG_TAG, "Found file "+f.getAbsolutePath()+", publishing progress");
                    publishProgress(BitmapFactory.decodeFile(f.getAbsolutePath()));
                    try {
                        Log.d(DEBUG_TAG, "Sleeping");
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        // nuttin
                    }
                } else
                    Log.d(DEBUG_TAG, "File "+f.getAbsolutePath()+" not found");
                i++;
                i = i % 10;
            }
            Log.d(DEBUG_TAG, "Returning from doInBackground");
            return null;
        }

        protected void onProgressUpdate(Bitmap... bms) {
            // TODO fadout and fadein
            Log.d(DEBUG_TAG, "onProgressUpdate");
            ImageView imgView = (ImageView) activity.findViewById(R.id.imageView);
            imgView.setImageBitmap(bms[0]);
        }
    }
}
