package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
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

    public StockDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stock_detail, container, false);
        mLineGraph = (LineChartView) rootView.findViewById(R.id.line_graph);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getGraphData();
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
        for (int i = 0; i < quotes.length(); i++) {
            JSONObject quote = quotes.getJSONObject(i);
            float close = (float) quote.getDouble("Close");
            // no label for now
            dataSet.addPoint("", close);
        }
        mLineGraph.addData(dataSet);
        mLineGraph.show();
    }

}
