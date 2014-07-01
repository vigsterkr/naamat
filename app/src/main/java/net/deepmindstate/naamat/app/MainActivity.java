package net.deepmindstate.naamat.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Random;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

public class MainActivity extends Activity {
    private static final String DEBUG_TAG = "Naamat";

    public static String START_ACTION = "net.deepmindstate.naamat.app.startSlideshow";
    public static String STOP_ACTION = "net.deepmindstate.naamat.app.stopSlideshow";
    public static String RESET_ACTION = "net.deepmindstate.naamat.app.resetSlideshow";

    Networking networkHandler;
    SlideShowControlReceiver ssControlReceiver;
    private boolean launched = false;

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
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width=dm.widthPixels;
        int height=dm.heightPixels;
        int dens=dm.densityDpi;
        Log.d(DEBUG_TAG, "Screen width "+width+", height "+height+", dpi "+dens);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        if (!launched) {
            networkHandler = new Networking(MainActivity.this);
            ssControlReceiver = new SlideShowControlReceiver();
            IntentFilter myIntentFilter = new IntentFilter();
            myIntentFilter.addAction(START_ACTION);
            myIntentFilter.addAction(STOP_ACTION);
            registerReceiver(ssControlReceiver, myIntentFilter);
        }
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

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        byte type = 0;
        // stupid trick to distinguish "remote control" from others
        // TODO: use a property
        File cfile = new File(Environment.getExternalStorageDirectory()+"/NAAMAT/CONTROL");
        super.onTouchEvent(e);
        if (cfile.exists()) {
            Log.d(DEBUG_TAG, "Found control file, parsing touch event");
            if (!launched && e.getAction()==MotionEvent.ACTION_UP) {
                Log.d(DEBUG_TAG, "Going to send start command");
                type = 'S';
            } else if (launched && e.getAction()==MotionEvent.ACTION_UP) {
                Log.d(DEBUG_TAG, "Going to send reset command");
                type = 'R';
            }
            if (type != 0) {
                Log.d(DEBUG_TAG, "Calling sendMulticast()");
                networkHandler.sendMulticast(type);
            }
        } else {
            Log.d(DEBUG_TAG, "Control file not found, ignoring touchevent");
        }
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
        return true;
    }

    public class SlideShowControlReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            String action  = intent.getAction();
            Log.d(DEBUG_TAG, "onReceive");
            if(action.equals(START_ACTION)) {
                Log.d(DEBUG_TAG, "START_ACTION");
                if (!launched) {
                    Log.d(DEBUG_TAG, "Start slideshow");
                    new ChangeImageTask(MainActivity.this).execute();
                    launched = true;
                } else {
                    Log.d(DEBUG_TAG, "Ignoring, already running");
                }
            } else if (action.equals(RESET_ACTION)) {
                Log.d(DEBUG_TAG, "RESET_ACTION");
                Log.d(DEBUG_TAG, "bailing out...");
                MainActivity.this.recreate();
            } else {
                Log.d(DEBUG_TAG, "unknown action received, ignoring");
            }
        }
    }

    public class ChangeImageTask extends AsyncTask<Void, Bitmap, Void> {
        Activity activity;
        private final Object syncToken = new Object();
        Bitmap current_image = null;
        Animation anim_first;
        Animation anim_in;
        Animation anim_out;

        public ChangeImageTask(Activity a) {
            activity = a;
            anim_out = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out);
            anim_out.setDuration(4000);
            anim_in  = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in);
            anim_in.setDuration(4000);
            anim_first  = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in);
            anim_first.setDuration(4000);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(DEBUG_TAG, "doInBackground");
            int i, images, delay_table[], total_delay, change;
            Random rand = new Random();
            File dir = new File(Environment.getExternalStorageDirectory()+"/NAAMAT/");
            do {
                File[] dirList = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return (filename.toLowerCase().endsWith(".jpg") &&
                                !filename.toLowerCase().startsWith("."));
                    }
                });
                if (dirList != null) {
                    Arrays.sort(dirList);
                    images = dirList.length;
                    Log.d(DEBUG_TAG, "Creating new delay table for " + images + " images");
                    delay_table = new int[images];
                    for (i=0; i< images; i++)
                        delay_table[i] = 40000;
                    for (i=0; i<(images -1); i++) {
                        // change = [-5000..+5000], increments of 100
                        change = (rand.nextInt(101)*100)-5000; // nextInt() excludes the upper limit
                        delay_table[i] += change;
                        delay_table[i+1] -= change;
                        Log.d(DEBUG_TAG, "Random change: " + change);
                    }
                    total_delay = 0;
                    for (i=0; i< images; i++) {
                        Log.d(DEBUG_TAG, "Image " + i + ": " + delay_table[i]);
                        total_delay += delay_table[i];
                    }
                    Log.d(DEBUG_TAG, "Total " + total_delay);
                    i = 0;
                    for (File f : dirList) {
                        Log.d(DEBUG_TAG, "Found file " + i +": " + f.getAbsolutePath() + ", publishing progress");
                        publishProgress(BitmapFactory.decodeFile(f.getAbsolutePath()));
                        Log.d(DEBUG_TAG, "Waiting while animating");
                        synchronized (syncToken) {
                            try {
                                syncToken.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d(DEBUG_TAG, "Animations ready");
                        try {
                            Log.d(DEBUG_TAG, "Sleeping for " + delay_table[i] + " milliseconds");
                            Thread.sleep(delay_table[i]);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i++;
                    }
                }
            } while (dir.isDirectory() && dir.canRead());
            Log.d(DEBUG_TAG, "Returning from doInBackground");
            return null;
        }

        protected void onProgressUpdate(Bitmap... bms) {
            final Drawable new_drawable;
            Log.d(DEBUG_TAG, "onProgressUpdate");
            final ImageView imgView = (ImageView) activity.findViewById(R.id.imageView);
            new_drawable = new BitmapDrawable(activity.getResources(), bms[0]);
            if (current_image != null) {
                Log.d(DEBUG_TAG, "Fading out, then in");
                anim_out.setAnimationListener(new Animation.AnimationListener() {
                                                  @Override
                                                  public void onAnimationStart(Animation animation) {}

                                                  @Override
                                                  public void onAnimationRepeat(Animation animation) {}

                                                  @Override
                                                  public void onAnimationEnd(Animation animation) {
                                                      imgView.setImageDrawable(new_drawable);
                                                      anim_in.setAnimationListener(new Animation.AnimationListener() {
                                                          @Override
                                                          public void onAnimationStart(Animation animation) {}

                                                          @Override
                                                          public void onAnimationEnd(Animation animation) {
                                                              synchronized (syncToken) {
                                                                  syncToken.notify();
                                                              }
                                                          }

                                                          @Override
                                                          public void onAnimationRepeat(Animation animation) {}
                                                      });
                                                      imgView.startAnimation(anim_in);
                                                  }
                                              });
                imgView.startAnimation(anim_out);
            } else {
                Log.d(DEBUG_TAG, "First image, only fading in");
                imgView.setImageDrawable(new_drawable);
                anim_first.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        synchronized (syncToken) {
                            syncToken.notify();
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                imgView.startAnimation(anim_first);
            }

            current_image = bms[0];
        }
    }
}
