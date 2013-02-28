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
@WebServlet("/HelloWorld")
public class HelloWorld extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_REPLACE_MESSAGE = "This is a new message!";
	private static final String START_MESSAGE = "Hello, User!";
	private static final String END_MESSAGE = "Bye!";
	private static final String COOKIE_NAME = "CS5300PROJ1SESSION";
	private static final int EXPIRATION_PERIOD = 600000; //10 minutes in milliseconds
	private static final int MAX_ENTRIES = 1000;
	private static final int TIMEOUT_VALUE = 600000; //10 minutes in milliseconds
	private static AtomicInteger sessionID = new AtomicInteger();
	private static AtomicInteger numEntries = new AtomicInteger();
	private static ConcurrentHashMap<Integer, SessionTableValue> sessionTable = new ConcurrentHashMap<Integer, SessionTableValue>();
	private Timer timer = new Timer();

	private class SessionTableValue {
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
	
	private class RunTimer extends TimerTask {
		   public void run() {
			  System.out.println("in run Timer");
			  runSessionTableCleaner();
		      timer.schedule(new RunTimer(), TIMEOUT_VALUE);
		   }
	    }
	
	/**
	 * Default constructor.
	 */
	public HelloWorld() {
		System.out.println("in main");
        RunTimer runTimer = new RunTimer();
        timer.schedule(runTimer, TIMEOUT_VALUE);
	}

	private String getCookieValue(Cookie[] cookies, String cookieName) {
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				int sessionID =  Integer.valueOf(cookie.getValue().split("\\s+")[0]);
				//check if there is a cookie and also an entry in the sessionTable to verify if it is valid
				if (cookieName.equals(cookie.getName()) && sessionTable.containsKey(sessionID))
					return (cookie.getValue());
			}
		}
		return null;
	}
	
	private Cookie getClientCookie(Cookie[] cookies, String cookieName){
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (cookieName.equals(cookie.getName()))
					return cookie;
			}
		}
		return null;
	}
	
	private int getSessionID(HttpServletRequest request) {
		String value = getCookieValue(request.getCookies(), COOKIE_NAME);
		if(value!= null) {
			int sessionID =  Integer.valueOf(value.split("\\s+")[0]);
			if(sessionTable.containsKey(sessionID))
			    return Integer.valueOf(value.split("\\s+")[0]);
		}
		return -1;
	}
	
	
	private boolean isCookieStale(HttpServletRequest request) {
		int sessionID = getSessionID(request);
		if(sessionTable.containsKey(sessionID)) {
			//compare date in cookie and date stored in sessionTable
			Date oldDate = sessionTable.get(sessionID).getDate();
			Timestamp oldTS = new Timestamp(oldDate.getTime());
			//TODO try to get date from http request and not current system time
			Timestamp currentTS = new Timestamp(new Date().getTime());
			long diffTS = currentTS.getTime() - oldTS.getTime();
			if (diffTS >= EXPIRATION_PERIOD) {
				return true;
			}
		}
		//Cookie is stale or there is no cookie for the in the sessionTable(new session)
		return false;
	}
	
	private void updateCookie(HttpServletRequest request, HttpServletResponse response, String startMessage) {
		Date date = new Date();
		Cookie clientCookie;	
		  if (getCookieValue(request.getCookies(), COOKIE_NAME) == null) {
			  //Create a new cookie for a new session
			  int versionNo = 1;
			  int session = sessionID.incrementAndGet();
			  String cookieValue = "" + session + " " + versionNo + " " + "location";
			  clientCookie = new Cookie(COOKIE_NAME, cookieValue);
			  SessionTableValue value = new SessionTableValue(versionNo, startMessage, date);
			  sessionTable.put(session, value);
			  numEntries.incrementAndGet();
		  } else {
			  //update the existing cookie with new values
			  clientCookie = getClientCookie(request.getCookies(), COOKIE_NAME);
			  int sessionID = getSessionID(request);
			  int versionNo = (sessionTable.get(sessionID).getVersion())+1;
			  String cookieValue = Integer.toString(sessionID) + " " + versionNo + " location";
			  clientCookie.setValue(cookieValue);
			  SessionTableValue value = new SessionTableValue(versionNo, startMessage, date);
			  sessionTable.replace(sessionID, value);
		  }
		  clientCookie.setMaxAge((int) (EXPIRATION_PERIOD/1000)); //in seconds
		  response.addCookie(clientCookie);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		System.out.println("In doGet ");
		response.setContentType("text/html");
		System.out.println(sessionTable);
		 
		//Time Expiry is calculated at current + 100s. 
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MILLISECOND, EXPIRATION_PERIOD);
		date = cal.getTime();
		
		SimpleDateFormat ft = new SimpleDateFormat(
				"MMMMM dd, yyyy hh:mm:ss a zzz");
		
		PrintWriter out = response.getWriter();
		
		String startMessage = START_MESSAGE;
		
		int sessionID = getSessionID(request);
		if(sessionID != -1)
			startMessage = sessionTable.get(sessionID).getMessage();

		out.println("<h2>"
				+ startMessage
				+ "</h2>"
				+ "<form action=\"\" method=\"post\"> "
				+ "<input style=\"display:inline;\"type=\"submit\" name=\"Action\" value=\"Replace\"/> <input type=\"text\" name=\"replace_string\"/></br><br/>"
				+ "<input type=\"submit\" name=\"Action\" value=\"Refresh\" /><br/><br/>"
				+ "<input type=\"submit\" name=\"Action\" value=\"Logout\" /><br/><br/></form>"
				+ "Session on " + InetAddress.getLocalHost().getHostName()
				+ ":" + request.getLocalPort() + "<br/><br/>" + "Expires "
				+ ft.format(date));
		updateCookie(request, response, startMessage);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		PrintWriter out = response.getWriter();
		int sessionID = getSessionID(request);
		if(isCookieStale(request)) {
			//Check if cookie is stale
			System.out.println(sessionTable.size());
			sessionTable.remove(sessionID);
			numEntries.decrementAndGet();
			System.out.println(sessionTable.size());	
			out.println("<h2>"+END_MESSAGE+"</h2>");
			return;
		}
		
		String startMessage = START_MESSAGE;
		response.setContentType("text/html");
		String action = request.getParameter("Action");
 
		if (action.equals("Replace") || action.equals("Refresh")) {
			//Replace the text with user typed text
			if(action.equals("Replace")) {
				if (!request.getParameter("replace_string").equals("")) {
					//User typed message is displayed
					startMessage = request.getParameter("replace_string");
				} 
				else{
					//If user doesn't enter anything in textbox, nothing is displayed
					startMessage = "";
				}
			} 
			//Refresh the page with the same text retained
			if(action.equals("Refresh")) {
					if(sessionID == -1 ) {
						//User Clicks refresh before changing any text
						startMessage = DEFAULT_REPLACE_MESSAGE;		
					} else {
						//Previously entered message is displayed
						startMessage = sessionTable.get(sessionID).getMessage();
					}
			}
			updateCookie(request, response, startMessage);
			response.sendRedirect("/HelloWorld/HelloWorld");
				
		} else if (action.equals("Logout")) {
			//Logout the user
			System.out.println(sessionTable.size());
			sessionTable.remove(sessionID);
			numEntries.decrementAndGet();
			System.out.println(sessionTable.size());
			out.println("<h2>"+END_MESSAGE+"</h2>");
		} else {	
			System.out.println("Code never reaches here!!!");
		}
		
	}	
		
	private void runSessionTableCleaner(){
		if(numEntries.get() >= MAX_ENTRIES){
		Iterator<Integer> it = sessionTable.keySet().iterator();
		while (it.hasNext()) {
			int key = it.next();
	    	Date oldDate = sessionTable.get(key).getDate();
	    	
	    	Timestamp oldTS = new Timestamp(oldDate.getTime());
	    	Timestamp currentTS = new Timestamp(new Date().getTime());
	    	long diffTS = currentTS.getTime() - oldTS.getTime();
	    	if (diffTS >= EXPIRATION_PERIOD){
				sessionTable.remove(key);
				numEntries.decrementAndGet();
			}	    	
	    }
	    System.out.println("Cleaned table: " + sessionTable.size());
	}
  }
}
