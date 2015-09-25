import java.text.MessageFormat;
import java.util.ArrayList;


public class TweetList extends ArrayList<Tweet> {
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(Constants.TWEET_LIST_START);
		for(Tweet t : this) {
			builder.append(convertToJSON(t) + ",");
		}
		
		String json = builder.substring(0, builder.length()-1);
		System.out.println("json:"+json);
		json += Constants.TWEET_LIST_END;
		System.out.println("json:"+json);
		
		System.out.println(json);
		
		return json;
	}
	
	private String convertToJSON(Tweet t){
		MessageFormat mf = new MessageFormat("{0,number,#.##}");
		String latitude = mf.format(Constants.LATITUDE, t.getLatitude());
		String longitude = mf.format(Constants.LONGITUDE, t.getLongitude());
		System.out.println(latitude);
		System.out.println(longitude);
		return "{ " + MessageFormat.format(Constants.TWEET_JSON, latitude, longitude) + " }";
	}
	
}
