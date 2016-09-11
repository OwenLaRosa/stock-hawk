package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
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
import com.sam_chordas.android.stockhawk.info.Article;
import com.sam_chordas.android.stockhawk.rest.NewsAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class StockDetailActivityFragment extends Fragment {

    private static final String LOG_TAG = StockDetailActivityFragment.class.getSimpleName();

    private OkHttpClient mClient = new OkHttpClient();

    private LineChartView mLineGraph;
    private ProgressBar mChartProgressBar;
    private SegmentedButton mChartSegmentedButton;
    private RecyclerView mRecyclerView;

    public StockDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stock_detail, container, false);
        mLineGraph = (LineChartView) rootView.findViewById(R.id.line_graph);
        mChartProgressBar = (ProgressBar) rootView.findViewById(R.id.chart_progress_bar);
        mChartSegmentedButton = (SegmentedButton) rootView.findViewById(R.id.chart_segmented_button);
        initSegmentedButton();
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.news_recycler_view);
        NewsAdapter newsAdapter = new NewsAdapter();
        mRecyclerView.setAdapter(newsAdapter);
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(getActivity(),
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        String url = ((NewsAdapter) mRecyclerView.getAdapter()).getArticle(position).link;
                        // open the link in the browser
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }
                }));

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

    private void initSegmentedButton() {
        mChartSegmentedButton.addButtonWithTitle("1 Month", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "1 month");
            }
        });
        mChartSegmentedButton.addButtonWithTitle("6 Months", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "6 months");
            }
        });
        mChartSegmentedButton.addButtonWithTitle("1 Year", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "1 year");
            }
        });
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
        // set the bounds to the rounded min and max values
        int upperBound = (int) Math.ceil(max / 1);
        int lowerBound = (int) Math.floor(min / 1);
        // get the interval between the extremes to calculate step value
        int step = (int) Math.ceil((max - min) / 5);
        // hide progress bar on the UI thread
        mChartProgressBar.post(new Runnable() {
            @Override
            public void run() {
                mChartProgressBar.setVisibility(View.GONE);
            }
        });
        displayChartWithData(dataSet, lowerBound, upperBound, step);
    }

    /**
     * Set the chart's data, set appearance, add animation, and display it onscreen
     * @param dataSet Points to be displayed on the graph
     * @param lowerBound Minimum value shown on the Y axis
     * @param upperBound Maximum value shown on the Y axis
     */
    private void displayChartWithData(LineSet dataSet, int lowerBound, int upperBound, int step) {
        // add the data and set any visual appearance
        // only access resources if the fragment is attached to prevent illegal state exception
        if (isAdded()) dataSet.setColor(getResources().getColor(R.color.material_blue_700));
        mLineGraph.addData(dataSet);
        mLineGraph.setAxisBorderValues(lowerBound, upperBound, step);
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
        ArrayList<Article> articleObjects = new ArrayList<Article>();
        for (int i = 0; i < articles.length(); i++) {
            // create an article object for each item
            String title = articles.getJSONObject(i).getString("title");
            String link = articles.getJSONObject(i).getString("link");
            Article newArticle = new Article(title, link);
            articleObjects.add(newArticle);
        }
        ((NewsAdapter) mRecyclerView.getAdapter()).addAllArticles(articleObjects);
    }

}
