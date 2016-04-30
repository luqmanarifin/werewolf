package werewolf.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

class WerewolfServerThread extends Thread {

  private BufferedReader is = null;
  private PrintStream os = null;
  private Socket clientSocket = null;
  private GameComponent GAMECOMPONENT;
  private int MAXCLIENT;
  private int myPlayerId; // Id player yang ditangani oleh thread ini

  public WerewolfServerThread(Socket clientSocket, GameComponent gc) {
    this.clientSocket = clientSocket;
    this.GAMECOMPONENT = gc;
    MAXCLIENT = gc.threads.length;
  }
  
  private void broadcastMessage(String message) {
      for (int i = 0; i < MAXCLIENT; i++) {
          if (GAMECOMPONENT.threads[i] != null) {
              GAMECOMPONENT.threads[i].sendMessage(message);
          }
      }
  }
  
  private void broadcastMessageExcept(String message, int playerId) {
      for (int i = 0; i < MAXCLIENT; i++) {
          if (GAMECOMPONENT.threads[i] != null && GAMECOMPONENT.players.get(i).getId() != playerId) {
              GAMECOMPONENT.threads[i].sendMessage(message);
          }
      }
  }
  
  private void sendMessage(String message) {
      os.println(message);
  }

  /********** RESPONSE METHOD FROM SERVER TO CLIENT ***********/
  
  
  /*
   * Add user to player list if username is valid
   */
  private void joinRes(JSONObject message) {
      String username = (String) message.get("username");
      String udpAddress = (String) message.get("udp_address");
      int udpPort = ((Long) message.get("udp_port")).intValue();
      int playerId = GAMECOMPONENT.players.size();
      myPlayerId = playerId;
      
      // Add new user to player list
      GAMECOMPONENT.players.add(new Player(username, playerId));
      
      JSONObject response = new JSONObject();
      // TODO: Check if username is valid
      // If username is valid
      if (true) {
        response.put("status", "ok");
        response.put("player_id", playerId);
        response.put("udp_address", udpAddress);
        response.put("udp_port", udpPort);
      } 
      // if username is not valid
      else if (false) {
          response.put("status", "fail");
          response.put("description", "user exists");
      }
      // if game is curently runing
      else {
          response.put("status", "fail");
          response.put("description", "please wait, game is currently running");
      }
        
      sendMessage(response.toJSONString());
  }
  
  private void leaveRes(JSONObject message) {
      // Remove playe from the list
      GAMECOMPONENT.players.remove(myPlayerId);
      JSONObject response = new JSONObject();
      response.put("status", "ok");
      sendMessage(response.toJSONString());
    
  }
  
  /*
   * Give list of players to client who requested it.
   * TODO: get user's port and address
   */
  private void clientAddressRes(JSONObject message) {
      ArrayList<HashMap<String, Object>> clients = new ArrayList<>();
      for (int i = 0; i < GAMECOMPONENT.players.size(); i++) {
        HashMap<String, Object> client = new HashMap<>();   
        client.put("player_id", GAMECOMPONENT.players.get(i).getId());
        client.put("is_alive", GAMECOMPONENT.players.get(i).getAlive());
        client.put("address", GAMECOMPONENT.players.get(i).getUdpAddress());
        client.put("port", GAMECOMPONENT.players.get(i).getUdpPort());
        client.put("username", GAMECOMPONENT.players.get(i).getUsername());
        clients.add(client);
      }
      
      JSONObject response = new JSONObject();
      response.put("status", "ok");
      response.put("clients", clients);
      sendMessage(response.toJSONString());
  }
  
  private void prepareProposalRes(JSONObject message) {
      
  }
  
  private void voteResultWerewolfRes(JSONObject message) {
      int vote_status = ((Long) message.get("vote_status")).intValue();
      
      if (vote_status == 1) {
          int player_killed = ((Long) message.get("player_killed")).intValue();
          GAMECOMPONENT.players.get(player_killed).die();
      }
      
      JSONObject response = new JSONObject();
      response.put("status", "ok");
      response.put("description", "");
      sendMessage(response.toJSONString());
  }
  
  private void voteResultCivilianRes(JSONObject message) {
      int vote_status = ((Long) message.get("vote_status")).intValue();

      if (vote_status == 1) {
          int player_killed = ((Long) message.get("player_killed")).intValue();
          GAMECOMPONENT.players.get(player_killed).die();
      }

      JSONObject response = new JSONObject();
      response.put("status", "ok");
      response.put("description", "");
      sendMessage(response.toJSONString()); 
  }
  
  
  
  /********** THREAD TO RECEIVE MESSAGE FROM CLIENT ***********/
  public void run() {
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
        
          switch (method) {
              case "join":
                  joinRes(message);
                  break;
              case "leave":
                  leaveRes(message);
                  break;
              case "client_address":
                  clientAddressRes(message);
                  break;
              case "prepare_proposal":
                  prepareProposalRes(message);
                  break;
              case "vote_result_werewolf":
                  // Flow: KPU -> server
                  voteResultWerewolfRes(message);
                  break;
              case "vote_result_civilian":
                  // Flow: KPU -> server
                  voteResultCivilianRes(message);
                  break;
              default:
                  break;
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
      for (int i = 0; i < MAXCLIENT; i++) {
        if (GAMECOMPONENT.threads[i] == this) {
          GAMECOMPONENT.threads[i] = null;
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