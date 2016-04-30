package werewolf.server;

/**
 *
 * @author Husni
 */
public class Player {
    private String username;
    private int id;
    private int isAlive;
    
    
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
    
    public int getId() {
        return id;
    }
    
    public int getAlive() {
        return isAlive;
    }
    
    public void die() {
        isAlive = 0;
    }
    
}
