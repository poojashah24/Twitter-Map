package com;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;


@ApplicationPath("/search")
public class SearchEndpoint extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> classes=new HashSet<>();
		   classes.add(TweetSSEService.class);
		   return classes;
	}
	
	@Override
	   public Set<Object> getSingletons() {
	   Set<Object> singletons=new HashSet<>();
	   singletons.add(new SseFeature());
	   return singletons;
	}
}
