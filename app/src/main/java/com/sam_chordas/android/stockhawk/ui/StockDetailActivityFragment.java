package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.db.chart.view.animation.easing.LinearEase;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * A placeholder fragment containing a simple view.
 */
public class StockDetailActivityFragment extends Fragment {

    private static final String LOG_TAG = StockDetailActivityFragment.class.getSimpleName();

    private OkHttpClient mClient = new OkHttpClient();

    private LineChartView mLineGraph;
    private ProgressBar mChartProgressBar;

    public StockDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stock_detail, container, false);
        mLineGraph = (LineChartView) rootView.findViewById(R.id.line_graph);
        mChartProgressBar = (ProgressBar) rootView.findViewById(R.id.chart_progress_bar);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getGraphData();
                    getCompanyNews();
                } catch (IOException e) {
                    Log.d(LOG_TAG, "Failed to download historical data" + e.toString());
                }
            }
        });

        return rootView;
    }

    // this should be moved outside the fragment
    private void getGraphData() throws IOException {
        // create a request, use hardcoded symbol to fetch data for now
        // URL referenced from https://discussions.udacity.com/t/plotting-the-stock-price-over-time-within-the-stock-hawk-project/159569/8
        mChartProgressBar.setVisibility(View.VISIBLE);
        Request request = new Request.Builder().url("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20%3D%20%22YHOO%22%20and%20startDate%20%3D%20%222009-09-11%22%20and%20endDate%20%3D%20%222010-03-10%22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys").build();
        Response response = mClient.newCall(request).execute();
        String result = response.body().string();
        try {
            showDataInGraph(result);
        } catch (JSONException e) {}
    }

    private void showDataInGraph(String json) throws JSONException {
        JSONObject rootObject = new JSONObject(json);
        JSONObject queryObject = rootObject.getJSONObject("query");
        JSONObject resultsObject = queryObject.getJSONObject("results");
        JSONArray quotes = resultsObject.getJSONArray("quote");
        LineSet dataSet = new LineSet();
        if (quotes.length() == 0) {
            return;
        }
        // start with an initial value for minimum and maximum
        // this is so we can adjust the bounds of the Y axis
        float min = (float) quotes.getJSONObject(0).getDouble("Close");
        float max = (float) quotes.getJSONObject(0).getDouble("Close");
        for (int i = 0; i < quotes.length(); i++) {
            JSONObject quote = quotes.getJSONObject(i);
            float close = (float) quote.getDouble("Close");
            Log.d(LOG_TAG, String.format("%g", close));
            // no label for now
            dataSet.addPoint("", close);
            // update the upper and lower bounds if needed
            if (close < min) min = close;
            if (close > max) max = close;
        }
        int multiple;
        // round the numbers to the nearest multiple of
        if (max < 10) {
            multiple = 1;
        } else if (max < 50) {
            multiple = 5;
        } else if (max < 100) {
            multiple = 10;
        } else if (max < 200) {
            multiple = 20;
        } else if (max < 400) {
            multiple = 50;
        } else {
            multiple = 100;
        }
        // get the bounds from the max and min closing prices
        int upperBound = roundValueToMultiple(max, multiple, true);
        int lowerBound = roundValueToMultiple(min, multiple, false);
        // hide progress bar on the UI thread
        mChartProgressBar.post(new Runnable() {
            @Override
            public void run() {
                mChartProgressBar.setVisibility(View.GONE);
            }
        });
        displayChartWithData(dataSet, lowerBound, upperBound);
    }

    /**
     * Round a number according to the multiple to the nearest multiple
     * @param value The number to be rounded
     * @param multiple Nearest multiple to be rounded to
     * @param roundUp true if the number should be rounded up, false if down
     * @return
     */
    public int roundValueToMultiple(float value, int multiple, boolean roundUp) {
        int multiplyBy = roundUp ? multiple + 1 : multiple;
        return (int) Math.floor((double) value / multiple) * multiplyBy;
    }

    /**
     * Set the chart's data, set appearance, add animation, and display it onscreen
     * @param dataSet Points to be displayed on the graph
     * @param lowerBound Minimum value shown on the Y axis
     * @param upperBound Maximum value shown on the Y axis
     */
    private void displayChartWithData(LineSet dataSet, int lowerBound, int upperBound) {
        // add the data and set any visual appearance
        dataSet.setColor(getResources().getColor(R.color.material_blue_700));
        mLineGraph.addData(dataSet);
        mLineGraph.setAxisBorderValues(lowerBound, upperBound);
        // add an animation to the chart, referenced from: http://stackoverflow.com/questions/36164123/animations-in-williamchart/38760291
        Animation animation = new Animation(500);
        animation.setEasing(new LinearEase());
        mLineGraph.show(animation);
    }

    private void getCompanyNews() throws IOException {
        Request request = new Request.Builder().url("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20rss%20where%20url%3D%22http%3A%2F%2Ffinance.yahoo.com%2Frss%2Fheadline%3Fs%3Dgoog%22&format=json&diagnostics=true&callback=").build();
        Response response = mClient.newCall(request).execute();
        String result = response.body().toString();
        try {
            showNewsInList(result);
        } catch (JSONException e) {}
    }

    private void showNewsInList(String json) throws JSONException {
        JSONObject rootObject = new JSONObject(json);
        JSONObject results = rootObject.getJSONObject("results");
        JSONArray articles = rootObject.getJSONArray("item");
    }

}
