/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package werewolf.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 
  @Override
  public void run() {
    try {
      while (WerewolfClient.isConnected){
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("UDP Response: " + received);
      }
    } catch(Exception e) {
      System.out.println("Unconnecting UDP");
    }
    System.out.println("keluar dari UDP");
  }
    
}
