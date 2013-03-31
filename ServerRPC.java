
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
	public int serverPort;
	public static boolean crashed = false;
	
	ServerRPC() throws SocketException {
		rpcSocket = new DatagramSocket();
		serverPort = rpcSocket.getLocalPort();
		inBuf = new byte[Project1bService.MAXPACKETSIZE];
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
	}
	
	public void teardown() {
		rpcSocket.close();
	}
	
	public void run() {
		String result;
		while(true) {
			if (crashed) return;
			/*System.out.println("Session table--------");
			System.out.println(Project1bService.sessionTable);
			System.out.println("SessionTableend00-=----------");
			byte[] inBuf = new byte[600];*/

			try {
				System.out.println("LOG: SERVER-waiting on SOCKET");
				rpcSocket.receive(recvPkt);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String[] arguments = new String(recvPkt.getData()).split("_");

			//obtain opcode
		    Project1bService.OPCODE opcode = Project1bService.OPCODE.lookup(Integer.valueOf(arguments[1]));
		    
			
		    byte[] outBuf = null;
			
		    switch (opcode) {
				case SESSIONREAD :
					//Read session value from Session Table and populate into outBuf.


					result = arguments[0] + "_" + sessionRead(arguments);
					
					if(result != null) {
		     		    try {
							outBuf = result.getBytes("UTF-8");
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
						
					break;
				case SESSIONWRITE:
					result = arguments[0] + "_" + sessionBackup(arguments);
					if(result != null) {
						try {
							outBuf = result.getBytes("UTF-8");
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					break;
				case SESSIONDELETE:
					sessionDelete(arguments);
					// no response needed or expected
					continue;
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
	
	private void sessionDelete(String[] arguments) {
		Project1bService.removeSessionTableEntry(arguments[2]);
	}

	private String sessionBackup(String[] arguments) {
		String key = arguments[2]+"_"+arguments[3]+"_"+arguments[4];
        String value = arguments[5]+"_"+arguments[6]+"_"+arguments[7];
		String IPP_backup = Project1bService.setSessionTableEntry(key, value);
		System.out.println("LOG: server-SessionBackup: "+IPP_backup);
		return IPP_backup;
	}

	private String sessionRead(String[] arguments) {
		String sessionID = arguments[2] + "_" + arguments[3] + "_" + arguments[4];
		String sessionEntry = Project1bService.getSessionTableEntry(sessionID);
		System.out.println("LOG: server-SessionRead: "+sessionEntry+"_"+Project1bService.getIPP());
		if(sessionEntry != null)
		    return sessionEntry+"_"+Project1bService.getIPP();
		else
			return null;
		
	}
}
