package com.example.montreuxclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.montreuxclient.AzureStore.ReportEntity;
import com.example.montreuxclient.AzureStore.UploadBlobTask;
import com.example.montreuxclient.AzureStore.UploadParams;
import com.example.montreuxclient.AzureStore.UploadTableTask;

public class PicActivity extends BasicActivity {

	String LOG_TAG = "pic_activity";
	static final int REQUEST_IMAGE_CAPTURE = 1;
	
	@SuppressLint({ "SetJavaScriptEnabled", "JavascriptInterface" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pic);
		
		WebView webView = (WebView) findViewById(R.id.webView1);
		webView.setWebViewClient(new MyWebViewClient());
		webView.addJavascriptInterface(webAppInterface, "Android");
		
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);		
		
		webView.loadUrl("http://okey.azurewebsites.net/page_pic.html");
	}
	
	String username;
	public void setUserName(String userName){
		this.username = userName;
	}
	
	String message;
	public void setMessage(String Message) {
		this.message = Message;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
	    	if( data != null) {
		    	
	    		Bundle extras = data.getExtras();
		        Bitmap imageBitmap = (Bitmap) extras.get("data");
		        
		        FileOutputStream out = null;
		        
		        try {
		        
		        	File outputDir = this.getCacheDir();
		        	String imageFileName = getTempFileName();
		        	File photoFile = File.createTempFile(imageFileName, ".png", outputDir);
					
					out = new FileOutputStream(photoFile);
					imageBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);

					UploadParams uploadParam = new UploadParams(photoFile, "pictures");
					
				   	ReportEntity report = new ReportEntity("kids", 
							   			this.username, this.message, 
							   			webAppInterface.getLocation(), 
										ReportEntity.REPORT_TYPE_BLOG, "");
					
					UploadBlobTask uploadTask = new UploadBlobTask(this, 
																	new UploadTableTask(this),
																	report);
					uploadTask.execute(uploadParam);
					
				} catch (IOException e) {
					Log.e(LOG_TAG, e.getMessage());
				} finally {
			        
					try {
						if (out != null)
						    out.close();
					} catch (IOException e) {
						Log.e(LOG_TAG, e.getMessage());
					}
				}

	    	}
	    }
	}		
	
	// Generates random file name 
	@SuppressLint("SimpleDateFormat") 
	private String getTempFileName() {
		
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return "Montreux_" + timeStamp;
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.pic, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private class MyWebViewClient extends WebViewClient {

	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	
	    	if (Uri.parse(url).getHost().equals("www.microsoft.com")) {
	            // This is my web site, so do not override; let my WebView load the page
	            return false;
	        }	    	
	    	
	        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
	        startActivity(intent);
	    	
	    	return true;
	    }
	}
		
}
