package com.example.montreuxclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.montreuxclient.wifip2p.ClientSocketHandler;
import com.example.montreuxclient.wifip2p.IFragmentWithList;
import com.example.montreuxclient.wifip2p.PeersListFragment;
import com.example.montreuxclient.wifip2p.WiFiChatFragment;
import com.example.montreuxclient.wifip2p.WiFiDirectBroadcastReceiver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllJoynActivity extends Activity
        implements WiFiDirectBroadcastReceiver.IWiFiStateChanges,

        WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.GroupInfoListener,
        WifiP2pManager.ChannelListener,

        PeersListFragment.DeviceClickListener{

    private static final String LOG_TAG = "Monrteux.AllJoyn";

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_wififastride";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    WiFiDirectBroadcastReceiver receiver;

    static final public int SERVER_PORT = 4545;
    static final public int SOCKET_TIMEOUT = 12000;

    private final IntentFilter intentFilter = new IntentFilter();
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    private boolean retryChannel = false;

    InetAddress mGroupOwnerAddress;

    private WifiP2pDnsSdServiceRequest mDnsSdServiceRequest;
    private WifiP2pUpnpServiceRequest mUpnpServiceRequest;

    ListView mStatusesListView;
    ArrayAdapter<String> mStatusesAdapter;

    ProgressDialog progressDialog = null;

    private WiFiChatFragment chatFragment;

    @Override
    public void trace(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                appendStatus(status);
                //mStatusesListView.smoothScrollToPosition(mStatusesAdapter.getCount());
                //mStatusesListView.setSelection(mStatusesAdapter.getCount() - 1);
            }
        });
    }

    @Override
    public void alert(final String strIntent){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if( which == DialogInterface.BUTTON_POSITIVE ) {
                    startActivity(new Intent(strIntent));
                }
        }};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable Wi-Fi on your device?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_all_joyn);

        mStatusesListView = (ListView)findViewById(R.id.listStatuses);

        mStatusesAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<String>());

        mStatusesListView.setAdapter(mStatusesAdapter);

        PeersListFragment peersList = new PeersListFragment();
        getFragmentManager()
                .beginTransaction()
                .add(R.id.container_root, peersList, "peers")
                .commit();

        if( !isWifiDirectSupported(this) ) {
            TextView txtMe = (TextView)findViewById(R.id.txtMe);
            txtMe.setText("P2P is unsupported!");

            return;
        }

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(this.WIFI_P2P_SERVICE);
        if( mManager != null ){
            mChannel = mManager.initialize(this, getMainLooper(), null);

            appendStatus("WiFiP2pManger initialized");

            try {
                Method[] methods = WifiP2pManager.class.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    if (methods[i].getName().equals("deletePersistentGroup")) {
                        // Delete any persistent group
                        for (int netid = 0; netid < 32; netid++) {
                            methods[i].invoke(mManager, mChannel, netid, null);
                        }
                    }
                }

            }catch(Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }

        } else {
            TextView txtMe = (TextView)findViewById(R.id.txtMe);
            txtMe.setText("No WIFI_P2P_SERVICE");
        }

        //EnumerateNetworkInterfaces();

    }

    private boolean isWifiDirectSupported(Context ctx) {
        PackageManager pm = (PackageManager) ctx.getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        for (FeatureInfo info : features) {
            if (info != null && info.name != null && info.name.equalsIgnoreCase("android.hardware.wifi.direct")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void connectP2p(WifiP2pDevice device) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        //config.groupOwnerIntent = 15;
        config.wps.setup = WpsInfo.PBC;
        //config.wps.setup = WpsInfo.LABEL;

        if( mDnsSdServiceRequest != null ) {
            mManager.removeServiceRequest(mChannel,
                                    mDnsSdServiceRequest,
                                    new TaggedActionListener("Remove service request"));
        }

        mManager.connect(mChannel, config, new TaggedActionListener("Connected request"));

    }

    public void disconnectP2p(WifiP2pDevice device){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

            String message;

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        mManager.cancelConnect(mChannel, new TaggedActionListener("Disconnect from device") );
                        //mManager.removeGroup(mChannel, new TaggedActionListener("Group removed"));
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to disconnect?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }

    public void onClientConfirm(View view) {

        new ClientAsyncTask(this, mGroupOwnerAddress, "From client").execute();

    }

    public void onClickPassenger(View view){
        //discoverService();
        startRegistrationAndDiscovery();
    }

    public void onClickDriver(View view){
        startRegistrationAndDiscovery();
    }

    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        mManager.addLocalService(mChannel, service, new TaggedActionListener("Add Local Service"));

        setupDnsSd();
//        CheckBox cbUpnp = (CheckBox)findViewById(R.id.cbUpnp);
//        if( cbUpnp.isChecked() ) {
//            setupUpnp();
//        }

        mManager.discoverServices(mChannel,
                new TaggedActionListener("Service discovery init"));

    }

    private void setupUpnp() {
        /*
        * Register listeners for Upnp services
         */
        mManager.setUpnpServiceResponseListener(mChannel,
                new WifiP2pManager.UpnpServiceResponseListener() {


                    @Override
                    public void onUpnpServiceAvailable(List<String> list, // list of unique service names
                                                       WifiP2pDevice device) {
                        String traceMessage = "Upnp device:" + device.deviceName;
                        Log.d(LOG_TAG, traceMessage);
                        trace(traceMessage);
                    }
                });

        mUpnpServiceRequest = WifiP2pUpnpServiceRequest.newInstance();

        mManager.addServiceRequest(mChannel, mUpnpServiceRequest,
                new TaggedActionListener("Add Upnp request"));

    }

    private void setupDnsSd() {

          /*
         * Register listeners for DNS-SD (Bonjour) services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
        mManager.setDnsSdResponseListeners(mChannel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType,
                                                        WifiP2pDevice device) {

                        // A service has been discovered. Is this our app?
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                            String traceMessage = "DNS-SD SRV Record: " + instanceName;
                            Log.d(LOG_TAG, traceMessage);
                            trace(traceMessage);

                            // update the UI and add the item the discovered
                            // device.
                            PeersListFragment fragment = (PeersListFragment) getFragmentManager()
                                    .findFragmentByTag("peers");
                            if( fragment != null) {
                                IFragmentWithList fragWithLIst = (IFragmentWithList) fragment;
                                PeersListFragment.WiFiPeersAdapter adapter =
                                        (PeersListFragment.WiFiPeersAdapter) fragWithLIst.getListAdapter();

                                adapter.add(device);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                },
                new WifiP2pManager.DnsSdTxtRecordListener() {

                    @Override
                     /* Callback includes:
                     * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
                     * record: TXT record dta as a map of key/value pairs.
                     * device: The device running the advertised service.
                     */
                    public void onDnsSdTxtRecordAvailable(String fullDomainName,
                                                          Map<String, String> record,
                                                          WifiP2pDevice device) {

                        String traceMessage = "DNS-SD TXT Record: " +
                                                device.deviceName + " is " + record.get(TXTRECORD_PROP_AVAILABLE);

                        trace(traceMessage);
                        Log.d(LOG_TAG, traceMessage);
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        mDnsSdServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        mManager.addServiceRequest(mChannel, mDnsSdServiceRequest,
                new TaggedActionListener("Add DNS-SD request"));


    }

    public void appendStatus(final String status) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatusesAdapter.add(status);
                mStatusesAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onRestart() {
        Fragment frag = getFragmentManager().findFragmentByTag("peers");
        if (frag != null) {
            getFragmentManager().beginTransaction().remove(frag).commit();
        }
        super.onRestart();
    }

    @Override
    protected void onStop() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new TaggedActionListener("remove group"));
        }
        super.onStop();
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {

        String message;

        if (group != null) {
            appendStatus("Group SSID: " + group.getNetworkName());
            appendStatus("Group Size: " + group.getClientList().size());
            appendStatus("Group Address: " + group.getInterface());
            appendStatus("Group Passphrase: " + group.getPassphrase());

            EnumerateNetworkInterfaces();

            for( WifiP2pDevice client : group.getClientList() ){
                appendStatus("Client: " + client.deviceName);
            }

            WifiP2pDevice p2pDevice = group.getOwner();
            message = "serviceDiscoveryCapable: " +
                    ((p2pDevice.isServiceDiscoveryCapable()) ? true : false);
            appendStatus(message);
            Log.d(LOG_TAG, message);

            message = "wpsDisplaySupported: " +
                    ((p2pDevice.wpsDisplaySupported()) ? true : false);
            appendStatus(message);
            Log.d(LOG_TAG, message);

            message = "wpsKeypadSupported: " +
                    ((p2pDevice.wpsKeypadSupported()) ? true : false);
            appendStatus(message);
            Log.d(LOG_TAG, message);

            message = "wpsPbcSupported: " +
                    ((p2pDevice.wpsPbcSupported()) ? true : false);
            appendStatus(message);
            Log.d(LOG_TAG, message);
        } else {
            message = "Group is NULL";
            appendStatus(message);
            Log.d(LOG_TAG, message);
        }


    }

    private void EnumerateNetworkInterfaces(){

        String message;

        try{
            for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                appendStatus("Interface name: " + intf.getDisplayName());
                appendStatus("Is P2P:" + intf.isPointToPoint());

                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    message = inetAddress.getHostAddress() + "Is Loopback? ";

                    if (!inetAddress.isLoopbackAddress()) {
                        message += "no";
//                            if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
//                                return inetAddress.getAddress();
//                            }
                    } else
                        message += "yes";

                    appendStatus(message);


                }
            }
        } catch (SocketException | NullPointerException ex) {
            //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        }

    }

    //
    // Implementation of WifiP2pManager.ConnectionInfoListener
    //

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {

        appendStatus("onConnectionChanged");

        if( !p2pInfo.groupFormed ) {
            appendStatus("Group not formed");
            return;
        }

         /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        TextView txtMe = (TextView)findViewById(R.id.txtMe);
        if (p2pInfo.isGroupOwner) {

            txtMe.setText("ME: GroupOwner, Group Owner IP: " + p2pInfo.groupOwnerAddress.getHostAddress());
            //new ServerAsyncTask(this).execute();
            new ServerAsyncTask(this)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        } else {

            txtMe.setText("ME: NOT GroupOwner, Group Owner IP: " + p2pInfo.groupOwnerAddress.getHostAddress());
            mGroupOwnerAddress = p2pInfo.groupOwnerAddress;

            new ClientAsyncTask(this, mGroupOwnerAddress, "From client").execute();
            findViewById(R.id.btnClientConfirm).setVisibility(View.VISIBLE);

         }

        // Optionally may request group info
        //mManager.requestGroupInfo(mChannel, this);

        chatFragment = new WiFiChatFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.container_root, chatFragment).commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_all_joyn, menu);
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

    //
    // ChannelListener implementation
    //
    @Override
    public void onChannelDisconnected() {

        // we will try once more
        if( mManager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            retryChannel = true;
            mManager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost permanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }


    class TaggedActionListener implements WifiP2pManager.ActionListener{

        String tag;

        TaggedActionListener(String tag){
            this.tag = tag;
        }

        @Override
        public void onSuccess() {
            String message = tag + " succeeded";
            appendStatus(message);
            Log.d(LOG_TAG, message);
        }

        @Override
        public void onFailure(int reasonCode) {
            String message = tag + " failed. Reason :" + failureReasonToString(reasonCode);
            appendStatus(message);
            Log.d(LOG_TAG, message);
        }

        private String failureReasonToString(int reason) {

            // Failure reason codes:
            // 0 - internal error
            // 1 - P2P unsupported
            // 2- busy

            switch ( reason ){
                case WifiP2pManager.ERROR:
                    return "Internal Error";

                case WifiP2pManager.P2P_UNSUPPORTED:
                    return "P2P unsupported";

                case WifiP2pManager.BUSY:
                    return "Busy";

                case WifiP2pManager.NO_SERVICE_REQUESTS:
                    return "No service request";

                default:
                    return "Unknown";
            }
        }
    }

    public static class ClientAsyncTask extends AsyncTask<Void, Void, String> {

        Context mContext;
        String mMessage;
        InetAddress mGroupHostAddress;

        public ClientAsyncTask(Context context,
                               InetAddress groupOwnerAddress,
                               String message){
            this.mContext = context;
            this.mGroupHostAddress = groupOwnerAddress;
            this.mMessage = message;
        }

        @Override
        protected String doInBackground(Void... voids) {

            Log.d(LOG_TAG, "ClientAsyncTask:doBackground() called");

            Socket socket = new Socket();
            try {

                String traceMessage = "Client socket created";
                Log.d(LOG_TAG, traceMessage);
                ((AllJoynActivity)mContext).appendStatus(traceMessage);

                // binds this socket to the local address.
                // Because the parameter is null, this socket will  be bound
                // to an available local address and free port
                socket.bind(null);
                InetAddress localAddress = socket.getLocalAddress();

                traceMessage = String.format("Local socket. Address: %s. Port: %d",
                                            localAddress.getHostAddress(),
                                            socket.getLocalPort());
                Log.d(LOG_TAG, traceMessage);
                ((AllJoynActivity)mContext).appendStatus(traceMessage);

                traceMessage = String.format("Server socket. Address: %s. Port: %d",
                                        mGroupHostAddress.getHostAddress(),
                                        SERVER_PORT);
                Log.d(LOG_TAG, traceMessage);
                ((AllJoynActivity)mContext).appendStatus(traceMessage);

                socket.connect(
                        new InetSocketAddress(mGroupHostAddress.getHostAddress(), SERVER_PORT),
                        SOCKET_TIMEOUT);

                traceMessage = "Client socket connected";
                Log.d(LOG_TAG, traceMessage);
                ((AllJoynActivity)mContext).appendStatus(traceMessage);

                OutputStream os = socket.getOutputStream();
                os.write(mMessage.getBytes());

                os.close();

            } catch (IOException ex) {
                ((AllJoynActivity)mContext).appendStatus(ex.getMessage());
                Log.e(LOG_TAG, ex.getMessage());
            } finally {
                try {
                    socket.close();
                } catch(Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }

            return null;
        }
    }

    public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {

        Context mContext;

        public ServerAsyncTask(Context context){
            mContext = context;
        }

        @Override
        protected String doInBackground(Void... voids) {

            Log.d(LOG_TAG, "ServerAsyncTask:doBackground() called");

            try {
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

                String traceMessage = "Server: Socket opened on port " + SERVER_PORT;
                Log.d(LOG_TAG, traceMessage);
                ((AllJoynActivity)mContext).appendStatus(traceMessage);

                Socket clientSocket = serverSocket.accept();

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                traceMessage = reader.readLine();
                Log.d(LOG_TAG, traceMessage);
                ((AllJoynActivity)mContext).appendStatus(traceMessage);

                serverSocket.close();

                traceMessage = "Server socket closed";
                Log.d(LOG_TAG, traceMessage);
                ((AllJoynActivity)mContext).appendStatus(traceMessage);

            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                ((AllJoynActivity)mContext).appendStatus(e.getMessage());
            }

            return null;
        }
    }
}

