package util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class UrlCache {
	private static final int TIMEOUT = 30 * 1000, RETRY = 2 * 1000;
	private Map<String, Document> docCache;
	private int stop;
	
	public UrlCache() {
		docCache = new HashMap<>();
	}
	
	public Optional<Document> load(String url) throws IOException {
		int stopID = stop + 1;
		if(!docCache.containsKey(url)) {
			Document doc = null;
			do {
				if(stop == stopID) return Optional.empty();
				try {
					doc = Jsoup.connect(url).timeout(TIMEOUT).get();
					break;
				} catch(HttpStatusException e) {
					if(e.getStatusCode() != 429)
						throw e;
				}
				
				if(stop == stopID) return Optional.empty();
				try {
					Thread.sleep(RETRY);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			} while(doc == null);
			
			docCache.put(url, doc);
		}
		
		return Optional.of(docCache.get(url));
	}
	
	public void stop() {
		stop++;
	}
}
