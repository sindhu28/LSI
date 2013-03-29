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
		rpcSocket = new DatagramSocket();
		inBuf = new byte[Project1bService.MAXPACKETSIZE];
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
	}
	
	public void run() {
		while(true) {
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
			String[] arguments = inBuf.toString().split("_");
		    int opcode = Integer.valueOf(arguments[1]);
		    byte[] outBuf = null;
			
		    switch (opcode) {
				case Project1bService.SESSIONREAD :
					//Read session value from Session Table and populate into outBuf.
					outBuf = SessionRead(arguments).getBytes(); 
					System.out.println(outBuf);
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
		return sessionEntry;
	}
}
