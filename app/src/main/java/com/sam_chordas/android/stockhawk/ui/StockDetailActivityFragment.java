package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
public class StockDetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = StockDetailActivityFragment.class.getSimpleName();

    private static final int LOADER_ID = 0;

    private OkHttpClient mClient = new OkHttpClient();
    private Uri mUri;

    private LineChartView mLineGraph;
    private ProgressBar mChartProgressBar;
    private SegmentedButton mChartSegmentedButton;
    private RecyclerView mRecyclerView;
    private ImageView mChangeImageView;
    private TextView mSymbolTextView;
    private TextView mPercentChangeTextView;

    public StockDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stock_detail, container, false);

        Intent intent = getActivity().getIntent();
        if (null != intent && intent.hasExtra(StockDetailActivity.DETAIL_URI)) {
            mUri = (Uri) intent.getParcelableExtra(StockDetailActivity.DETAIL_URI);
        }

        mLineGraph = (LineChartView) rootView.findViewById(R.id.line_graph);
        mChartProgressBar = (ProgressBar) rootView.findViewById(R.id.chart_progress_bar);
        mChartSegmentedButton = (SegmentedButton) rootView.findViewById(R.id.chart_segmented_button);
        initSegmentedButton();
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.news_recycler_view);
        NewsAdapter newsAdapter = new NewsAdapter();
        mRecyclerView.setAdapter(newsAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
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

        mChangeImageView = (ImageView) rootView.findViewById(R.id.change_image_view);
        mSymbolTextView = (TextView) rootView.findViewById(R.id.symbol_text_view);
        mPercentChangeTextView = (TextView) rootView.findViewById(R.id.percent_change_text_view);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportLoaderManager().initLoader(LOADER_ID, null, this);
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
    private void getGraphData(String symbol) throws IOException {
        // create a request, use hardcoded symbol to fetch data for now
        // URL referenced from https://discussions.udacity.com/t/plotting-the-stock-price-over-time-within-the-stock-hawk-project/159569/8
        mChartProgressBar.setVisibility(View.VISIBLE);
        Request request = new Request.Builder().url("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20%3D%20%22" + symbol + "%22%20and%20startDate%20%3D%20%222009-09-11%22%20and%20endDate%20%3D%20%222010-03-10%22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys").build();
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

    private void getCompanyNews(String symbol) throws IOException {
        Request request = new Request.Builder().url("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20rss%20where%20url%3D%22http%3A%2F%2Ffinance.yahoo.com%2Frss%2Fheadline%3Fs%3D" + symbol + "%22&format=json&diagnostics=true&callback=").build();
        Response response = mClient.newCall(request).execute();
        String result = response.body().string();
        try {
            showNewsInList(result);
        } catch (JSONException e) {}
    }

    private void showNewsInList(String json) throws JSONException {
        JSONObject rootObject = new JSONObject(json);
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
        // add articles to adapter and update UI on main thread
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                ((NewsAdapter) mRecyclerView.getAdapter()).addAllArticles(articleObjects);
            }
        });
    }

    // method related to the cursor loader

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (null != mUri) {
            return new CursorLoader(getActivity(), mUri, null, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) return;

        final String symbol = data.getString(data.getColumnIndex("symbol"));
        String percentChange = data.getString(data.getColumnIndex("percent_change"));
        mSymbolTextView.setText(symbol);
        mPercentChangeTextView.setText(percentChange);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getGraphData(symbol);
                    getCompanyNews(symbol);
                } catch (IOException e) {
                    Log.d(LOG_TAG, "Failed to download historical data" + e.toString());
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // do nothing
    }
}
