package com.example.montreuxclient.fastcv;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.montreuxclient.R;
import com.example.montreuxclient.fastcv.FeatureDetection.base.GraphicalActivity;

public class CornerActivity extends GraphicalActivity {


    /** Function which retrieves title based on module used. */
    protected void initTitle(){
        title = "Montreux FastCV CornerDetection";
    }

    protected Runnable mUpdateTimeTask = new Runnable(){
        public void run(){
            float camFPS = util.getCameraFPS();
            int numCorners =  getNumCorners();
        }
    };



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_corner, menu);



        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Function to pass camera frame for native, FastCV processing.
     * @param data Byte buffer for data.
     * @param w Width of data
     * @param h Height of data
     */
    public native void update( byte[] data, int w, int h );

    /**
     * Retrieves the latest number of corners for debug purposes.
     *
     * @return int Number of corners.
     */
    protected native int getNumCorners();
}
