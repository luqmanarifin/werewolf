
package werewolf.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
  static boolean isPlaying = false;
  static boolean isConnected = false;
  static boolean isReceived = false;
  static boolean isReady = false;
  static boolean isGameOver = false;
  
  static ArrayList<String> friends;
  static Scanner sc;
  static String time = "night";
  static int days = 0;
  static String role = null; 

  String responseLine;
  static BufferedReader is;
  static PrintStream os;
  Socket clientSocket;
  DatagramClientThread udpClient = new DatagramClientThread();
  Thread udpThread = new Thread(udpClient);

  
  public WerewolfClient(Socket socket) throws IOException{
    clientSocket = socket; 
    this.is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    this.os = new PrintStream(clientSocket.getOutputStream());
  }
  
  public String getResponseLine(){
    return responseLine;
  }
  
  
  
  /***************** RESPONSE FROM SERVER ****************/
  
  public static void startRes(JSONObject message) {
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
  
  
  
  /***************** REQUEST TO SERVER ****************/
  
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
//    System.out.print("Address UDP:");
//    udpAddress = sc.next();
//    System.out.print("Port UDP:");
//    udpPort = sc.nextInt();

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
        
        jsonObj = (JSONObject) new JSONParser().parse(client.getResponseLine());
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
          case "ready": {
            if (!isReady) {
              readyReq();
            } else {
              System.out.println("Anda sudah siap!");
            }
          }
          break;
          case "leave": {
            leaveReq();
            
            is.close();
            os.close();
            clientSocket.close();
            System.out.println("Keluar dari permainan...");
          }
          break;
          default: {
            System.out.println("Perintah salah!");
          }
        }
        
      } while (!cmd.equals("leave") || isGameOver == true);
      
          
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
                    case "start": {
                      time = (String) obj.get("time");
                      role = (String) obj.get("role");
                      friends = new ArrayList<String>();
                      JSONArray arr = (JSONArray) obj.get("friend");
                      Iterator i = arr.iterator();
                      while (i.hasNext()){
//                        friends.add((String)i.next());
//                        System.out.println("Teman: " + (String)arr.iterator().next());
                      }
                      startRes(obj);
                    }
                    break;
                    case "change_phase":{
                      time = (String) obj.get("time");
                      days = (int) obj.get("days");
                      changePhaseRes(obj);
                    }
                    break;
                    case "vote_now":{
                      if (obj.get("phase").equals("day")){
                        // Protokol 10
                        clientAddressReq();
                        JSONObject obj2 = (JSONObject) parser.parse(responseLine);
                        JSONArray arr = (JSONArray)obj2.get("clients");
                        Iterator i = arr.iterator();
                        
                        while (i.hasNext()){
                          obj2 = (JSONObject)i.next();
                          System.out.println(obj2.get("id") + ". " + obj2.get("username"));
                        }
                        System.out.print("Pilih id yang akan dieksekusi: ");
                        
                      }
                      else if (obj.get("phase").equals("night")){
                        // Protokol 8
                      }
                    }
                    break;
                    case "game_over":{
                      System.out.println("GAME OVER!");
                      System.out.println("Winner: " + obj.get("winner"));
                      isPlaying = false;
                      isGameOver = true;
                      // Declare winner and exit
                    }
                    break;
                    default:
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