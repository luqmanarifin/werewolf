package werewolf.client;

/**
 *
 * @author Luqman
 */
public class Player {
    public String username;
    public int id;
    public int isAlive;
    public String role = "civilian";
    
    public int udpPort;
    public String udpAddress;
    
    public Player(Player p) {
      username = p.username;
      id = p.id;
      isAlive = p.isAlive;
      role = p.role;
      udpPort = p.udpPort;
      udpAddress = p.udpAddress;
    }
    
    public Player() {
        this.username = null;
        this.id = 0;
        isAlive = 0;
    }
    
    public Player(String username, int id) {
        this.username = username;
        this.id = id;
        this.isAlive = 1;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void reset() {
      isAlive = 1;
    }
    
    public int getId() {
        return id;
    }
    
    public int getAlive() {
        return isAlive;
    }
    
    public void die() {
        isAlive = 0;
    }
    
    public String getUdpAddress() {
        return udpAddress;
    }
    
    public int getUdpPort() {
        return udpPort;
    }
    
    public String getRole() {
      return role;
    }
}
