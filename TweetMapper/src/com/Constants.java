package com;

public final class Constants {
	public static final String TWEET_LIST_START = "{\"tweet\": [";
	public static final String TWEET_LIST_END = "]}";
	public static final String TWEET_JSON = "{0},{1},{2},{3},{4}";
	public static final String LATITUDE = "\"latitude\":\"{0}\"";
	public static final String LONGITUDE = "\"longitude\":\"{0}\"";	
	public static final String ID = "\"id\":\"{0,number,#}\"";
	public static final String USERNAME = "\"username\":\"{0}\"";
	public static final String CONTENT = "\"content\":\"{0}\"";
	
	public static final String CREATE_QUERY = "CREATE TABLE TWEETS (tweet_id BIGINT auto_increment, username VARCHAR(30) not null, content VARCHAR(500) not null, latitude DOUBLE not null, longitude DOUBLE not null, tweet_ts TIMESTAMP, primary key (tweet_id));";
	public static final String FETCH_TWEETS = "SELECT * from TWEETS WHERE tweet_id > ? ORDER BY tweet_id";
	public static final String FETCH_TWEETS_TAG = "SELECT * from TWEETS, TAGS WHERE TWEETS.tweet_id > ? AND TWEETS.tweet_id = TAGS.tweet_id AND TAGS.tag like ";
	public static final String FETCH_TWEETS_TAG_MULTI = "SELECT * from TWEETS, TAGS WHERE TWEETS.tweet_id > ? AND TWEETS.tweet_id = TAGS.tweet_id AND ";
	public static final String OPENING_BRACE = "(";
	public static final String CLOSING_BRACE = ")";
	public static final String LIKE_CLAUSE = "TAGS.tag like ";
	public static final String OR_CLAUSE = " OR ";
}
