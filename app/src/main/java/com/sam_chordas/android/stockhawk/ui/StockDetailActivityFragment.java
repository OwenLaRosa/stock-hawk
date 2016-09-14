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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.info.Article;
import com.sam_chordas.android.stockhawk.rest.NewsAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.StockClient;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class StockDetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = StockDetailActivityFragment.class.getSimpleName();

    private static final int LOADER_ID = 0;

    private OkHttpClient mClient = new OkHttpClient();
    private StockClient stockClient = new StockClient();
    private Uri mUri;
    private String mSymbol;

    private LineChartView mLineGraph;
    private ProgressBar mChartProgressBar;
    private SegmentedButton mChartSegmentedButton;
    private RecyclerView mRecyclerView;
    private ImageView mChangeImageView;
    private TextView mSymbolTextView;
    private TextView mPercentChangeTextView;
    private Button mFirstButton;

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
        mSymbolTextView = (TextView) rootView.findViewById(R.id.price_text_view);
        mPercentChangeTextView = (TextView) rootView.findViewById(R.id.percent_change_text_view);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    private void initSegmentedButton() {
        mFirstButton = mChartSegmentedButton.addButtonWithTitle("1 Month", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        getGraphData(Utils.getFormattedOneMonthAgo());
                    }
                });
            }
        });
        mChartSegmentedButton.addButtonWithTitle("6 Months", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        getGraphData(Utils.getFormattedSixMonthsAgo());
                    }
                });
            }
        });
        mChartSegmentedButton.addButtonWithTitle("1 Year", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        getGraphData(Utils.getFormattedOneYearAgo());
                    }
                });
            }
        });
    }

    private void getGraphData(final String startDate) {
        float[] quotes = new float[0];
        Log.d(LOG_TAG, Utils.getFormattedToday());
        Log.d(LOG_TAG, Utils.getFormattedOneYearAgo());
        try {
            quotes = stockClient.getHistoricalDataForStock(mSymbol, Utils.getFormattedOneMonthAgo());
        } catch (IOException e) {

        } catch (JSONException e) {

        } finally {
            if (quotes.length == 0) {
                // no data
            } else {
                final LineSet dataSet = new LineSet();
                float min = quotes[0];
                float max = quotes[0];
                for (int i = 0; i < quotes.length; i++) {
                    float close = quotes[i];
                    // update the upper and lower bounds if needed
                    if (close < min) min = close;
                    if (close > max) max = close;
                    // points should be unlabeled
                    dataSet.addPoint("", quotes[i]);
                }
                // set the bounds to the rounded min and max values
                final int upperBound = (int) Math.ceil(max / 1);
                final int lowerBound = (int) Math.floor(min / 1);
                // get the interval between the extremes to calculate step value
                final int step = (int) Math.ceil((max - min) / 5);
                // must be final in order to be used in runnable
                getView().post(new Runnable() {
                    @Override
                    public void run() {
                        mChartProgressBar.setVisibility(View.GONE);
                        displayChartWithData(dataSet, lowerBound, upperBound, step);
                    }
                });
            }
        }
    }

    private void getCompanyNews() {
        final ArrayList<Article> articles = new ArrayList<Article>();
        try {
            articles.addAll(stockClient.getNewsForStock(mSymbol));
        } catch (IOException e) {

        } catch (JSONException e) {

        } finally {
            getView().post(new Runnable() {
                @Override
                public void run() {
                    ((NewsAdapter) mRecyclerView.getAdapter()).addAllArticles(articles);
                }
            });
        }
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
        mLineGraph.dismiss();
        mLineGraph.addData(dataSet);
        mLineGraph.setAxisBorderValues(lowerBound, upperBound, step);
        mLineGraph.show();
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
        mSymbol = symbol;
        String price = data.getString(data.getColumnIndex("bid_price"));
        String percentChange = data.getString(data.getColumnIndex("percent_change"));
        mSymbolTextView.setText(price);
        mPercentChangeTextView.setText("(" + percentChange + ")");
        if (percentChange.startsWith("-")) {
            // price is down, show a red arrow
            mChangeImageView.setImageResource(R.drawable.down_arrow);
        } else {
            // price is up or even, show a green arrow
            mChangeImageView.setImageResource(R.drawable.up_arrow);
        }

        mFirstButton.callOnClick();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                getCompanyNews();
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // do nothing
    }
}
