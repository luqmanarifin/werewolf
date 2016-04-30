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
public class DatagramClientThread implements Runnable{

  DatagramPacket packet;
  DatagramSocket socket;
  byte[] buf;

  public DatagramClientThread() {
    buf = new byte[4096];
    packet = new DatagramPacket(buf, buf.length);
    try {
      socket = new DatagramSocket();
    } catch (SocketException ex) {
      Logger.getLogger(DatagramClientThread.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
 
  @Override
  public void run() {
    try {
      while (WerewolfClient.isPlaying){
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("UDP Response: " + received);
      }
    } catch (IOException ex) {
      Logger.getLogger(DatagramClientThread.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
    
}
