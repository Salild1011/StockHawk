package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.util.ArrayList;

public class StockListWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class StockRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private int mAppWidgetId;
    private ArrayList<String> mQuoteList, mPriceList, mChangeList;
    private Cursor mCursor;
    private boolean mShowPercent;

    StockRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        mQuoteList = new ArrayList<>();
        mPriceList = new ArrayList<>();
        mChangeList = new ArrayList<>();
        mShowPercent = Utils.showPercent;

        fetchData();
    }

    @Override
    public void onDataSetChanged() {
        final long identityToken = Binder.clearCallingIdentity();

        mQuoteList = new ArrayList<>();
        mPriceList = new ArrayList<>();
        mChangeList = new ArrayList<>();
        mShowPercent = Utils.showPercent;
        fetchData();

        Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public void onDestroy() {
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
    }

    @Override
    public int getCount() {
        return mQuoteList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.list_layout);
        rv.setTextViewText(R.id.stock_text_view, mQuoteList.get(position));
        rv.setTextViewText(R.id.price_text_view, mPriceList.get(position));

        //Set the colour of text according to change in price
        if (mChangeList.get(position).contains("+")) {
            rv.setInt(R.id.change_text_view,"setBackgroundResource",
                    R.drawable.percent_change_pill_green);
        }
        else if (mChangeList.get(position).contains("-")) {
            rv.setInt(R.id.change_text_view,"setBackgroundResource",
                    R.drawable.percent_change_pill_red);
        }

        rv.setTextViewText(R.id.change_text_view, mChangeList.get(position));

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    private void fetchData() {
        mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                null, null, null, null);

        if (mCursor != null && mCursor.getColumnCount() > 0) {
            mCursor.moveToFirst();
            do {
                String name = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
                String price = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE));
                String change = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE));
                String per_change = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));

                if (!mQuoteList.contains(name)) {
                    mQuoteList.add(name);
                    mPriceList.add(price);
                    if (mShowPercent) {
                        mChangeList.add(per_change);
                    } else {
                        mChangeList.add(change);
                    }
                }

                mCursor.moveToNext();
            } while (!mCursor.isAfterLast());

            mCursor.close();
        }
    }
}