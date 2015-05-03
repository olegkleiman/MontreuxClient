package com.example.montreuxclient.wifip2p;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.montreuxclient.R;

public class WiFiChatFragment extends Fragment {

    private View view;
    private TextView chatLine;

    public WiFiChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_wi_fi_chat, container, false);

        chatLine = (TextView) view.findViewById(R.id.txtChatLine);
        view.findViewById(R.id.button1).setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                chatLine.setText("");
                chatLine.clearFocus();
            }
        });

        return view;

    }

}
