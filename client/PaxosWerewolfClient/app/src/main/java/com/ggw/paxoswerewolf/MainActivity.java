package com.ggw.paxoswerewolf;

import android.Manifest;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
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

public class MainActivity extends AppCompatActivity {

    Socket socket;
    String ip;
    String port;
    String response;
    private class TestConnectionTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {

            try {
                socket = new Socket(ip, Integer.parseInt(port));
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                response = input.readLine();
            } catch (IOException e) {
                Log.d("Socket","IO ERROR!");
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response){
            Log.d("response", response);
            Toast toast = Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET},1);

        TextView ip = (TextView) findViewById(R.id.ipaddress);
        ip.setText("IP:" + getWifiIpAddress());

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
        Log.d("ip", ip);
        Log.d("port", port);
        new TestConnectionTask().execute(ip,port);

    }
}
