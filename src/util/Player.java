package util;

import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jsoup.select.Elements;

public class Player {
	private String name, tag;
	private int trophies;
	private Map<ImageUtil, Integer> mainDeck, battleDeck;
	private Map<String, Integer> mainDeckURLs, battleDeckURLs;
	private UrlCache cache;
	
	/**
	 * will parse name and tag from html result
	 * 
	 *  @param playerResult	the html link(a) of a player result
	 * @throws IOException 
	 */
	public Player(String playerResult, UrlCache cache) throws IOException {
		tag = getPlayerTag(playerResult);
		name = getPlayerName(playerResult);
		mainDeck = new HashMap<>();
		battleDeck = new HashMap<>();
		mainDeckURLs = new HashMap<>();
		battleDeckURLs = new HashMap<>();
		trophies = -1;
		this.cache = cache;
	}
	public String getName() {
		return name;
	}
	public String getTag() {
		return tag;
	}
	public Optional<Integer> getTrophies() {
		if(trophies == -1)
			try {
				trophies = getPlayerTrophies(tag, cache);
			} catch (IOException e) {
				return Optional.empty();
			}
		return Optional.of(trophies);
	}
	/**
	 * returns a map with the cards' image-url as key and level as value; or an empty map if an IO Exception occurs
	 */
	public Map<String, Integer> getMainDeckURLs() {
		if(!mainDeckURLs.isEmpty())
			return mainDeckURLs;
		try {
			Elements cardsMainDeck, levelsMainDeck;
			var doc = cache.load("https://royaleapi.com/player/" + getTag() + "");
			if(doc.isEmpty()) return Map.of();
			cardsMainDeck = doc.get().select("img.deck_card");
			levelsMainDeck = doc.get().select("div.card-level");
	
			for(int i = 0; i < cardsMainDeck.size(); i++) {
				var card = cardsMainDeck.get(i).toString();
				var level = levelsMainDeck.get(i).toString();
				
				var urlPrefix = "src=\""; // src="
				var urlStart = card.indexOf(urlPrefix) + urlPrefix.length();
				var urlEnd = card.indexOf('"', urlStart);
				
				var levelEnd = level.lastIndexOf('<');
				var levelStart = level.lastIndexOf('>', levelEnd);
				var levelI = Integer.parseInt(level.substring(levelStart + 1, levelEnd - 1).trim());
				
				var imgURL = card.substring(urlStart, urlEnd);
				
				mainDeckURLs.put(imgURL, levelI);
			}
		} catch (IOException e) {
			return Map.of();
		}
		
		return mainDeckURLs;
	}
	public Map<ImageUtil, Integer> getMainDeck() throws IOException {
		if(!mainDeck.isEmpty())
			return mainDeck;

		for(var entry : getMainDeckURLs().entrySet()) {
			mainDeck.put(ImageUtil
				.loadURL(entry.getKey())
				.to(new AtomicReference<Image>()), 
			entry.getValue());
		}
		
		return mainDeck;
	}
	public Map<String, Integer> getBattleDeckURLs() {
		if(!battleDeckURLs.isEmpty())
			return battleDeckURLs;
		
		try {
			Elements cardsBattleDeck, levelsBattleDeck;
			var doc = cache.load("https://royaleapi.com/player/" + getTag() + "/battles");
			if(doc.isEmpty()) return Map.of();
			var deck0 = doc.get().select("div.ui.padded.grid");
			if(deck0.size() <= 0) {
				System.out.println("Battle deck not found");
				return battleDeckURLs;
			}
			cardsBattleDeck = deck0.get(0).select("img.deck_card");
			levelsBattleDeck = deck0.get(0).select("div.card-level");
			
			for(int i = 0; i < cardsBattleDeck.size(); i++) {
				var card = cardsBattleDeck.get(i).toString();
				var level = levelsBattleDeck.get(i).toString();
				
				var urlPrefix = "src=\""; // src="
				var urlStart = card.indexOf(urlPrefix) + urlPrefix.length();
				var urlEnd = card.indexOf('"', urlStart);
				
				var levelEnd = level.lastIndexOf('<');
				var levelStart = level.lastIndexOf('>', levelEnd);
				var levelI = Integer.parseInt(level.substring(levelStart + 1, levelEnd - 1).trim());
				
				var imgURL = card.substring(urlStart, urlEnd);
				
				battleDeckURLs.put(imgURL, levelI);
			}
		} catch(IOException e) {
			return Map.of();
		}
		
		return battleDeckURLs;
	}
	public Map<ImageUtil, Integer> getBattleDeck() throws IOException {
		if(!battleDeck.isEmpty())
			return battleDeck;
		
		for(var entry : getBattleDeckURLs().entrySet()) {
			battleDeck.put(ImageUtil
				.loadURL(entry.getKey())
				.to(new AtomicReference<Image>()), 
			entry.getValue());
		}
		
		return battleDeck;
	}
	
	public static String getPlayerTag(String player) {
		var tagPrefix = "data-tag=\"";
		var tagStart = player.indexOf(tagPrefix) + tagPrefix.length();
		var tagEnd = player.indexOf('"', tagStart);
		
		return player.substring(tagStart, tagEnd);
	}
	public static String getPlayerName(String player) {
		var nameStart = player.indexOf('>');
		var nameEnd = player.indexOf('<', nameStart);
		var nameQoute = player.substring(nameStart, nameEnd).trim(); // should return " player "
		
		return nameQoute.substring(1).trim();
	}
	public static int getPlayerTrophies(String tag, UrlCache cache) throws IOException {
		var elements = cache.load("https://royaleapi.com/player/" + tag);
		if(elements.isEmpty()) return -1;
		var profileContainer = elements.get().select("div.player__profile_header_container");
		var trophyItem = profileContainer.select("div.item").get(0).toString();
		
		var start = trophyItem.indexOf('>');
		var end = trophyItem.indexOf('/', start);
		var res = trophyItem.substring(start + 2, end).trim();
		
		return Integer.parseInt(res);
	}
	
	@Override
	public String toString() {
		return "Player@" + name + ":" + tag;
	}
}
