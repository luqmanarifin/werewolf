
package werewolf;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import org.json.simple.JSONObject;

/**
 *
 * @author Husni
 */
public class WerewolfClient implements Runnable {

  
  private static Socket clientSocket = null;
  
  private static PrintStream os = null;
  private static BufferedReader is = null;

  private static BufferedReader inputLine = null;
  private static boolean closed = false;
  
  public static void main(String[] args) {

    
    int portNumber = 8080;
    String host = "localhost";

    // Membuat socket dengan host dan port number yang telah diberikan
    try {
      clientSocket = new Socket(host, portNumber);
      inputLine = new BufferedReader(new InputStreamReader(System.in));
      os = new PrintStream(clientSocket.getOutputStream());
      is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      
    } catch (UnknownHostException e) {
      System.err.println("Don't know about host " + host);
    } catch (IOException e) {
      System.err.println("Couldn't get I/O for the connection to the host " + host);
    }

    /********** SENDING MESSAGE TO SERVER ***********/
    if (clientSocket != null && os != null && is != null) {
      try {

        // Membuat thread yang berfungsi untuk menerima data dari sevever
        new Thread(new WerewolfClient()).start();
        
        // Menuliskan data ke thread
        while (!closed) {
            String username = inputLine.readLine();
            JSONObject message = new JSONObject();
            message.put("method", "join");
            message.put("username", username);
          os.println(message.toJSONString());
        }
        
        /*
         * Close the output stream, close the input stream, close the socket.
         */
        
        os.close();
        is.close();
        clientSocket.close();
      } catch (IOException e) {
        System.err.println("IOException:  " + e);
      }
    }
  }

  /********** THREAD TO RECEIVE MESSAGE FROM SERVER ***********/
  public void run() {
    /*
     * Keep on reading from the socket 
     * TODO: Add condition for leaving the connection
     */
    String responseLine;
    try {
      while ((responseLine = is.readLine()) != null) {
        System.out.println(responseLine);
        
        // TODO: Add condition for leaving the connection
        if (false)
          break;
      }
      closed = true;
      
    } catch (IOException e) {
      System.err.println("IOException:  " + e);
    }
  }
}
