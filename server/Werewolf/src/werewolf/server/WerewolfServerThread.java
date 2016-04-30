package werewolf.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

class WerewolfServerThread extends Thread {

  private BufferedReader is = null;
  private PrintStream os = null;
  private Socket clientSocket = null;
  private GameComponent GC;
  private boolean isLeave = false;
  private int myPlayerId; // Id player yang ditangani oleh thread ini

  public WerewolfServerThread(Socket clientSocket, GameComponent gc, int playerId) {
    this.clientSocket = clientSocket;
    this.GC = gc;
    myPlayerId = playerId;
  }
  
  private void broadcastMessage(JSONObject message) {
      for (int i = 0; i < GC.MAX_CLIENT; i++) {
          if (GC.threads[i] != null) {
              GC.threads[i].sendMessage(message);
          }
      }
  }
  
  private void broadcastMessageExcept(JSONObject message, int playerId) {
      for (int i = 0; i < GC.MAX_CLIENT; i++) {
          if (GC.threads[i] != null && i != playerId) {
              GC.threads[i].sendMessage(message);
          }
      }
  }
  
  private void sendMessage(JSONObject message) {
      os.println(message.toJSONString());
  }

  /********** RESPONSE METHOD FROM SERVER TO CLIENT ***********/
 
  /*
   * Add user to player list if username is valid
   */
  private void joinRes(JSONObject message) {
      String username = (String) message.get("username");
      String udpAddress = (String) message.get("udp_address");
      int udpPort = ((Long) message.get("udp_port")).intValue();
      
      JSONObject response = new JSONObject();
      
      boolean usernameValid = true;
      for(int i = 0; i < GC.MAX_CLIENT; i++) {
        if(GC.threads[i] == null || i == myPlayerId) continue;
        System.out.println(i + " " + username);
        if(username.equals(GC.players[i].getUsername())) {
          usernameValid = false;
          break;
        }
      }
      
      if(GC.isGameStarted) {
          response.put("status", "fail");
          response.put("description", "please wait, game is currently running");
      } else {
        if (usernameValid) {
          response.put("status", "ok");
          response.put("player_id", myPlayerId);
          response.put("udp_address", udpAddress);
          response.put("udp_port", udpPort);
          
          // Add new user to player list
          GC.players[myPlayerId] = new Player(username, myPlayerId);
          GC.connectedPlayer++;
        } else  {
            response.put("status", "fail");
            response.put("description", "user exists");
        }
      }
      sendMessage(response);
  }
  
  private void leaveRes(JSONObject message) {
    // Remove playe from the list
    GC.players[myPlayerId] = null;
    GC.connectedPlayer--;
    isLeave = true;

    JSONObject response = new JSONObject();
    response.put("status", "ok");
    sendMessage(response);
  }
  
  private void readyRes(JSONObject message) {
    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("description", "waiting for other player to start");
    
    GC.readyCount++;
    if (GC.readyCount == GC.connectedPlayer && GC.readyCount >= 6) {
      startReq();
    }
  }
  /*
   * Give list of players to client who requested it.
   */
  private void clientAddressRes(JSONObject message) {
    ArrayList<HashMap<String, Object>> clients = new ArrayList<>();
    for (int i = 0; i < GC.MAX_CLIENT; i++) {
      if(GC.threads[i] == null) continue;
      HashMap<String, Object> client = new HashMap<>();   
      client.put("player_id", i);
      client.put("is_alive", GC.players[i].getAlive());
      client.put("address", GC.players[i].getUdpAddress());
      client.put("port", GC.players[i].getUdpPort());
      client.put("username", GC.players[i].getUsername());
      clients.add(client);
    }

    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("clients", clients);
    sendMessage(response);
  }
  
  /**
   * bisa berkali kali
   * @param message 
   */
  private void voteResultWerewolfRes(JSONObject message) {
      int vote_status = ((Long) message.get("vote_status")).intValue();
      
      if (vote_status == 1) {
        int player_killed = ((Long) message.get("player_killed")).intValue();
        GC.players[player_killed].die();
        
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        response.put("description", "");
        sendMessage(response);
        
        changePhaseReq();
      } else {
        voteNowReq();
      }
  }
  
