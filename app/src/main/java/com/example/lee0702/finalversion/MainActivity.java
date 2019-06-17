package com.example.lee0702.finalversion;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {
    TextView textView;
    //////----------------網路功能-------------------///////
    String showUrl = "http://35.201.178.93/app.php";
    RequestQueue requestQueue;
    /////------------------資料顯示----------------//////
    ArrayList<HashMap<String,String>> contactList;
    ArrayAdapter<String> adapter;
    List<String> allCountries;
    Spinner spnumber;
    /////------------------我是分隔線----------------//////
    private ListView listView;
    public static List<Person> personList;
    MyAdapter myAdapter;

    /*******************************
        主程式
        ********************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);
        //////----------------網路功能-------------------///////
        funJsonArrayRequest();
        /////------------------資料顯示----------------//////
        contactList = new ArrayList<>();

        listView = findViewById(R.id.listview);

        personList = new ArrayList<Person>();
        // 新增MyAdapter
        myAdapter = new MyAdapter(this);
        // 向listview中添加Adapter
        listView.setAdapter(myAdapter);

        allCountries = new ArrayList<String>();

        /////------------------我是分隔線----------------//////
        adapter = new ArrayAdapter<String>(MainActivity.this,R.layout.support_simple_spinner_dropdown_item,allCountries);

        /////------------------我是分隔線----------------//////
        registerForContextMenu(listView);
        //短按
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String Copyname = adapter.getItem(position);
                String [] temp = null;
                temp = Copyname.split(" ");
                Log.i("temp::::::::::::::", adapter.getItem(position));
                String name = temp[0];
                String number = temp[1];
                //new一個intent物件，並指定Activity切換的class
                Intent intent = new Intent(MainActivity.this,Main2Activity.class);
                //new一個Bundle物件，並將要傳遞的資料傳入
                Bundle bundle = new Bundle();
                bundle.putString("name",name);
                bundle.putString("number", number);
                //將Bundle物件assign給intent
                intent.putExtras(bundle);
                //切換Activity
                startActivity(intent);
            }
        });
    }

    /*******************************
        MENU區
        ********************************/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.added:
                AddNew();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    /*******************************
        資料新增區
        ********************************/
    private void AddNew() {
        final String[] setnumber = new String[1];
        final View item = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_add,null);

        //初始化
        spnumber = (Spinner) item.findViewById(R.id.spinner);
        //建立數據
        spnumber.setAdapter(adapter);
        spnumber.setOnItemSelectedListener( new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String[]splittedStr = allCountries.get(position).split(":");
                final String[] splittedStr2 = splittedStr[2].split(" ");
                setnumber[0] = splittedStr2[0];
                //Toast.makeText(MainActivity.this,"Click"+ setnumber[0],Toast.LENGTH_LONG).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("修改").setView(item).setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {


            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText editName = (EditText)item.findViewById(R.id.name);
                final String name = editName.getText().toString();
                Pattern pattern = Pattern.compile("^[\u4E00-\u9FA5AA-Za-z0-9]+$");
                Matcher matcher = pattern.matcher(name);
                if(matcher.matches())
                {
                    adapter.notifyDataSetChanged();
                    ////////-------------POST---------------///////
                    requestQueue = Volley.newRequestQueue(getApplicationContext());
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, showUrl, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            funJsonArrayRequest();
                            Log.i("Response", response);
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
                            params.put("name",name);
                            params.put("number", String.valueOf(setnumber[0]));
                            return params;
                        }
                    };
                    contactList.clear();
                    requestQueue.add(stringRequest);
                }
                else
                    {
                        Toast.makeText(MainActivity.this,"請勿輸入特殊符號!",Toast.LENGTH_LONG).show();
                    }
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                personList.clear();
                all();
                myAdapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        }).create().show();

    }

    //////----------------網路功能-------------------///////
    private void funJsonArrayRequest(){
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        final JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(showUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    textView.setText("目前所有家電");
                    //////----------------JSON讀取-------------------///////
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject person = (JSONObject) response.get(i);
                        String name = person.getString("name");
                        String number = person.getString("number");
                        String ip = person.getString("ip");
                        String mac = person.getString("mac");
                        /////------------------資料顯示----------------//////
                        HashMap<String,String> contact = new HashMap();
                        contact.put("name",name);
                        contact.put("number",number);
                        contact.put("ip",ip);
                        contact.put("mac",mac);
                        contactList.add(contact);
                        /////------------------我是分隔線----------------//////
                    }
                    Log.i("成功:::::::::::::::::::::", String.valueOf(contactList));
                    myAdapter.notifyDataSetChanged();
                    all();
                } catch (JSONException e) {
                    textView.setText("目前無家電");
                    Log.e("onERROR:", e.getLocalizedMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("目前無連線");
                Log.e("onERROR:",error.getLocalizedMessage());
            }
        });
        //對一個Request重新請求(超時時間,最大重試次數,曲線增加下次請求秒數)
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(500000,0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonArrayRequest);
    }
    /////------------------資料顯示----------------//////
    void all()
    {
        personList.clear();
        allCountries.clear();
        int len = contactList.size();
        for(int i =0;i<len;i++)
        {
            String name = contactList.get(i).get("name");
            String number = contactList.get(i).get("number");
            String ip = contactList.get(i).get("ip");
            String mac = contactList.get(i).get("mac");

            Log.i("number:::::",number);
            allCountries.add(String.format("家電:%s 序號:%s",name,number));
            Person person = new Person(name,number);
            personList.add(person);
        }
        adapter.notifyDataSetChanged();
        Log.i("all:::::::::::::::::::", String.valueOf(allCountries));
        myAdapter.notifyDataSetChanged();
    }
    /*******************************
        資料顯示區
        ********************************/
    /////------------------資料顯示----------------//////
    class ViewHolder {
        public TextView txt_name;
        public TextView txt_number;
    }
    public class MyAdapter extends BaseAdapter {
        private Context context;
        private LayoutInflater inflater;


        public MyAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
        }


        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return MainActivity.personList.size();
        }

        @Override
        public Object getItem(int position) {
            return MainActivity.personList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 從personList取出Person
            Person p = MainActivity.personList.get(position);
            ViewHolder viewHolder = null;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = inflater.inflate(R.layout.listitem_layout, null);
                viewHolder.txt_name = (TextView) convertView
                        .findViewById(R.id.LA_name);
                viewHolder.txt_number = (TextView) convertView
                        .findViewById(R.id.LA_number);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            //在TextView中插入資料
            viewHolder.txt_name.setText(" 家電:"+p.getName());
            viewHolder.txt_number.setText(" 序號:"+p.getNumber());

            return convertView;
        }
    }

}