package edu.cornell.cs5300.project1a;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
@WebServlet("/Project1aService")
public class Project1aService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String START_MESSAGE = "Hello, User!";
	private static final String END_MESSAGE = "Bye!";
	private static final String COOKIE_NAME = "CS5300PROJ1SESSIONvn76";
	private static final int EXPIRATION_PERIOD = 60000; //10 minutes in milliseconds
	private static final int MAX_STRING_LENGTH = 512; //10 minutes in milliseconds
	private static final int MAX_ENTRIES = 1000;
	private static final int TIMEOUT_VALUE = 600000; //10 minutes in milliseconds
	private static AtomicInteger sessionID = new AtomicInteger();
	private static ConcurrentHashMap<Integer, SessionTableValue> sessionTable = new ConcurrentHashMap<Integer, SessionTableValue>();
	private Timer timer = new Timer();

	/**
	 * Inner class for Session Table 
	 */
	private class SessionTableValue {
		//Class to hold data in Session Table
		int version;
		String message;
		Date date;
	
		SessionTableValue(int version, String message, Date date) {
			this.version = version;
			this.message = message;
			this.date = date;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
		
	}
	
	/**
	 * Inner class RunTimer to schedule the thread for session table cleaning. 
	 */
	private class RunTimer extends TimerTask {
		   public void run() {
			   runSessionTableCleaner();
			  
			   //Schedule a timer to call session Table cleaner function
			   timer.schedule(new RunTimer(), TIMEOUT_VALUE);
		   }
	    }
	
	/**
	 * Default constructor.
	 */
	public Project1aService() {
		//Initialize and schedule timer for cleaner thread
        RunTimer runTimer = new RunTimer();
        timer.schedule(runTimer, TIMEOUT_VALUE);
	}
	
	/** 
	 * Given an array of cookies return the cookie if its name matches cookieName
	 * and cookieName is present in session table and return its value (String)
	 * @param cookies
	 * @param cookieName
	 * @return Cookie
	 */
	private Cookie getCookie(Cookie[] cookies, String cookieName){
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				int sessionID =  Integer.valueOf(cookie.getValue().split("\\s+")[0]);
				//verify if cookie is valid
				//check if there is a cookie returned and also if an entry exists in the sessionTable 
				if (cookieName.equals(cookie.getName()) && sessionTable.containsKey(sessionID))
					return (cookie);
			}
		}
		return null;
	}
	
	/**
	 * Given a HttpServletRequest, return the sesionID of the cookie if present.
	 * Else return -1
	 * 
	 * @param request
	 * @return int sessionID
	 */
	private int getSessionID(HttpServletRequest request) {
		Cookie cookie = getCookie(request.getCookies(), COOKIE_NAME); 
		if (cookie != null) {
			String value = cookie.getValue();
			if(value!= null) {
				int sessionID =  Integer.valueOf(value.split("\\s+")[0]);
				if(sessionTable.containsKey(sessionID))
				    return Integer.valueOf(value.split("\\s+")[0]);
			}
		}
		return -1;
	}
	
	/**
	 * Given a cookie sesionID, refer session table and return if cookie 
	 * has expired (stale) or not.
	 * 
	 * @param sessionID
	 * @return true or false
	 */
	private boolean isCookieStale(int sessionID) {
		if(sessionTable.containsKey(sessionID)) {
			//compare date in cookie and date stored in sessionTable
			Date oldDate = sessionTable.get(sessionID).getDate();
			Timestamp oldTS = new Timestamp(oldDate.getTime());
			Timestamp currentTS = new Timestamp(new Date().getTime());
			long diffTS = currentTS.getTime() - oldTS.getTime();
			
			if (diffTS >= EXPIRATION_PERIOD) { //Cookie is stale
				return true;
			} else { //Cookie is not stale
				return false;
			}
		}
		//Cookie is stale by default
		return true;
	}
	
	/**
	 * Given HttpServletRequest, HttpServletResponse and a String message, create a new 
	 * cookie if one is not present or has become stale or update the cookie value
	 * if cookie is found.
	 * 
	 * @param request
	 * @param response
	 * @param startMessage
	 */
	private void updateCookie(HttpServletRequest request, HttpServletResponse response, String startMessage) {
		Date date = new Date();
		Cookie clientCookie = getCookie(request.getCookies(), COOKIE_NAME);	
		if (clientCookie == null) { //Create a new cookie for a new session if one does not exist 
			int versionNo = 1;
			int session = sessionID.incrementAndGet();
			  
			//Location metadata will be appropriately added when needed
			String cookieValue = "" + session + " " + versionNo + " " + "location";
			clientCookie = new Cookie(COOKIE_NAME, cookieValue);
			SessionTableValue value = new SessionTableValue(versionNo, startMessage, date);
			sessionTable.put(session, value);
		} else { // Update the existing cookie with new values
			int sessionID = getSessionID(request);
			int versionNo = (sessionTable.get(sessionID).getVersion())+1;
			  
			//Location metadata will be appropriately added when needed
			String cookieValue = Integer.toString(sessionID) + " " + versionNo + " location";
			clientCookie.setValue(cookieValue);
			SessionTableValue value = new SessionTableValue(versionNo, startMessage, date);
			sessionTable.replace(sessionID, value);
		}
		clientCookie.setMaxAge((int) (EXPIRATION_PERIOD/1000)); //in seconds
		response.addCookie(clientCookie);
	}

	/**
	 * Generate HTML markup
	 * @param startMessage
	 * @param hostname
	 * @param port
	 * @return markup
	 */
	protected String generateMarkup(String startMessage, String hostname, int port) {
		//Time Expiry is calculated at current + 10 minutes. 
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MILLISECOND, EXPIRATION_PERIOD);
		date = cal.getTime();
				
		SimpleDateFormat ft = new SimpleDateFormat(
				"MMMMM dd, yyyy hh:mm:ss a zzz");	
		
		String markup = "<h2>"
				+ startMessage
				+ "</h2>"
				+ "<form action=\"\" method=\"post\"> "
				+ "<input style=\"display:inline;\"type=\"submit\" name=\"Action\" value=\"Replace\"/> <input type=\"text\" name=\"replace_string\"/></br><br/>"
				+ "<input type=\"submit\" name=\"Action\" value=\"Refresh\" /><br/><br/>"
				+ "<input type=\"submit\" name=\"Action\" value=\"Logout\" /><br/><br/></form>"
				+ "Session on " + hostname
				+ ":" + port + "<br/><br/>" + "Expires "
				+ ft.format(date);
		
		return markup;
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		
		PrintWriter out = response.getWriter();
		String startMessage = START_MESSAGE;
		int sessionID = getSessionID(request);
		
		if(sessionID != -1) {
			startMessage = sessionTable.get(sessionID).getMessage();
		}
		
		//Give the user a cookie on first access to our service.
		updateCookie(request, response, startMessage);
		
		out.println(generateMarkup(startMessage, InetAddress.getLocalHost().getHostName(), request.getLocalPort()));
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		int sessionID = getSessionID(request);
		
		String startMessage = START_MESSAGE;
		response.setContentType("text/html");
		String action = request.getParameter("Action");
 
		if (action.equals("Logout")) {
			//remove session table entry and print bye message
			sessionTable.remove(sessionID);
			out.println("<h2>"+END_MESSAGE+"</h2>");	
		} else {
			//Extract replace string and set to startMessage
			if (action.equals("Replace")) {
				startMessage = request.getParameter("replace_string");
			}
			
			//Handle valid and stale(expired) cookies 
			if(!isCookieStale(sessionID)) {
				//Refresh the page with the same text retained only if cookie is valid
				if(action.equals("Refresh")) {
					startMessage = sessionTable.get(sessionID).getMessage();
				}
			} else { //Cookie is stale so remove entry from session table
				sessionTable.remove(sessionID);
			}
			
			//Validate startMessage
			//Ensure the entered message is <= MAX_STRING_LENGTH(512 bytes)
			if(startMessage.length() > MAX_STRING_LENGTH){
				startMessage = startMessage.substring(0, MAX_STRING_LENGTH);
			}
			
			//Update cookie for all further actions except Logout
			updateCookie(request, response, startMessage);
			
			out.println(generateMarkup(startMessage, InetAddress.getLocalHost().getHostName(), request.getLocalPort()));
		}
	}	
		
	/**
	 * Cleans the session table based on the size of the table.
	 */
	private void runSessionTableCleaner(){
		//Clean the session table only if its size has exceeded beyond MAX_ENTRIES
		if(sessionTable.size() >= MAX_ENTRIES){
			Iterator<Integer> it = sessionTable.keySet().iterator();
			Timestamp currentTS = new Timestamp(new Date().getTime());
			while (it.hasNext()) {
				//Remove all stale(expired) cookie entries from Session Table
				int key = it.next();
		    	Date oldDate = sessionTable.get(key).getDate();
		    	Timestamp oldTS = new Timestamp(oldDate.getTime());
		    	long diffTS = currentTS.getTime() - oldTS.getTime();
		    	if (diffTS >= EXPIRATION_PERIOD){
					sessionTable.remove(key);
				}	    	
		    }
		}
	}
}
