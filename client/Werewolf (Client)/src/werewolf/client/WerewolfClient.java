
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


    
    public static void main(String[] args) {
        int pid = 0; // Player ID
        int status = 0; // 0 : belum join, 1 : Belum Ready, 2 : Ready
        int portNumber = 0; // Port host
        String host; // Alamat host
        String cmd; // Command
        JSONObject jsonObj; // JSON Object

        // Scanner
        Scanner sc;
        sc = new Scanner(System.in);

        // Socket etc
        Socket clientSocket = null;
        PrintStream os = null;
        BufferedReader is = null;
        BufferedReader inputLine = null;
        boolean closed = false;

        // Input alamat
        System.out.println("Server Address:");
        host = sc.next();
        System.out.println("Port:");
        portNumber = sc.nextInt();

        // Membuat socket dengan host dan port number yang telah diberikan
        try {
          clientSocket = new Socket(host, portNumber);
          inputLine = new BufferedReader(new InputStreamReader(System.in));
          os = new PrintStream(clientSocket.getOutputStream());
          is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        } catch (UnknownHostException e) {
          System.err.println("Alamat tidak valid!" + host);
        } catch (IOException e) {
          System.err.println("I/O gagal!" + host);
        }
        
        
        // Main loop
        do {
            // Baca command untuk persiapan
            cmd = sc.next();
            switch (cmd){
                case "join":
                {
                    if (status == 0){
                        System.out.println("Masukkan username:");
                        String username = sc.next();
                        jsonObj = new JSONObject();
                        jsonObj.put("method", "join");
                        jsonObj.put("username", username);
                        status = 1;
                    }
                    else {
                        System.out.println("Anda sudah masuk!");
                    }
                }
                break;
                case "readyUp":
                    {
                switch (status) {
                    case 1:
                        jsonObj = new JSONObject();
                        jsonObj.put("method", "ready");
                        // Send method
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
                default :
                {
                    System.out.println("Perintah salah!");
                }
            }
        } while (!cmd.equals("exit"));
      }
    
        private static void joinMenu(String host, int port){
            String cmd;
            JSONObject jsonObj;
            Scanner sc = new Scanner(System.in);
            
            // TODO: join method
            System.out.println("Berhasil bergabung di " + host + ":" + port);
            do {
                cmd = sc.next();
                switch (cmd){
                    
                    default :
                    {
                        System.out.println("Ketik 'leave' untuk keluar");
                    }
                }
            } while (!cmd.equals("leave"));
            System.out.println("Keluar...");
            // TODO: leave method
            
        }
        
        private static void gameLoop(){ // Loop utama
            
        }
}