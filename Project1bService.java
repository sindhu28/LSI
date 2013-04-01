
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet implementation class HelloWorld
 */
@WebServlet("/Project1bService")
public class Project1bService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String START_MESSAGE = "Hello, User!";
	private static final String END_MESSAGE = "Bye!";
	private static final String COOKIE_NAME = "CS5300PROJ1SESSIONvn76";
	private static final int EXPIRATION_PERIOD = 300000; //5 minutes in milliseconds
	private static final int MAX_STRING_LENGTH = 460; 
	private static final int MAX_ENTRIES = 1000;
	private static final int SCHEDULER_TIMEOUT = 300000; //5 minutes in milliseconds
	private static AtomicInteger sessionID = new AtomicInteger();
	public static ConcurrentHashMap<String, String> sessionTable = new ConcurrentHashMap<String, String>();
	private Timer timer = new Timer();
	private static ConcurrentHashMap<String,Integer> memberSet = new ConcurrentHashMap<String,Integer>();
	private static int serverPort;
	private static String IPP;	
	//OPCODES FOR RPC 
	public static final int SESSIONREAD = 1000;
	public static final int SESSIONWRITE = 1001;
	public static final int SESSIONREMOVE = 1002;
	public static final int GETMEMBERSET = 1003;
	public static final int REMOVESTALE = 1004;
	public static final int NOOPCODE = 0;
	public static final int NOCALLID = 0;
	public static final int NOVERSION = 0;
	public static final String NOMESSAGE = "";
	public static final String NODATE = null;
	public static final int MAXPACKETSIZE = 100;
	public static final int RPCTIMEOUT = 1500;
	public static final String DUMMYIP = "0.0.0.0";
	public static final String DUMMYIPP = "0.0.0.0_0";
	public static final int TOU = 2000; //milliseconds
	public static final int DELTA = 1000; //milliseconds
	
	public static boolean CRASH = false;
	public static final int K = 3;
	
	//TODO:HACK
	//public static String myIP = "192.168.1.5";
	
	/**
	 * Returns the message in sessionTableEntry
	 */	
	public String getMessage(String value) {
		String message;
		try{
			message = value.split("_")[1];
		}catch (Exception e) {
			message = NOMESSAGE;
		}
		return message;
	}
	
	/**
	 * Returns the version in sessionTableEntry
	 */	
	public int getVersion(String value) {
		int version;
		try{
			version = Integer.valueOf(value.split("_")[0]);
		}catch (Exception e) {
			version = NOVERSION;
		}
		return version;
	}
	
	/**
	 * Returns the date in sessionTableEntry
	 */	
	public String getDate(String value) {
		String date;
		try{
			date = value.split("_")[2];
		}catch (Exception e) {
			date = NODATE;
		}
		return date;
	}
	
	/**
	 * Returns IPP of the server side
	 * @throws UnknownHostException 
	 */
	public static InetAddress getIP() throws UnknownHostException{
		return InetAddress.getByName(IPP.split("_")[0]);
	}
	
	/**
	 * Returns the IPP(IP + Port) of the server serving the request
	 * @throws UnknownHostException 
	 */
	public static String getIPP(){
		return IPP;
	}
	
	/**
	 * Returns the dummy IP address - IP_null
	 * @throws UnknownHostException 
	 */
	public static String getIPPNull(){
		return DUMMYIPP;
	}
	
	public static InetAddress getIPNull() throws UnknownHostException{
		return InetAddress.getByName(DUMMYIP);
	}
	
	/**
	 * Returns the value from Session table for sessionID 
	 */
	public static String getSessionTableEntry(String sessionID) {
		return sessionTable.get(sessionID);
	}
		
	/**
	 * Inner class RunTimer to schedule the thread for session table cleaning. 
	 */
	private class RunTimer extends TimerTask {
		   public void run() {
			   runSessionTableCleaner();  
			   //Schedule a timer to call session Table cleaner function
			   timer.schedule(new RunTimer(), SCHEDULER_TIMEOUT);
		   }
	    }
	
	/**
	 * Default constructor.
	 * @throws SocketException 
	 */
	public Project1bService() throws SocketException {
		//Initialize and schedule timer for cleaner thread
		//TODO:HACK
//		memberSet.add("192.168.1.7_51310");
		RunTimer runTimer = new RunTimer();
        timer.schedule(runTimer, SCHEDULER_TIMEOUT);
        ServerRPC server = new ServerRPC();
        serverPort = server.rpcSocket.getLocalPort();
        new Thread(server).start();
	}
	
	/**
	 * Given an array of cookies return the cookie if its name matches cookieName
	 * and cookieName is present in session table and return its value (String)
	 * @param cookies
	 * @param cookieName
	 * @return Cookie
	 * @throws UnsupportedEncodingException 
	 */
	private Cookie getCookie(Cookie[] cookies, String cookieName) throws UnsupportedEncodingException{
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
//				verify if cookie is valid
				Cookie cookie = cookies[i];
				if (cookieName.equals(cookie.getName()))
					return (cookie);
			}
		}
		return null;
	}
	
	/**
	 * Given a HttpServletRequest, return the sesionID of the cookie if present.
	 * Else return null
	 * 
	 * @param request
	 * @return String SID : Session ID
	 * @throws UnknownHostException 
	 * @throws SocketException 
	 * @throws UnsupportedEncodingException 
	 */
	private String getSessionID(HttpServletRequest request) throws UnknownHostException, SocketException, UnsupportedEncodingException {
		Cookie cookie = getCookie(request.getCookies(), COOKIE_NAME);
		String SID = null;
		String[] values;
		if (cookie != null) {
			values = getValuesFromCookie(cookie);
			SID = getSIDFromCookieValues(values);
		}
		return SID;
	}
	
	private String getSessionValue(HttpServletRequest request) throws UnknownHostException, SocketException, UnsupportedEncodingException{
		//check if SID is in other session tables AND if any of those session tables have the most recent value
		Cookie cookie = getCookie(request.getCookies(), COOKIE_NAME);
		String sessionTableValue = null;
		String[] values;
		String SID;
		int sessionTableVersion;
		int version;
		if(cookie != null){	
			values = getValuesFromCookie(cookie);
			SID = getSIDFromCookieValues(values);
			version = getversionFromCookieValues(values);
			sessionTableVersion = getVersion(sessionTable.get(SID));
			//check if SID is in local session table AND if the local table has the most recent value
			//TODO:HACK to work on local machine
			if(SID != null && sessionTable.containsKey(SID) && version != NOVERSION && version == sessionTableVersion){
				sessionTableValue = getSessionTableEntry(SID)+"_"+IPP;
			}
			else{
//				if(values != null && values.length > 7){
//					InetAddress[] destAddrs = {InetAddress.getByName(values[4]),InetAddress.getByName(values[6])};
//					int[] destPorts = {Integer.valueOf(values[5]), Integer.valueOf(values[7])};
//					sessionTableValue = RPCSessionTableLookup(SID, version, destAddrs, destPorts);
					sessionTableValue = RPCSessionTableLookup(SID, version, values);
//				}
			}
			
		}
		return sessionTableValue;		
	}
		
	/**
	 * Given a cookie sesionID, refer session table and return if cookie 
	 * has expired (stale) or not.
	 * 
	 * @param sessionID
	 * @return true or false
	 * @throws ParseException 
	 */
	private boolean isCookieStale(String sessionTableValue){
		//TODO 3. Check Timestamp with delta
		boolean stale = true;
		try {
			//compare date in cookie and date stored in sessionTable
			String values[] = sessionTableValue.split("_");
			Date oldDate = new SimpleDateFormat("MMMMM dd, yyyy hh:mm:ss a ", Locale.US).parse(values[2]);
			Timestamp oldTS = new Timestamp(oldDate.getTime());
			Timestamp currentTS = new Timestamp(new Date().getTime());
			//TOU and DELTA account for different in server clocks and network delays
			long diffTS = currentTS.getTime() - oldTS.getTime() - TOU - DELTA;
			if (diffTS < EXPIRATION_PERIOD)
				stale = false;  //Cookie is not stale
		}catch (Exception e) {
			stale = true;
		}
		//Cookie is stale by default
		return stale;
	}
	
	/**
	 * Given HttpServletRequest, HttpServletResponse and a String message, create a new 
	 * cookie if one is not present or has become stale or update the cookie value
	 * if cookie is found.
	 * 
	 * @param request
	 * @param response
	 * @param startMessage
	 * @throws SocketException 
	 * @throws UnknownHostException 
	 * @throws UnsupportedEncodingException 
	 */
	private String updateCookie(HttpServletRequest request, HttpServletResponse response, String startMessage) throws UnknownHostException, SocketException, UnsupportedEncodingException {
		Date date = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("MMMMM dd, yyyy hh:mm:ss a ", Locale.US);
		String time = ft.format(date);
		Cookie clientCookie = getCookie(request.getCookies(), COOKIE_NAME);	
		String SID = null;
		String value = null;
		int versionNo = NOVERSION;
		String IPP_primary = DUMMYIPP;
		String IPP_backup = DUMMYIPP;
		String IPP_stale = DUMMYIPP;
			
		if (clientCookie == null) { 
			//Create a new cookie for a new session if one does not exist 
			IPP_primary = InetAddress.getLocalHost().getHostAddress() + "_" +serverPort;
			//TODO:HACK
//			IPP_primary = myIP + "_" +serverPort;
			versionNo = 1;
			int session = sessionID.incrementAndGet(); 
			SID = ""+session+"_"+IPP_primary;
			value = ""+versionNo +"_" + startMessage + "_" +time;
			if(SID!=null) {
				sessionTable.put(SID, value);
			}
			
			IPP_backup = RPCSessionTableUpdate(SID, value);
			String cookieValue = SID + "_" + versionNo +"_"+ IPP_primary +"_"+ IPP_backup+"_"+IPP_stale;
			clientCookie = new Cookie(COOKIE_NAME, URLEncoder.encode(cookieValue, "UTF-8"));
		} else { 
			// Update the existing cookie with new values
			SID = getSessionID(request);
			String values[] = getValuesFromCookie(clientCookie);
			versionNo = getversionFromCookieValues(values);
			if(versionNo != NOVERSION)
				versionNo += 1;
			
			IPP_primary =  getPrimaryFromCookieValues(values);
			if(IPP_primary!=null && !IPP_primary.equals(IPP) && !memberSet.containsKey(IPP_primary) && !IPP_primary.equals(DUMMYIPP)){
				System.out.println("LOG: Adding " +IPP_primary +" to memberset");
				memberSet.put(IPP_primary,1);
			}
				
			IPP_backup =  getBackupFromCookieValues(values);
			if(IPP_backup!=null && !IPP_backup.equals(IPP) && !memberSet.containsKey(IPP_backup) && !IPP_backup.equals(DUMMYIPP)){
				System.out.println("LOG: Adding " +IPP_backup +" to memberset");
				memberSet.put(IPP_backup,1);
			}
			
			IPP_stale =  getstaleFromCookieValues(values);
			RPCSessionTableRemoveStale(IPP_stale, SID);
					
			addToMemberSet(IPP_primary, IPP_backup);	
			
			System.out.println("LOG: In updateCookie() - add to memberset: "+IPP_primary+" "+IPP_backup+" "+IPP_stale);
			
			value = versionNo +"_" + startMessage + "_" +time;
			if(SID!= null) {
				synchronized(this) {
					if(sessionTable.containsKey(SID))
						sessionTable.replace(SID, value);
					else
						sessionTable.put(SID, value);
				}
			}
			
			IPP_stale = IPP_backup;
			IPP_primary = InetAddress.getLocalHost().getHostAddress() + "_" +serverPort; //new IPP_primary
			IPP_backup = RPCSessionTableUpdate(SID, value);								 //new IPP_backup
			if(IPP_stale.equals(IPP_primary) || IPP_stale.equals(getIPPBackupFrom(IPP_backup)))
				IPP_stale = DUMMYIPP;
			String cookieValue = SID + "_" + versionNo +"_"+ IPP_primary +"_"+ IPP_backup+"_"+IPP_stale;
			clientCookie.setValue(URLEncoder.encode(cookieValue, "UTF-8"));
		}
		clientCookie.setMaxAge((int) (EXPIRATION_PERIOD/1000)); //in seconds
		response.addCookie(clientCookie);
		return IPP_backup;
	}
	
	private String getIPPBackupFrom(String iPP_backup) {
		// TODO Auto-generated method stub
		String result = null;
		try{
		    result = (iPP_backup.split("_")[0]+"_"+iPP_backup.split("_")[1]);
		}catch (Exception e){
			result = null;
		}
		return result;
	}

	private void addToMemberSet(String IPP_primary, String IPP_backup) {
		String memberset = RPCGetMemberSet(IPP_primary);
		if(memberset == "")
			memberset = RPCGetMemberSet(IPP_backup);
		if(memberset != ""){
			//add the IPPs to memberset
			String[] members = getValues(memberset);
			if(members != null){
				int length = members.length-1;
				if(length % 2 != 0)
					length = length - 1;
				length = length -2; //To account for incomplete IPP in the end of the packet
				if(length > 0){
					for(int i=1 ; i< length; i++){
						//add member to memberset
						String member = members[i] + members[i+1];
						memberSet.put(member,1);
						i++;
					}
				}
			}
		}
	}
	
	private String getSIDFromCookieValues(String[] values) {
		String SID;
		try{
			SID = values[0]+"_"+values[1]+"_"+values[2];
		}catch (Exception e) {
			SID = null;
		}
		return SID;	
	}

	private String getBackupFromCookieValues(String[] values) {
		String IPP_Backup;
		try{
			IPP_Backup = values[6]+"_"+values[7];
		}catch (Exception e) {
			IPP_Backup = DUMMYIPP;
		}
		return IPP_Backup;
	}
	
	private String getstaleFromCookieValues(String[] values) {
		String IPP_stale;
		try{
			IPP_stale = values[8]+"_"+values[9];
		}catch (Exception e) {
			IPP_stale = DUMMYIPP;
		}
		return IPP_stale;
	}

	private String getPrimaryFromCookieValues(String[] values) {
		String IPP_Primary;
		try{
			IPP_Primary = values[4]+"_"+values[5];
		}catch (Exception e) {
			IPP_Primary = DUMMYIPP;
		}
		return IPP_Primary;
	}

	private int getversionFromCookieValues(String[] values) {
		int version;
		try{
			version = Integer.valueOf(values[3]);
		}catch (Exception e) {
			version = NOVERSION;
		}
		return version;
	}

	private String[] getValuesFromCookie(Cookie clientCookie) {
		String[] values;
		try{
			String value = URLDecoder.decode(clientCookie.getValue(), "UTF-8").trim();
			values =value.split("_");
		}catch (Exception e) {
			values = null;
		}
		return values;
	}
	
	public static String[] getValues(String sessionTableValue) {
		String[] values;
		try{
			values = sessionTableValue.split("_");
		}catch (Exception e) {
			values = null;
		}
		return values;
	}
	
	private String extractCookieIPP(String sessionTableValue) {
		String[] values = getValues(sessionTableValue);
		String cookieIPP;
		try{
			cookieIPP = (values[values.length-2]+"_"+values[values.length-1]).trim();
		}catch (Exception e) {
			cookieIPP = DUMMYIPP;
		}
		return cookieIPP;
	}
	
	private String extractSessionTableValue(String sessionTableValue) {
		String value = "";
		String[] values = getValues(sessionTableValue);
		try{
			for(int i=0; i< values.length-3; i++)
				value += values[i]+"_";
			value += values[values.length-3];
		}catch (Exception e) {
			value = null;
		}
		return value;
	}

	private String RPCSessionTableUpdate(String SID, String value){
		//Sends RPC to all servers in ServerList
		//Returns the IPP of response from the first server
		InetAddress[] destAddrs = new InetAddress[memberSet.size()];
		int[] destPorts = new int[memberSet.size()];
		String IPP_backup = DUMMYIPP;
		int i=0;	
		String member;
		Iterator it = memberSet.entrySet().iterator();
		while (it.hasNext()) {
			member = ((Map.Entry<String, Integer>)it.next()).getKey();
			try {
				String[] values = member.split("_");
				destAddrs[i] = InetAddress.getByName(values[0]);
				destPorts[i] = Integer.valueOf(values[1].trim());
				i++;
			} catch (Exception e) {
				//do nothing
			}	
		}
		String arguments = SESSIONWRITE +"_" + SID +"_" + value.toString(); 
		ClientRPC client = new ClientRPC(arguments, destAddrs, destPorts);
		String result = client.run();
		try{
			int index = IPP_backup.indexOf("_");
			IPP_backup = IPP_backup.substring(index+1);
		}catch (Exception e) {
			//do nothing
		}
		return IPP_backup;
	}
	
	private void addIPPBackup(String iPP_backup) {
		String[] values = getValues(iPP_backup);
		System.out.println(values.toString());
		if(values != null){
			int length = values.length;
			if(length % 2 != 0)
				length = length-1;
			for(int i=0; i<length; i+=2){
				String ipp = values[i]+"_"+values[i+1];
				if(ipp!=null && !ipp.equals(IPP) && !memberSet.containsKey(ipp) && !ipp.equals(DUMMYIPP)){
					System.out.println("LOG: Adding " +ipp +" to memberset");
					memberSet.put(ipp,1);
				}
			}
		}
	}
	
