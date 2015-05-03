package com.example.montreuxclient;

import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class NFCWriteActivity extends Activity {

	private static final String LOG_TAG = "montreux";
	
	private boolean mWriteMode = true;
	private NfcAdapter mNfcAdapter;
	private PendingIntent mNfcPendingIntent;
	private IntentFilter[] mWriteTagFilters;
	private boolean writeProtect = false;  
	
	private AlertDialog writeNFCDialog;
	
	private Context context;  
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfcwrite);
		
		context = getApplicationContext();
		
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		
		IntentFilter discovery=new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);  
	    IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);      
	    IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);  

	    // Intent filters for writing to a tag  
	    mWriteTagFilters = new IntentFilter[] { discovery };
	}

	protected void onResume() {  
        super.onResume(); 
        
        enableNdefExchangeMode();
	}
	
	@Override  
    protected void onPause() {  
         super.onPause();  
         if(mNfcAdapter != null) mNfcAdapter.disableForegroundDispatch(this);  
    } 
	
	public void OnWriteNFCButtonClicked(View v){
		
        // Write to a tag for as long as the dialog is shown.
        disableNdefExchangeMode();
        enableTagWriteMode();
		
        writeNFCDialog = new AlertDialog.Builder(NFCWriteActivity.this).setTitle("Touch tag to write")
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                disableTagWriteMode();
                enableNdefExchangeMode();
            }
        }).create();
        
        writeNFCDialog.show();
	}
	
    private void enableTagWriteMode() {
        mWriteMode = true;
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[] {
            tagDetected
        };
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
    }

    private void disableTagWriteMode() {
        mWriteMode = false;
        mNfcAdapter.disableForegroundDispatch(this);
    }
	
    private void disableNdefExchangeMode() {
        mNfcAdapter.disableForegroundDispatch(this);
    }
    
    private void enableNdefExchangeMode() {
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
    }
	
	@Override  
    protected void onNewIntent(Intent intent) {  
          super.onNewIntent(intent); 
          
          // NDEF exchange mode
          if (!mWriteMode && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
              NdefMessage[] msgs = getNdefMessages(intent);
              promptForContent(msgs[0]);
          }

          // Tag writing mode
          if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
              Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
              if(supportedTechs(detectedTag.getTechList())) {  
	              WriteResponse wr = writeTag(getTagAsNdef(), detectedTag);
	
	              String message = (wr.getStatus() == 1? "Success: " : "Failed: ") + wr.getMessage();  
	              Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
              }
              
              if( writeNFCDialog != null )
            	  writeNFCDialog.dismiss();
          }          

	}

    private void promptForContent(final NdefMessage msg) {
        new AlertDialog.Builder(this).setTitle("Replace current content?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    String body = new String(msg.getRecords()[0].getPayload());
                    //setNoteBody(body);
                }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    
                }
            }).show();
    }
	
	 private NdefMessage[] getNdefMessages(Intent intent) {
	        
		 // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[] {};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {
                    record
                });
                msgs = new NdefMessage[] {
                    msg
                };
            }
        } else {
            Log.d(LOG_TAG, "Unknown intent.");
            finish();
        }
        return msgs;
	}
	
	private WriteResponse writeTag(NdefMessage message, Tag tag) {  

		int size = message.toByteArray().length;  
	    String mess = "";  
	    try {  
	       Ndef ndef = Ndef.get(tag);  
	       if (ndef != null) {  
	         ndef.connect();  
	         if (!ndef.isWritable()) {  
	           return new WriteResponse(0,"Tag is read-only");  
	         }  
	         if (ndef.getMaxSize() < size) {  
	           mess = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size  
	               + " bytes.";  
	           return new WriteResponse(0,mess);  
	         }  
	         ndef.writeNdefMessage(message);  
	         if(writeProtect) ndef.makeReadOnly();  
	         mess = "Wrote message to pre-formatted tag.";  
	         return new WriteResponse(1,mess);  
	       } else {  
	         NdefFormatable format = NdefFormatable.get(tag);  
	         if (format != null) {  
	           try {  
	             format.connect();  
	             format.format(message);  
	             mess = "Formatted tag and wrote message";  
	             return new WriteResponse(1,mess);  
	           } catch (IOException e) {  
	             mess = "Failed to format tag.";  
	             return new WriteResponse(0,mess);  
	           }  
	         } else {  
	           mess = "Tag doesn't support NDEF.";  
	           return new WriteResponse(0,mess);  
	         }  
	       }  
	     } catch (Exception e) {  
	       mess = "Failed to write tag";  
	       return new WriteResponse(0,mess);  
	     }  
   } 
	
	private NdefMessage getTagAsNdef() {  
        boolean addAAR = false;
        
        EditText editText = (EditText)findViewById(R.id.note);
        String textToWrite = editText.getText().toString();
     
        byte[] uriField = textToWrite.getBytes(Charset.forName("US-ASCII"));  
        byte[] payload = new byte[uriField.length + 1];       //add 1 for the URI Prefix  
        payload[0] = 0x03;   //prefixes http://www. to the URI  
        System.arraycopy(uriField, 0, payload, 1, uriField.length); //appends URI to payload  
        NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, 
        										 NdefRecord.RTD_URI, 
        										 new byte[0], payload);  
     
        if(addAAR) {  
          // note: returns AAR for different app (nfcreadtag)  
          return new NdefMessage(new NdefRecord[] {  
        		  		rtdUriRecord, 
        		  		NdefRecord.createApplicationRecord("com.tapwise.nfcreadtag")  
          			});   
        } else {  
          return new NdefMessage(new NdefRecord[] {  
               rtdUriRecord});  
     }  
   }  

	public static boolean supportedTechs(String[] techs) {  
        boolean ultralight=false;  
        boolean nfcA=false;  
        boolean ndef=false;  
        for(String tech:techs) {  
             if(tech.equals("android.nfc.tech.MifareUltralight")) {  
                  ultralight=true;  
             }else if(tech.equals("android.nfc.tech.NfcA")) {   
                  nfcA=true;  
             } else if(tech.equals("android.nfc.tech.Ndef") || tech.equals("android.nfc.tech.NdefFormatable")) {  
                  ndef=true;  
             }  
        }  
     if(ultralight && nfcA && ndef) {  
          return true;  
     } else {  
          return false;  
     }  
      } 
	
	private class WriteResponse {  
        int status;  
        String message;  
        WriteResponse(int Status, String Message) {  
             this.status = Status;  
             this.message = Message;  
        }  
        public int getStatus() {  
             return status;  
        }  
        public String getMessage() {  
             return message;  
        }  
   } 
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.nfcwrite, menu);
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		if (id == R.id.action_settings) {
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}
}
