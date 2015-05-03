package com.example.montreuxclient.nfc;

import java.util.ArrayList;
import java.util.List;

import com.example.montreuxclient.ntf.record.ParsedNdefRecord;
import com.example.montreuxclient.ntf.record.SmartPoster;
import com.example.montreuxclient.ntf.record.TextRecord;
import com.example.montreuxclient.ntf.record.UriRecord;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

/**
 * Utility class for creating {@link ParsedNdefMessage}s.
 */
public class NdefMessageParser {

    // Utility class
    private NdefMessageParser() {

    }

    /** Parse an NdefMessage */
    public static List<ParsedNdefRecord> parse(NdefMessage message) {
        return getRecords(message.getRecords());
    }

    public static List<ParsedNdefRecord> getRecords(NdefRecord[] records) {
        List<ParsedNdefRecord> elements = new ArrayList<ParsedNdefRecord>();
        for (NdefRecord record : records) {
            if (UriRecord.isUri(record)) {
                elements.add(UriRecord.parse(record));
            } else if (TextRecord.isText(record)) {
                elements.add(TextRecord.parse(record));
            } else if (SmartPoster.isPoster(record)) {
                elements.add(SmartPoster.parse(record));
            }
        }
        return elements;
    }
}
