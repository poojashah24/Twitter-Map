package com;
import java.sql.Date;


public class Tweet {
	private int id;
	private String userName;
	private String content;
	private double longitude;
	private double latitude;
	private Date date;
	
	public Tweet( double lat, double lon){
		this.latitude = lat;
		this.longitude = lon;
	}
	
	public Tweet(int id, String userName, String content, java.util.Date createdAt, double lat, double lon) {
		this.id = id;
		this.userName = userName;
		this.content = content;
		this.latitude = lat;
		this.longitude = lon;
		this.date = new Date(createdAt.getTime());
	}
	
	public int getTweetId() {
		return this.id;
	}

	public String getUserName() {
		return this.userName;
	}
	
	public String getContent() {
		return this.content;
	}
	
	public double getLatitude(){
		return this.latitude;
	}
	
	public double getLongitude(){
		return this.longitude;
	}
	
	public Date getDate() {
		return this.date;
	}
}
