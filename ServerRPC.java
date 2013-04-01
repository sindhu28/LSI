
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Map;


public class ServerRPC implements Runnable{
	DatagramSocket rpcSocket;
	DatagramPacket recvPkt;
	byte[] inBuf;     //arguments callid + arguments
	byte[] outBuf;
	
	ServerRPC() throws SocketException {
//		HACK: To make server
//		rpcSocket = new DatagramSocket(51310);
		rpcSocket = new DatagramSocket();
		inBuf = new byte[Project1bService.MAXPACKETSIZE];
		outBuf = null;
		recvPkt = new DatagramPacket(inBuf, inBuf.length);
	}
	
	public void sendPacket(InetAddress returnAddr, int returnPort){
		//here outBuf should contain the callID and results of the call
		DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
		try {
			rpcSocket.send(sendPkt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run() {
		String result;
		int opcode;
		String callid;
		String[] arguments;
		
		while(true) {
			try {
				System.out.println("LOG: SERVER-waiting on SOCKET");
				System.out.println(Project1bService.sessionTable);
				rpcSocket.receive(recvPkt);
				if(Project1bService.CRASH == true){
					try {
						Thread.sleep(1000000000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
					} 
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			obtain opcode
			arguments = new String(recvPkt.getData()).split("_");
			opcode = Project1bService.NOOPCODE;
			callid = ""+Project1bService.NOCALLID;
			if(arguments.length >1){
				callid = arguments[0];
				try{
					opcode = Integer.valueOf(arguments[1]);
					}catch (Exception e) {
						//do nothing
				}	
			}
		    
//		  find addr and port to return result to
			InetAddress returnAddr = recvPkt.getAddress();
			int returnPort = recvPkt.getPort();
			
		    switch (opcode) {
				case Project1bService.SESSIONREAD :
					//Read session value from Session Table and populate into outBuf.
					result = callid + "_" + SessionRead(arguments);
					
					if(result != null) {
		     		    try {
							outBuf = result.getBytes("UTF-8");
						} catch (Exception e) {
//							 TODO Auto-generated catch block
//							e.printStackTrace();
						}
					}
					sendPacket(returnAddr, returnPort);
					break;
				case Project1bService.SESSIONWRITE:
					result = callid + "_" + SessionBackup(arguments);
					System.out.println("RESULT:"+result);
					if(result != null) {
						try {
							outBuf = result.getBytes("UTF-8");
						} catch (Exception e) {
//							 TODO Auto-generated catch block
//							e.printStackTrace();
						}
					}
					sendPacket(returnAddr, returnPort);
					break;
				case Project1bService.SESSIONREMOVE:
					SessionRemove(arguments);
					break;
				case Project1bService.GETMEMBERSET:
					result = callid + "_" + Project1bService.GetMemberSet();
					if(result != null) {
						try {
							outBuf = result.getBytes("UTF-8");
						} catch (Exception e) {
//							 TODO Auto-generated catch block
//							e.printStackTrace();
						}
					}
					break;
				case Project1bService.REMOVESTALE:
					RemoveStale(arguments);
					break;
				default:
					continue;
		    }
		}
	}



	private String SessionRead(String[] arguments) {
		String result = null;
		if(arguments.length > 4){
			String sessionID = arguments[2] + "_" + arguments[3] + "_" + arguments[4];
			String sessionEntry = Project1bService.getSessionTableEntry(sessionID);
			System.out.println("LOG: server-SessionRead: "+sessionEntry+"_"+Project1bService.getIPP());
			if(sessionEntry != null)
				result = sessionEntry+"_"+Project1bService.getIPP();
		}
		return result;
	}
	
	private String SessionBackup(String[] arguments){
		String IPP_backup = Project1bService.getIPPNull();
		if(arguments.length > 7){
			String key = arguments[2]+"_"+arguments[3]+"_"+arguments[4];
			String value = arguments[5]+"_"+arguments[6]+"_"+arguments[7];
			IPP_backup = Project1bService.setSessionTableEntry(key, value);
			System.out.println("LOG: server-SessionBackup: "+IPP_backup);
		}
		return IPP_backup;
	}
	
	private void RemoveStale(String[] arguments) {
		if(arguments.length > 4){
			String SID = arguments[2] + "_" + arguments[3] + "_" + arguments[4];
			Project1bService.removeSessionTableEntry(SID);	
		}
		
	}
	
	private void SessionRemove(String[] arguments) {
		if(arguments.length > 4){
			String SID = arguments[2] + "_" + arguments[3] + "_" + arguments[4];
			Project1bService.removeSessionTableEntry(SID);	
		}
	}
}
