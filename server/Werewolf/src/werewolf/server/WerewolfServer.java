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
  private static final GameComponent GAME_COMPONENT = new GameComponent();

  public static void main(String args[]) {
    int portNumber = 8080;
    
    // Membuat server socket
    try {
      serverSocket = new ServerSocket(portNumber);
    } catch (IOException e) {
      System.out.println(e);
    }
    System.out.println("Server started!");
    
    // Membuat socket baru setiap ada koneksi dari client
    while (true) {
      try {
        clientSocket = serverSocket.accept();
        int i = 0;
        for (i = 0; i < GAME_COMPONENT.MAX_CLIENT; i++) {
          if (GameComponent.threads[i] == null) {
            (GameComponent.threads[i] = new WerewolfServerThread(clientSocket, GAME_COMPONENT, i)).start();
            break;
          }
        }
        if (i == GAME_COMPONENT.MAX_CLIENT) {
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
