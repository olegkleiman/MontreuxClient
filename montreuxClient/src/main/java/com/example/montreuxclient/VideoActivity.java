package com.example.montreuxclient;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class VideoActivity extends BasicActivity {

	static final int REQUEST_VIDEO_CAPTURE = 2;
	
	@SuppressLint("SetJavaScriptEnabled") 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video);
		
		WebView webView = (WebView) findViewById(R.id.webView1);
		webView.setWebViewClient(new MyWebViewClient());
		webView.addJavascriptInterface(webAppInterface, "Android");
		
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);		
		
		webView.loadUrl("http://okey.azurewebsites.net/page_video.html");
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
	    if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
	    	if( data != null) {
	    		
	    		try {
	    			
					Uri videoUri = data.getData();
					Toast.makeText(this, videoUri.toString(), Toast.LENGTH_LONG).show();
					
//					ReportEntity report = new ReportEntity("kids", 
//							this.username, this.message, 
//							webAppInterface.getLocation(), 
//							ReportEntity.REPORT_TYPE_BLOG, "");
//					
//					UploadParams uploadParam = new UploadParams();//photoFile, "pictures");
//					
//					UploadBlobTask uploadTask = new UploadBlobTask(this, 
//												new UploadTableTask(this),
//												report);
//					uploadTask.execute(uploadParam);
					
				} catch (Exception e) {
					Log.e(LOG_TAG, e.getMessage());
				}
	    	}
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.video, menu);
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

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
}
