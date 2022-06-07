package util;

public class Player {
	private String name, tag;
	
	/**
	 * will parse name and tag from html result
	 * 
	 *  @param playerResult	the html link(a) of a player result
	 */
	public Player(String playerResult) {
		tag = getPlayerTag(playerResult);
		name = getPlayerName(playerResult);
	}
	public String getName() {
		return name;
	}
	public String getTag() {
		return tag;
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
		
		return nameQoute.substring(2);
	}
	
	@Override
	public String toString() {
		return "Player@" + name + ":" + tag;
	}
}
