package net.deepmindstate.naamat.app;

import android.app.Activity;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

public class MainActivity extends Activity {

    Networking networkHandler;

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
        new ChangeImageTask(MainActivity.this).execute();

        networkHandler.enableWifi();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(networkHandler.wifiChangeReceiver);
        super.onPause();
    }
}
