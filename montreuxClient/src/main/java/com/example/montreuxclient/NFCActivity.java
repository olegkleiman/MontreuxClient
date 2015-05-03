package com.example.montreuxclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.montreuxclient.AzureStore.ReportEntity;
import com.example.montreuxclient.AzureStore.UploadBlobTask;
import com.example.montreuxclient.AzureStore.UploadParams;
import com.example.montreuxclient.AzureStore.UploadTableTask;
import com.example.montreuxclient.data.tlvPicture;
import com.example.montreuxclient.data.tlvVisit;
import com.example.montreuxclient.nfc.NdefMessageParser;
import com.example.montreuxclient.ntf.record.ParsedNdefRecord;
import com.example.montreuxclient.ntf.record.SmartPoster;
import com.example.montreuxclient.ntf.record.UriRecord;
import com.google.gson.JsonElement;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.Ignore;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TableResult;
import com.microsoft.azure.storage.table.TableServiceEntity;
import com.microsoft.windowsazure.mobileservices.ApiJsonOperationCallback;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.ServiceFilterResponseCallback;
import com.microsoft.windowsazure.mobileservices.TableOperationCallback;

public class NFCActivity extends BasicActivity implements NfcAdapter.OnNdefPushCompleteCallback,
														  NfcAdapter.CreateNdefMessageCallback
{
	private static final String LOG_TAG = "montreux";
	static final int REQUEST_IMAGE_CAPTURE = 1;
	
    TextView mTitle;
    LinearLayout mTagContent;
    
    NfcAdapter nfcAdapter;
    private Handler mHandler;
    
    NdefMessage mNdefMessage;
    private PendingIntent mPendingIntent;
//    private IntentFilter[] mFilters;
//    private String[][] mTechList;
    
    private MobileServiceClient mClient;
	static MobileServiceTable<tlvVisit> visitsTable;
	static MobileServiceTable<tlvPicture> picturesTable;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfc);
		
        mTagContent = (LinearLayout) findViewById(R.id.list);
        mTitle = (TextView) findViewById(R.id.title);
        
        mTagContent.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mTagContent.removeAllViews();
	
			}
		});
        
		try{
	        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			long boothID = sharedPrefs.getLong("boothid", 1);
			if( boothID == 0) // executed for first time w/o shared settings
				boothID = 1;
			
			TextView txtBoothName = (TextView)findViewById(R.id.txtAuthor);
			String[] booths = getResources().getStringArray(R.array.booths);
			String boothName = booths[(int) boothID-1];
			txtBoothName.setText(boothName);
			
		} catch(Exception ex) {
			Log.i(LOG_TAG, ex.getMessage());
		}
		
        try {
			mClient = new MobileServiceClient(
			        "https://itlv.azure-mobile.net/",
			        "tTEoHqnFzuKqCYPxetqHTFrHudOzcP83", 
			        this).withFilter(new ProgressFilter());

			visitsTable = mClient.getTable("itlv", tlvVisit.class);
			picturesTable = mClient.getTable("pictures", tlvPicture.class);

		} catch (MalformedURLException e) {
			createAndShowDialog(e, "Error");
		}catch (Exception e) {
			Log.i(LOG_TAG, e.getMessage());
		}
        
		resolveIntent(getIntent());
		
		nfcAdapter =  NfcAdapter.getDefaultAdapter(this);
		if( nfcAdapter == null ) {
			Toast.makeText(this, "NFC is not supported", Toast.LENGTH_LONG).show();
		} else if( !nfcAdapter.isEnabled() ) {
				Toast.makeText(this, "NFC is disabled", Toast.LENGTH_LONG).show();
		} else {
			mNdefMessage = newNdefMessage("Message from Montreux");
			
			mHandler = new Handler() {
		        @Override
		        public void handleMessage(Message msg) {
		            switch (msg.what) {
		                case 1:
		                    Toast.makeText(getApplicationContext(),"BEAM completed",
		                                   Toast.LENGTH_LONG).show();
		                    break;
		            } 
		        }
		    }; // end new
			
			nfcAdapter.setNdefPushMessageCallback(this, this);
			nfcAdapter.setOnNdefPushCompleteCallback(this, this);
		}
		
		mPendingIntent = PendingIntent.getActivity(this, 0,
	                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		
//		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
//		try {
//			ndef.addDataType("*/*");
//		} catch(MalformedMimeTypeException e) {
//			e.printStackTrace();
//			
//		}
//		
//		IntentFilter td = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
//		
//		mFilters = new IntentFilter[]  {
//				ndef, td
//		};
//
//		
//		mTechList = new String[][] { new String[] {
//				NfcV.class.getName(),
//				NfcF.class.getName(),
//				NfcA.class.getName(),
//				NfcB.class.getName()
//			} };
	}
	
	// NOT Called on main UI thread
	@Override
	public void onNdefPushComplete(NfcEvent event) {
		mHandler.obtainMessage(1).sendToTarget();
	}
	
	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		return mNdefMessage;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if( nfcAdapter != null)
			nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null); //mFilters, mTechList);
		//nfcAdapter.enableForegroundNdefPush(this, mNdefMessage);
		
		try{
	        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			long boothID = sharedPrefs.getLong("boothid", 1);
			if( boothID == 0) // executed for first time w/o shared settings
				boothID = 1;
			
			TextView txtBoothName = (TextView)findViewById(R.id.txtAuthor);
			String[] booths = getResources().getStringArray(R.array.booths);
			String boothName = booths[(int) boothID-1];
			txtBoothName.setText(boothName);
			
		} catch(Exception ex) {
			Log.i(LOG_TAG, ex.getMessage());
		}
	}
	
	@Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);

    }
	
	private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                Parcelable tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
                msgs = new NdefMessage[] { msg };
            }
            
            TextView txtTitle = (TextView)this.findViewById(R.id.title);
            txtTitle.setVisibility(View.VISIBLE);
            
            View contentView = (View)this.findViewById(R.id.nfcContent);
            contentView.setBackgroundColor(Color.DKGRAY);
            
            // Setup the views
            buildTagViews(msgs);
            
            persistByWAMS(msgs);
        }
	}
	
	@Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
        	nfcAdapter.disableForegroundDispatch(this);
        	//nfcAdapter.disableForegroundNdefPush(this);
        }
    }

	private void processWAMS(final String userid, final int boothID) {
		
		tlvVisit item = new tlvVisit();
		item.setUserName(userid);
		item.setBoothID(boothID);

		Date date = new Date();
		item.setWhenVisited(date);
		
		//
		// Insert to Azure table
		//
		visitsTable.insert(item, new TableOperationCallback<tlvVisit>() {

			@Override
			public void onCompleted(tlvVisit entity, 
					Exception exception,
					ServiceFilterResponse response) {
				if (exception == null) {
					//
					// Push to Service Bus queue - implemented as custom API within MobileService
					//
					ArrayList<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
					parameters.add(new Pair<String, String>("username", userid));
					parameters.add(new Pair<String, String>("boothid", Integer.toString(boothID)));
					mClient.invokeApi("push", "post", parameters, new ApiJsonOperationCallback(){

						@Override
						public void onCompleted(JsonElement jsonObject, 
								Exception exception,
								ServiceFilterResponse response) {
							if( exception != null ) {
								createAndShowDialog("WAMS operation failed: " + exception.getMessage() + " Make sure you are connected to the Internet.", 
													"Error with Azure SB");
								return;
							}
							
							Log.i(LOG_TAG, "Custom WAMS API invoked");
						}
						
					});
				}
				else {
					Log.e(LOG_TAG, exception.getMessage());
					createAndShowDialog("WAMS operation failed: " + exception.getMessage() + " Make sure you are connected to the Internet.", 
										"Error with Azure WAMS");
				}
				
			}
		});
		Log.i(LOG_TAG, "Insert WAMS API invoked");
		

	}
	
	void persistByWAMS(NdefMessage[] msgs){
    	try {
			if (msgs == null || msgs.length == 0) 
			    return;
			
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			int nBoothID = (int)sharedPrefs.getLong("boothid", 1);
			
			String userid = "";
			
			List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);
			final int size = records.size();
			for (int i = 0; i < size; i++) {
				
				ParsedNdefRecord parsedRecord = records.get(i);
				
				if( parsedRecord instanceof UriRecord ) {
					UriRecord record = (UriRecord)records.get(i);

					Uri _uri = record.getUri();
					String urlQuery = _uri.getQuery();
					if( urlQuery != null 
							&& !urlQuery.isEmpty() ) {
						String[] urlTokens = urlQuery.split("=");
						userid = urlTokens[1];
					}
				}
				else if( parsedRecord instanceof SmartPoster) {
					SmartPoster poster = (SmartPoster)records.get(i);
					
					userid = poster.getTitle().getText();
				}

				processWAMS(userid, nBoothID);

			}
		}
    	catch (Exception e) {
			Log.i(LOG_TAG, e.getMessage());
		}
	}
	
    void buildTagViews(NdefMessage[] msgs) {
        
    	try {
			if (msgs == null || msgs.length == 0) {
			    return;
			}
			
			LayoutInflater inflater = LayoutInflater.from(this);
			LinearLayout content = mTagContent;
			
			// Clear out any old views in the content area, for example if you scan
			// two tags in a row.
			content.removeAllViews();
			
			List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);
			final int size = records.size();
			for (int i = 0; i < size; i++) {
			    ParsedNdefRecord record = records.get(i);
			    content.addView(record.getView(this, inflater, content, i));
			    inflater.inflate(R.layout.tag_divider, content, true);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.i(LOG_TAG, e.getMessage());
		}
    }
	
    private NdefMessage newNdefMessage(String text) {
    	return new NdefMessage(new NdefRecord[] { newTextRecord(
				text, Locale.ENGLISH, true) });
    }
    
    private NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);

        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.nfc, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, ITLVSettingsActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onTakePicture(View v) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		try {
			this.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
		}catch(Exception ex) {
        	Log.e(LOG_TAG, ex.getMessage());
        }

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
					SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					int nBoothID = (int)sharedPrefs.getLong("boothid", 1);
					
					Annual2014ReportEntity report = new Annual2014ReportEntity("annual2014", 
				   						nBoothID,
				   						ReportEntity.REPORT_TYPE_BLOG, "");
					UploadAnnual2014BlobTask uploadTask = new UploadAnnual2014BlobTask(this, 
																						report);
					uploadTask.execute(uploadParam);
		        	
		        } catch (IOException e) {
					Log.e(LOG_TAG, e.getMessage());
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
	
    // POJO
    public static class Annual2014ReportEntity extends TableServiceEntity {
    	
    	public static final int REPORT_TYPE_UNKNOWN = 0;
    	public static final int REPORT_TYPE_TABLE_ENTITY = 1;
    	public static final int REPORT_TYPE_BLOG = 2;
    	
    	public Annual2014ReportEntity(String topic, int boothid, 
    						int reportType, String blobURL) {

    		this.boothid = boothid;
    		this.reportType = reportType;
    		this.blobURL = blobURL;
 		
    		this.tableName = "montreux";
    		this.seen = false;
    		
    		this.partitionKey = topic;
    		UUID uuid = UUID.randomUUID();
    	    this.rowKey = uuid.toString();
    	}
    	
//    	public ReportEntity() 
//    	{
//    		this("","", ReportEntity.REPORT_TYPE_UNKNOWN, "");
//    	}
    	
  
    	public int boothid;
    	public int getBoothId() {
    		return boothid;
    	}
    	public void setBoothId(int bid) {
    		this.boothid = bid;
    	}
    	
    	private int reportType;
    	public int getReportType() {
    		return reportType;
    	}
    	public void setReportType(int type){
    		reportType = type;
    	}
    	
    	private String blobURL;
    	public String getBlobURL() {
    		return blobURL;
    	}
    	public void setBlobURL(String blob) {
    		blobURL = blob;
    	}
    	
    	private Boolean seen;
    	public Boolean getSeen() {
    		return seen;
    	}
    	public void setSeen(Boolean seen) {
    		this.seen = seen;
    	}
    	
    	private String tableName;
    	@Ignore // prevents from serialization
    	public void setTableName(String tableName) {
    		this.tableName = tableName;
    	}
    	@Ignore // prevents from serialization
    	public String getTableName() {
    		return tableName;
    	}
    }


	public static class UploadAnnual2014BlobTask extends AsyncTask<UploadParams, String, Boolean> {

		Context context;
		Exception error;
		URI publishedUri;

		Annual2014ReportEntity report;
		
		public static final String storageConnectionString = 
			    "DefaultEndpointsProtocol=http;" + 
			    "AccountName=telaviv;" + 
			    "AccountKey=oJXjRr6HRBf2k0oX3/IlE+qZkfI1r5p5pvm8vsa3WjpbyZ36GIBVjKL746yjYrQaxSq/kd1K0QHLUEiFZgBUGw==";
		
		public UploadAnnual2014BlobTask(Context ctx, 
										Annual2014ReportEntity report){
			context = ctx;

			this.report = report;
		}
		
		@Override
	    protected void onPostExecute(Boolean result) {
			if( !result  && error != null ) {
				Toast.makeText(context, "Azure SB operation failed: " + error.getMessage() + " Make sure you are connected to the Internet.",
										Toast.LENGTH_LONG).show();
			}	
			else
			{
				tlvPicture picture = new tlvPicture();
				picture.setBoothID(report.getBoothId());
				picture.setLink(publishedUri.toString());
				
				picturesTable.insert(picture, new TableOperationCallback<tlvPicture>(){

					@Override
					public void onCompleted(tlvPicture entity, 
											Exception exception,
											ServiceFilterResponse response) {
						if (exception == null) {
							
						}
						else {
							//createAndShowDialog(exception, "Error");
						}
					}
					
				});
//				if( continuationTask != null ){
//					
//					report.setBlobURL(publishedUri.toString());
//					continuationTask.execute(report);
//				}
		    	
			}
		}
		
		@Override
		protected Boolean doInBackground(UploadParams... params) {
			
			try {
				
				String containerName = params[0].getContainerName();
				File photoFile = params[0].getFile();
				
				if( photoFile != null ) {
					CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
					CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
					CloudBlobContainer container = blobClient.getContainerReference(containerName);

					String fileName = photoFile.getName();
				    CloudBlockBlob blob = container.getBlockBlobReference(fileName);

				    blob.upload(new FileInputStream(photoFile), photoFile.length());
				    
				    publishedUri = blob.getQualifiedUri();
				}
				 
			} catch(Exception e) {
				error = e;
				return false;
			}
			
			return true;
		}
		
	}
	
	public void onSettings(View v) {
		Intent intent = new Intent(this, ITLVSettingsActivity.class);
		startActivity(intent);
	}
	
	private void createAndShowDialog(Exception exception, String title) {
	    createAndShowDialog(exception.toString(), title);
	}
	
	private void createAndShowDialog(String message, String title) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);

	    builder.setMessage(message);
	    builder.setTitle(title);
	    builder.create().show();
	}

	private class ProgressFilter implements ServiceFilter {

		@Override
		public void handleRequest(ServiceFilterRequest request,
				NextServiceFilterCallback nextServiceFilterCallback,
				final ServiceFilterResponseCallback responseCallback) {
			
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					//if (mProgressBar != null) 
					//	mProgressBar.setVisibility(ProgressBar.VISIBLE);
					
				}
			
			});
			
			nextServiceFilterCallback.onNext(request, new ServiceFilterResponseCallback() {

				@Override
				public void onResponse(ServiceFilterResponse response,
						Exception exception) {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {

							//if (mProgressBar != null) 
							//	mProgressBar.setVisibility(ProgressBar.GONE);
							
						}
						
					});
					
					if (responseCallback != null)  
						responseCallback.onResponse(response, exception);
				}

			});
			
		}
	
	}
	

}
