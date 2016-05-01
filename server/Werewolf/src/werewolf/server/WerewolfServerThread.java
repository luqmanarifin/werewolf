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
  
  private static boolean isVotingKpu = false;
  private static boolean isWaiting = false;
  private static int[] votes = new int[GameComponent.MAX_CLIENT];
  private static int voteCount = 0;
  private static long time;
  
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
          GC.players[myPlayerId] = new Player(username, myPlayerId, udpAddress, udpPort);
          
          GC.connectedPlayer++;
        } else  {
            response.put("status", "fail");
            response.put("description", "user exists");
        }
      }
      System.out.println(response.toJSONString());
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
    sendMessage(response);

    GameComponent.players[myPlayerId].isReady = true;
    
    if (allReady() && GC.connectedPlayer >= 6) {
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
      if(GC.players[i].getAlive() == 0) {
        client.put("role", GC.players[i].getRole());
      }
      clients.add(client);
      System.out.println("added : " + client.toString());
    }

    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("clients", clients);
    System.out.println(clients.toString());
    sendMessage(response);
    System.out.println("Mengirim list player ke ID : " + myPlayerId);
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
        
        changePhaseReq(GC.players[player_killed].getUsername());
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
    System.out.println(GC.remainingVote + " votes remaining");
    if (vote_status == 1) {
      int player_killed = ((Long) message.get("player_killed")).intValue();
      GC.players[player_killed].die();

      JSONObject response = new JSONObject();
      response.put("status", "ok");
      response.put("description", "");
      sendMessage(response);

      changePhaseReq(GC.players[player_killed].getUsername());
    } else {
      if(GC.remainingVote > 0) {
        voteNowReq();
      } else {
        changePhaseReq(null);
      }
    }
  }
  
  private void acceptedProposalRes(JSONObject message) {
    int kpu_id = ((Long) message.get("kpu_id")).intValue();
    System.out.println("bef : isWaiting " + isWaiting + " isVotingKpu " + isVotingKpu);
    if(!isWaiting) {
      if(isVotingKpu) {
        System.out.println("get vote " + kpu_id + " as kpu");
        isWaiting = true;
        isVotingKpu = false;
        time = System.nanoTime();
        for(int i = 0; i < GC.MAX_CLIENT; i++) votes[i] = 0;
        voteCount = 1;

        votes[kpu_id]++;
      }
    } else {
      System.out.println("get vote " + kpu_id + " as kpu");
      long delta = System.nanoTime() - time;
      votes[kpu_id]++;
      voteCount++;
      System.out.println("get vote " + voteCount + "/" + GC.connectedPlayer);
      System.out.println("time collapsed " + delta / 1e9 + " sec");
      if(delta > 1e10 || GC.connectedPlayer <= voteCount) {
        isWaiting = false;
        int best = -1, p = -1;
        for(int i = 0; i < GC.MAX_CLIENT; i++) {
          if(votes[i] > best) {
            best = votes[i];
            p = i;
          }
        }
        // kpu selected vote now
        kpuSelectedReq(p);
        voteNowReq();
      }
    }
    System.out.println("aft : isWaiting " + isWaiting + " isVotingKpu " + isVotingKpu);
  }
  
  /********** REQUEST METHOD FROM SERVER TO CLIENT ***********/
  private void startReq() {
    GC.isDay = true;
    GC.days = 0;
    GC.isGameStarted = true;
    GC.remainingVote = 2;
    
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
      good = (wolf >= 1 && civ >= 2 && wolf != civ); 
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
        response.put("friend", new ArrayList<>());
      }
      GC.threads[i].sendMessage(response);
    }
    System.out.println("Game started");
    isVotingKpu = true;
  }
  
  /*
   * Dikirimkan oleh server kepada seluruh client jika ada
   * perubahan waktu
   */
  private void changePhaseReq(String who) {
    if(!GC.isDay) {
      GC.days++;
    }
    GC.isDay = !GC.isDay;
    
    JSONObject message = new JSONObject();
    message.put("method", "change_phase");
    message.put("time", GC.getTime());
    message.put("days", GC.days);
    if(who != null) {
      message.put("description", who + " (" + GameComponent.getRole(who) + ") has been killed by " + (GC.isDay? "WEREWOLF" : "civilians"));
    } else {
      message.put("description", "No one killed.");
    }
    broadcastMessage(message);
    
    if(GC.isDay) {
      isVotingKpu = true;
    } else {
      voteNowReq();
    }
    
    GC.remainingVote = 2;
    System.out.println("Change phase");
  }
  
  /*
   * Dikirimkan oleh server kepada seluruh client ketika
   * akan dilakukan voting pemain yang akan dibunuh
   */
  private void voteNowReq() {
    JSONObject message = new JSONObject();
    message.put("method", "vote_now");
    message.put("phase", GC.getTime());
    
    broadcastMessage(message);
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
    broadcastMessage(message);
  }
  
  /*
   * Dikirimkan oleh server ketika KPU terpilih
   * METHODNYA BELUM KELAR
   */  
  private void kpuSelectedReq(int num) {
    JSONObject message = new JSONObject();
    message.put("method", "kpu_selected");
    message.put("kpu_id", num);
    System.out.println("Choose " + num + " as KPU");
    
    broadcastMessage(message);
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
      if(GC.threads[i] == null || GC.players[i] == null) continue;
      if(GC.players[i].getAlive() == 1) {
        if(GC.players[i].isWolf) {
          wolfAlive++;
        } else {
          civAlive++;
        }
      }
    }
    System.out.println("wolf: " + wolfAlive + " civ: " + civAlive);
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
  
  private boolean allReady() {
    for(int i = 0; i < GameComponent.MAX_CLIENT; i++) {
      if(GameComponent.threads[i] == null) continue;
      if(!GameComponent.players[i].isReady) return false;
    }
    return true;
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
          break;
        }
        
        String method = (String) message.get("method");
        
        try {
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
            case "accepted_proposal":
              acceptedProposalRes(message);
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
        } catch(NullPointerException e) {
          System.out.println("Got OK from " + myPlayerId);
        }
        int stateWinner = isWin();
        if(stateWinner != 0 && GC.isGameStarted) {
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
      
    } catch (IOException ee) {
      Exception e = new Exception();
      System.out.println(ee);
      System.out.println("Good bye " + myPlayerId);
      GameComponent.threads[myPlayerId] = null;

      try {
        is.close();
        os.close();
        clientSocket.close();
      } catch (IOException ex) {
        Logger.getLogger(WerewolfServerThread.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}