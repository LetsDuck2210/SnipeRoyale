package util;

import java.awt.Image;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;

public class Clan {
	private String name, tag;
	private Image badge;
	private List<Player> players;
	
	/** 
	 * will parse name, tag and badge from html result
	 * 
	 *  @param clanResult	the html div of a clan result
	 */
	public Clan(String clanResult) throws MalformedURLException, IOException {
		name = getClanName(clanResult);
		tag = getClanTag(clanResult);
		badge = getClanBadge(clanResult);
		
		players = getClanPlayers(tag);
	}
	public Clan(String name, String tag, Image badge, List<Player> players) {
		this.name = name;
		this.tag = tag;
		this.badge = badge;
		this.players = players;
	}
	public String getName() {
		return name;
	}
	public String getTag() {
		return tag;
	}
	public Image getBadge() {
		return badge;
	}
	public Player[] getPlayers() {
		return players.toArray(new Player[0]);
	}
	
	public static List<Player> getClanPlayers(String tag) throws IOException {
		var doc = Jsoup.connect("https://royaleapi.com/clan/" + tag).get();
		var players = new ArrayList<Player>();
		
		var playerElements = doc.select("a.member_link");
		for(int i = 0; i < playerElements.size(); i++) {
			var playerElement = playerElements.get(i);
			players.add(new Player(playerElement.toString()));
		}
		
		return players;
	}
	
	public static Image getClanBadge(String clan) throws MalformedURLException, IOException {
		var badgePrefix = "https://cdn.royaleapi.com/static/img/badge-fs8";
		int badgeStart = clan.indexOf(badgePrefix);
		if(badgeStart < 0)
			throw new IllegalArgumentException("invalid clan result");
		String imageURL = clan.substring(badgeStart, clan.indexOf('"', badgeStart));
		return ImageIO.read(new URL(imageURL));
	}
	public static String getClanName(String clan) {
		var doc = Jsoup.parse(clan);
		
		var nameLink = doc.select("a.header").get(0).toString();
		var nameStart = nameLink.indexOf('>') + 1;
		var nameEnd = nameLink.indexOf('<', nameStart);
		
		return nameLink.substring(nameStart, nameEnd);
	}
	public static String getClanTag(String clan) {
		var tagPrefix = "data-clantag=\"";
		var tagStart = clan.indexOf(tagPrefix) + tagPrefix.length();
		var tagEnd = clan.indexOf('"', tagStart);
		
		return clan.substring(tagStart, tagEnd);
	}
}
