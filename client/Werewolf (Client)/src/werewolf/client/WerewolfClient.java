
package werewolf.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Husni & Adin
 */
public class WerewolfClient implements Runnable{
  static boolean isConnected = false;
  static boolean isReceived = false;
  static boolean startPaxos = false;
  
  static final int MAX_CLIENT = 10;
  
  // game variable
  static boolean isPlaying = false;
  static boolean isReady = false;
  static int days = 0;
  static String time = "night";
  static boolean canVote = false;
  
  static String role = null;
  static ArrayList<String> friends = new ArrayList<String>();
  static ArrayList<Player> players = new ArrayList<Player>();
  static Player kpu = null;
  static Player me = null;
  static boolean amIProposer = false;
  static boolean amIKpu = false;
  static int[] votes = new int[MAX_CLIENT];
  static int allVote = 0;
  
  static int lastProposal = -1;
  static int lastKpu = -1;
  static int numProposal = 0;   // hanya berlaku untuk proposer
  
  // IO something
  static Scanner sc = new Scanner(System.in);;
  String responseLine;
  static BufferedReader is;
  static PrintStream os;
  
  // socket something
  static Socket clientSocket;
  static DatagramReceiverThread udpClient;
  Thread udpThread = new Thread(udpClient);

  static String username;
  static int playerId = 0; // Player ID
  static String host = null; // Alamat host
  static int hostPort = 0; // Port host

  static String udpAddress = null; // Alamat host
  static int udpPort = 0; // Port host

  static String cmd; // Command
  static JSONObject jsonObj; // JSON Object
  static BufferedReader iis;
  static PrintStream oos;
  static WerewolfClient client;
  static Socket cs = null;
  
  public WerewolfClient(Socket socket) throws IOException{
    clientSocket = socket; 
    this.is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    this.os = new PrintStream(clientSocket.getOutputStream());
  }
  
  /***************** RESPONSE FROM SERVER ****************/
  
  public static void sendOK() {
    JSONObject response = new JSONObject();
    response.put("status", "ok");
    os.println(response.toJSONString());
  }
  
  public static void kpuSelectedRes(JSONObject message) {
    int kpu_id = ((Long)message.get("kpu_id")).intValue();
    
    for(Player p : players) {
      if(p.id == kpu_id) {
        kpu = new Player(p);
      }
    }
    if(kpu.id == me.id) {
      amIKpu = true;
      for(int i = 0; i < MAX_CLIENT; i++) votes[i] = 0;
      allVote = 0;
    }
    
    sendOK();
  }
  
  public static void startRes(JSONObject message) {
    try {
      isPlaying = true;
      isReady = false;
      days = 0;
      time = (String) message.get("time");
      canVote = false;

      role = (String) message.get("role");
      friends = new ArrayList<String>();
      JSONArray arr = (JSONArray) message.get("friend");
      Iterator it = arr.iterator();
      while (it.hasNext()) {
        String friend = (String) it.next();
        friends.add(friend);
        System.out.println("Teman: " + friend);
      }
      kpu = null;
      amIProposer = false;
      amIKpu = false;
      lastProposal = -1;
      lastKpu = -1;
      numProposal = 0;
      isReceived = false;
      
      sendOK();
      clientAddressReq();
      startPaxos = true;
    } catch (Exception e) {
      System.out.println(e);
    }
  }
  
  public static void triggerPaxos() {
    if (amIProposer) {
      System.out.println("seharusnya aku paksos");
      DatagramReceiverThread.prepareProposalReq();
    }
  }
  
  public static void changePhaseRes(JSONObject message) {
    time = (String) message.get("time");
    days = ((Long) message.get("days")).intValue();
    String news = (String) message.get("description");
    
    System.out.println(news);
    System.out.println("");
    
    sendOK();
    
    if(time.equals("day")) {
      System.out.println("MALAM BERGANTI SIANG...");
      System.out.println("");
      amIKpu = false;
      if(amIProposer) {
        DatagramReceiverThread.prepareProposalReq();
      }
    } else {
      System.out.println("SIANG BERGANTI MALAM...");
      System.out.println("");
    }
  }
  
  public static void voteNowRes(JSONObject message) {
    time = (String) message.get("phase");
    
    if(me.isAlive == 0) {
      System.out.println("Now time to vote.");
      System.out.println("But you dead. Please wait...");
      return;
    }
    
    //System.out.println("now " + time + " role " + me.role.equals("werewolf"));
    allVote = 0;
    for(int i = 0; i < votes.length; i++) votes[i] = 0;
    
    canVote = true;
    if(time.equals("day")) {
      System.out.println("Now you can vote!");
      System.out.println("Type voteCivilian to vote");
    } else if(time.equals("night") && me.role.equals("werewolf")){
      System.out.println("You are werewolf. Now you can vote!");
      System.out.println("Type voteWerewolf to vote");
    }
    
    sendOK();
  }
  
