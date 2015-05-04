package com.example.montreuxclient;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.example.montreuxclient.wifi.ClientScanResult;
import com.example.montreuxclient.wifi.FinishScanListener;
import com.example.montreuxclient.wifi.TraceAdapter;
import com.example.montreuxclient.wifi.WIFI_AP_STATE;
import com.example.montreuxclient.wifi.WifiApManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class WiFiActivity extends Activity {

    private static final String LOG_TAG = "Monrteux.WiFi";

    private WifiManager mWiFiManager;
    TraceAdapter mTraceAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi);

        mWiFiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        ListView listView = (ListView)findViewById(R.id.networksListView);
        listView.setFastScrollEnabled(true);

        mTraceAdapter = new TraceAdapter(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<String>());

        listView.setAdapter(mTraceAdapter);
    }

    private WifiConfiguration getFastRideConfig() {

        WifiConfiguration netConfig = new WifiConfiguration();

        netConfig.SSID = "Fast Ride";
        netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        return netConfig;
    }

    public void onBtnConnect(View view){

        int frNetworkID = -1;
        String traceMessage;

        if( !mWiFiManager.isWifiEnabled() )
            mWiFiManager.setWifiEnabled(true);

        WifiConfiguration frConfiguration = getFastRideConfig();


        List<WifiConfiguration> configs = mWiFiManager.getConfiguredNetworks();
        boolean bFRFound = false;
        if( configs != null
                && !configs.equals(Collections.EMPTY_LIST)) {

            for(WifiConfiguration config : configs) {
                String trimmedConfigSSID = config.SSID.replaceAll("^\"+|\"+$", "");
                if( trimmedConfigSSID.equals(frConfiguration.SSID) ) {
                    frNetworkID = config.networkId;
                    bFRFound = true;
                    break;
                }
            }

            if( !bFRFound ) {

                // Add "Fast Ride" network to the set of configured networks.
                // The newly added network will be marked DISABLED by default.
                frNetworkID = mWiFiManager.addNetwork(frConfiguration);
            }
        } else {
            traceMessage = "Configured network list is empty";

            mTraceAdapter.add(traceMessage);
            mTraceAdapter.notifyDataSetChanged();
            Log.d(LOG_TAG, "Network enabling failed");
        }

        if( frNetworkID != -1 ) {
            mWiFiManager.disconnect();

            // Because second parameter of enableNetwork() - disableOthers -
            // set to true, an attempt to connect to this network is initialized.
            if (!mWiFiManager.enableNetwork(frNetworkID, true)) {
                traceMessage = "Network enabling failed";
            } else {
                traceMessage = "Connect initialized";

                mWiFiManager.reconnect();
            }
            mTraceAdapter.add(traceMessage);
            mTraceAdapter.notifyDataSetChanged();
            Log.d(LOG_TAG, "Network enabling failed");

        }
    }

    public void onBtnSetAP(View view){

        WifiConfiguration netConfig = getFastRideConfig();

        WifiApManager wifiApManager = new WifiApManager(this);
        wifiApManager.setWifiApEnabled(netConfig, true);
        while( !wifiApManager.isWifiApEnabled() ) {

        }
        WIFI_AP_STATE state = wifiApManager.getWifiApState();

        WifiConfiguration config = wifiApManager.getWifiApConfiguration();
        String traceMessage = "\nSSID:"+netConfig.SSID+"\nPassword:"+netConfig.preSharedKey+"\n";
        mTraceAdapter.add(traceMessage);
        mTraceAdapter.notifyDataSetChanged();
        Log.e("CLIENT", traceMessage);

    }

    public void onBtnClient(View view) {
        WifiApManager wifiApManager = new WifiApManager(this);
        wifiApManager.getClientList(false, new FinishScanListener(){

            @Override
            public void onFinishScan(ArrayList<ClientScanResult> clients) {
                for(ClientScanResult scan : clients) {

                    String traceMessage =  scan.getDevice() + " "
                                            + scan.getIpAddr()
                                            + "\n" + scan.getHWAddr()
                                            + " Is reachable: " + scan.isReachable();
                    Log.e("CLIENT", traceMessage);
                    mTraceAdapter.add(traceMessage);
                    mTraceAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wi_fi, menu);
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

}
