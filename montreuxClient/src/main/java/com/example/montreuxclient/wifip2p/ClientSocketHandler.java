package com.example.montreuxclient.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.example.montreuxclient.AllJoynActivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Oleg Kleiman on 19-Apr-15.
 */
public class ClientSocketHandler extends Thread{

    private static final String TAG = "ClientSocketHandler";
    private Handler handler;
    //private ChatManager chat;
    private InetAddress mAddress;

    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                    AllJoynActivity.SERVER_PORT), 5000);
            Log.d(TAG, "Launching the I/O handler");
            //chat = new ChatManager(socket, handler);
            //new Thread(chat).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }

}
