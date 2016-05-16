/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package werewolf.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import static werewolf.client.WerewolfClient.canVote;
import static werewolf.client.WerewolfClient.kpuSelectedRes;

/**
 *
 * @author adinb
 */
public class DatagramReceiverThread implements Runnable{

  public static DatagramPacket packet;
  public static DatagramSocket socket;
  int listenPort;
  
  byte[] buf;

  public DatagramReceiverThread(int listenPort) {
    buf = new byte[4096];
    packet = new DatagramPacket(buf, buf.length);
    this.listenPort = listenPort;

    try {
      socket = new DatagramSocket(listenPort);
    } catch (SocketException ex) {
      Logger.getLogger(DatagramReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Send message pake UDP
   *
   * @param message JSON Object yang mau dikirim
   * @param address Alamat tujuan
   * @param udpPort Port tujuan
   */
  public static void sendUDPMessage(JSONObject message, String address, int udpPort) {
    System.out.println("send: " + message.toString());
    System.out.println("to: " + address + " " + udpPort);
    
    try {
      InetAddress IPAddress = InetAddress.getByName(address);
      int targetPort = udpPort;

      UnreliableSender unreliableSender = new UnreliableSender(socket);

      byte[] sendData = message.toJSONString().getBytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
      unreliableSender.send(sendPacket);
      
    } catch (UnknownHostException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    }

  }
  
  public static void sendUDPUnreliable(JSONObject message, String address, int udpPort) {
    System.out.println("send: " + message.toString());
    System.out.println("to: " + address + " " + udpPort);
    
    try {
      InetAddress IPAddress = InetAddress.getByName(address);
      int targetPort = udpPort;

      UnreliableSender unreliableSender = new UnreliableSender(socket);

      byte[] sendData = message.toJSONString().getBytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
      unreliableSender.unreliableSend(sendPacket);
      
    } catch (UnknownHostException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(WerewolfClient.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public static void sendToKpu(JSONObject message) {
    sendUDPMessage(message, WerewolfClient.kpu.udpAddress, WerewolfClient.kpu.udpPort);
  }
  
  public static void sendToAll(JSONObject message) {
    for (Player p : WerewolfClient.players) {
      sendUDPMessage(message, p.udpAddress, p.udpPort);
    }
  }
  
  public static void sendToAllUnreliable(JSONObject message) {
    for (Player p : WerewolfClient.players) {
      sendUDPUnreliable(message, p.udpAddress, p.udpPort);
    }
  }
  
  public static boolean isAlive(int id) {
    for(Player p : WerewolfClient.players) {
      if(p.id == id) {
        return p.isAlive == 1;
      }
    }
    return false;
  }
  
  public static boolean isCivilian(int id) {
    for(Player p : WerewolfClient.players) {
      if(p.id == id) {
        return p.role.equals("civilian");
      }
    }
    return false;
  }
  
  public static String getAddress(int id) {
    for(Player p : WerewolfClient.players) {
      if(p.id == id) {
        return p.udpAddress;
      }
    }
    return null;
  }
  
  public static int getPort(int id) {
    for(Player p : WerewolfClient.players) {
      if(p.id == id) {
        return p.udpPort;
      }
    }
    return 0;
  }
  
  /**
   * *************** CLIENT TO CLIENT REQUEST & RESPONSE ***************
   */
  public static void prepareProposalReq() {
    System.out.println("Prepare proposal request");
    JSONObject message = new JSONObject();
    message = new JSONObject();
    message.put("method", "prepare_proposal");
    
    ArrayList<Integer> arr = new ArrayList<Integer>();
    arr.add(++WerewolfClient.numProposal);
    arr.add(WerewolfClient.me.id);
    message.put("proposal_id", arr);
    
    sendToAll(message);
  }

  public static void prepareProposalRes(JSONObject obj, String address, int port) {
    System.out.println("Prepare proposal response");
    ArrayList<Integer> arr = new ArrayList<Integer>();
    JSONArray json = (JSONArray) obj.get("proposal_id");
    for(int i = 0; i < json.size(); i++) {
      arr.add(((Long)json.get(i)).intValue());
    }
    int past = WerewolfClient.lastKpu; 
    int newNum = arr.get(0);
    int newId = arr.get(1);
    if(newNum > WerewolfClient.lastProposal) {
      WerewolfClient.lastProposal = newNum;
      WerewolfClient.lastKpu = newId;
    } else if(newNum == WerewolfClient.lastProposal && newId > WerewolfClient.lastKpu){
      WerewolfClient.lastProposal = newNum;
      WerewolfClient.lastKpu = newId;
    }
    
    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("description", "accepted");
    if(past != -1) {
      response.put("previous_accepted", past);
    }
    sendUDPMessage(response, address, port);
  }

  public static void acceptProposalReq(String address, int port) {
    System.out.println("Accept proposal request");
    
    JSONObject response = new JSONObject();
    response.put("method", "accept_proposal");
    ArrayList<Integer> arr = new ArrayList<Integer>();
    arr.add(WerewolfClient.numProposal);
    arr.add(WerewolfClient.me.id);
    response.put("proposal_id", arr);
    response.put("kpu_id", WerewolfClient.me.id);
    System.out.println("SENDING MESSAGE WITH UNRELIABLE CONNECTION");
    sendUDPMessage(response, address, port);
  }

  public static void acceptProposalRes(JSONObject obj, String address, int port) {
    System.out.println("Accept proposal response");
    
    ArrayList<Integer> arr = new ArrayList<Integer>();
    JSONArray json = (JSONArray) obj.get("proposal_id");
    for(int i = 0; i < json.size(); i++) {
      arr.add(((Long)json.get(i)).intValue());
    }
    int newNum = arr.get(0);
    int newId = arr.get(1);
    int kpu_id = ((Long) obj.get("kpu_id")).intValue();
    
    if(kpu_id == WerewolfClient.lastKpu) {
      WerewolfClient.clientAcceptedReq();
    }
    
    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("description", "accepted proposal");
    sendUDPUnreliable(response, address, port);
  }

  /**
   * Dikirimkan oleh pemain ke KPU ketika melakukan voting siapa yang akan
   * dibunuh di malam hari
   */
  public static void voteWerewolfReq() {
    if(!WerewolfClient.canVote) {
      System.out.println("You cannot vote now");
      return;
    }
    if(WerewolfClient.me.isAlive == 0 || !WerewolfClient.me.role.equals("werewolf")) {
      System.out.println("Werewolf now voting...");
      return;
    }
    System.out.println("You are werewolf!");
    System.out.println("Who do you want to kill?");
    
    boolean valid = false;
    int vote = -1;
    while(!valid) {
      WerewolfClient.clientAddressReq();
      System.out.println("");
      
      vote = WerewolfClient.sc.nextInt();
      valid = isAlive(vote) && isCivilian(vote);
      if(!valid) {
        System.out.println("Number invalid. Or he is dead. Or he is werewolf");
        System.out.println("Please entry again");
      }
    }
    
    JSONObject message = new JSONObject();
    message.put("method", "vote_werewolf");
    message.put("player_id", vote);
    sendToKpu(message);
  }

  public static void voteWerewolfRes(JSONObject obj, String address, int port) {
    int player_id = ((Long) obj.get("player_id")).intValue();
    WerewolfClient.votes[player_id]++;
    WerewolfClient.allVote++;
    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("description", "");
    sendUDPMessage(response, address, port);
    WerewolfClient.voteResultWerewolfReq();
  }

  /**
   * Dikirimkan oleh pemain ke KPU ketika melakukan voting siapa yang akan
   * dibunuh di siang hari
   */
  public static void voteCivilianReq() {
    if (!WerewolfClient.canVote) {
      System.out.println("You cannot vote now");
      return;
    }
    if (WerewolfClient.me.isAlive == 0) {
      System.out.println("Civilian now voting...");
      return;
    }
    System.out.println("Who do you want to kill?");

    boolean valid = false;
    int vote = -1;
    while (!valid) {
      WerewolfClient.clientAddressReq();
      System.out.println("");

      vote = WerewolfClient.sc.nextInt();
      valid = isAlive(vote);
      if (!valid) {
        System.out.println("Number invalid. Or he is dead.");
        System.out.println("Please entry again");
      }
    }

    JSONObject message = new JSONObject();
    message.put("method", "vote_civilian");
    message.put("player_id", vote);
    sendToKpu(message);
  }

  public static void voteCivilianRes(JSONObject obj, String address, int port) {
    int player_id = ((Long) obj.get("player_id")).intValue();
    WerewolfClient.votes[player_id]++;
    WerewolfClient.allVote++;
    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("description", "");
    sendUDPMessage(response, address, port);
    WerewolfClient.voteResultCivilianReq();
  }
 
  @Override
  public void run() {
    JSONParser parser = new JSONParser();
    try {
      while (WerewolfClient.isConnected){
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("UDP Response: " + received);
        String address = packet.getAddress().getHostAddress();
        int port = packet.getPort();
        System.out.println("From: " + address + " " + port);
        System.out.println("");
        
        try {
          JSONObject obj = (JSONObject) parser.parse(received);
          String method = (String) obj.get("method");
          if(method != null) {
            switch(method) {
              case "prepare_proposal":
                prepareProposalRes(obj, address, port);
                break;
              case "accept_proposal":
                acceptProposalRes(obj, address, port);
                break;
              case "vote_civilian":
                voteCivilianRes(obj, address, port);
                break;
              case "vote_werewolf":
                voteWerewolfRes(obj, address, port);
                break;
              default:
                break;
            }
          } else {
            String status = (String) obj.get("status");
            if(status.equals("ok")) {
              WerewolfClient.canVote = false;
            }
            String desc = (String) obj.get("description");
            if(desc.equals("accepted")) {
              acceptProposalReq(address, port);
            }
          }
        } catch (Exception e) {
          System.out.println(e);
        }
      }
    } catch(Exception e) {
      System.out.println("Unconnecting UDP");
    }
    System.out.println("keluar dari UDP");
  }
    
}
