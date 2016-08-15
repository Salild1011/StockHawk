package com.sam_chordas.android.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Salil on 18-07-2016.
 * Credits to @dfbernardino for the WilliamChart Graph Library
 * https://github.com/diogobernardino/WilliamChart
 */
public class LineGraphActivity extends AppCompatActivity {

    private Intent mServiceIntent;
    private LineChartView mChartView;
    private TextView mDateTextView;

    private float[] mPriceHigh;
    private String[] mDates;
    private int mMin, mMax;
    private String mName;
    private static String mDateRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        mDateTextView = (TextView) findViewById(R.id.chart_date_text_view);
        mChartView = (LineChartView) findViewById(R.id.linechart);

        if (savedInstanceState != null) {
            mMin = savedInstanceState.getInt("min");
            mMax = savedInstanceState.getInt("max");
            mDates = savedInstanceState.getStringArray("dates");
            mPriceHigh = savedInstanceState.getFloatArray("price");

            displayGraph();
        }
        else {
            if (getIntent().hasExtra("name")) {
                mName = getIntent().getStringExtra("name");

                mServiceIntent = new Intent(this, StockIntentService.class);
                mServiceIntent.putExtra("tag", "list");
                mServiceIntent.putExtra("symbol", mName);

                getDateString();

                startService(mServiceIntent);
            }
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("data");
            quoteJsonToHistoricalData(message);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter iff= new IntentFilter("Historical_Data");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, iff);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("min", mMin);
        outState.putInt("max", mMax);
        outState.putStringArray("dates", mDates);
        outState.putFloatArray("price", mPriceHigh);
    }

    private void quoteJsonToHistoricalData(String JSON) {
        JSONObject jsonObject;
        JSONArray resultsArray;

        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                resultsArray = jsonObject.getJSONObject("query").getJSONObject("results").getJSONArray("quote");

                mPriceHigh = new float[resultsArray.length()];
                mDates = new String[resultsArray.length()];

                for (int i = 0; i < resultsArray.length(); i++) {
                    mDates[i] = resultsArray.getJSONObject(i).getString("Date");
                    mPriceHigh[i] = (float) resultsArray.getJSONObject(i).getDouble("High");

                    if (i == 0) {
                        mMin = mMax = (int) mPriceHigh[i];
                    }
                    else {
                        if (mMax < (int) mPriceHigh[i]) {
                            mMax = (int) mPriceHigh[i];
                        }
                        if (mMin > (int) mPriceHigh[i]) {
                            mMin = (int) mPriceHigh[i];
                        }
                    }
                }

                adjustMinMax();
                displayGraph();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void adjustMinMax() {
        int mod;

        mod = mMin % 10;
        mMin -= mod;

        mod = mMax % 10;
        mMax += (10 - mod);
    }

    private void displayGraph() {
        mDateTextView.setText(mDateRange);

        LineSet dataset = new LineSet(mDates, mPriceHigh);

        dataset.setColor(Color.parseColor("#3399FF"))
                .setFill(Color.parseColor("#5580BFFF"))
                .setDotsColor(Color.parseColor("#0073E6"))
                .setThickness(4)
                .beginAt(0);
        mChartView.addData(dataset);

        // Chart
        mChartView.setBorderSpacing(Tools.fromDpToPx(15))
                .setAxisBorderValues(mMin, mMax, 2)
                .setAxisColor(Color.parseColor("#FF01579B"))
                .setXLabels(AxisController.LabelPosition.NONE)
                .setLabelsColor(Color.parseColor("#FFFFFF"));

        mChartView.show();
    }

    public void getDateString() {
        Date date = new Date();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        Date date_mnth_back = cal.getTime();

        SimpleDateFormat format = new SimpleDateFormat("dd-MM", Locale.getDefault());

        mDateRange = mName + ": "
                + format.format(date_mnth_back)
                + " TO "
                + format.format(date);

        mDateTextView.setText(mDateRange);
    }
}
