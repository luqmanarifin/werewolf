package werewolf.server;

import java.util.ArrayList;

/**
 *
 * @author Husni
 */
public class GameComponent {
    public static final int maxClient = 10;
    
    public static WerewolfServerThread[] threads = new WerewolfServerThread[maxClient];
    public static ArrayList<Player> players = new ArrayList<>();
   
}
