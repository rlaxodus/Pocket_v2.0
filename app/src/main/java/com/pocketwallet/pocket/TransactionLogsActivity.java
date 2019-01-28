package com.pocketwallet.pocket;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TransactionLogsActivity extends AppCompatActivity {

    private RecyclerView transactionListView;
    private RecyclerView.Adapter adapter;

    private List<Transaction> listTransactions;
    private ArrayList transactionsArrayList;

    private String userId;
    private String urlRetrieveTransactionHistory = "http://pocket.ap-southeast-1.elasticbeanstalk.com/transactional/transactionhistory";

    Bundle extras;

    private final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_logs);

        getSupportActionBar().setTitle("Transaction History");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        //Transaction List
        transactionListView = findViewById(R.id.transactionsListView);
        transactionListView.setHasFixedSize(true);
        transactionListView.setLayoutManager(new LinearLayoutManager(this));

        listTransactions = new ArrayList<>();
        transactionsArrayList = new ArrayList<>();

        for(int i=0; i<1; i++){
            Transaction listTransaction = new Transaction(
                    "TestID",
                    "type",
                    "senderID",
                    "receiverID",
                    "amount",
                    "date");
            listTransactions.add(listTransaction);
        }
        extras = getIntent().getExtras();
        if (extras != null) {
            userId = extras.getString("userId");
        }
        getTransactions();
        processGraph();
        CreateAdapterView();
    }

    public void getTransactions(){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("user_id", userId);
            System.out.println("User ID: " +jsonBody);
            urlRetrieveTransactionHistory += "/" + userId;
            System.out.println("urlRetrieveTransactionHistory: " + urlRetrieveTransactionHistory);
            final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlRetrieveTransactionHistory, jsonBody,
                    new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String result = response.getString("result");
                        System.out.println("Results: " + result);
                        if(result.equalsIgnoreCase("Success")){
                            JSONArray transactionArray = response.getJSONArray("transactions");
                            for(int i = 0; i < transactionArray.length(); i++){
                                JSONObject tempTransaction = transactionArray.getJSONObject(i);
                                if(tempTransaction.getString("from").equals("-")){
                                   Transaction transaction = new Transaction(tempTransaction.getString("transactionID"), tempTransaction.getString("type"),
                                                                             tempTransaction.getString("from"),tempTransaction.getString("to"),"-" + tempTransaction.getString("amount"),
                                                                                tempTransaction.getString("timestamp"));
                                    listTransactions.add(transaction);
                                }else{
                                    Transaction transaction = new Transaction(tempTransaction.getString("transactionID"), tempTransaction.getString("type"),
                                            tempTransaction.getString("from"),tempTransaction.getString("to"), tempTransaction.getString("amount"),
                                            tempTransaction.getString("timestamp"));
                                    listTransactions.add(transaction);
                                }
                            }
                        }
                    }catch(JSONException e){
                        System.out.println("Error: " + e);
                    }
                    CreateAdapterView();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    System.out.println("Error Message: " + error.getMessage());
                    System.out.println("Error Network Response Data: " + new String(error.networkResponse.data));
                    System.out.println("Error Network Response Status Code" + error.networkResponse.statusCode);
                    //onBackPressed();
                }
            });
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void CreateAdapterView(){
        adapter = new TransactAdapter(listTransactions,this);
        transactionListView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        transactionListView.setAdapter(adapter);
    }

    public void processGraph() {

        //Graphs
        LineChart chart = (LineChart) findViewById(R.id.chart);
        List<Entry> entries = new ArrayList<Entry>();

        //Add data x and y data here
        entries.add(new Entry(0, 4));
        entries.add(new Entry(1, 1));
        entries.add(new Entry(2, 2));
        entries.add(new Entry(3, 4));
        entries.add(new Entry(4, 4));
        entries.add(new Entry(5, 1));
        entries.add(new Entry(6, 2));
        entries.add(new Entry(7, 4));
        entries.add(new Entry(8, 1));
        entries.add(new Entry(9, 2));
        entries.add(new Entry(10, 4));
        entries.add(new Entry(11, 3));

        LineDataSet dataSet = new LineDataSet(entries, "Transactions");

        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return months[(int) value];
            }
        };

        //Line style
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        dataSet.setColor(getColor(R.color.colorPrimary));
        dataSet.setDrawHighlightIndicators(false);
        dataSet.setCircleColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(10f);

        //Chart Style
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getXAxis().setValueFormatter(formatter);
        chart.getXAxis().setGranularity(1f);
        //chart.getXAxis().setTextColor(getResources().getColor(R.color.colorPrimary));
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setDrawAxisLine(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setTextSize(14f);
        chart.setExtraOffsets(10, 10, 10, 10);
        chart.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        chart.getRenderer().getPaintRender().setShadowLayer(1, 0, 2, Color.GRAY);

        //Set Data
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.invalidate();
        chart.setVisibleXRange(1,3);
        chart.animateY(2000, Easing.Linear);
        chart.centerViewToAnimated(Calendar.getInstance().get(Calendar.MONDAY),0, YAxis.AxisDependency.LEFT,2000);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
