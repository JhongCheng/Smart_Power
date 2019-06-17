package com.example.lee0702.finalversion;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;


import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main2Activity extends AppCompatActivity implements View.OnClickListener,DatePicker.OnDateChangedListener,TimePicker.OnTimeChangedListener {
    private TextView textopen,textclose;
    private LinearLayout open;
    private LinearLayout close;
    private Context context;
    private int year,month,day,hour,minute;
    private  StringBuffer date,time;

    private Handler handler = new Handler(); //UI
    private Handler TH,THW,THT; //main
    RequestQueue requestQueue;
    String showUrl = "http://35.201.178.93/watt/wattjson.php";
    String TimeShowUrl = "http://35.201.178.93/watt/watthour.php";
    String ButtonShowUrl = "http://35.201.178.93/switch.php";
    String SetTimeShowUrl = "http://35.201.178.93/timeswitch.php";
    String[] setnumber;
    String endwatt;
    private HandlerThread thread,threadhw,threadbutton;
    public List<String> listtime;
    List<Float> listwatt;

    private Button ButtonOpen,ButtonOff;
    private ImageView imageView;
    String boolswitch;
    boolean Timeopcl;

    private LineChart mLineChart;
    private YAxis leftAxis;
    private YAxis rightAxis;
    private XAxis xAxis;
    private ArrayList<Entry> yVals1;
    /*******************************
        主程式
        ********************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        boolswitch ="";
        context = this;
        date = new StringBuffer();
        time = new StringBuffer();
        initView();
        initDateTime();
        /////------------------我是分隔線----------------//////

        super.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //取得intent中的bundle物件
        Bundle bundle =this.getIntent().getExtras();

        String name = bundle.getString("name");
        String number = bundle.getString("number");
        setnumber = number.split(":");

        TextView tename = (TextView)findViewById(R.id.name);
        TextView tenumber = (TextView)findViewById(R.id.number);


        tename.setText(name);
        tenumber.setText(number);

        ButtonOpen = findViewById(R.id.ButtonOpen);
        ButtonOff = findViewById(R.id.ButtonOff);
        imageView = findViewById(R.id.imageView2);
        /////------------------我是分隔線----------------//////
        mLineChart = (LineChart)findViewById(R.id.dynamic_chart1);

        /////------------------我是分隔線----------------//////
        thread = new HandlerThread(name);
        thread.start();
        threadhw = new HandlerThread(name);
        threadhw.start();
        threadbutton = new HandlerThread(name);
        threadbutton.start();

        TH = new Handler(thread.getLooper());
        THT = new Handler(threadbutton.getLooper());
        THW = new Handler(threadhw.getLooper());


        TH.post(watt);
        THT.post(togglebutton);
        THW.post(hourwatt);

    }
    private Runnable watt = new Runnable() {
        @Override
        public void run() {
            try {
                while (setnumber[1]!=null)
                {
                    WattReadAndSet();
                    thread.sleep(1000);
                    handler.post(setwatt);
                }
            } catch (InterruptedException e) {
                    e.printStackTrace();
            }
        }
    };
    private Runnable hourwatt = new Runnable() {
        @Override
        public void run() {
            try {
                while(true)
                {
                    getHourWatt();
                    thread.sleep(60*(60*1000));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    private Runnable togglebutton = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    getButtonChecked();
                    threadbutton.sleep(2*1000);
                    handler.post(settogglebutton);
                }
            }catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    };
    private Runnable settogglebutton = new Runnable() {

        @Override
        public void run() {
                switch (boolswitch)
                {
                    case "open":
                        imageView.setImageResource(R.drawable.flashon);
                        break;
                    case "close":
                        imageView.setImageResource(R.drawable.flashoff);
                        break;
                }
                ButtonOpen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setButtonChecked("open");
                    }
                });
                ButtonOff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setButtonChecked("close");
                    }
                });
        }
    };
    private Runnable setwatt = new Runnable() {
        @Override
        public void run() {
                TextView tewatt = (TextView) findViewById(R.id.ip);
                tewatt.setText(endwatt);
        }
    };
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case android.R.id.home:
                thread.interrupt();
                threadhw.interrupt();
                threadbutton.interrupt();
                finish();
        }
        return super.onOptionsItemSelected(item);
    }
    //抓取每秒瓦特
    public void WattReadAndSet()
    {
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, showUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.i("Response", response);
                String[] split = response.split("\"");
                try{
                    if(split[3]!=" ")
                    {
                        String number = split[3];
                        endwatt = "目前瓦特數 : "+number;
                    }
                }catch (ArrayIndexOutOfBoundsException e)
                {
                    endwatt = "目前無資料";
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error.Response",error.toString());
            }
        }){
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> params = new HashMap<String, String>();
                //Log.i("number:::::::::::::",setnumber[1]);
                params.put("number", String.valueOf(setnumber[1]));
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(5*1000,1,1.0f));
        requestQueue.add(stringRequest);
    }
    /*******************************
         時間區
        ********************************/
    //時間設定
    //初始畫控件
    private void initView()
    {
        textopen = (TextView)findViewById(R.id.textopentime);
        textclose = (TextView)findViewById(R.id.textclosetime);
        open = (LinearLayout)findViewById(R.id.open);
        open.setOnClickListener(this);
        close = (LinearLayout)findViewById(R.id.close);
        close.setOnClickListener(this);
    }

    //獲取當前時間和日期
    private void initDateTime(){
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.open:
                date(textopen);
                Timeopcl = true;
                break;
            case R.id.close:
                date(textclose);
                Timeopcl = false;
                break;
        }
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        this.year = year;
        this.month = monthOfYear;
        this.day = dayOfMonth;
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        this.hour = hourOfDay;
        this.minute = minute;
    }
    //改變時間的監聽事件
    public void date(final TextView datetext)
    {
        //跳出對話方塊
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false).setPositiveButton("設置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(date.length()>0)
                {
                    date.delete(0,date.length());
                }
                datetext.setText(date.append(String.valueOf(year)).append("-")
                        .append(String.valueOf(month+1)).append("-")
                        .append(String.valueOf(day)).append(" "));
                time(datetext);//呼叫時間對話方塊
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(context,R.layout.dialog_date,null);
        final DatePicker datePicker = (DatePicker)dialogView.findViewById(R.id.datePicker);

        dialog.setTitle("設置日期");
        dialog.setView(dialogView);
        dialog.show();
        //初始化日期
        datePicker.init(year,month,day,this);
    }
    public void time(final TextView timetext)
    {
        //跳出對話方塊
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setCancelable(false).setPositiveButton("設置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(time.length()>0){
                    time.delete(0,time.length());
                }
                timetext.setText(date.append(time.append(String.valueOf(hour)).append(":")
                        .append(String.valueOf(minute))));
                getTime(String.valueOf(timetext.getText()),Timeopcl);//POST時間跟開關
                dialog.dismiss();
            }
        });
        builder1.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog1 = builder1.create();
        View dialogView1 = View.inflate(context,R.layout.dialog_time,null);
        TimePicker timePicker = (TimePicker)dialogView1.findViewById(R.id.timePicker);
        timePicker.setHour(hour);
        timePicker.setMinute(minute);
        timePicker.setIs24HourView(true);
        timePicker.setOnTimeChangedListener(this);
        dialog1.setTitle("設置時間");
        dialog1.setView(dialogView1);
        dialog1.show();
    }
    private void getTime(final String setTime, final boolean setopcl) {
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.POST,SetTimeShowUrl , new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error.Response",error.toString());
            }
        }){
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> params = new HashMap<String, String>();
                params.put("number", String.valueOf(setnumber[1]));
                if (setopcl == true) {
                    params.put("timeopen", setTime);
                } else if (setopcl == false) {
                    params.put("timeclose", setTime);
                }
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(5*1000,1,1.0f));
        requestQueue.add(stringRequest);
    }

    /*******************************
         圖表區
        ********************************/
    //每小時WATT
    public void getHourWatt()
    {
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, TimeShowUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("Response", response);
                    String[] onesegmentation = response.split("\"");
              try{
                    Float onereadywatt = Float.parseFloat(onesegmentation[7]);
                    String[] onereadytime = onesegmentation[11].split(" ");
                    String setcut1 = onereadytime[1];


                    Float tworeadywatt = Float.parseFloat(onesegmentation[19]);
                    String[] tworeadytime = onesegmentation[23].split(" ");
                    String setcut2 = tworeadytime[1];

                    Float threereadywatt = Float.parseFloat(onesegmentation[31]);
                    String[] threereadytime = onesegmentation[35].split(" ");
                    String setcut3 = threereadytime[1];


                    Float fourreadywatt = Float.parseFloat(onesegmentation[43]);
                    String[] fourreadytime = onesegmentation[47].split(" ");
                    String setcut4 = fourreadytime[1];


                    Float fivesreadywatt = Float.parseFloat(onesegmentation[55]);
                    String[] fivesreadytime = onesegmentation[59].split(" ");
                    String setcut5 = fivesreadytime[1];

                    listwatt = new ArrayList<Float>();
                    listtime = new ArrayList<String>();
                    listwatt.add(onereadywatt);
                    listtime.add(setcut1);
                    listwatt.add(tworeadywatt);
                    listtime.add(setcut2);
                    listwatt.add(threereadywatt);
                    listtime.add(setcut3);
                    listwatt.add(fourreadywatt);
                    listtime.add(setcut4);
                    listwatt.add(fivesreadywatt);
                    listtime.add(setcut5);
                    LineData data;
                    LineDataSet dataSet;
                    ArrayList<String> xVals = new ArrayList<>();
                    yVals1 = new ArrayList<>();
                    //可觸碰
                    mLineChart.setTouchEnabled(true);
                    //可拖曳
                    mLineChart.setDragEnabled(true);
                    //可縮放
                    mLineChart.setScaleEnabled(true);
                    //XY同時縮放
                    mLineChart.setPinchZoom(true);

                    Legend l = mLineChart.getLegend();
                    l.setForm(Legend.LegendForm.LINE);
                    l.setTextSize(11f);
                    l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
                    l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);

                    leftAxis = mLineChart.getAxisLeft();
                    leftAxis.setAxisMaxValue(Collections.max(listwatt));
                    leftAxis.setDrawTopYLabelEntry(true);
                    leftAxis.setLabelCount(6, false);
                    leftAxis.setAxisMinValue(0.0f);
                    rightAxis = mLineChart.getAxisRight();
                    rightAxis.setEnabled(false);

                    xAxis = mLineChart.getXAxis();
                    xAxis.setSpaceBetweenLabels(0);
                    xAxis.resetLabelsToSkip();
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    mLineChart.animateX(2500);
                    mLineChart.setDescription("每小時度數");
                    mLineChart.setNoDataText("暫無資料");
                    mLineChart.setDrawBorders(true);

                    for (int i = 0; i < listwatt.size(); i++) {
                        yVals1.add(new Entry(listwatt.get(i), i));
                        xVals.add(listtime.get(i));
                    }
                    dataSet = new LineDataSet(yVals1, "度數");
                    dataSet.setColor(Color.BLACK);
                    dataSet.setLineWidth(1.75f);
                    dataSet.setCircleColor(Color.RED);
                    dataSet.setDrawFilled(true);
                    dataSet.setCubicIntensity(2.0f);
                    dataSet.setFillColor(Color.BLUE);
                    data = new LineData(xVals, dataSet);
                    data.setValueTextSize(9f);
                    mLineChart.setData(data);
                    mLineChart.notifyDataSetChanged();
                    mLineChart.invalidate();
                }catch (ArrayIndexOutOfBoundsException e){
                    mLineChart.setNoDataText("暫無資料");
                    mLineChart.notifyDataSetChanged();
                    mLineChart.invalidate();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error.Response",error.toString());
            }
        }){
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> params = new HashMap<String, String>();
                Log.i("number:::::::::::::",setnumber[1]);
                params.put("number", String.valueOf(setnumber[1]));
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,1,1.0f));
        requestQueue.add(stringRequest);
    }


    /*******************************
        按鈕區
        ********************************/
    private void getButtonChecked()
    {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
            StringRequest stringRequest = new StringRequest(Request.Method.POST, ButtonShowUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("Response", response);
                    String [] switchbb = response.split("\"");
                    try{
                        boolswitch = switchbb[3];
                        //Log.i("switch::::", switchbb[3]);
                    }catch (ArrayIndexOutOfBoundsException e)
                    {

                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("Error.Response",error.toString());
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    //Log.i("number:::::::::::::", setnumber[1]);
                    params.put("number", String.valueOf(setnumber[1]));

                    return params;
                }
            };
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(5*1000,1,1.0f));
            requestQueue.add(stringRequest);
    }
    private void setButtonChecked(final String status)
    {
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ButtonShowUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("Response", response);
                String [] switchbb = response.split("\"");
                try {
                    boolswitch = switchbb[3];
                    Log.i("switch::::", switchbb[3]);
                }catch (ArrayIndexOutOfBoundsException e)
                {
                    Toast.makeText(Main2Activity.this,"目前無連線",Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error.Response",error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                //Log.i("number:::::::::::::", setnumber[1]);
                params.put("number", String.valueOf(setnumber[1]));
                if(status != "")
                {
                    params.put("switch", status);
                }
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(5*1000,1,1.0f));
        requestQueue.add(stringRequest);
    }

    ///------按下返回鍵------///
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            thread.interrupt();
            threadhw.interrupt();
            threadbutton.interrupt();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
