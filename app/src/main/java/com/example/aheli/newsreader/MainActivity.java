package com.example.aheli.newsreader;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase articleDB;
    String apiKey = BuildConfig.ApiKey;

    ListView listview;
    final ArrayList<String> titles = new ArrayList<String>();
    final ArrayList<String> content = new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articleDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles(published_at CHAR PRIMARY KEY, title VARCHAR, content VARCHAR)");
        updateListView();

        try {
            DownloadTask task = new DownloadTask();
            task.execute("https://newsapi.org/v2/top-headlines?country=in&apiKey=" + apiKey).get();

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),"Couldn't find news",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        ListView listview = findViewById(R.id.listview);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titles);//simple list item1: simple string with label.
        //set array adapter to list view
        listview.setAdapter(arrayAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long id) {
                Intent intent  = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", content.get(i));
                startActivity(intent);
            }
        });
        updateListView();
    }

    public void updateListView() {
        Cursor c = articleDB.rawQuery("SELECT * FROM articles", null);
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst()) {
            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());
            arrayAdapter.notifyDataSetChanged(); //update all info
        }
    }

    public class DownloadTask extends AsyncTask<String,Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            //cannot interact with UI
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);
                int data = reader.read();
                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }
                Log.i("result: ",result);
//                JSONArray jsonArray = new JSONArray(result);
//                int numberOfItems = 20;
//                if(jsonArray.length() < 20) {
//                    numberOfItems = jsonArray.length();
//                }
//                Log.i("length", (Integer.toString(numberOfItems)));
                articleDB.execSQL("DELETE FROM articles ");

                JSONObject jsonObject = new JSONObject(result);
                String newsInfo = jsonObject.getString("articles");
                Log.i("news info ", newsInfo);
                JSONArray arr = new JSONArray(newsInfo);
                Log.i("length of arr:", Integer.toString(arr.length()));

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject jsonPart = arr.getJSONObject(i);
                    String title = "";
                    String description = "";
                    String published_at = jsonPart.getString("publishedAt");

                    title = jsonPart.getString("title");
                    description = jsonPart.getString("description");

                    String stringUrl = jsonPart.getString("url");
                    url = new URL(stringUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);
                    data = reader.read();
                    String ArticleContent = "";
                    while (data != -1) {
                        char current = (char) data;
                        ArticleContent += current;
                        data = reader.read();
                    }
                    Log.i("HTML:", ArticleContent);

                    String sql = "INSERT INTO articles(published_at, title, content) VALUES(?, ?, ?)";
                    SQLiteStatement statement = articleDB.compileStatement(sql);
                    statement.bindString(1, published_at);
                    statement.bindString(2, title);
                    statement.bindString(3, description);
                }

                //Log.i("result:", result);
                return result;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {  //called when do in background completed. passes whatever we return from doInBackground.
            super.onPostExecute(s);
        }
    }
}
