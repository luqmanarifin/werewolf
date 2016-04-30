
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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;

/**
 *
 * @author Husni & Adin
 */
public class WerewolfClient {


    
    public static void main(String[] args) {
        int playerId = 0; // Player ID
        int status = 0; // 0 : belum join, 1 : Belum Ready, 2 : Ready, 3 : Start
        int hostPort = 0; // Port host
        int udpPort = 0; // Port host
        String udpAddress = null; // Alamat host
        String host = null; // Alamat host
        String cmd; // Command
        JSONObject jsonObj; // JSON Object
        WerewolfClientReceiverThread serverThread;
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
            System.out.println("Connecting...");
            clientSocket = new Socket(host, hostPort);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            System.out.println("Connected");
          
        } catch (UnknownHostException e) {
            System.err.println("Alamat tidak valid!" + host);
        } catch (IOException e) {
            System.err.println("I/O gagal!" + host);
        }
        
        serverThread = new WerewolfClientReceiverThread(clientSocket);
        new Thread(serverThread).start();
        
        // Main loop
        do {
            System.out.print("> ");
            // Baca command untuk persiapan
            cmd = sc.next();
            switch (cmd){
                case "join":
                {
                    if (status == 0){
                        System.out.print("Masukkan username: ");
                        String username = sc.next();
                        
                        jsonObj = new JSONObject();
                        jsonObj.put("method", "join");
                        jsonObj.put("username", username);
                        jsonObj.put("udp_address", udpAddress);
                        jsonObj.put("udp_port", udpPort);
                        os.println(jsonObj.toJSONString());
                        
                        // Ambil response
//                        JSONParser parser = new JSONParser();
//                        try {
//                            synchronized("response"){                                
//                                jsonObj = (JSONObject) parser.parse(serverThread.getResponseLine());
//                            }
//                            playerId = ((Long) jsonObj.get("player_id")).intValue();
//                            System.out.println("ID : " + playerId);
//                        } catch (ParseException ex) {
//                            Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
//                        }
                        
                        status = 1;
                    }
                    else {
                        System.out.println("Anda sudah masuk!");
                    }
                }
                break;
                case "leave":
                {
                    jsonObj = new JSONObject();
                    jsonObj.put("method", "leave");
//                    serverThread.setCommand(jsonObj.toJSONString());
//                    serverThread.run();
                    os.println(jsonObj.toJSONString());
                    System.out.println("keluar...");
                    status = 0;   
                }
                break;
                case "readyUp":
                {
                switch (status) {
                    case 1:
                        jsonObj = new JSONObject();
                        jsonObj.put("method", "ready");
                        status = 2;
//                        serverThread.setCommand(jsonObj.toJSONString());
//                        serverThread.run();
                        
                        break;
                    case 0:
                        System.out.println("Anda belum join");
                        break;
                    default:
                        System.out.println("Anda sudah siap!");
                        break;
                    }
                }
                break;
                case "getList":
                {
                    if (status != 0) {
                        jsonObj = new JSONObject();
                        jsonObj.put("method", "client_address");
//                    serverThread.setCommand(jsonObj.toJSONString());
//                    serverThread.run();
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
                    if (status == 0){
                        System.out.println("Keluar dari program...");
                    }
                    else {
                        System.out.println("ketik 'leave' terlebih dahulu");
                    }
                }
                break;
                default :
                {
                    System.out.println("Perintah salah!");
                }
            }
        } while (!(status == 0 && cmd.equals("exit")));
      }
}