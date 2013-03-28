import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClientRPC {
	DatagramSocket rpcSocket;
	int serverPort;
	byte[] outBuf;     //arguments = callID, opcode, sessionID = sessionNum+IPP, VersionNum
	byte[] inBuf;
	String opcode;
	String callid;
	DatagramPacket sendPkt;
	DatagramPacket recvPkt;
	private final int SESSIONREAD = 1000;
	private final int maxPacketSize = 512;
	
	ClientRPC(int opcode, String arguments) throws SocketException {
		rpcSocket = new DatagramSocket(); 
		serverPort = rpcSocket.getLocalPort();
		inBuf = new byte[maxPacketSize];
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
		this.callid = ""+(10000*serverPort);
		this.opcode =  ""+opcode;
		outBuf = (callid+"_"+this.opcode+"_"+arguments).getBytes();
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
}
