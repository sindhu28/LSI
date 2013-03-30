package edu.cornell.cs5300.project1b;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ServerRPC implements Runnable{
	public static boolean crashed = false; 
	//TODO Aaron: above is technically kind of hacky... Maybe fix
	// we could send an RPC call to our server instead.
	DatagramSocket rpcSocket;
	DatagramPacket recvPkt;
	byte[] inBuf;     //arguments callid + arguments
	public int serverPort;
	
	ServerRPC() throws SocketException {
		rpcSocket = new DatagramSocket();
		serverPort = rpcSocket.getLocalPort();
		inBuf = new byte[Project1bService.MAXPACKETSIZE];
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
		System.out.println("In ServerRPC");
	}
	
	public void run() {
		String result;
		System.out.println("RPC Thread started");
		while(true) {
			System.out.println("Session table--------");
			System.out.println(Project1bService.sessionTable);
			System.out.println("SessionTableend00-=----------");
			byte[] inBuf = new byte[600];
			try {
				//System.out.println(rpcSocket.getLocalPort());
				//System.out.println(rpcSocket.getLocalAddress());
				rpcSocket.receive(recvPkt);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (crashed) return;
			//obtain opcode
			String[] arguments = new String(recvPkt.getData()).split("_");
			
			/*String s = new String(inBuf);
			System.out.println("buffer contents:");
			System.out.println(s);
			for(String si :arguments) {
				System.out.println(si);
			}*/
		    Project1bService.OPCODE opcode = Project1bService.OPCODE.lookup(Integer.valueOf(arguments[1]));
		    
		    byte[] outBuf = null;
			
		    switch (opcode) {
				case SESSIONREAD :
					//Read session value from Session Table and populate into outBuf.
					System.out.println("Session Read-----------");
					System.out.println(arguments);
					System.out.println("================");
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
						System.out.println("Session read:" + new String(outBuf));
					}
					break;
					
				case SESSIONWRITE:
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
						System.out.println("Session write:" + new String(outBuf));
					}
					break;
				case SESSIONDELETE:
					continue; //TODO actually delete what we're supposed to
				default:
					continue; //TODO Aaron: I am wary of defaults...
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
		String IPP_backup = Project1bService.setSessionTableEntry(arguments[2], arguments[3]);
		System.out.println("Session entry" + IPP_backup);
		return IPP_backup;
	}

	private String SessionRead(String[] arguments) {
		String sessionID = arguments[2] + "_" + arguments[3] + "_" + arguments[4];
		String sessionEntry = Project1bService.getSessionTableEntry(sessionID);
		System.out.println("sessionid: "+ sessionID+"-----");
		System.out.println("session entry" + sessionEntry); 
		return sessionEntry;
		
	}
}
