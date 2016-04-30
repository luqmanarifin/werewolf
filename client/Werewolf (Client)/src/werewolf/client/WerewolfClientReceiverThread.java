/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package werewolf.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adinb
 */
public class WerewolfClientReceiverThread implements Runnable{
    
    
     // Socket etc
    private Socket clientSocket = null;
    private PrintStream os = null;
    private BufferedReader is = null;
    private BufferedReader inputLine = null;
    private boolean closed = false;
    
    private String responseLine;
    private String cmd;

    public WerewolfClientReceiverThread(Socket socket) {
        clientSocket = socket;
        inputLine = new BufferedReader(new InputStreamReader(System.in));
        try {
            os = new PrintStream(clientSocket.getOutputStream());
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(WerewolfClientReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
        }
       
    }
    
    public void setCommand(String cmd){
        this.cmd = cmd;
    }
    
    public String getResponseLine(){
        return responseLine;
    }
    
    @Override
    public void run() {
        try {
            synchronized("response"){
                while ((responseLine = is.readLine()) != null) {
                    System.out.println(responseLine);
                }
            }
        } catch (IOException e) {
          System.err.println("IOException:  " + e);
        }
    }
    
}
