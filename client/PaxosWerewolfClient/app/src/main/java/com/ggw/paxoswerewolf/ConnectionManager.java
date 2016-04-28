package com.ggw.paxoswerewolf;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by adinb on 26/04/2016.
 */
public final class ConnectionManager {

    String ip;
    int port;
    Socket socket;

    private ConnectionManager(){}
    private static ConnectionManager instance = null;

    public static ConnectionManager getInstance(){
        if (instance == null){
            instance = new ConnectionManager();
        }

        return instance;
    }

    public String connect(){
        String response = null;
        try {
            socket = new Socket(ip, port);
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            response = input.readLine();
        } catch (IOException e) {
            Log.d("Socket","IO ERROR!");
            e.printStackTrace();
        }
        Log.d("RESPONSE", response);
        return response;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