  /**
   * maksimal 2 kali
   * @param message 
   */
  private void voteResultCivilianRes(JSONObject message) {
      int vote_status = ((Long) message.get("vote_status")).intValue();
      GC.remainingVote--;
      
      if (vote_status == 1) {
        int player_killed = ((Long) message.get("player_killed")).intValue();
        GC.players[player_killed].die();
        
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        response.put("description", "");
        sendMessage(response);
        
        changePhaseReq();
      } else {
        if(GC.remainingVote > 0) {
          voteNowReq();
        } else {
          changePhaseReq();
        }
      }
  }
  
  
  /********** REQUEST METHOD FROM SERVER TO CLIENT ***********/
  private void startReq() {
    GC.isDay = true;
    GC.days = 0;
    GC.isGameStarted = true;
    
    for(int i = 0; i < GC.MAX_CLIENT; i++) {
      if(GC.threads[i] == null) continue;
      GC.players[i].reset();
    }
    
    Random random = new Random();
    boolean good = false;
    while(!good) {
      for(int i = 0; i < GC.MAX_CLIENT; i++) {
        if(GC.threads[i] == null) continue;
        if(random.nextDouble() < (double) 1 / 3) {
          GC.players[i].isWolf = false; 
        } else {
          GC.players[i].isWolf = true; 
        }
      }
      int wolf = 0, civ = 0;
      for(int i = 0; i < GC.MAX_CLIENT; i++) {
        if(GC.threads[i] == null) continue;
        if(GC.players[i].isWolf) {
          wolf++;
        } else {
          civ++;
        }
      }
      good = (wolf >= 2 && civ >= 4 && wolf != civ); 
    }
    ArrayList<String> listWolf = new ArrayList();
    for(int i = 0; i < GC.MAX_CLIENT; i++) {
      if(GC.threads[i] == null) continue;
      if(GC.players[i].isWolf) {
        listWolf.add(GC.players[i].getUsername());
      }
    }
    
    for(int i = 0; i < GC.MAX_CLIENT; i++) {
      if(GC.threads[i] == null) continue;
      JSONObject response = new JSONObject();
      response.put("method", "start");
      response.put("time", GC.getTime());
      response.put("description", "game is started");
      
      if(GC.players[i].isWolf) {
        response.put("role", "werewolf");
        response.put("friend", listWolf);
      } else {
        response.put("role", "civilian");
        response.put("friend", "");
      }
      GC.threads[i].sendMessage(response);
    }
  }
  
  /*
   * Dikirimkan oleh server kepada seluruh client jika ada
   * perubahan waktu
   */
  private void changePhaseReq() {
    if(!GC.isDay) {
      GC.days++;
    }
    GC.isDay = !GC.isDay;
    
    JSONObject message = new JSONObject();
    message.put("method", "change_phase");
    message.put("time", GC.getTime());
    message.put("days", GC.days);
    message.put("description", "Ganti fase");
    
    sendMessage(message);
    GC.remainingVote = 2;
    voteNowReq();
  }
  
  /*
   * Dikirimkan oleh server kepada seluruh client ketika
   * akan dilakukan voting pemain yang akan dibunuh
   */
  private void voteNowReq() {
    JSONObject message = new JSONObject();
    message.put("method", "vote_now");
    message.put("phase", GC.getTime());
    
    sendMessage(message);
  }
  
  /*
   * Dikirimkan oleh server ketika permainan berkahir
   */
  private void gameOverReq(int winner) {
    GC.isGameStarted = false;
    JSONObject message = new JSONObject();
    message.put("method", "game_over");
    String name = winner == 1? "werewolf" : "civilian";
    message.put("winner", name);
    message.put("description", name + " win");
  }
  
  /**
   * 1 if werewolf win
   * -1 if civilian win
   * 0 if none win
   * @return 
   */
  private int isWin() {
    int wolfAlive = 0, civAlive = 0;
    for(int i = 0; i < GC.MAX_CLIENT; i++) {
      if(GC.threads[i] == null) continue;
      if(GC.players[i].getAlive() == 1) {
        if(GC.players[i].isWolf) {
          wolfAlive++;
        } else {
          civAlive++;
        }
      }
    }
    if(wolfAlive == 0) {
      return -1;
    } else if(wolfAlive == civAlive) {
      return 1;
    }
    return 0;
  }
  
  private void showDescription(JSONObject object) {
    String message = (String) object.get("description");
    System.out.println("Log : " + message);
  }
  
  /********** THREAD TO RECEIVE MESSAGE FROM CLIENT ***********/
  public void run() {
    try {
      
      is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      os = new PrintStream(clientSocket.getOutputStream());
      
      while (!isLeave) {
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
          case "ready":
            readyRes(message);
            break;
          case "client_address":
            clientAddressRes(message);
            break;
          case "vote_result_werewolf":
            // Flow: KPU -> server
            voteResultWerewolfRes(message);
            break;
          case "vote_result_civilian":
            // Flow: KPU -> server
            voteResultCivilianRes(message);
            break;

          case "failed":
            showDescription(message);
            break;
          case "error":
            showDescription(message);
            break;
          default:
              break;
        }
        int stateWinner = isWin();
        if(stateWinner != 0) {
          gameOverReq(stateWinner);
        }
      }
      System.out.println("Good bye " + myPlayerId);
      GameComponent.threads[myPlayerId] = null;

      /*
       * Close the output stream, close the input stream, close the socket.
       */
      is.close();
      os.close();
      clientSocket.close();
      /*
       * Clean up. Set the current thread variable to null so that a new client
       * could be accepted by the server.
       */
      
    } catch (IOException e) {
      System.out.println("Good bye " + myPlayerId);
      GameComponent.threads[myPlayerId] = null;

      try {
        /*
        * Close the output stream, close the input stream, close the socket.
        */
        is.close();
        os.close();
        clientSocket.close();
      } catch (IOException ex) {
        Logger.getLogger(WerewolfServerThread.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}