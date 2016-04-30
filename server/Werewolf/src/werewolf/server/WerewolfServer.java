package werewolf.server;

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
  private static final int MAXCLIENT = 6;
  private static final GameComponent GAMECOMPONENT = new GameComponent();

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
        for (i = 0; i < MAXCLIENT; i++) {
          if (GameComponent.threads[i] == null) {
            (GameComponent.threads[i] = new WerewolfServerThread(clientSocket, GAMECOMPONENT)).start();
            break;
          }
        }
        if (i == MAXCLIENT) {
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