//	private String RPCSessionTableLookup(String SID, int version, InetAddress[] destAddrs, int[] destPorts){
	private String RPCSessionTableLookup(String SID, int version, String[] values){
		//Looks up for a valid entry in IPP_primary and IPP_backup
		//It gets back values in response
		//Call RPCClient
		String result = null;
		if(values != null){
			int length = values.length;
			int size = 0;
			if(length > 4)
				size = (length-4)/2;
			InetAddress[] destAddrs =  new InetAddress[size];
			int[] destPorts =  new int[size];
//			Integer.valueOf(values[5]), Integer.valueOf(values[7])
			int idx = 0;
			for(int i=4; i<length; i+=2){
				//find all inetaddresses and ports
				try {
					destAddrs[idx] = InetAddress.getByName(values[i]);
					destPorts[idx] = Integer.valueOf(values[i+1]);
					idx++;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
			String arguments = SESSIONREAD+"_"+SID+"_"+version;
			ClientRPC client = new ClientRPC(arguments, destAddrs, destPorts);
			result = client.run();
		}
		return result;
	}
	
	private String RPCGetMemberSet(String iPP){
		String memberset = "";
		InetAddress[] destAddrs = new InetAddress[1];
		int[] destPorts = new int[1];
		if(iPP != null && iPP != IPP){
			try {
				String[] values = iPP.split("_");
				System.out.println(values.toString());
				destAddrs[0] = InetAddress.getByName(values[0]);
				destPorts[0] = Integer.valueOf(values[1].trim());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String arguments = ""+GETMEMBERSET; 
			ClientRPC client = new ClientRPC(arguments, destAddrs, destPorts);
			memberset = client.run();
		}	
		return memberset;
	}

	private void RPCSessionTableRemove(String SID){
		//Sends RPC to all servers in ServerList
		InetAddress[] destAddrs = new InetAddress[memberSet.size()];
		int[] destPorts = new int[memberSet.size()];
		int i=0;
		String member;
		Iterator it = memberSet.entrySet().iterator();
		while (it.hasNext()) {
			member = ((Map.Entry<String, Integer>)it.next()).getKey();
			try {
				String[] values = member.split("_");
				destAddrs[i] = InetAddress.getByName(values[0]);
				destPorts[i] = Integer.valueOf(values[1].trim());
				i++;
			} catch (Exception e) {
				//do nothing
			}	
		}
				
		String arguments = SESSIONREMOVE+"_"+SID;
		ClientRPC client = new ClientRPC(arguments, destAddrs, destPorts);
		client.run();
	}
	
	private void RPCSessionTableRemoveStale(String iPP_stale, String SID) {
		InetAddress[] destAddrs = new InetAddress[1];
		int[] destPorts = new int[1];
		if(iPP_stale != null){
			try {
				String[] values = iPP_stale.split("_");
				System.out.println(values.toString());
				destAddrs[0] = InetAddress.getByName(values[0]);
				destPorts[0] = Integer.valueOf(values[1].trim());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String arguments = REMOVESTALE+"_"+SID;
		ClientRPC client = new ClientRPC(arguments, destAddrs, destPorts);
		client.run();
	}
	
	/**
	 * Generate HTML markup
	 * @param startMessage
	 * @param hostname
	 * @param port
	 * @return markup
	 */
	protected String generateMarkup(String startMessage, String hostname, int port, String CookieIPP, String cookieBackup) {
		//Time Expiry is calculated at current + 10 minutes. 
		Date serverDate = new Date();
		TimeZone estTZ= TimeZone.getTimeZone("EST");
		Calendar estCal= Calendar.getInstance(estTZ);
		estCal.add(Calendar.HOUR, 1);
		SimpleDateFormat sdf= new SimpleDateFormat("MMMMM dd, yyyy hh:mm:ss a");
		sdf.setTimeZone(estTZ);
		Date estDate= estCal.getTime();
		String time = sdf.format(estDate);
		System.out.println("time: "+time);
		
		String members = "";
		Iterator<String> it = memberSet.keySet().iterator();
		while (it.hasNext()) {
			//Remove all stale(expired) cookie entries from Session Table
			try{
				String key = it.next();
				members += key+"    ";
			}catch (Exception e) {
				//do nothing
			}
	    }
		
		String markup = "<h2>"
				+ startMessage
				+ "</h2>"
				+ "<form action=\"\" method=\"post\"> "
				+ "<input style=\"display:inline;\"type=\"submit\" name=\"Action\" value=\"Replace\"/> <input type=\"text\" name=\"replace_string\"/></br><br/>"
				+ "<input type=\"submit\" name=\"Action\" value=\"Refresh\" /><br/><br/>"
				+ "<input type=\"submit\" name=\"Action\" value=\"Logout\" /><br/><br/>	"
				+ "<input type=\"submit\" name=\"Action\" value=\"Crash\" /><br/><br/></form>"
				+ "Session on " + hostname
				+ ":" + port + "<br/><br/>"
				+ "Found on IPP: " + CookieIPP.split("_")[0] + "<br/><br/>"
				+ "New IPP Primary: " + IPP + "<br/><br/>"
				+ "New IPP Backup: " + cookieBackup + "<br/><br/>"
				+ "Expires "+ time + " EST" + "<br/><br/>"
		        + "Member Set: " + members;
		
		return markup;
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		if(CRASH == true){
			try {
				Thread.sleep(1000000000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			} 
		}
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		String startMessage = START_MESSAGE;
		String SID = getSessionID(request);
		IPP = InetAddress.getLocalHost().getHostAddress()+"_"+serverPort;
		//TODO:HACK
//		IPP = myIP+"_"+serverPort;
		System.out.println("sessiontable: "+sessionTable);
		System.out.println("SID: "+SID);
		if(SID != null) {
			startMessage = getMessage(sessionTable.get(SID));
			if(startMessage == "")
				startMessage = START_MESSAGE;
		}
		
//		Give the user a cookie on first access to our service.
//		TODO: Hack - Removed to make server
//		String cookieBackup = IPP_null;
		String cookieBackup = updateCookie(request, response, startMessage);

		if(cookieBackup == DUMMYIPP)
			cookieBackup = "IPP_null";
		out.println(generateMarkup(startMessage, InetAddress.getLocalHost().getHostAddress(), request.getServerPort(),"NONE", cookieBackup));
		
		//TODO:HACK
//		out.println(generateMarkup(startMessage, myIP, request.getServerPort(),"NONE", cookieBackup));
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		if(CRASH == true){
			try {
				Thread.sleep(1000000000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			} 
		}
		
		System.out.println("sessiontable: "+sessionTable);
		PrintWriter out = response.getWriter();
		String SID = getSessionID(request);
		String startMessage = START_MESSAGE;
		response.setContentType("text/html");
		String action = request.getParameter("Action");
		String CookieIPP = "NONE";
		IPP = InetAddress.getLocalHost().getHostAddress()+"_"+serverPort;
		String sessionTableValue = null;
		Cookie cookie = getCookie(request.getCookies(), COOKIE_NAME);
		if (action.equals("Logout")) {
			//remove session table entry and print bye message
			synchronized(this) {
				if(SID!=null && sessionTable.containsKey(SID))
					sessionTable.remove(SID);
			}
			RPCSessionTableRemove(SID);
			//Deletes cookie from browser on logout
			cookie.setMaxAge(0);
			response.addCookie(cookie);
			out.println("<h2>"+END_MESSAGE+"</h2>");	
		} else if(action.equals("Crash")) {
				CRASH = true;
		} else {
			if(cookie != null){
				String[] values = getValuesFromCookie(cookie);
//			    String value =  URLDecoder.decode(cookie.getValue(), "UTF-8").trim();
			    if(values != null){
//			    	String[] values = value.split("_");
			    	String primary = getPrimaryFromCookieValues(values);
//			    	String primary = values[4]+"_"+values[5];
			    	String backup = getBackupFromCookieValues(values);
//			    	String backup = values[6]+"_"+values[7];
			    	String cache = IPP;
			
			    	sessionTableValue = getSessionValue(request);
			    	System.out.println("LOG: sessiontablevalue before: "+sessionTableValue);
			
			    	if(sessionTableValue != null){
//			    		tokens : (sessionTableValue + IPP) where it was found
//			    		String[] tokens = getValues(sessionTableValue);
//			    		System.out.println("LOG: tokens: "+Arrays.toString(tokens));
			    		CookieIPP = extractCookieIPP(sessionTableValue);
			    		sessionTableValue = extractSessionTableValue(sessionTableValue);
			    		System.out.println("LOG: sessiontablevalue after: "+sessionTableValue);
			    		
			    		System.out.println("LOG: cookieIP: "+CookieIPP+" "+CookieIPP.equals(cache)+" "+CookieIPP.equals(primary)+" "+CookieIPP.equals(backup));
			    		if(CookieIPP.equals(DUMMYIPP))
			    			CookieIPP = "NONE";
			    		else if(CookieIPP.equals(cache))
			    			CookieIPP = "cache";
			    		else if(CookieIPP.equals(primary))
			    			CookieIPP = "IPP_Primary";
			    		else if(CookieIPP.equals(backup))
			    			CookieIPP = "IPP_Backup";
			    		else
			    			CookieIPP = "NONE";			    		
			    	}
			    	
			    	//Extract replace string and set to startMessage
					if (action.equals("Replace")) {
						startMessage = request.getParameter("replace_string");
					}
			    	if(sessionTableValue == null) {
			    		cookie.setMaxAge(0);
						response.addCookie(cookie);
			    		out.println("<h2>"+"SessionTimeout occurred"+"</h2>");
			    		return;
			    	} else {
			    	if(!isCookieStale(sessionTableValue)) {
//			    		Refresh the page with the same text retained only if cookie is valid
						if(action.equals("Refresh")) {
							startMessage = getMessage(sessionTable.get(SID));
						}
					} else { 
//			    		Cookie is stale so remove entry from session table
						synchronized(this) {
							if(SID!=null && sessionTable.containsKey(SID)) {
								sessionTable.remove(SID);
							}
						}
					}
			    }
			    	//Validate startMessage
			    	//Ensure the entered message is <= MAX_STRING_LENGTH(512 bytes)
			    	if(startMessage.length() > MAX_STRING_LENGTH){
			    		startMessage = startMessage.substring(0, MAX_STRING_LENGTH);
			    	}
			   }
			}
			
			//Update cookie for all further actions except Logout
			String cookieBackup = updateCookie(request, response, startMessage);
			if(cookieBackup == DUMMYIPP)
				cookieBackup = "IPP_null";
			out.println(generateMarkup(startMessage, InetAddress.getLocalHost().getHostAddress(), request.getLocalPort(), CookieIPP, cookieBackup));			
			//TODO:HACK
//			out.println(generateMarkup(startMessage, myIP, request.getLocalPort(), CookieIPP, cookieBackup));
		}
	}	
		

	/**
	 * Cleans the session table based on the size of the table.
	 * @throws ParseException 
	 */
	private void runSessionTableCleaner(){
		//Clean the session table only if its size has exceeded beyond MAX_ENTRIES
		if(sessionTable.size() >= MAX_ENTRIES){
			synchronized(this) {
				Iterator<String> it = sessionTable.keySet().iterator();
				Timestamp currentTS = new Timestamp(new Date().getTime());
				while (it.hasNext()) {
					//Remove all stale(expired) cookie entries from Session Table
					try{
						String key = it.next();
						Date oldDate = new SimpleDateFormat("MMMMM dd, yyyy hh:mm:ss a ", Locale.US).parse(getDate(sessionTable.get(key)));
						Timestamp oldTS = new Timestamp(oldDate.getTime());
						long diffTS = currentTS.getTime() - oldTS.getTime();
						if (diffTS >= EXPIRATION_PERIOD){
							sessionTable.remove(key);
						}	
					}catch (Exception e) {
						//do nothing
					}
			    }
			}
		}
	}

	public static String setSessionTableEntry(String sessionID, String sessionValue) {
		if(sessionID == null || sessionValue == null) {
			return null;
		}
		sessionTable.put(sessionID, sessionValue);	
		return IPP;
	}

	public static synchronized void removeSessionTableEntry(String sessionID) {
		if(sessionID != null)
			sessionTable.remove(sessionID);		
	}
	
	public static String GetMemberSet() {
		String memberset = "";
		int i=0;
		String member;
		Iterator it = memberSet.entrySet().iterator();
		while (it.hasNext()) {
			member = ((Map.Entry<String, Integer>)it.next()).getKey();
			memberset += member+"_";
		}
		return memberset;
	}
	
}
