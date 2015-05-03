package com.example.montreuxclient.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.montreuxclient.R;

import java.util.List;

/**
 * Created by Oleg on 03-May-15.
 */
public class TraceAdapter extends ArrayAdapter<String> {

    private List<String> items;
    Context mContext;
    LayoutInflater m_inflater = null;

    public TraceAdapter(Context context, int textViewResourceId,
                            List<String> objects){
        super(context, textViewResourceId, objects);

        mContext = context;
        items = objects;

        m_inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ConfigurationHolder holder = null;

        if( row == null ) {
            holder = new ConfigurationHolder();

            row = m_inflater.inflate(android.R.layout.simple_list_item_1, null);
            holder.Name = (TextView)row.findViewById(android.R.id.text1);

            row.setTag(holder);

        } else {
            holder = (ConfigurationHolder)row.getTag();
        }

        String str = items.get(position);
        holder.Name.setText(str);

        return row;
    }


    static class ConfigurationHolder{
        TextView Name;
    }
}
