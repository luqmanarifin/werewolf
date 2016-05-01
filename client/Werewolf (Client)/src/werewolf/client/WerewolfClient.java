
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
  
  // game variable
  static boolean isPlaying = false;
  static boolean isReady = false;
  static int days = 0;
  static String time = "night";
  
  static String role = null;
  static ArrayList<String> friends;
  static ArrayList<Player> players;
  static Player kpu = null;
  static boolean amIProposer = false;
  
  // IO something
  static Scanner sc;
  String responseLine;
  static BufferedReader is;
  static PrintStream os;
  
  // socket something
  Socket clientSocket;
  static DatagramReceiverThread udpClient;
  Thread udpThread = new Thread(udpClient);

  
  public WerewolfClient(Socket socket) throws IOException{
    clientSocket = socket; 
    this.is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    this.os = new PrintStream(clientSocket.getOutputStream());
  }
  
    /**
   * Send message pake UDP
   * @param message JSON Object yang mau dikirim
   * @param address Alamat tujuan
   * @param udpPort Port tujuan
   */
  public static void sendUDPMessage(JSONObject message, String address, int udpPort) {
    
    try {
      InetAddress IPAddress = InetAddress.getByName(address);
      int targetPort = udpPort;

      DatagramSocket datagramSocket = new DatagramSocket();
      UnreliableSender unreliableSender = new UnreliableSender(datagramSocket);
    
      byte[] sendData = message.toJSONString().getBytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
      unreliableSender.send(sendPacket);
      
      datagramSocket.close();
    } catch (UnknownHostException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    }
    
  }
  
  /***************** RESPONSE FROM SERVER ****************/
  
  public static void kpuSelectedRes(JSONObject message) {
    
  }
  
  public static void startRes(JSONObject message) {
    time = (String) message.get("time");
    role = (String) message.get("role");
    friends = new ArrayList<String>();
    JSONArray arr = (JSONArray) message.get("friend");
    Iterator i = arr.iterator();
    while (i.hasNext()) {
//                        friends.add((String)i.next());
//                        System.out.println("Teman: " + (String)arr.iterator().next());
    }
    
    isPlaying = true;
    
    JSONObject response = new JSONObject();
    response.put("status", "ok");
    
    os.println(response.toJSONString());
  }
  
  public static void changePhaseRes(JSONObject message) {
    time = (String) message.get("time");
    days = ((Long) message.get("days")).intValue();
    JSONObject response = new JSONObject();
    response.put("status", "ok");

    os.println(response.toJSONString());
  }
  
  public static void voteNowRes(JSONObject message) {
    String phase = (String) message.get("phase");
    
    if (phase.equals("day")) {
      voteWerewolfReq();
    } else {
      voteCivilianReq();
    }
  }
  
  public static void gameOverRes(JSONObject message) {
    
  }
  
  /***************** CLIENT REQUEST TO SERVER ****************/
  
  public static void joinReq() {
    
  }
  
  public static void leaveReq() {
    JSONObject message = new JSONObject();
    message.put("method", "leave");
    os.println(message.toJSONString());
    
    isConnected = false;
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
    
  }
  
   /***************** PAXOS ****************/
  
  public static void prepareProposalReq() {
    
  }
  
  public static void prepareProposalRes() {
    
  }
  
  public static void acceptProposalReq() {
    
  }
  
  public static void acceptProposalRes() {
    
  }
  
  /**
   * Dikirimkan oleh pemain ke KPU ketika melakukan voting
   * siapa yang akan dibunuh di malam hari
   */
  public static void voteWerewolfReq() {
    
  }
  
  /**
   * Dikirimkan oleh pemain ke KPU ketika melakukan voting
   * siapa yang akan dibunuh di siang hari
   */
  public static void voteCivilianReq() {
    
  }
  
  /**
   * Dikirimkan oleh KPU ke server pada malam hari ketika semua
   * pemain telah melakukan voting
   * 
   * vote status = 1 jika ada player yang terbunuh
   * vote status = -1 jika tidak ada keputusan
   */
  public static void voteResultCivilianReq() {
    
  }
  
  /**
   * Dikirimkan oleh KPU ke server pada siang hari ketika semua
   * pemain telah melakukan voting
   * vote status = 1 jika ada player yang terbunuh
   * vote status = -1 jika tidak ada keputusan
   */
  public static void voteResultWerewolfReq() {
    
  }
  
 
  public static void main(String[] args) {

    int playerId = 0; // Player ID
    int hostPort = 0; // Port host
    int udpPort = 0; // Port host
    String udpAddress = null; // Alamat host
    String host = null; // Alamat host
    String cmd; // Command
    JSONObject jsonObj; // JSON Object
    BufferedReader is;
    PrintStream os;
    WerewolfClient client;
    Socket clientSocket = null;

    // Scanner
    sc = new Scanner(System.in);

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

    // Membuat socket dengan host dan port number yang telah diberikan
    try {
      // Join sampai berhasil
      do {
        // Koneksi ke server
        System.out.println("Connecting...");
        clientSocket = new Socket(host, hostPort);
        is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        os = new PrintStream(clientSocket.getOutputStream());
        isConnected = true;
        System.out.println("Connected");

        // Threads
        client = new WerewolfClient(clientSocket);
        Thread tcpThread = new Thread(client);
        tcpThread.start();
        
        
        // Threads for UDP listener
        udpClient = new DatagramReceiverThread(udpPort);
        Thread udpThread = new Thread(udpClient);
        udpThread.start();
      
        System.out.print("Masukkan username: ");
        String username = sc.next();

        jsonObj = new JSONObject();
        jsonObj.put("method", "join");
        jsonObj.put("username", username);
        jsonObj.put("udp_address", udpAddress);
        jsonObj.put("udp_port", udpPort);
        
        isReceived = false;
        os.println(jsonObj.toJSONString());
        System.out.println(jsonObj.toJSONString());
        while(!isReceived){
          Thread.sleep(10);
        }
        
        jsonObj = (JSONObject) new JSONParser().parse(client.responseLine);
      } while (jsonObj.get("status").equals("fail"));
      
      
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
            
            is.close();
            os.close();
            clientSocket.close();
            System.out.println("Keluar dari permainan...");
          break;
          default:
            System.out.println("Perintah salah!");
            break;
        }
        
      } while (!cmd.equals("leave") && isPlaying);
      
          
    } catch (UnknownHostException e) {
      System.err.println("Alamat tidak valid!" + host);
    } catch (IOException e) {
      System.err.println("I/O gagal!" + host);
    } catch (InterruptedException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (ParseException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  
  private void handleOK(JSONObject obj) {
    String client = (String) obj.get("client");
  }
  
  @Override
  public void run() {
    JSONObject obj;
    JSONParser parser = new JSONParser();
    try {
      while (isConnected){
        // Message from server to client
        responseLine = is.readLine();
        if (responseLine != null){
          System.out.println(responseLine);
          isReceived = true;
          Thread.sleep(20);

          try {
            obj = (JSONObject) parser.parse(responseLine);
            String method = (String) obj.get("method");

            switch(method) {
              case "kpu_selected" :
                
                break;
            }
          } catch (Exception e) {

          }
        }
      }

    } catch (IOException e) {
      System.out.println("Connection ended");
    } catch (InterruptedException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
}