package com.ggw.paxoswerewolf;

import android.Manifest;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class LoginActivity extends AppCompatActivity {

    String ip;
    String port;

    ConnectionManager cm;
    private class TestConnectionTask extends AsyncTask<Void, String, String> {


        @Override
        protected String doInBackground(Void... params) {
            String response = cm.connect();
            return response;
        }

        @Override
        protected void onPostExecute(String response){
            Toast toast = Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG);
            toast.show();

            if (response != null){
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("username", ((EditText) findViewById(R.id.username_input)).getText().toString());
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET},1);

        TextView ip = (TextView) findViewById(R.id.ipaddress);
        ip.setText("IP:" + getWifiIpAddress());

        cm = ConnectionManager.getInstance();

    }

    public String getWifiIpAddress(){
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);

        int ipaddr = wm.getConnectionInfo().getIpAddress();

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipaddr = Integer.reverseBytes(ipaddr);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipaddr).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public void testConnection(View view){
        ip = ((TextView) findViewById(R.id.ip)).getText().toString();
        port = ((TextView) findViewById(R.id.port)).getText().toString();
        cm.setIp(ip);
        cm.setPort(Integer.parseInt(port));
        Log.d("ip", ip);
        Log.d("port", port);
        new TestConnectionTask().execute();

    }
}
