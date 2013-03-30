import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

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
	
	ClientRPC(String arguments, InetAddress[] destAddrs, int[] destPorts) throws SocketException, UnsupportedEncodingException {
		String[] args = arguments.split("_");
		rpcSocket = new DatagramSocket(); 
		rpcSocket.setSoTimeout(Project1bService.RPCTIMEOUT);
		serverPort = rpcSocket.getLocalPort();
		inBuf = new byte[Project1bService.MAXPACKETSIZE];
		//outBuf = new byte[Project1bService.MAXPACKETSIZE];
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
		callid = ""+(10000*serverPort);
		this.opcode =  Integer.valueOf(args[0]);
		this.sessionID = args[1]+"_"+args[2]+"_"+args[3];
		this.version = Integer.valueOf(args[4]);
		this.destAddrs = destAddrs;
		this.destPorts = destPorts;
		outBuf = (this.callid+"_"+arguments).getBytes("UTF-8");
		String s = new String(outBuf);
	}
	
	public void sendPacket(InetAddress addr, int destPort) throws IOException{
		DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, addr, destPort);
		rpcSocket.send(sendPkt);
	}
	
	public String receivePacket(){
		boolean flag = true;
		String str = null;
		try {
			do {
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				String[] data = new String(recvPkt.getData()).split("_");
//				System.out.println(data[0]+"callid"+this.callid);
				if(data[0].equals(this.callid)) {
					flag = false;
					str = new String(recvPkt.getData());
				}
			} while(flag); //TODO while(the callID in inBuf is not the expected one);
		} catch(SocketTimeoutException e) {
			System.out.println("RPC Timeout occurred. Deleting session info from sessionTable");
			rpcSocket.close();
			return null;
		} catch(InterruptedIOException iioe) {
			//timeout
//			System.out.println("IO exception");
			recvPkt = null;
		} catch(IOException ioe) {
			//other error
			//TODO retry receiving here
//			System.out.println("error");
		}
		rpcSocket.close();
		return str;
	}
	
	//
	// SessionReadClient(sessionID, sessionVersionNum)
	// with multiple [destAddr, destPort] pairs
	//
	public String SessionReadClient() throws IOException {
		String result;
		for(int i = 0; i < destAddrs.length; i++){
			//TODO destPort is the corresponding port of destPorts for the addr in destAddrs
			//TODO: HACK
			destAddrs[0] = InetAddress.getByName("192.168.1.9");
			destPorts[0] = 51310;
			//System.out.println("destaddr: "+destAddrs[i]+"   "+Project1bService.getIPP());
			if(destAddrs[i].equals("/"+Project1bService.getIPP()) || destAddrs[i].equals("/0.0.0.0")){
				//do nothing
			}
			else{
			    sendPacket(destAddrs[i], destPorts[i]);
			}
		}
		result = receivePacket();
		return result;
	}

	private String SessionBackup() throws IOException {
		for(int i = 0; i < destAddrs.length; i++){
			//TODO destPort is the corresponding port of destPorts for the addr in destAddrs
			//TODO: HACK
			destAddrs[0] = InetAddress.getByName("192.168.1.9");
			destPorts[0] = 51310;
			//System.out.println("destaddr: "+destAddrs[i]+"   "+Project1bService.getIPP());
			if(destAddrs[i].equals( Project1bService.getIPP())){
				//do nothing
			}
			else{
			    sendPacket(destAddrs[i], destPorts[i]);
			}
		}
		String result = receivePacket();
		return result;
	}
	
	public String run() {
//		System.out.println("opcode is : "+this.opcode+"----");
		if(this.opcode == Project1bService.SESSIONREAD){
			try {
//				System.out.println("before sessionReadClient");
				String sessionTableValue = SessionReadClient();
//				System.out.println("after sessionREADCLIENT");
				if(sessionTableValue == null) {
					return null;
				}
				int sessionTableVersion = Integer.valueOf(sessionTableValue.split("_")[0]);
				if(version == sessionTableVersion)
					return sessionTableValue;
				else
					return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
//				System.out.println("rpc failed");
			} 
		} else if(this.opcode == Project1bService.SESSIONWRITE) {
			try {
				String IPP_backup = SessionBackup();
				return IPP_backup;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
//				System.out.println("rpc failed");
			}
		}
		return null;
	}
}
