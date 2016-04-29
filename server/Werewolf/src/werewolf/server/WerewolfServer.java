package werewolf.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Husni
 */
public class WerewolfServer {
  private static ServerSocket serverSocket = null;
  private static Socket clientSocket = null;
  private static final int maxClient = 6;
  private static final WerewolfServerThread[] threads = new WerewolfServerThread[maxClient];

  public static void main(String args[]) {
    int portNumber = 8080;
    
    // Membuat server socket
    try {
      serverSocket = new ServerSocket(portNumber);
    } catch (IOException e) {
      System.out.println(e);
    }

    // Membuat socket baru setiap ada koneksi dari client
    while (true) {
      try {
        clientSocket = serverSocket.accept();
        int i = 0;
        for (i = 0; i < maxClient; i++) {
          if (threads[i] == null) {
            (threads[i] = new WerewolfServerThread(clientSocket, threads)).start();
            break;
          }
        }
        if (i == maxClient) {
          PrintStream os = new PrintStream(clientSocket.getOutputStream());
          os.println("Server too busy. Try later.");
          os.close();
          clientSocket.close();
        }
      } catch (IOException e) {
        System.out.println(e);
      }
    }
  }
}
