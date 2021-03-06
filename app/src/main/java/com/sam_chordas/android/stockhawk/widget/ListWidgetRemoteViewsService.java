package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.StockDetailActivity;

/**
 * Created by Owen LaRosa on 9/16/16.
 */

public class ListWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                // clear and restore data when using non-exported content provider
                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        null,
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                int count = data == null ? 0 : data.getCount();
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION || data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_list_item);
                String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
                int isUp = (int) data.getInt(data.getColumnIndex(QuoteColumns.ISUP));
                String price = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
                remoteViews.setTextViewText(R.id.widget_symbol_text_view, symbol);
                remoteViews.setTextViewText(R.id.widget_price_text_view, getString(R.string.currency_symbol, price));
                remoteViews.setTextViewText(R.id.widget_updown_text_view, isUp == 1 ? "▲" : "▼");

                final Intent fillIntent = new Intent();
                Uri uri = QuoteProvider.Quotes.withSymbol(symbol);
                fillIntent.setData(uri);
                fillIntent.putExtra(StockDetailActivity.DETAIL_URI, uri);
                String name = data.getString(data.getColumnIndex(QuoteColumns.NAME));
                String title = getResources().getString(R.string.stock_name_formatter, name, symbol);
                fillIntent.putExtra(StockDetailActivity.DETAIL_TITLE, title);
                remoteViews.setOnClickFillInIntent(R.id.widget_list_item, fillIntent);

                return remoteViews;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position)) {
                    return data.getLong(data.getColumnIndex("_id"));
                }
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }

}
