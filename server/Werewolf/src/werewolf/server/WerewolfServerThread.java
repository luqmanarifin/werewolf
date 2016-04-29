package werewolf.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

class WerewolfServerThread extends Thread {

  private BufferedReader is = null;
  private PrintStream os = null;
  private Socket clientSocket = null;
  private final WerewolfServerThread[] threads;
  private int maxClient;

  public WerewolfServerThread(Socket clientSocket, WerewolfServerThread[] threads) {
    this.clientSocket = clientSocket;
    this.threads = threads;
    maxClient = threads.length;
  }

  
  /********** THREAD TO RECEIVE MESSAGE FROM CLIENT ***********/
  public void run() {
    int maxClient = this.maxClient;
    WerewolfServerThread[] threads = this.threads;

    try {
      
      is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      os = new PrintStream(clientSocket.getOutputStream());
      
      while (true) {
        String line = is.readLine();
        
        JSONObject message = new JSONObject();
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(line);
            message = (JSONObject) obj;
        } catch (ParseException e) {
            System.err.println(e);
            break;
        }
        
        String method = (String) message.get("method");
        
        if (method.equals("join")) {
            // Flow: client -> server
        } else if (method.equals("leave")) {
            // Flow: client -> server
        } else if (method.equals("client_address")) {
            // Flow: client -> server
        } else if (method.equals("prepare_proposal")) {
            // Flow: client(acceptor) -> server (learner)
        } else if (method.equals("vote_result_werewolf")) {
            // Flow: KPU -> server
        } else if (method.equals("vote_result_civilian")) {
            // Flow: KPU -> server
        }
        
        
        // Sample code: broadcasting message to connected client
        for (int i = 0; i < maxClient; i++) {
          if (threads[i] != null) {
            threads[i].os.println("Requesting method: " + method);
          }
        }
        
        
        // TODO: Add condition for leaving the connection
        if (false) {
            break;
        }
      }

      /*
       * Clean up. Set the current thread variable to null so that a new client
       * could be accepted by the server.
       */
      for (int i = 0; i < maxClient; i++) {
        if (threads[i] == this) {
          threads[i] = null;
        }
      }

      /*
       * Close the output stream, close the input stream, close the socket.
       */
      is.close();
      os.close();
      clientSocket.close();
      
    } catch (IOException e) {
        System.err.println(e);
    }
  }
}