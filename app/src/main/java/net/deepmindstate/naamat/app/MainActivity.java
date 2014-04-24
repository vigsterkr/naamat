package net.deepmindstate.naamat.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

public class MainActivity extends Activity {

    public static String START_ACTION = "net.deepmindstate.naamat.app.startSlideshow";
    public static String STOP_ACTION = "net.deepmindstate.naamat.app.stopSlideshow";

    Networking networkHandler;
    NetworkChangeReceiver nwc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        networkHandler = new Networking(MainActivity.this);
    }

    @Override
    protected void onResume() {
        IntentFilter netIntentFilter = new IntentFilter();
        netIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        netIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(networkHandler.wifiChangeReceiver, netIntentFilter);
        nwc = new NetworkChangeReceiver();
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(START_ACTION);
        myIntentFilter.addAction(STOP_ACTION);
        registerReceiver(nwc, myIntentFilter);

        super.onResume();
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

        networkHandler.enableWifi();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(networkHandler.wifiChangeReceiver);
        unregisterReceiver(nwc);
        super.onPause();
    }

    class NetworkChangeReceiver extends BroadcastReceiver {
        boolean launched = false;
        @SuppressLint("UseValueOf")
        public void onReceive(Context c, Intent intent) {
            String action  = intent.getAction();
            if(action.equals(START_ACTION)) {
                Log.d("broadcast", "got stuff");
                if (!launched) {
                    Log.d("broadcast", "start slideshow");
                    new ChangeImageTask(MainActivity.this).execute();
                    launched = true;
                }
            }
        }
    }
}
