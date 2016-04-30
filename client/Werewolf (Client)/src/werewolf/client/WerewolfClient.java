
package werewolf.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
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

  String responseLine;
  BufferedReader is;
  PrintStream os;
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
  
  public static void main(String[] args) {
    boolean isReady = false;

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
    Scanner sc;
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

      // Join the game
      System.out.print("Masukkan username: ");
      String username = sc.next();

      jsonObj = new JSONObject();
      jsonObj.put("method", "join");
      jsonObj.put("username", username);
      jsonObj.put("udp_address", udpAddress);
      jsonObj.put("udp_port", udpPort);
      os.println(jsonObj.toJSONString());
      
      while(!isReceived){
        Thread.sleep(10);
      }
      // Main loop
      do {
        // Print status
        if (isPlaying){
          System.out.println("Status: Playing");
        }
        else {
          if (isReady){
            System.out.println("Status: Ready");
          }
          else{
            System.out.println("Status: Not Ready");
          }
        }
        
        System.out.print("> ");
        cmd = sc.next();
        switch (cmd) {
          case "ready": {
            if (!isReady) {
              if (!isReady) {
                jsonObj = new JSONObject();
                jsonObj.put("method", "ready");
                os.println(jsonObj.toJSONString());
                isReady = true;
              }
            } else {
              System.out.println("Anda sudah siap!");
            }
          }
          break;
          case "getList": {
            if (isPlaying) {
              jsonObj = new JSONObject();
              jsonObj.put("method", "client_address");
              os.println(jsonObj.toJSONString());
            }
          }
          break;
          case "voteWerewolf": {

          }
          break;
          case "voteCivilian": {

          }
          break;
          case "leave": {
            isConnected = false;
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
        
      } while (!cmd.equals("leave"));
      
          
    } catch (UnknownHostException e) {
      System.err.println("Alamat tidak valid!" + host);
    } catch (IOException e) {
      System.err.println("I/O gagal!" + host);
    } catch (InterruptedException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  @Override
    public void run() {
      JSONObject obj;
      JSONParser parser = new JSONParser();
        try {
            while (isConnected){
              responseLine = is.readLine();
              if (responseLine != null){
                System.out.println(responseLine);
                isReceived = true;
              }
              else {
                try {
                  obj  = (JSONObject)parser.parse(responseLine);
                  if (obj.get("method").equals("start")){
                    udpThread.start();
                    isPlaying = true;
                  }
                } catch (ParseException ex) {
                  Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
                }
              }
            }

        } catch (IOException e) {
          System.out.println("Connection ended");
        }
    }
  
}