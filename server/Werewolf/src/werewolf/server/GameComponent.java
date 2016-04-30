package werewolf.server;

import java.util.ArrayList;

/**
 *
 * @author Husni
 */
public class GameComponent {
  public static final int MAX_CLIENT = 10;
  public static WerewolfServerThread[] threads = new WerewolfServerThread[MAX_CLIENT];
  public static Player[] players = new Player[MAX_CLIENT];
  public static int connectedPlayer = 0;
  
  public static boolean isGameStarted = false;
  
  public static int remainingVote;
  
  // menandakan malam/siang
  public static boolean isDay = false;
  
  // menandakan hari ke berapa
  public static int days = 0;
  
  public static String getTime() {
    return isDay? "day" : "night";
  }
}
