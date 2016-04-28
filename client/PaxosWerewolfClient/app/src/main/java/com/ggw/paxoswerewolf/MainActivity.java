package com.ggw.paxoswerewolf;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private class sendRequest extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {

            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView usernameLabel = (TextView) findViewById(R.id.player_username);
        TextView id = (TextView) findViewById(R.id.player_id);
        String username = getIntent().getStringExtra("username");
        JSONObject request = new JSONObject();

        usernameLabel.setText(username);

        try {
            request.put("method", "join");
            request.put("username", username);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
