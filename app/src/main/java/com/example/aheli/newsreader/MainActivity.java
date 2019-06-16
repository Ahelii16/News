package com.example.aheli.newsreader;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> url = new ArrayList<>();
    ArrayList<String> publish = new ArrayList<>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase articleDB;

    String apiKey = BuildConfig.ApiKey;

    ListView listview;
    Cursor c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articleDB = this.openOrCreateDatabase("articleDB", MODE_PRIVATE, null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles(publishedAt VARCHAR PRIMARY KEY,  title VARCHAR, url VARCHAR)");

        try {
            DownloadTask task = new DownloadTask();
            task.execute("https://newsapi.org/v2/top-headlines?country=in&apiKey=" + apiKey).get();

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),"Couldn't find news",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        ListView listview = findViewById(R.id.listview);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);//simple list item1: simple string with label.
        listview.setAdapter(arrayAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long id) {
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
//                startActivity(browserIntent);

                Intent intent  = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", url.get(i));
                startActivity(intent);
            }
        });
        updateListView();
    }

    public void updateListView() {
        c = articleDB.rawQuery("SELECT * FROM articles", null);

        int urlIndex = c.getColumnIndex("url");
        int titleIndex = c.getColumnIndex("title");
        int publishIndex = c.getColumnIndex("publishedAt");

        if (c.moveToFirst()) {
            titles.clear();
            url.clear();
            publish.clear();

            do {

                titles.add(c.getString(titleIndex));
                url.add(c.getString(urlIndex));
                publish.add(c.getString(publishIndex));

            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
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
                articleDB.execSQL("DELETE FROM articles ");

                JSONObject jsonObject = new JSONObject(result);
                String newsInfo = jsonObject.getString("articles");
                Log.i("news info ", newsInfo);
                JSONArray arr = new JSONArray(newsInfo);
                Log.i("length of arr:", Integer.toString(arr.length()));

                for (int i = 0; i < arr.length(); i++) {

                    JSONObject jsonPart = arr.getJSONObject(i);
                    String title = jsonPart.getString("title");
                    String publishedAt = jsonPart.getString("publishedAt");
                    String stringUrl = jsonPart.getString("url");

//                    url = new URL(stringUrl);
//                    urlConnection = (HttpURLConnection) url.openConnection();
//                    inputStream = urlConnection.getInputStream();
//                    reader = new InputStreamReader(inputStream);
//                    data = reader.read();
//                    String ArticleContent = "";
//                    while (data != -1) {
//                        char current = (char) data;
//                        ArticleContent += current;
//                        data = reader.read();
//                    }
//                    Log.i("HTML:", ArticleContent);

                    String sql = "INSERT INTO articles(publishedAt, title, url) VALUES(?, ?, ?)";
                    SQLiteStatement statement = articleDB.compileStatement(sql);
                    statement.bindString(1, publishedAt);
                    statement.bindString(2, title);
                    statement.bindString(3, stringUrl);

                    statement.execute();
                }
                Log.i("URL Content", result);
                return result;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {  //called when do in background completed. passes whatever we return from doInBackground.

            super.onPostExecute(s);
            updateListView();
        }
    }
}
