import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

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
	
	ClientRPC() throws SocketException {
		//TODO Aaron: should this perhaps be private (as a kind of constructor helper)?
		rpcSocket = new DatagramSocket(); 
		serverPort = rpcSocket.getLocalPort();
		inBuf = new byte[Project1bService.MAXPACKETSIZE];
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
		this.callid = ""+(10000*serverPort);
		
	}
	
	ClientRPC(String arguments, InetAddress[] destAddrs, int[] destPorts) throws SocketException {
		String[] args = arguments.split("_");
		//TODO Aaron: should this call the above ClientRPC() method?
		
		this.opcode =  Integer.valueOf(args[0]);
		this.sessionID = args[1]+"_"+args[2]+"_"+args[3];
		this.version = Integer.valueOf(args[4]);
		this.destAddrs = destAddrs;
		this.destPorts = destPorts;
		outBuf = (callid+"_"+arguments).getBytes();
	}
	
	public void sendPacket(InetAddress addr, int destPort) throws IOException{
		DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, addr, destPort);
		System.out.println(sendPkt.getPort());
		rpcSocket.send(sendPkt);
	}
	
	public String receivePacket(){
		try {
			//do {
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				//System.out.println(recvPkt.getData());
			//} while(true); //TODO while(the callID in inBuf is not the expected one);
		} catch(InterruptedIOException iioe) {
			//timeout
			recvPkt = null;
		} catch(IOException ioe) {
			//other error
			//TODO retry receiving here
		}
		rpcSocket.close();
		return inBuf.toString();
	}
	
	//
	// SessionReadClient(sessionID, sessionVersionNum)
	// with multiple [destAddr, destPort] pairs
	//
	public String SessionReadClient() throws IOException {
		String result;
		for(int i = 0; i < destAddrs.length; i++) {
			//TODO destPort is the corresponding port of destPorts for the addr in destAddrs
			sendPacket(destAddrs[i], destPorts[i]);			
		}
		result = receivePacket();
		return result;
	}
	
	public String run() {
		if(this.opcode == Project1bService.SESSIONREAD){
			try {
				String sessionTableValue = SessionReadClient();
				int sessionTableVersion = Integer.valueOf(sessionTableValue.split("_")[0]);
				if(version == sessionTableVersion)
					return sessionTableValue;
				else
					return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}
