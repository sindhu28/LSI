package sample1;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

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
	private static final String END_MESSAGE = "Bye!";
	private static final String COOKIE_NAME = "CS5300PROJ1SESSION";
	private static final long EXPIRATION_PERIOD = 999999999;
	private String startMessage;
	private Cookie clientCookie;
	private static volatile int sessionID = 1;
	private int versionNo;
	private static ConcurrentHashMap<Integer, SessionTableValue> sessionTable = new ConcurrentHashMap<>();
	

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
	
	/**
	 * Default constructor.
	 */
	public HelloWorld() {
		// TODO Auto-generated constructor stub
		startMessage = "Hello, User!";
		versionNo = 1;

	}

	private String getCookieValue(Cookie[] cookies, String cookieName) {
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				System.out.println(cookie.getValue());
				if (cookieName.equals(cookie.getName()))
					return (cookie.getValue());
			}
		}
		return null;
	}
	
	private void updateCookie(HttpServletRequest request, HttpServletResponse response) {
		Date date = new Date();
		Timestamp tStamp = new Timestamp(date.getTime()+EXPIRATION_PERIOD);
		
		System.out.println("In doPost: "
				+ getCookieValue(request.getCookies(), COOKIE_NAME));
		
		  if (getCookieValue(request.getCookies(), COOKIE_NAME) == null) {
			  String cookieValue = "" + sessionID++ + " " + versionNo + " " + "location";
			  clientCookie = new Cookie(COOKIE_NAME, cookieValue);
			  SessionTableValue value = new SessionTableValue(versionNo, startMessage, date);
			  sessionTable.put(sessionID, value);
			  
		  } else {
		  
			  String cookieValue = getCookieValue(request.getCookies(), COOKIE_NAME); 
			  String sessionID = cookieValue.split("\\s+")[0]; 
			  cookieValue = sessionID + " " + versionNo++ + " location";
			  assert(clientCookie!=null);
			  System.out.println(clientCookie);
			  clientCookie.setValue(cookieValue);
			  SessionTableValue value = new SessionTableValue(versionNo, startMessage, date);
			  sessionTable.replace(Integer.valueOf(sessionID), value);
		  }
		  
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
		
		SimpleDateFormat ft = new SimpleDateFormat(
				"MMMMM dd, yyyy hh:mm:ss a zzz");
		System.out.println("In Get: " + getCookieValue(request.getCookies(), COOKIE_NAME));
		
		PrintWriter out = response.getWriter();

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
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		//TODO isCookieStale()
			//get oldDate from Map 
			//Convert oldDate to oldDateTS
			//find difference between tStamp and oldDateTS 
			//This should be less than or equal to EXPIRATION_PERIOD 
		
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		String action = request.getParameter("Action");
		out.println("<h2>" + action + "</h2>");
 
		if (action.equals("Replace") || action.equals("Refresh")) {
			if (!request.getParameter("replace_string").equals("")) {
				startMessage = request.getParameter("replace_string");
			} else {
				startMessage = DEFAULT_REPLACE_MESSAGE;
			}
			out.println("<h3>" + startMessage + "</h3>");
			// update session timeout and add/update cookie entry to table
			updateCookie(request, response);
			response.sendRedirect("/Practise/HelloWorld");
				
		} else if (action.equals("Logout")) {
			startMessage = END_MESSAGE;
			// update cookie table
		} else {	
			System.out.println("Code never reaches here!!!");
		}
		
	}
	
}
