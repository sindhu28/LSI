package edu.cornell.cs5300.Project1b;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ClientRPC {
	DatagramSocket rpcSocket;
	int serverPort;
	byte[] outBuf;     //arguments = opcode, sessionID = sessionNum+IPP, VersionNum
	byte[] inBuf;
	int opcode;
	String sessionID;
	int version;
	InetAddress[] destAddrs;
	int[] destPorts;
	String callid;
	DatagramPacket sendPkt;
	DatagramPacket recvPkt;
	String initArguments;
	
	ClientRPC(String arguments, InetAddress[] destAddrs, int[] destPorts){
		this.initArguments = arguments;
		this.destAddrs = destAddrs;
		this.destPorts = destPorts;
	}
	
	public void initRequest(){
		String[] args = Project1bService.getValues(this.initArguments);
		try {
			rpcSocket = new DatagramSocket();
			serverPort = rpcSocket.getLocalPort();
			rpcSocket.setSoTimeout(Project1bService.RPCTIMEOUT);
			callid = ""+(10000*serverPort);
		} catch (SocketException e) {
			rpcSocket = null;
			serverPort = 0;
			callid = null;
		} 
		
		inBuf = new byte[Project1bService.MAXPACKETSIZE];
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
		this.opcode =  getOpcode(args);
		this.sessionID = getSessionID(args);	
		this.version = getVersion(args);
		this.outBuf = getOutBuf(this.callid,this.initArguments);		
	}
	
	private byte[] getOutBuf(String callid, String arguments) {
		byte[] out;
		try {
			out = (callid+"_"+arguments).getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			out = null;
		}
		return out;
	}

	private int getVersion(String[] args) {
		int version;
		try{
			version = Integer.valueOf(args[4]);
		}catch (Exception e) {
			version = Project1bService.NOVERSION;
		}
		return version;
	}

	private String getSessionID(String[] args) {
		String sessionID;
		try{
			sessionID = args[1]+"_"+args[2]+"_"+args[3];
		}catch (Exception e) {
			sessionID = null;
		}
		return sessionID;
	}

	private int getOpcode(String[] args) {
		int opcode;
		try{
			opcode = Integer.valueOf(args[0]);
		}catch (Exception e) {
			opcode = Project1bService.NOOPCODE;
		}
		return opcode;
	}
	

	public void sendPacket(InetAddress returnAddr, int returnPort){
		DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
		try {
			rpcSocket.send(sendPkt);
		} catch(IOException e) {
//			System.out.println(e.getMessage());
		}
	}
	
	public String receivePacket(){
		boolean flag = true;
		String str = null;
		try {
			do {
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				String[] data = new String(recvPkt.getData()).split("_");
				if(data != null && data[0].equals(this.callid)) {
					flag = false;
					str = new String(recvPkt.getData());
				}
			} while(flag); //while(the callID in inBuf is not the expected one);
		} catch(SocketTimeoutException e) {
			System.out.println("LOG: RPC Timeout occurred. Deleting session info from sessionTable");
		} catch(Exception e) {
//		  Error	
		} 
		try{
		rpcSocket.close();
		} catch(Exception e) {
			//do nothing
		}
		return str;
	}
	
	public String run(){
		initRequest();
	    String result = null;
	    switch (this.opcode) {
		case Project1bService.SESSIONREAD :
			try {
				String sessionTableValue = SessionReadClient();
				int sessionTableVersion = Project1bService.NOVERSION;
				if(sessionTableValue != null) {
					int indexofcallid = sessionTableValue.indexOf("_");
					sessionTableValue = sessionTableValue.substring(indexofcallid+1);
					try{
						sessionTableVersion = Integer.valueOf(sessionTableValue.split("_")[0]);
						if(version == sessionTableVersion)
							result = sessionTableValue;
					}catch (Exception e) {
						result = null;
					}
				}
			} catch (IOException e) {
				result = null;
				System.out.println("LOG: rpc failed");
			} 
			break;
		case Project1bService.SESSIONWRITE:
			try {
//				System.out.println("In sessionWrite: opcode" +this.opcode);
				result = SessionBackup();
			} catch (IOException e) {
				result = null;
				System.out.println("LOG: rpc failed");
			}
			break;
		case Project1bService.SESSIONREMOVE:
			try {
				SessionRemoveClient();
			} catch (UnknownHostException e) {
				//do nothing
			}
			break;
		default:
			break;
	    }
		return result;  
	}
	
	//
	// SessionReadClient(sessionID, sessionVersionNum)
	// with multiple [destAddr, destPort] pairs
	//
	public String SessionReadClient() throws IOException {
		String result = null;
		int count = 0;
		for(int i = 0; i < destAddrs.length; i++){
//			TODO: Hack to sent locally
//			if(destAddrs.equals(Project1bService.getIPNull())){
			if(destAddrs[i].equals(Project1bService.getIP()) || destAddrs.equals(Project1bService.getIPNull())){
				//do nothing
				count++;
				System.out.println("LOG: NO RPC");
			}
			else{
				String s= ""+destAddrs[i];
				System.out.println("LOG: CLIENT addr: "+ s);
			    sendPacket(destAddrs[i], destPorts[i]);
			}
		}
		System.out.println("count: "+count + " length: "+destAddrs.length);
		if(count != destAddrs.length)
		    result = receivePacket();
		return result;
	}

	private String SessionBackup() throws IOException {
		for(int i = 0; i < destAddrs.length; i++){
//			TODO: Hack to sent locally
//			if(destAddrs.equals(Project1bService.getIPNull())){
			if(destAddrs[i].equals(Project1bService.getIP()) || destAddrs.equals(Project1bService.getIPNull())){
				//do nothing
			}
			else{
			    sendPacket(destAddrs[i], destPorts[i]);
			}
		}
		String result = receivePacket();
		return result;
	}
	
	private void SessionRemoveClient() throws UnknownHostException {
		// TODO Auto-generated method stub
		for(int i = 0; i < destAddrs.length; i++){
//			TODO: Hack to sent locally
//			if(destAddrs.equals(Project1bService.getIPNull())){
			if(destAddrs[i].equals(Project1bService.getIP()) || destAddrs.equals(Project1bService.getIPNull())){
				//do nothing
				System.out.println("LOG: NO RPC");
			}
			else{
				String s= ""+destAddrs[i];
				System.out.println("LOG: CLIENT addr: "+ s);
			    sendPacket(destAddrs[i], destPorts[i]);
			}
		}	
	}
}
