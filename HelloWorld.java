

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
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
	private static final int EXPIRATION_PERIOD = 100;
	private String startMessage;
	private Cookie clientCookie;
	private static volatile int sessionID = 1;
	private int versionNo;

	/**
	 * Default constructor.
	 */
	public HelloWorld() {
		// TODO Auto-generated constructor stub
		startMessage = "Hello, User!";
		versionNo = 1;

	}

	public String getCookieValue(Cookie[] cookies, String cookieName) {
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (cookieName.equals(cookie.getName()))
					return (cookie.getValue());
			}
		}
		return null;
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

		Date date = new Date();
		SimpleDateFormat ft = new SimpleDateFormat(
				"MMMMM dd, yyyy hh:mm:ss a zzz");
		System.out.println("In Get: " + getCookieValue(request.getCookies(), COOKIE_NAME));
		/*
		 * if (request.getCookies() == null) { String cookieValue = "" +
		 * sessionID++ + " " + versionNo++ + " " + ft.format(date) +
		 * startMessage; clientCookie = new Cookie(COOKIE_NAME, cookieValue);
		 * response.addCookie(clientCookie);
		 * 
		 * } else { System.out.println("Cookie extracted is : " +
		 * getCookieValue(request.getCookies(),COOKIE_NAME)); }
		 * if(getCookieValue(request.getCookies(), COOKIE_NAME) != null ) {
		 * System.out.println("In doGet : Cookie extracted is : " +
		 * getCookieValue(request.getCookies(),COOKIE_NAME)); }
		 */
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
		// TODO Auto-generated method stub
		System.out.println("In doPost");
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		String action = request.getParameter("Action");
		
		Date date = new Date();
		SimpleDateFormat ft = new SimpleDateFormat(
				"MMMMM dd, yyyy hh:mm:ss a zzz");
		
		if (getCookieValue(request.getCookies(), COOKIE_NAME) != null) {
			//check if expired and redirect to home page if required
			int i:
			String oldDate;
			for(i=2;i<=6;i++)
			    oldDate += cookieValue.split("\\s+")[i]+" ";
			
			ft.format(date)
			}

		if (action.equals("Replace")) {
			if (!request.getParameter("replace_string").equals("")) {
				startMessage = request.getParameter("replace_string");
			} else {
				startMessage = DEFAULT_REPLACE_MESSAGE;
			}
			out.println("<h3>" + startMessage + "</h3>");
			// update session timeout and add cookie entry to table
		} else if (action.equals("Refresh")) {
			// update session timeout
			// update cookie table
		} else if (action.equals("Logout")) {
			startMessage = END_MESSAGE;
			// update cookie table
		}
		getCookieValue(request.getCookies(), COOKIE_NAME);
		
		  if (getCookieValue(request.getCookies(), COOKIE_NAME) == null) {
		  String cookieValue = "" + sessionID++ + " " + versionNo + " " +
		  ft.format(date) + startMessage; clientCookie = new
		  Cookie(COOKIE_NAME, cookieValue);
		  
		  } else {
		  
		  //System.out.println("Cookie extracted is : " +
		  String cookieValue = getCookieValue(request.getCookies(), COOKIE_NAME); 
		  String sessionID = cookieValue.split("\\s+")[0]; 
		  cookieValue = sessionID + " " + this.versionNo++ + " " + ft.format(date) + startMessage;
		  System.out.println(cookieValue); 
		  clientCookie.setValue(cookieValue);
		  
		  }
		 
		response.addCookie(clientCookie);
		response.sendRedirect("/HelloWorld/HelloWorld");
		
	}
}
