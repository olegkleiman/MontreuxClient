package com.example.montreuxclient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.example.montreuxclient.AzureStore.ReportEntity;
import com.example.montreuxclient.AzureStore.UploadTableTask;

public class WebAppInterface {

	final String LOG_TAG = "montreux";
	static final int REQUEST_IMAGE_CAPTURE = 1;
	static final int REQUEST_VIDEO_CAPTURE = 2;
    Context mContext;
    
    private Location location;
    public void setLocation(Location l) {
    	location = l;
    }
    public Location getLocation() {
    	return location;
    }
    
    /** Instantiate the interface and set the context */
    WebAppInterface(Context c, Location l) {
        mContext = c;
        location = l;
    }
    
    /** Submit report from web page */
    @JavascriptInterface
    public void submitReport(String userName, String message) {
    	
    	ReportEntity report = new ReportEntity("kids", userName, message, 
    											location, 
    											ReportEntity.REPORT_TYPE_TABLE_ENTITY, "");
    	
    	UploadTableTask uploadTask = new UploadTableTask(mContext);
    	uploadTask.execute(report);
 
    }

    @JavascriptInterface
    public void submitReportWithVideo(String userName, String message){
    
    	Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    	
	    try {	
	    	Activity activity = (Activity)mContext;
	    	if( activity != null ) {

		    	PackageManager packageManager = activity.getPackageManager();
		        if (takeVideoIntent.resolveActivity(packageManager) != null) {
		        	
	        		// System activities do not allow putting extras on their intents
	        		// We need to store the extras (username, message ) somewhere else.
	        		((VideoActivity)mContext).setUserName(userName);
	        		((VideoActivity)mContext).setMessage(message);
		        	
	        		takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
		        	activity.startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
		        }
	    	}
	    }catch(Exception ex) {
	    	Log.e(LOG_TAG, ex.getMessage());
	    }
    }
    
    @JavascriptInterface
    public void submitReportWithPicture(String userName, String message) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        try{
	        Activity activity = (Activity)mContext;
	        if( activity != null ) {
	        
	        	PackageManager packageManager = activity.getPackageManager();
	        	if (takePictureIntent.resolveActivity(packageManager) != null) {
	        		
	        		// System activities do not allow putting extras on their intents
	        		// We need to store the extras (username, message ) somewhere else.
	        		((PicActivity)mContext).setUserName(userName);
	        		((PicActivity)mContext).setMessage(message);

	        		activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
	        }
        }
        }catch(Exception ex) {
        	Log.e(LOG_TAG, ex.getMessage());
        }
    }
    

    
	
	
}
