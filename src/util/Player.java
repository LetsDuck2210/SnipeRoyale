package util;

import java.io.IOException;

import org.jsoup.Jsoup;

public class Player {
	private String name, tag;
	private int trophies;
	
	/**
	 * will parse name and tag from html result
	 * 
	 *  @param playerResult	the html link(a) of a player result
	 * @throws IOException 
	 */
	public Player(String playerResult) throws IOException {
		tag = getPlayerTag(playerResult);
		name = getPlayerName(playerResult);
		trophies = -1;
	}
	public String getName() {
		return name;
	}
	public String getTag() {
		return tag;
	}
	public int getTrophies() throws IOException {
		if(trophies == -1)
			trophies = getPlayerTrophies(tag);
		return trophies;
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
	public static int getPlayerTrophies(String tag) throws IOException {
		var elements = Jsoup.connect("https://royaleapi.com/player/" + tag).get();
		var profileContainer = elements.select("div.player__profile_header_container");
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
