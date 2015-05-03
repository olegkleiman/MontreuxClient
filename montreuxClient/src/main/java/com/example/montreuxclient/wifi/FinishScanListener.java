package com.example.montreuxclient.wifi;

import java.util.ArrayList;

/**
 * Created by Oleg on 03-May-15.
 */
public interface FinishScanListener {


    /**
     * Interface called when the scan method finishes. Network operations should not execute on UI thread
     * @param  clients of {@link ClientScanResult}
     */

    public void onFinishScan(ArrayList<ClientScanResult> clients);

}