

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import twitter4j.FilterQuery;
import twitter4j.HashtagEntity;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Servlet implementation class StartupServlet
 */
public class StartupServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String O_AUTH_CONSUMER_KEY = "";
	private static final String O_AUTH_CONSUMER_SECRET = "";
	private static final String O_AUTH_ACCESS_TOKEN = "";
	private static final String O_AUTH_ACCESS_TOKEN_SECRET = "";
     
	private static final Logger logger = Logger.getLogger("StartupServlet");
	private static Connection connection;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public StartupServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		initDb();
		fetchTweets();
	}
	
	private void fetchTweets() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
	     cb.setDebugEnabled(true)
	       .setOAuthConsumerKey(O_AUTH_CONSUMER_KEY)
	       .setOAuthConsumerSecret(O_AUTH_CONSUMER_SECRET)
	       .setOAuthAccessToken(O_AUTH_ACCESS_TOKEN)
	       .setOAuthAccessTokenSecret(O_AUTH_ACCESS_TOKEN_SECRET);
	     
	    TweetList tweetList = new TweetList();
        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
	    StatusListener listener = new TwitterStatusListener(connection, tweetList);
	    TwitterFetcher fetcher = new TwitterFetcher(twitterStream);
	    twitterStream.addListener(listener);
	    Timer timer = new Timer();
	    timer.scheduleAtFixedRate(fetcher, 0, 210*1000);
	}
	
	
	private boolean initDb() {
		String dbName = "tweetdatabase";
		String userName = "tweetmap";
		String password = "Ganapat1";
		String hostname = "clouddbinstance.ci8zhgzbiolo.us-east-1.rds.amazonaws.com";
		String port = "3306";
		// String jdbcUrl =
		// "jdbc:mysql://aagn7d55hw9lgo.ci8zhgzbiolo.us-east-1.rds.amazonaws.com:3306/ebdb?user=twittmap&password=Ganapat1";
		String jdbcUrl = "jdbc:mysql://" + hostname + ":" + port + "/" + dbName
				+ "?user=" + userName + "&password=" + password;
	   
	// Load the JDBC Driver
		try {
			System.out.println("Loading driver...");
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Driver loaded!");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"Cannot find the driver in the classpath!", e);
		}
	   
		try {
			connection = DriverManager.getConnection(jdbcUrl);
			if (connection != null) {
				System.out.println("Connected to RDS instance");
				java.util.logging.Logger.getLogger("WorkerServlet").info(
						"Connected to RDS instance");
			}
			//Connection conn = null;
			  java.sql.Statement setupStatement = null;

			  try {
			    // Create connection to RDS instance
			    //conn = DriverManager.getConnection(jdbcUrl);
			    
			    // Create a table and write two rows
			    setupStatement = connection.createStatement();
			    
			    setupStatement.execute(Constants.CREATE_TAGS_QUERY);
			    setupStatement.execute(Constants.CREATE_QUERY);
			    
			    setupStatement.close();
			    
			  } catch (SQLException ex) {
			    // handle any errors
				  ex.printStackTrace();
			    System.out.println("SQLException: " + ex.getMessage());
			    System.out.println("SQLState: " + ex.getSQLState());
			    System.out.println("VendorError: " + ex.getErrorCode());
			  } finally {
			    //System.out.println("Closing the connection.");
			    //if (conn != null) try { conn.close(); } catch (SQLException ignore) {}
			  }
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}

class TwitterStatusListener implements StatusListener {
	private TweetList tweetList;
	Connection connection = null;
	Logger logger = Logger.getLogger("TwitterStatusListener");
	private int tweet_id;
	
	public TwitterStatusListener(Connection con, TweetList tweetList) {
		this.connection = con;
		this.tweetList = tweetList;
		
		try {
			Statement getStartIndexStatement = connection.createStatement();
			getStartIndexStatement.execute(Constants.GET_INDEX);
			ResultSet rs = getStartIndexStatement.getResultSet();
			if(rs.next()) {
				tweet_id = rs.getInt("maxid") + 1;
			}
			System.out.println("tweet_id:" + tweet_id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
			
	}
	@Override
    public void onStatus(Status status) {
    	if (status.getGeoLocation() != null) {
	        System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
	        HashtagEntity[] he = status.getHashtagEntities();
	        for(HashtagEntity e1 : he) {
	        	System.out.println("hashtag:" + e1.getText());
	        }
			System.out.println("@geo-location: "
					+ status.getGeoLocation().getLatitude() + " "
					+ status.getGeoLocation().getLongitude());
			
			Tweet tweet = new Tweet(status.getUser().getScreenName(), 
					status.getText(),
					status.getCreatedAt(),
					status.getGeoLocation().getLatitude(),
					status.getGeoLocation().getLongitude());
			
			for(HashtagEntity tagEntity: he) {
				tweet.addTag(tagEntity.getText());
			}
			
			tweetList.add(tweet);
			
			if(tweetList.size() >= 5) {
				System.out.println("Sending to DB now");
				PreparedStatement insertStatement = null;
				PreparedStatement insertTagStatement = null;
				try {
					insertStatement =  connection.prepareStatement(Constants.INSERT_QUERY);
					insertTagStatement = connection.prepareStatement(Constants.INSERT_TAG_QUERY);
					for(Tweet t : tweetList) {
						insertStatement.setInt(1, tweet_id);
						insertStatement.setString(2, t.getUserName());
						insertStatement.setString(3, t.getContent());
						insertStatement.setDouble(4, t.getLatitude());
						insertStatement.setDouble(5, t.getLongitude());
						insertStatement.setDate(6, t.getDate());
						
						insertStatement.addBatch();
						
						for(String tag : t.getTags()) {
							insertTagStatement.setInt(1, tweet_id);
							insertTagStatement.setString(2, tag);
							
							insertTagStatement.addBatch();
						}
						tweet_id++;
					}
					int[] res = insertStatement.executeBatch();
					int[] res1 = insertTagStatement.executeBatch();
					System.out.println("result on insertion:" + res);
					insertStatement.close();
				} catch(SQLException sqle) {
					sqle.printStackTrace();
					logger.info("DB error occurred while inserting");
				}
				finally {
					tweetList.clear();
				}
				
			}
    	}
		
    }
	
 	@Override
	public void onDeletionNotice(StatusDeletionNotice arg0) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onScrubGeo(long arg0, long arg1) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onException(Exception arg0) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onStallWarning(StallWarning arg0) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onTrackLimitationNotice(int arg0) {
		// TODO Auto-generated method stub
	}
}

class TwitterFetcher extends TimerTask {

	private TwitterStream twitterStream;
	
	public TwitterFetcher(TwitterStream twitterStream) {
		this.twitterStream = twitterStream;
	}
	
	@Override
	public void run() {
		try {
			FilterQuery fd = new FilterQuery();
	        String[] keywords = {"#music","#apple","#love","#friends","#samsung","#holiday","#trip","#dance","#party", "#family",
	        		"#friendship, #manu, #nyc, #monday"};
	        fd.track(keywords); 
	        
	        /*double[][] locations = {{-180.0d,-90.0d},{180.0d,90.0d}};
	        fd.locations(locations);*/

			twitterStream.filter(fd);
			//twitterStream.sample();
			Thread.sleep(180*1000);
			System.out.println("shutting down now");
			twitterStream.cleanUp();
			twitterStream.shutdown();
		} catch (Exception e) {
			System.err.println(e);
		}		
	}
	
}
