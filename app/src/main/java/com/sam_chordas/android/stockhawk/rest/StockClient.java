package com.sam_chordas.android.stockhawk.rest;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by Owen LaRosa on 9/13/16.
 */

public class StockClient {

    private static final String LOG_TAG = StockClient.class.getSimpleName();

    private OkHttpClient mClient = new OkHttpClient();

    public float[] getHistoricalDataForStock(String symbol) throws IOException, JSONException {
        // create a request, use hardcoded symbol to fetch data for now
        // URL referenced from https://discussions.udacity.com/t/plotting-the-stock-price-over-time-within-the-stock-hawk-project/159569/8
        Request request = new Request.Builder().url("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20%3D%20%22" + symbol + "%22%20and%20startDate%20%3D%20%222009-09-11%22%20and%20endDate%20%3D%20%222010-03-10%22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys").build();
        Response response = mClient.newCall(request).execute();
        String result = response.body().string();
        // after the download, parse the result
        JSONObject rootObject = new JSONObject(result);
        JSONObject queryObject = rootObject.getJSONObject("query");
        JSONObject resultsObject = queryObject.getJSONObject("results");
        JSONArray quotes = resultsObject.getJSONArray("quote");

        // extract closing prices into an array
        float[] dataSet = new float[quotes.length()];
        for (int i = 0; i < quotes.length(); i++) {
            JSONObject quote = quotes.getJSONObject(i);
            float close = (float) quote.getDouble("Close");
            dataSet[i] = close;
        }
        return dataSet;
    }

}
