package com.example.montreuxclient.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by Oleg Kleiman on 18-Apr-15.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "BR: ";

    public interface IWiFiStateChanges{
        public void trace(String status);
        public void alert(String intent);
    }

    IWiFiStateChanges activity;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;


    public WiFiDirectBroadcastReceiver(WifiP2pManager manager,
                                       WifiP2pManager.Channel channel,
                                       IWiFiStateChanges activity) {
        super();

        this.mManager = manager;
        this.mChannel = channel;
        this.activity = activity;

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            onStateChanged(intent);
            //      Peers will be obtained from DNS-SD listeners
            //        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            //                onPeersChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            onConnectionChanged(intent);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            onLocalDeviceChanged(intent);
        } else if( WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            onDiscoveryChanged(intent);
        }
    }

    private void onConnectionChanged(Intent intent){

        String traceMessage = "Connection changed";
        Log.d(LOG_TAG, traceMessage);
        activity.trace(LOG_TAG + traceMessage);

        WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

        if(networkInfo.isConnected()) {
            // we are connected with the other device, request connection
            // info to find group owner IP

            traceMessage = "BR: Network connected";
            Log.d(LOG_TAG, traceMessage);
            activity.trace(traceMessage);

            mManager.requestConnectionInfo(mChannel,
                    (WifiP2pManager.ConnectionInfoListener) activity);

        } else {
            traceMessage = "BR: Network disconnected";
            Log.d(LOG_TAG, traceMessage);
            activity.trace(traceMessage);
        }
    }

    private void onLocalDeviceChanged(Intent intent) {
        String traceMessage = "This device changed";
        Log.d(LOG_TAG, traceMessage);
        activity.trace(LOG_TAG + traceMessage);

        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

        traceMessage = "\nDevice name: " + device.deviceName
                + "\nAddress: " + device.deviceAddress
                + "\nType: " + device.primaryDeviceType
                + "\nConnected?" + ((device.status == WifiP2pDevice.CONNECTED) ? "yes" : "no");
        Log.d(LOG_TAG, traceMessage);
        activity.trace(LOG_TAG + traceMessage);

    }

    private void onDiscoveryChanged(Intent intent) {

        String traceMessage;

        int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
        if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
            traceMessage = "Wifi P2P discovery started";
        } else {
            traceMessage = "Wifi P2P discovery stopped";
        }

        Log.d(LOG_TAG, traceMessage);
        activity.trace(LOG_TAG + traceMessage);

    }

    private void onPeersChanged(Intent intent) {
        String traceMessage = "WifiP2p Peers has changed";
        Log.d(LOG_TAG, traceMessage);
        activity.trace(LOG_TAG + traceMessage);

        // Request available peers from the wifi p2p manager. This is an
        // asynchronous call and the calling activity is notified with a
        // callback on PeerListListener.onPeersAvailable()
        if (mManager != null) {
            mManager.requestPeers(mChannel, (WifiP2pManager.PeerListListener) activity);
        }
    }

    private void onStateChanged(Intent intent) {

        String traceMessage;

        // Determine if Wifi P2P mode is enabled or not, alert the Activity.
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            traceMessage = "P2P state changed to enabled";
        } else {
            traceMessage = "P2P state changed to disabled";
            activity.alert(Settings.ACTION_WIFI_SETTINGS);
        }

        Log.d(LOG_TAG, traceMessage);
        activity.trace(LOG_TAG + traceMessage);

    }
}

