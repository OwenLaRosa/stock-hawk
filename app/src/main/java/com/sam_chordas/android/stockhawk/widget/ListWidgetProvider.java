package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.ui.StockDetailActivity;

/**
 * Created by Owen LaRosa on 9/16/16.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ListWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int id: appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_list);

            // launch the main activity when user taps icon
            Intent launchAppIntent = new Intent(context, MyStocksActivity.class);
            PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, launchAppIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget, appPendingIntent);

            // launch detail activity when user taps specifc stock
            Intent launchDetailIntent = new Intent(context, StockDetailActivity.class);
            PendingIntent detailPendingIntent = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(launchDetailIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setPendingIntentTemplate(R.id.widget_list, detailPendingIntent);
            remoteViews.setEmptyView(R.id.widget_list, R.id.widget_empty);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                setRemoteAdapter(context, remoteViews);
            } else {
                setRemoteAdapterV11(context, remoteViews);
            }
            appWidgetManager.updateAppWidget(id, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Utils.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            // refresh the list widget when data updates
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(R.id.widget_list,
                new Intent(context, ListWidgetRemoteViewsService.class));
    }

    @SuppressWarnings("deprecation")
    private void setRemoteAdapterV11(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(0, R.id.widget_list,
                new Intent(context, ListWidgetRemoteViewsService.class));
    }

}
