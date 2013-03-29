package edu.cornell.cs5300.project1b;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ServerRPC implements Runnable{
	DatagramSocket rpcSocket;
	DatagramPacket recvPkt;
	byte[] inBuf;     //arguments callid + arguments
	
	ServerRPC() throws SocketException {
		rpcSocket = new DatagramSocket(51303);//TODO: HACK-port no. hardcoded. Remove.
		inBuf = new byte[Project1bService.MAXPACKETSIZE];
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
		System.out.println("In ServerRPC");
	}
	
	public void run() {
		System.out.println("RPC Thread started");
		while(true) {
			System.out.println(Project1bService.sessionTable);
			byte[] inBuf = new byte[600];
			try {
				System.out.println(rpcSocket.getLocalPort());
				System.out.println(rpcSocket.getLocalAddress());
				rpcSocket.receive(recvPkt);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//obtain opcode
			String[] arguments = new String(recvPkt.getData()).split("_");
			String s = new String(inBuf);
			System.out.println("buffer contents:");
			System.out.println(s);
			for(String si :arguments) {
				System.out.println(si);
			}
		    int opcode = Integer.valueOf(arguments[1]);
		    byte[] outBuf = null;
			
		    switch (opcode) {
				case Project1bService.SESSIONREAD :
					//Read session value from Session Table and populate into outBuf.
					System.out.println(arguments);
					String result = SessionRead(arguments);
					
					if(result == null) {
						continue;
					} else {
						outBuf = result.getBytes(); 
						System.out.println(outBuf);
					}
					break;
				case Project1bService.SESSIONWRITE:
					break;
				default:
					continue;
		    }
		
		    //call specific function
			InetAddress returnAddr = recvPkt.getAddress();
			int returnPort = recvPkt.getPort();
			System.out.println(returnAddr);
			System.out.println(returnPort);
			
			//here outBuf should contain the callID and results of the call
			DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
			try {
				System.out.println(new String(outBuf));
				rpcSocket.send(sendPkt);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String SessionRead(String[] arguments) {
		String sessionID = arguments[2];
		String sessionEntry = Project1bService.getSessionTableEntry(sessionID);
		System.out.println("session entry" + sessionEntry); 
		return sessionEntry;
		
	}
}
