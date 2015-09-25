
public final class Constants {
	public static final String TWEET_LIST_START = "{\"tweet\": [";
	public static final String TWEET_LIST_END = "]}";
	public static final String TWEET_JSON = "{0},{1}";
	public static final String LATITUDE = "\"latitude\":\"{0}\"";
	public static final String LONGITUDE = "\"longitude\":\"{0}\"";	
	
	public static final String CREATE_QUERY = "CREATE TABLE TWEETS (tweet_id BIGINT, username VARCHAR(30) not null, content VARCHAR(500) not null, latitude DOUBLE not null, longitude DOUBLE not null, tweet_ts TIMESTAMP, primary key (tweet_id));";
	public static final String CREATE_TAGS_QUERY = "CREATE TABLE TAGS(tweet_id BIGINT not null, tag VARCHAR(30) not null, primary key(tweet_id, tag));";
	//public static final String INSERT_QUERY = "insert into TWEETS values (DEFAULT, ?, ?, ?, ?, ?);";
	public static final String INSERT_QUERY = "insert into TWEETS values (?, ?, ?, ?, ?, ?);";
	public static final String INSERT_TAG_QUERY = "insert into TAGS values (?,?);";
	public static final String GET_INDEX = "select max(tweet_id) as maxid from TWEETS;";
}
