
package werewolf.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import org.json.simple.JSONObject;

/**
 *
 * @author Husni & Adin
 */
public class WerewolfClient {
  static boolean isPlaying = false;
  
  public static void main(String[] args) {
      boolean isReady = false;
      int playerId = 0; // Player ID
      int hostPort = 0; // Port host
      int udpPort = 0; // Port host
      String udpAddress = null; // Alamat host
      String host = null; // Alamat host
      String cmd; // Command
      JSONObject jsonObj; // JSON Object
      WerewolfClientThread serverThread;
      DatagramClientThread udpThread;
      Socket clientSocket = null;
      PrintStream os = null;
      BufferedReader inputLine = null;

      // Scanner
      Scanner sc;
      sc = new Scanner(System.in);

      // Input alamat
      System.out.println("Selamat datang di Werewolf!");

      System.out.print("Server Address: ");
      host = sc.next();
      System.out.print("Server Port:");
      hostPort = sc.nextInt();
      System.out.print("Address UDP:");
      udpAddress = sc.next();
      System.out.print("Port UDP:");
      udpPort = sc.nextInt();

      // Membuat socket dengan host dan port number yang telah diberikan
      try {
        // Koneksi ke server
        System.out.println("Connecting...");
        clientSocket = new Socket(host, hostPort);
        inputLine = new BufferedReader(new InputStreamReader(System.in));
        os = new PrintStream(clientSocket.getOutputStream());
        System.out.println("Connected");
          
        // Threads
        serverThread = new WerewolfClientThread(clientSocket);
        udpThread = new DatagramClientThread();
        new Thread(serverThread).start();
        new Thread(udpThread).start();
      
        // Join the game
        System.out.print("Masukkan username: ");
        String username = sc.next();

        jsonObj = new JSONObject();
        jsonObj.put("method", "join");
        jsonObj.put("username", username);
        jsonObj.put("udp_address", udpAddress);
        jsonObj.put("udp_port", udpPort);
        os.println(jsonObj.toJSONString());

      } catch (UnknownHostException e) {
          System.err.println("Alamat tidak valid!" + host);
      } catch (IOException e) {
          System.err.println("I/O gagal!" + host);
      }
      
      // Main loop
      do {
          System.out.print("> ");
          cmd = sc.next();
          switch (cmd){
              case "leave":
              {
                if (isReady){
                  jsonObj = new JSONObject();
                  jsonObj.put("method", "leave");
                  os.println(jsonObj.toJSONString());
                  System.out.println("keluar...");
                }
                else {
                  System.out.println("Anda");
                }
              }
              break;
              case "readyUp":
              {
                if (!isReady){
                  if (!isPlaying){
                    jsonObj = new JSONObject();
                    jsonObj.put("method", "ready");
                  }
                }
                else {
                  System.out.println("Anda sudah siap!");
                }       
              }       
              break;
              case "getList":
              {
                  if (isPlaying) {
                      jsonObj = new JSONObject();
                      jsonObj.put("method", "client_address");
                      os.println(jsonObj.toJSONString());
                  }
              }
              break;
              case "voteWerewolf":
              {

              }
              break;
              case "voteCivilian":
              {

              }
              break;
              case "exit":
              {
                
              }
              break;
              default :
              {
                  System.out.println("Perintah salah!");
              }
          }
      } while (true);
    }
}