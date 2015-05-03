package com.example.montreuxclient;


import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;


//
// For images palette, see http://www.color-hex.com/color-palette/872
// 

public class ImageAdapter extends BaseAdapter {
    private Context mContext;

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
        return mThumbIds.length;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) { 
        	textView = new TextView(mContext);
        	textView.setLayoutParams(new GridView.LayoutParams(200, 200));
        	textView.setPadding(8, 8, 8, 8);
        	textView.setText(mTitles[position]);
        	textView.setTextColor(Color.WHITE);
        	textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        }
        else
        	textView = (TextView) convertView;
        
        final Drawable drawable = mContext.getResources().getDrawable(mThumbIds[position]);
        textView.setBackground(drawable);
        
        return textView;
    }

    // references to our images
    private Integer[] mThumbIds = {
            R.drawable.circle_azure, 
            R.drawable.circle_pink,
            R.drawable.circle_firebrick1,
            R.drawable.circle_blue2,
            R.drawable.circle_black,
            R.drawable.circle_azure,
            R.drawable.circle_purple,
            R.drawable.circle_purple900,
            R.drawable.circle_yellow1,
            R.drawable.circle_firebrick1
    };
    
    // and their captions
    private String[] mTitles = { "Reports", "Pictures", "Videos", "NFC",
                                "Write NFC", "BLE", "OpenCV", "FastCV",
                                "Prefs", "AllJoyn"};
}
