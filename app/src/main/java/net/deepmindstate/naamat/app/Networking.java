package net.deepmindstate.naamat.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Networking {
    private static final String DEBUG_TAG = "Naamat";

    Activity activity;
    NetworkChangeReceiver nwChangeReceiver;
    WifiManager wm;
    boolean multicastTriggerUp = false;

    private MulticastSocket socket, sendsocket;
    private String multicastAddress = "224.0.0.1";
    private InetAddress group;
    private int port = 31337;

    public Networking(Activity a) {
        activity = a;
        wm = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager conn =  (ConnectivityManager)
                activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        if (!multicastTriggerUp && networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.d(DEBUG_TAG, "WiFi connection already available, listening for multicast");
            multicastTriggerUp = true;
            listenMulticast();
        } else {
            Log.d(DEBUG_TAG, "No WiFi connection available, setting change receiver");
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            nwChangeReceiver= new NetworkChangeReceiver();
            activity.registerReceiver(nwChangeReceiver, filter);
        }
    }

    public void sendMulticast(final byte t) {
        Log.d(DEBUG_TAG, "Sending datagram packet '" + t + "'");
        try {
            group = InetAddress.getByName(multicastAddress);
            sendsocket = new MulticastSocket();
        } catch (IOException e) {
            e.printStackTrace();
            Log.wtf(DEBUG_TAG, "Network error: "+e.getMessage());
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i;
                byte buf[] = new byte[1];
                buf[0] = t;
                DatagramPacket pack = new DatagramPacket(buf, buf.length, group, port);
                for (i=0; i<5; i++) {
                    try {
                        sendsocket.send(pack);
                        Thread.sleep(100);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                sendsocket.close();
                Log.d(DEBUG_TAG, "Sending socket closed");
            }
        }).start();
    }

    // TODO creates extra threads
    private void listenMulticast() {
        try {
            group = InetAddress.getByName(multicastAddress);
            socket = new MulticastSocket(port);
            socket.joinGroup(group);
            socket.setBroadcast(true);
        } catch (IOException e) {
            e.printStackTrace();
            Log.wtf(DEBUG_TAG, "Network error: "+e.getMessage());
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                WifiManager.MulticastLock multicastLock = wm.createMulticastLock(DEBUG_TAG);
                multicastLock.acquire();

                byte[] buf = new byte[1];
                while (true) try {
                    Intent msg = new Intent();
                    DatagramPacket packet = new DatagramPacket(buf, 1);
                    socket.receive(packet);
                    byte[] pdu = packet.getData();
                    if (pdu[0] == 'X') {
                        /* stop slideshow packet */
                        Log.d(DEBUG_TAG, "Received stop slideshow packet");
                        msg.setAction(MainActivity.STOP_ACTION);
                    } else if (pdu[0] == 'S') {
                        /* start slideshow packet */
                        Log.d(DEBUG_TAG, "Received start slideshow packet");
                        msg.setAction(MainActivity.START_ACTION);
                    } else if (pdu[0] == 'R') {
                        /* reset slideshow packet */
                        Log.d(DEBUG_TAG, "Received reset slideshow packet");
                        msg.setAction(MainActivity.RESET_ACTION);
                    }
                    if (msg.getAction() != null) {
                        activity.sendBroadcast(msg);
                    }
                } catch (IOException e) {
                    Log.d(DEBUG_TAG, "Networking error: " + e.getMessage());
                }
            }
        }).start();
    }

    class NetworkChangeReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            ConnectivityManager conn =  (ConnectivityManager)
                    activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conn.getActiveNetworkInfo();
            if (!multicastTriggerUp && networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(DEBUG_TAG, "WiFi connection became available, listening for multicast");
                listenMulticast();
            }
        }
    }
}