  public static void gameOverRes(JSONObject message) {
    String winner = (String) message.get("winner");
    System.out.println("");
    System.out.println("Game over!");
    System.out.println(winner + " WIN THE GAME!");
    System.out.println("");
    
    isPlaying = false;
    isReady = false;
    
    sendOK();
  }
  
  /***************** CLIENT REQUEST TO SERVER ****************/
  
  public static void joinReq() throws Exception {
    // Koneksi ke server
    System.out.println("Connecting...");
    cs = new Socket(host, hostPort);
    iis = new BufferedReader(new InputStreamReader(cs.getInputStream()));
    oos = new PrintStream(cs.getOutputStream());
    isConnected = true;
    System.out.println("Connected");

    // Threads
    client = new WerewolfClient(cs);
    Thread tcpThread = new Thread(client);
    tcpThread.start();

    // Threads for UDP listener
    udpClient = new DatagramReceiverThread(udpPort);
    Thread udpThread = new Thread(udpClient);
    udpThread.start();
    
    boolean ok = false;
    
    // Join sampai berhasil
    do {
      System.out.print("Masukkan username: ");
      username = sc.next();

      jsonObj = new JSONObject();
      jsonObj.put("method", "join");
      jsonObj.put("username", username);
      jsonObj.put("udp_address", udpAddress);
      jsonObj.put("udp_port", udpPort);
      isReceived = false;
      oos.println(jsonObj.toJSONString());

      System.out.println(jsonObj.toJSONString());
      isReceived = false;
      while (!isReceived) {
        Thread.sleep(10);
      }

      jsonObj = (JSONObject) new JSONParser().parse(client.responseLine);
      ok = jsonObj.get("status").equals("ok");
      if(ok) playerId = ((Long) jsonObj.get("player_id")).intValue();
    } while (!ok);

    me = new Player(username, playerId);
    me.isAlive = 1;
    me.udpPort = udpPort;
    me.udpAddress = udpAddress;
  }
  
