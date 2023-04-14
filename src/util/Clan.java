package util;

import static main.Main.load;

import java.awt.Image;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;

public class Clan {
	private String name, tag, html, badgeURL;
	private Image badge;
	private List<Player> players;
	
	/** 
	 * will parse name, tag and badge from html result
	 * 
	 *  @param clanResult	the html div of a clan result
	 */
	public Clan(String clanResult, boolean loadPlayers) throws MalformedURLException, IOException {
		name = getClanName(clanResult);
		tag = getClanTag(clanResult);
		this.html = clanResult;
//		badge = getClanBadge(clanResult);
	
		if(loadPlayers)
			loadPlayers();
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
	public Image getBadge() throws IOException {
		if(badge == null)
			badge = getClanBadge(html);
		return badge;
	}
	public Optional<String> getBadgeURL() {
		if(badgeURL == null)
			try {
				badgeURL = getClanBadgeURL(html);
			} catch (IOException e) {
				return Optional.empty();
			}
		return Optional.of(badgeURL);
	}
	public List<Player> getPlayers() {
		return List.copyOf(players);
	}
	public void loadPlayers() throws IOException {
		players = getClanPlayers(tag);
	}
	public List<Player> filterPlayers(String name, boolean matchExact) {
		players.removeIf(p -> 
			matchExact ? 
					!p.getName().equals(name) 
				  : !p.getName().toLowerCase().contains(name.toLowerCase())
		);
		
		return players;
	}
	
	public static List<Player> getClanPlayers(String tag) throws IOException {
		var doc = load("https://royaleapi.com/clan/" + tag);
		var players = new ArrayList<Player>();
		
		var playerElements = doc.select("a.member_link");
		for(int i = 0; i < playerElements.size(); i++) {
			var player = new Player(playerElements.get(i).toString());
			players.add(player);
		}
		
		return players;
	}
	
	public static Image getClanBadge(String clanHtml) throws IOException {
		var badgePrefix = "https://cdn.royaleapi.com/static/img/badge-fs8";
		int badgeStart = clanHtml.indexOf(badgePrefix);
		if(badgeStart < 0)
			throw new IllegalArgumentException("invalid clan result");
		String imageURL = clanHtml.substring(badgeStart, clanHtml.indexOf('"', badgeStart));
		try {
			return ImageUtil.loadURL(imageURL).sync();
		} catch (MalformedURLException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static String getClanBadgeURL(String clanHtml) throws IOException {
		var badgePrefix = "https://cdn.royaleapi.com/static/img/badge-fs8";
		int badgeStart = clanHtml.indexOf(badgePrefix);
		if(badgeStart < 0)
			throw new IllegalArgumentException("invalid clan result");
		String imageURL = clanHtml.substring(badgeStart, clanHtml.indexOf('"', badgeStart));
		
		return imageURL;
	}
	public static String getClanName(String clanHtml) {
		var doc = Jsoup.parse(clanHtml);
		
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
	
	public String toString() {
		return "Clan@" + name + "(" + tag + ")";
	}
}
