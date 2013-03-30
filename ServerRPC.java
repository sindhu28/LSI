package edu.cornell.cs5300.project1b;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ServerRPC implements Runnable{
	DatagramSocket rpcSocket;
	DatagramPacket recvPkt;
	byte[] inBuf;     //arguments callid + arguments
	
	ServerRPC() throws SocketException {
		rpcSocket = new DatagramSocket(51320);//TODO: HACK-port no. hardcoded
		inBuf = new byte[Project1bService.MAXPACKETSIZE];
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
//		System.out.println("In ServerRPC");
	}
	
	public void run() {
		String result;
//		System.out.println("RPC Thread started");
		while(true) {
//			System.out.println("Session table--------");
			System.out.println(Project1bService.sessionTable);
//			System.out.println("SessionTableend00-=----------");
			byte[] inBuf = new byte[600];
			try {
				//System.out.println(rpcSocket.getLocalPort());
				//System.out.println(rpcSocket.getLocalAddress());
				rpcSocket.receive(recvPkt);
				System.out.println("received packet");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//obtain opcode
			String[] arguments = new String(recvPkt.getData()).split("_");
					
		    int opcode = Integer.valueOf(arguments[1]);
		    byte[] outBuf = null;
			
		    switch (opcode) {
				case Project1bService.SESSIONREAD :
					//Read session value from Session Table and populate into outBuf.
//					System.out.println("Session Read-----------");
//					System.out.println(arguments);
//					System.out.println("================");
					result = arguments[0] + "_" + SessionRead(arguments);
					
					if(result == null) {
						continue;
					} else {
						try {
							outBuf = result.getBytes("UTF-8");
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}						
					}
					break;
				case Project1bService.SESSIONWRITE:
					result = arguments[0] + "_" + SessionBackup(arguments);
					if(result == null) {
						continue;
					} else {
						try {
							outBuf = result.getBytes("UTF-8");
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					break;
				default:
					continue;
		    }
		
		    //call specific function
			InetAddress returnAddr = recvPkt.getAddress();
			int returnPort = recvPkt.getPort();
			
			//here outBuf should contain the callID and results of the call
			DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
			try {
				rpcSocket.send(sendPkt);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String SessionBackup(String[] arguments) {
		String key = arguments[2]+"_"+arguments[3]+"_"+arguments[4];
        String value = arguments[5]+"_"+arguments[6]+"_"+arguments[7];
		String IPP_backup = Project1bService.setSessionTableEntry(key, value);
//		System.out.println("Session entry" + IPP_backup);
		return IPP_backup;
	}

	private String SessionRead(String[] arguments) {
		String sessionID = arguments[2] + "_" + arguments[3] + "_" + arguments[4];
		String sessionEntry = Project1bService.getSessionTableEntry(sessionID);
//		System.out.println("sessionid: "+ sessionID+"-----");
//		System.out.println("session entry" + sessionEntry); 
		return sessionEntry;
		
	}
}
