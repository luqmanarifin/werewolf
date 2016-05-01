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
import static werewolf.client.WerewolfClient.canVote;

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

    try {
      InetAddress IPAddress = InetAddress.getByName(address);
      int targetPort = udpPort;

      DatagramSocket datagramSocket = new DatagramSocket();
      UnreliableSender unreliableSender = new UnreliableSender(datagramSocket);

      byte[] sendData = message.toJSONString().getBytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
      unreliableSender.send(sendPacket);

      datagramSocket.close();
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
  
  /**
   * *************** CLIENT TO CLIENT REQUEST & RESPONSE ***************
   */
  public static void prepareProposalReq() {
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
    JSONObject response = new JSONObject();
    response.put("method", "accept_proposal");
    ArrayList<Integer> arr = new ArrayList<Integer>();
    arr.add(WerewolfClient.numProposal);
    arr.add(WerewolfClient.me.id);
    response.put("proposal_id", arr);
    response.put("kpu_id", WerewolfClient.me.id);
    sendUDPMessage(response, address, port);
  }

  public static void acceptProposalRes(JSONObject obj) {
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
    response.put("description", "accepted");
    sendToKpu(response);
  }

  /**
   * Dikirimkan oleh pemain ke KPU ketika melakukan voting siapa yang akan
   * dibunuh di malam hari
   */
  public static void voteWerewolfReq() {
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
    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("description", "");
    sendUDPMessage(response, address, port);
  }

  /**
   * Dikirimkan oleh pemain ke KPU ketika melakukan voting siapa yang akan
   * dibunuh di siang hari
   */
  public static void voteCivilianReq() {
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
    JSONObject response = new JSONObject();
    response.put("status", "ok");
    response.put("description", "");
    sendUDPMessage(response, address, port);
  }
 
  @Override
  public void run() {
    try {
      while (WerewolfClient.isConnected){
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("UDP Response: " + received);
        System.out.println("From: " + packet.getAddress() + " " + packet.getPort());
        System.out.println("");
      }
    } catch(Exception e) {
      System.out.println("Unconnecting UDP");
    }
    System.out.println("keluar dari UDP");
  }
    
}
