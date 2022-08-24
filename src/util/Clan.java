package util;

import static main.Main.load;

import java.awt.Image;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
	public Clan(String clanResult, boolean loadPlayers) throws MalformedURLException, IOException {
		name = getClanName(clanResult);
		tag = getClanTag(clanResult);
		badge = getClanBadge(clanResult);
	
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
	public Image getBadge() {
		return badge;
	}
	public Player[] getPlayers() {
		return players.toArray(new Player[0]);
	}
	public void loadPlayers() throws IOException {
		players = getClanPlayers(tag);
	}
	public void onlyPlayersByName(String name, boolean matchExact) {
		players.removeIf(new Predicate<Player>() {

			@Override
			public boolean test(Player p) {
				if(matchExact)
					return !p.getName().equals(name);
				else
					return !p.getName().toLowerCase().contains(name.toLowerCase());
			}
		});
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
	
	public String toString() {
		return "Clan@" + name + "(" + tag + ")";
	}
}
