package com.example.montreuxclient.wifip2p;


import android.app.ListFragment;
import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.montreuxclient.AllJoynActivity;
import com.example.montreuxclient.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PeersListFragment extends Fragment
    implements IFragmentWithList {

    WiFiPeersAdapter mPeersAdapter;

    @Override
    public ArrayAdapter getListAdapter() {
        return mPeersAdapter;
    }

    public interface DeviceClickListener{
        void connectP2p(WifiP2pDevice device);
        void disconnectP2p(WifiP2pDevice device);
    }

    public PeersListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_peers_list, container, false);

        mPeersAdapter = new WiFiPeersAdapter(this.getActivity(),
                R.layout.row_devices,
                new ArrayList<WifiP2pDevice>());

        ListView listView = (ListView)view.findViewById(R.id.listViewPeers);
        listView.setAdapter(mPeersAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long l) {
                WifiP2pDevice device = (WifiP2pDevice) parent.getItemAtPosition(position);

                if (device.status == WifiP2pDevice.CONNECTED) {
                    ((DeviceClickListener) getActivity()).disconnectP2p(device);
                } else if (device.status == WifiP2pDevice.AVAILABLE) {

                    ((DeviceClickListener) getActivity()).connectP2p(device);
//        ((TextView) v.findViewById(android.R.id.text2)).setText("Connecting");
                }

            }
        });

        return view;

    }

    @Override
    public void onPause(){
        super.onPause();
    }

    public class WiFiPeersAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;
        Context mContext;

        LayoutInflater m_inflater = null;

        public WiFiPeersAdapter(Context context, int textViewResourceId,
                                List<WifiP2pDevice> objects){
            super(context, textViewResourceId, objects);

            mContext = context;
            items = objects;

            m_inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public void add(WifiP2pDevice item) {

            if( !items.contains(item) )
              items.add(item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View row = convertView;
            DeviceHolder holder = null;

            if( row == null ) {

                holder = new DeviceHolder();

                row = m_inflater.inflate(R.layout.row_devices, null);

                holder.deviceName = (TextView)row.findViewById(R.id.device_name);
                holder.deviceDetails = (TextView)row.findViewById(R.id.device_details);
                holder.deviceType = (TextView)row.findViewById(R.id.device_type);
                holder.deviceStatus = (TextView)row.findViewById(R.id.device_status);

                row.setTag(holder);
            } else {
                holder = (DeviceHolder)row.getTag();
            }

            WifiP2pDevice device = items.get(position);
            holder.deviceName.setText(device.deviceName);
            holder.deviceDetails.setText(device.deviceAddress);
            holder.deviceType.setText(device.primaryDeviceType);
            holder.deviceStatus.setText(getDeviceStatus(device.status));

            return row;
        }

        private String getDeviceStatus(int deviceStatus) {
            switch (deviceStatus) {
                case WifiP2pDevice.AVAILABLE:
                    return "Available";
                case WifiP2pDevice.INVITED:
                    return "Invited";
                case WifiP2pDevice.CONNECTED:
                    return "Connected";
                case WifiP2pDevice.FAILED:
                    return "Failed";
                case WifiP2pDevice.UNAVAILABLE:
                    return "Unavailable";
                default:
                    return "Unknown";

            }
        }
    }

    static class DeviceHolder{
        TextView deviceName;
        TextView deviceDetails;
        TextView deviceType;
        TextView deviceStatus;
    }


}
