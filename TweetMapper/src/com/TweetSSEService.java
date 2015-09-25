package com;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

@Path("/")
public class TweetSSEService {
	private Connection connection = null;
	private TweetList tweetList = new TweetList();
	private Session session = null;
	private int lastRead = 0;
	private static EventOutput eventOuput;// = new EventOutput();
	private static final ScheduledExecutorService sch = Executors.newSingleThreadScheduledExecutor();
	
	public TweetSSEService() {
		String dbName = "tweetdatabase";
		String userName = "tweetmap";
		String password = "";
		String hostname = "clouddbinstance.ci8zhgzbiolo.us-east-1.rds.amazonaws.com";
		String port = "3306";
		
		String jdbcUrl = "jdbc:mysql://" + hostname + ":" + port + "/" + dbName
				+ "?user=" + userName + "&password=" + password;
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(jdbcUrl);
			if (connection != null) {
				System.out.println("Connected to RDS instance");
				java.util.logging.Logger.getLogger("WorkerServlet").info(
						"Connected to RDS instance");
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"Cannot find the driver in the classpath!", e);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@GET
	@Produces("text/event-stream")
	@Path("/hang/{filter}")
	public EventOutput getMessages(@PathParam("filter") String filter) {
		
		System.out.println("client request!" + filter);
		String[] params = filter.split(":");
		eventOuput = new EventOutput();
		lastRead = Integer.parseInt(params[params.length-1]);
		SearchTweetTask tweetTask = new SearchTweetTask(connection, eventOuput, filter, lastRead);
		sch.scheduleWithFixedDelay(tweetTask , 0, 15, TimeUnit.SECONDS);
		return eventOuput;
	}
}

class SearchTweetTask implements Runnable {
	private Connection connection;
	private EventOutput eventOutput;
	private String filter;
	private int lastRead = 0;
	private TweetList tweetList = new TweetList();
	
	public SearchTweetTask(Connection connection, EventOutput eventOutput, String filter, int lastRead) {
		this.connection = connection;
		this.eventOutput = eventOutput;
		this.filter = filter;
		this.lastRead = lastRead;
	}
	
	public void run() {
		String[] params = filter.split(":");
		//lastRead = Integer.parseInt(params[1]);
		System.out.println("lastRead:"+lastRead);
		
		try {
			PreparedStatement statement = null;
			String query = new String();
			System.out.print("params.length:" + params.length);
			if(params[0].equals("all"))
				statement = connection.prepareStatement(Constants.FETCH_TWEETS);
			else {
				if(params.length == 2 && !params[0].trim().isEmpty()) {
					query = Constants.FETCH_TWEETS_TAG + "\'%" + params[0] + "%\'";	
				} else if(params.length > 2){
					query = Constants.FETCH_TWEETS_TAG_MULTI + Constants.OPENING_BRACE;
					String clause = new String();
					for(int i=0; i<params.length-2; i++) {
						clause += Constants.LIKE_CLAUSE + "\'%" + params[i] + "%\'";
						clause += Constants.OR_CLAUSE;
					}
					clause += Constants.LIKE_CLAUSE + "\'%" + params[params.length-2] + "%\'";
					query += clause;
					query += Constants.CLOSING_BRACE;
				}
				else {
					System.out.println("No tag selected");
					OutboundEvent.Builder b = new OutboundEvent.Builder();
					b.mediaType(MediaType.APPLICATION_JSON_TYPE);
					b.data(String.class, "");
					OutboundEvent e = b.build();
					try {
						eventOutput.write(e);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					return;
				}
				statement = connection.prepareStatement(query);
			}
			statement.setInt(1, lastRead);
			System.out.println(statement.toString());
			ResultSet rs = statement.executeQuery();
			while(rs.next()) {
				//lastRead++;
				int id = rs.getInt("tweet_id");
				String username = rs.getString("username");
				String content = rs.getString("content").replaceAll("\\r\\n|\\r|\\n", " ");
				content = content.replaceAll("\r\n", " ");
				content = content.replace("\r"," ");
				content = content.replaceAll(" +"," ");	
				
				if(content.contains("\\")){
					content = content.replace("\\", "");

				}
				
				if(content.contains("\"")) {
					content = content.replaceAll("\"", "\\\\\"");
				}

				content = content.trim();
								
				double latitude = rs.getDouble("latitude");
				double longitude = rs.getDouble("longitude");
				String sentiment = rs.getString("sentiment");
				Date date = rs.getDate("tweet_ts");
				
				Tweet t = new Tweet(id, username, content, date, latitude, longitude, sentiment);
				tweetList.add(t);
				lastRead = rs.getInt("tweet_id");
				if(tweetList.size() == 1500)
					break;
				
			}
			
		}
		catch(SQLException sqle) {
			sqle.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		if (tweetList != null && !tweetList.isEmpty()) {
			String json = tweetList.toString();
			tweetList.clear();
			OutboundEvent.Builder b = new OutboundEvent.Builder();
			b.mediaType(MediaType.APPLICATION_JSON_TYPE);
			b.data(String.class, json);
			OutboundEvent e = b.build();
			try {
				System.out.println("sending output:lastRead" + lastRead);
				eventOutput.write(e);
				System.out.println("sent output:lastRead" + lastRead);
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch(Exception e1) {
				e1.printStackTrace();
			}
		}
		else {
			OutboundEvent.Builder b = new OutboundEvent.Builder();
			b.mediaType(MediaType.APPLICATION_JSON_TYPE);
			b.data(String.class, "");
			OutboundEvent e = b.build();
			try {
				eventOutput.write(e);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
