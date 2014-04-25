package net.deepmindstate.naamat.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;

public class Networking {
    private static final String DEBUG_TAG = "Naamat";

    Activity activity;
    WifiManager wm;
    WifiChangeReceiver wifiChangeReceiver;

    private MulticastSocket socket;
    private String multicastAddress = "224.0.0.1";
    private InetAddress group;
    private int port = 31337;

    public Networking(Activity a) {
        activity = a;
        wm = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        wifiChangeReceiver = new WifiChangeReceiver();
    }

    public boolean enableWifi() {
        if(wm != null) {
            if (!wm.isWifiEnabled()) {
                if (!wm.setWifiEnabled(true)) {
                    Log.d(DEBUG_TAG, "Couldn't enable Wifi!");
                    return false;
                }
            }
        } else {
            Log.d(DEBUG_TAG, "Mo wifi manager!!");
        }

        return false;
    }

    private void startMulticast() {
        try {
            group = InetAddress.getByName(multicastAddress);
            socket = new MulticastSocket(port);
            socket.joinGroup(group);
            socket.setBroadcast(true);
        } catch (IOException e) {
            e.printStackTrace();
            Log.wtf("init", e.getMessage());
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                WifiManager.MulticastLock multicastLock = wm.createMulticastLock(DEBUG_TAG);
                multicastLock.acquire();

                byte[] buf = new byte[1];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, 1);
                        socket.receive(packet);
//                        byte[] pdu = packet.getData();
//                        if (pdu[0] == 0x0) {
//                            /* stop slideshow packet */
//                            Intent msg = new Intent();
//                            msg.setAction(MainActivity.STOP_ACTION);
//                            activity.sendBroadcast(msg);
//                        } else {
//                            //if (pdu[0] == 0xFF) {
//                            /* start slideshow packet */
//                            Log.d(DEBUG_TAG, "got start slideshow packet");
                            Intent msg = new Intent();
                            msg.setAction(MainActivity.START_ACTION);
                            activity.sendBroadcast(msg);
//                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
    }

    private boolean connectToNetwork() {
        String requiredNetworkSSID = "\"Laruman\"";

        WifiInfo netInfo = wm.getConnectionInfo();
        if (netInfo.getSSID().compareTo(requiredNetworkSSID) == 0) {
            startMulticast();
            return true;
        } else {
            List<WifiConfiguration> networks = wm.getConfiguredNetworks();
            if (networks == null) {
                Log.d(DEBUG_TAG, "There are no networks configured!");
                return false;
            }

            for (int i = 0; i < networks.size(); i++) {
                WifiConfiguration c = networks.get(i);
                if (c.SSID.compareTo(requiredNetworkSSID) == 0) {
                    // connect to network
                    if (wm.enableNetwork(c.networkId, true)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    class WifiChangeReceiver extends BroadcastReceiver {
        @SuppressLint("UseValueOf")
        public void onReceive(Context c, Intent intent) {
            String action  = intent.getAction();
            if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int res = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN);

                switch(res) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        connectToNetwork();
                        break;

                    case WifiManager.WIFI_STATE_ENABLING:
                    case WifiManager.WIFI_STATE_DISABLING:
                    case WifiManager.WIFI_STATE_DISABLED:
                    default:
                        Log.d(DEBUG_TAG, "Wifi disabled!");
                }

            } else if(action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)){
                    // we should have an ip address now, let's do multicast
                    Log.d(DEBUG_TAG, "Supplicant connected");
                    startMulticast();
                } else {
                    // wifi connection was lost
                }
            }
        }
    }

}
