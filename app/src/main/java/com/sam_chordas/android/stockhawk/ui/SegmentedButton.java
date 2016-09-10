package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.sam_chordas.android.stockhawk.R;

import java.util.ArrayList;

/**
 * Created by Owen LaRosa on 9/9/16.
 */

public class SegmentedButton extends LinearLayout {

    private static final String LOG_TAG = SegmentedButton.class.getSimpleName();

    private Context mContext;

    private ArrayList<Button> buttons;

    private Button clickedButton;

    public SegmentedButton(Context context) {
        super(context);
        mContext = context;
        setup();
    }

    public SegmentedButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
        setup();
    }

    public SegmentedButton(Context context, AttributeSet attributeSet, int defaultStyle) {
        super(context, attributeSet, defaultStyle);
        mContext = context;
        setup();
    }

    private void setup() {
        // inflate the linear layout
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.segmented_button_layout, this);
        // initialize the buttons array list
        buttons = new ArrayList<Button>();

    }

    /**
     * Add a button to the end of the view
     * @param title Text to be displayed on the button
     * @return The newly added button
     */
    public Button addButtonWithTitle(String title, OnClickListener callback) {
        Button button = new AppCompatButton(mContext);
        button.setText(title);
        button.setTextColor(getResources().getColor(R.color.material_blue_500));
        button.setBackgroundColor(getResources().getColor(R.color.white));
        // set the weight so buttons will take up equal space
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
        // leave enough room for bottom shadow when buttons are elevated
        // also add some spacing inbetween the buttons
        params.setMargins(4, 0, 4, 8);
        button.setLayoutParams(params);
        // add the callback to the click listener's default behavior
        BaseOnClickListener clickListener = new BaseOnClickListener();
        clickListener.callbackListener = callback;
        button.setOnClickListener(clickListener);
        button.setBackgroundColor(getResources().getColor(R.color.transparent));
        buttons.add(button);
        addView(button);
        return button;
    }

    /**
     * Remove a button from the view
     * @param index Index of the button to be removed
     */
    public void removeButtonAtIndex(int index) {
        if (buttons.get(index) == clickedButton) {
            clickedButton = null;
        }
        buttons.remove(index);
    }

    private class BaseOnClickListener implements OnClickListener {
        OnClickListener callbackListener;
        @Override
        public void onClick(View v) {
            if (clickedButton != null) {
                // reset button to flat state
                clickedButton.setBackgroundColor(getResources().getColor(R.color.transparent));
            }
            clickedButton = (Button) v;
            // setting an opaque background shows a border
            clickedButton.setBackgroundColor(getResources().getColor(R.color.white));
            // after configuring the UI, invoke the custom callback
            if (callbackListener != null) callbackListener.onClick(v);
        }
    }

}