  public static void leaveReq() {
    try {
      JSONObject message = new JSONObject();
      message.put("method", "leave");
      os.println(message.toJSONString());
      
      isConnected = false;
      iis.close();
      oos.close();
      cs.close();
      is.close();
      os.close();
      clientSocket.close();
      DatagramReceiverThread.socket.close();
      
      System.out.println("Keluar dari permainan...");
      
    } catch (IOException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public static void readyReq() {
    JSONObject message = new JSONObject();
    message = new JSONObject();
    message.put("method", "ready");
    os.println(message.toJSONString());
    
    isReady = true;
  }
  
  public static void clientAddressReq() {
    JSONObject message = new JSONObject();
    message = new JSONObject();
    message.put("method", "client_address");
    os.println(message.toJSONString());
  }
  
  public static void clientAcceptedReq() {
    System.out.println("Sending " + lastKpu + " as best KPU to server");
    
    JSONObject message = new JSONObject();
    message = new JSONObject();
    message.put("method", "accepted_proposal");
    message.put("kpu_id", lastKpu);
    message.put("description", "Kpu is selected");
    os.println(message.toJSONString());    
  }
  
  /**
   * Dikirimkan oleh KPU ke server pada malam hari ketika semua pemain telah
   * melakukan voting
   *
   * vote status = 1 jika ada player yang terbunuh vote status = -1 jika tidak
   * ada keputusan
   */
  public static void voteResultWerewolfReq() {
    System.out.println(allVote + "/" + players.size() + " voted");
    if(allVote < players.size()) return;
    JSONObject message = new JSONObject();
    message = new JSONObject();
    message.put("method", "vote_result_werewolf");
    int best = 0, pt = -1;
    ArrayList<ArrayList<Integer>> res = new ArrayList<ArrayList<Integer>>();
    for(Player p : players) {
      if(votes[p.id] > best) {
        best = votes[p.id];
        pt = p.id;
      } else if(votes[p.id] == best) {
        pt = -1;
      }
      if(p.isAlive == 1 && p.role.equals("civilian")) {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        arr.add(p.id);
        arr.add(votes[p.id]);
        res.add(arr);
      }
    }
    message.put("vote_result", res);
    if(pt == -1) {
      message.put("vote_status", -1);
    } else {
      message.put("vote_status", 1);
      message.put("player_killed", pt);
    }
    
    os.println(message.toJSONString());
  }

  /**
   * Dikirimkan oleh KPU ke server pada siang hari ketika semua pemain telah
   * melakukan voting vote status = 1 jika ada player yang terbunuh vote status
   * = -1 jika tidak ada keputusan
   */
  public static void voteResultCivilianReq() {
    System.out.println(allVote + "/" + players.size() + " voted");
    if(allVote < players.size()) return;
    JSONObject message = new JSONObject();
    message = new JSONObject();
    message.put("method", "vote_result_civilian");
    int best = 0, pt = -1;
    ArrayList<ArrayList<Integer>> res = new ArrayList<ArrayList<Integer>>();
    for (Player p : players) {
      if (votes[p.id] > best) {
        best = votes[p.id];
        pt = p.id;
      } else if (votes[p.id] == best) {
        pt = -1;
      }
      if (p.isAlive == 1) {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        arr.add(p.id);
        arr.add(votes[p.id]);
        res.add(arr);
      }
    }
    message.put("vote_result", res);
    if (pt == -1) {
      message.put("vote_status", -1);
    } else {
      message.put("vote_status", 1);
      message.put("player_killed", pt);
    }

    os.println(message.toJSONString());
  }
  
  public static void initiateInput() {
    // Input alamat
    System.out.println("Selamat datang di Werewolf!");
    System.out.print("Server Address: ");
    host = sc.next();
    System.out.print("Server Port: ");
    hostPort = sc.nextInt();
    System.out.print("Your Address: ");
    udpAddress = sc.next();
    System.out.print("Port UDP: ");
    udpPort = sc.nextInt();
  }
  
  
  public static void main(String[] args) {
    initiateInput();
            
    // Membuat socket dengan host dan port number yang telah diberikan  
    try {
      joinReq();
      
      // Main loop
      do {
        // Print status
        if (isPlaying){
          System.out.println("Status: Playing");
          System.out.println("Time: " + time);
          System.out.println("Day: " + days);
        }
        else {
          if (isReady){
            System.out.println("Status: Ready");
          }
          else{
            System.out.println("Status: Not Ready");
          }
        }
       
        cmd = sc.next();
        switch (cmd) {
          case "ready":
            if (!isReady) {
              readyReq();
            } else {
              System.out.println("Anda sudah siap!");
            }
            break;
          case "leave":
            leaveReq();
            break;
          case "listPlayer":
            clientAddressReq();
            break;
          case "voteWerewolf":
            DatagramReceiverThread.voteWerewolfReq();
            break;
          case "voteCivilian":
            DatagramReceiverThread.voteCivilianReq();
            break;
          default:
            System.out.println("Perintah salah!");
            break;
        }
      } while (!cmd.equals("leave"));
      System.out.println("Why did thou leave?");
          
    } catch (UnknownHostException e) {
      System.err.println("Alamat tidak valid!" + host);
    } catch (IOException e) {
      System.err.println("I/O gagal!" + host);
    } catch (InterruptedException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (ParseException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (Exception ee) {
      
    }
  }
  
  @Override
  public void run() {
    JSONObject obj = null;
    JSONParser parser = new JSONParser();
    try {
      while (isConnected){
        if(startPaxos && isReceived) {
          startPaxos = false;
          isReceived = false;
          triggerPaxos();
        }
        // Message from server to client
        responseLine = is.readLine();
        if (responseLine != null){
          isReceived = true;
          Thread.sleep(20);

          try {
            System.out.println("receive: " + responseLine);
            obj = (JSONObject) parser.parse(responseLine);
            String method = (String) obj.get("method");
            switch(method) {
              case "kpu_selected":
                kpuSelectedRes(obj);
                break;
              case "start":
                startRes(obj);
                break;
              case "change_phase":
                changePhaseRes(obj);
                break;
              case "vote_now":
                voteNowRes(obj);
                break;
              case "game_over":
                gameOverRes(obj);
                break;
              default:
                break;
            }
          } catch (Exception e) {
            handleOK(obj);
          }
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Connection ended");
    } catch (InterruptedException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  private void printPlayers() {
    for(Player p : players) {
      System.out.println(p.id + ") " + p.username
              + " (role : " + p.role + ")"
              + (p.isAlive == 0? " (dead) " : ""));
    }
  }
  
  private void updateAmIProposer() {
    ArrayList<Integer> a = new ArrayList<Integer>();
    for(Player p : players) {
      a.add(p.id);
    }
    Collections.sort(a);
    Collections.reverse(a);
    amIProposer = me.id == a.get(0) || me.id == a.get(1);
    System.out.println("debug proposer " + amIProposer);
    isReceived = true;
  }
  
  private void updateWerewolfFriend() {
    for(String name : friends) {
      for(Player p : players) {
        if(name.equals(p.username)) {
          p.role = "werewolf";
        }
        if(name.equals(me.username)) {
          me.role = "werewolf";
        }
      }
    }
  }
  
  private void handleOK(JSONObject obj) {
    String status = (String) obj.get("status");
    if(status != null && status.equals("ok")) {
      System.out.println("Receive OK from server.");
      JSONArray clients = (JSONArray) obj.get("clients");
      if(clients != null) {
        players = new ArrayList<Player>();
        for(int i = 0; i < clients.size(); i++) {
          JSONObject client = (JSONObject) clients.get(i);
          players.add(new Player((String)client.get("username"),
                  ((Long)client.get("player_id")).intValue()));
          int at = players.size() - 1;
          players.get(at).isAlive = ((Long)client.get("is_alive")).intValue();
          players.get(at).udpAddress = (String) client.get("address");
          players.get(at).udpPort = ((Long)client.get("port")).intValue();
          if(players.get(at).isAlive == 0) {
            players.get(at).role = (String) client.get("role");
          }
        }
        updateAmIProposer();
        updateWerewolfFriend();
        printPlayers();
      }
    }
  }
}