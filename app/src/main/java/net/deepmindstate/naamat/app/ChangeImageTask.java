package net.deepmindstate.naamat.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.File;

public class ChangeImageTask extends AsyncTask<String, Bitmap, String> {
    Activity activity;
    File f;
    public ChangeImageTask(Activity a) {
        activity = a;
    }
    @Override
    protected String doInBackground(String... params) {
        int i = 0;
        while (i<1000) {
            f = new File("/sdcard/naamat/"+i+".jpg");
            if (f.exists()) {
                publishProgress(BitmapFactory.decodeFile(f.getAbsolutePath()));
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    // nuttin
                }
            }
            i++;
            i = i % 10;
        }
        return "Foo.";

    }
    protected void onProgressUpdate(Bitmap... bms) {
        ImageView imgView = (ImageView) activity.findViewById(R.id.imageView);
        imgView.setImageBitmap(bms[0]);
    }
}
