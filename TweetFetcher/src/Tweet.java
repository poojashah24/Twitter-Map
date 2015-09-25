import java.sql.Date;
import java.util.ArrayList;
import java.util.List;


public class Tweet {
	private String userName;
	private String content;
	private double longitude;
	private double latitude;
	private Date date;
	
	List<String> tags;
	
	public Tweet(String userName, String content, java.util.Date createdAt, double lat, double lon) {
		this.userName = userName;
		this.content = content;
		this.latitude = lat;
		this.longitude = lon;
		this.date = new Date(createdAt.getTime());
		this.tags = new ArrayList<String>();
	}

	public void addTag(String tag) {
		this.tags.add(tag);
	}
	
	public List<String> getTags() {
		return this.tags;
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
