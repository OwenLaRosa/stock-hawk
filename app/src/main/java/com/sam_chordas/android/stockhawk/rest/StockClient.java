package com.sam_chordas.android.stockhawk.rest;

import android.util.Log;

import com.sam_chordas.android.stockhawk.info.Article;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Owen LaRosa on 9/13/16.
 */

public class StockClient {

    private static final String LOG_TAG = StockClient.class.getSimpleName();

    private OkHttpClient mClient = new OkHttpClient();

    private static final StringCache cache = new StringCache();

    public float[] getHistoricalDataForStock(String symbol, String startDate) throws IOException, JSONException {
        // should be equal to today's date
        String endDate = Utils.getFormattedToday();
        // create a request, using the specified start and end dates
        // URL referenced from https://discussions.udacity.com/t/plotting-the-stock-price-over-time-within-the-stock-hawk-project/159569/8
        String urlString = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20%3D%20%22" + symbol + "%22%20and%20startDate%20%3D%20%22" + startDate + "%22%20and%20endDate%20%3D%20%22" + endDate + "%22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
        // if already fetched, get the old data
        String result = cache.getString(urlString);
        if (result == null) {
            Request request = new Request.Builder().url(urlString).build();
            Log.d(LOG_TAG, request.url().toString());
            Response response = mClient.newCall(request).execute();
            result = response.body().string();
            cache.putString(urlString, result);
        }
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

    public ArrayList<Article> getNewsForStock(String symbol) throws IOException, JSONException {
        Request request = new Request.Builder().url("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20rss%20where%20url%3D%22http%3A%2F%2Ffinance.yahoo.com%2Frss%2Fheadline%3Fs%3D" + symbol + "%22&format=json&diagnostics=true&callback=").build();
        Response response = mClient.newCall(request).execute();
        String result = response.body().string();
        // parse the result
        JSONObject rootObject = new JSONObject(result);
        JSONObject query = rootObject.getJSONObject("query");
        JSONObject results = query.getJSONObject("results");
        JSONArray articles = results.getJSONArray("item");
        final ArrayList<Article> articleObjects = new ArrayList<Article>();
        for (int i = 0; i < articles.length(); i++) {
            // create an article object for each item
            String title = articles.getJSONObject(i).getString("title");
            String link = articles.getJSONObject(i).getString("link");
            Article newArticle = new Article(title, link);
            articleObjects.add(newArticle);
        }
        return articleObjects;
    }

    public JSONObject getStockForSearchTerm(String searchTerm) throws IOException, JSONException {
        Request request = new Request.Builder().url("http://d.yimg.com/autoc.finance.yahoo.com/autoc?query=" + URLEncoder.encode(searchTerm) +"&region=US&lang=en-us").build();
        Response response = mClient.newCall(request).execute();
        String responseBody = response.body().string();

        JSONObject rootObject = new JSONObject(responseBody);
        JSONObject resultSet = rootObject.getJSONObject("ResultSet");
        JSONArray results = resultSet.getJSONArray("Result");

        JSONObject stock;
        // used to filter only stocks, exclude other types of assets
        ArrayList<JSONObject> equities = new ArrayList<JSONObject>();
        // try to favor U.S. equities, this could be localized depending on the user's location
        String[] exchanges = {"NASDAQ", "NYSE"};
        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            String typeDisp = result.getString("typeDisp");
            if (result.getString("typeDisp").equals("Equity")) {
                equities.add(result);
            }
        }
        for (int i = 0; i < equities.size(); i++) {
            if (arrayContains(exchanges, equities.get(i).getString("exchDisp"))) {
                return equities.get(i);
            }
        }
        // in case there are any non-US equities
        if (equities.size() != 0) {
            // get the most relevant result
            return equities.get(0);
        } else {
            // null means that the search failed to return any relevant equities
            return null;
        }
    }

    /**
     * Helper to determine if an array contains the given string
     * @param array Strings to search
     * @param value String to search for
     * @return true if array contains the string, otherwise return false
     */
    private boolean arrayContains(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (value.equals(array[i])) {
                return true;
            }
        }
        return false;
    }

}
