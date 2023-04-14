package main;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import server.HttpStatus;
import server.Server;
import server.Session;
import util.Clan;
import util.DefaultLogger;
import util.FileUtil;
import util.Player;

public class SnipeServer {
	private static DefaultLogger logger;
	public static DefaultLogger getLogger() {
		if(logger == null)
			logger = new DefaultLogger("SnipeLogger");
		return logger;
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length < 1) {
			System.out.println("Expected at least 1 argument: <port> [debug-logfile]");
			return;
		}
		final int port = Integer.parseInt(args[0]);
		getLogger().setLevel(Level.INFO);
		if(args.length >= 2)
			getLogger().setLowlevelOutput(new File(args[1]));
		
		var server = new Server(port, getLogger());
		
		// non-regex will always match first
		server.route("/change", SnipeServer::change);
		server.route("/changec", SnipeServer::change);
		
		server.route("\\/\\w*", SnipeServer::index);
		//									 		  v   '+' for visible files only, '*' for all files (e.g.  /.DS_Store will only be accepted with '*')
		server.route("\\/assets\\/([\\w-]+\\/)*([\\w-]*\\.\\w+)?", SnipeServer::assets);
		server.start();
	}
	
	public static void index(String method, String resource, Session session) throws IOException {
		if(!method.equals("GET") && !method.equals("POST")) {
			getLogger().log(Level.WARNING, "405 " + method + " Not Allowed");
			session.sendStatus(HttpStatus.METHOD_NOT_ALLOWED);
			return;
		}
		
		switch(resource.substring(1).toLowerCase()) {
			case "clans" -> clans(session);
			case "clan" -> clan(session);
			case "player" -> player(session); 
			case "" -> session.sendBody(read("frontend/index.html", Map.of(), session));
			default -> session.sendBody(error(HttpStatus.NOT_FOUND, "Not Found", "The requested resource couldn't be found", false, resource, session));
		}
	}
	public static void assets(String method, String resource, Session session) throws IOException {
		if(!method.equals("GET") && !method.equals("POST")) {
			getLogger().log(Level.WARNING, "405 " + method + " Not Allowed");
			session.sendStatus(HttpStatus.METHOD_NOT_ALLOWED);
			return;
		}
		
		getLogger().log(Level.INFO, "Requested " + resource);
		FileUtil.sendFile(FileUtil.sanitize("frontend" + resource), session);
	}
	
	/**
	 * returns the parsed template of the specified file or a generic 404 error if the file was not found.
	 * */
	public static String read(String resource, Map<String, Supplier<String>> vars, Session session) throws IOException {
		var file = FileUtil.loadTemplate(resource, vars);
		if(file.isPresent()) {
			session.sendStatus(HttpStatus.OK);
			return file.get();
		} else {
			logger.log(Level.WARNING, "404 Not Found: " + resource);
			return error(HttpStatus.NOT_FOUND, "404 Not Found", "404 File not Found", false, "index does not exist", session);
		}
	}
	
	/**
	 * returns a generic error message with title, message and redirect code as specified and logs a warning.
	 * automatically sends the specified status code to the client
	 * 
	 * @return a string with the error html document 
	 * */
	public static String error(HttpStatus status, String title, String msg, boolean redirect, String debug, Session session) throws IOException {
		session.sendStatus(status);
		var errFile = FileUtil.loadTemplate("frontend/error/generic.html", Map.of(
			"errormsg", () -> msg,
			"errortitle", () -> title,
			"redirect", () -> "" + redirect
		));
		getLogger().log(Level.WARNING, msg + (debug.isBlank() ? "" : ": " + debug));
		if(errFile.isPresent())
			return errFile.get();
		return "";
	}
	
	// 	expected:	POST /clans
	//	data:		player=<PLAYER_NAME>&clan=<CLAN_NAME>
	public static void clans(Session session) throws IOException {
		
		var body = session.getRequestBody();
		if(body.isBlank()) {
			session.sendStatus(HttpStatus.BAD_REQUEST);
			getLogger().log(Level.WARNING, "400 Bad Request: Expected Body");
			return;
		}
		var params = body.split("&");
		var entries = new HashMap<String, String>(Map.of("player","", "clan",""));
		for(var param : params) {
			var entry = param.split("=");
			if(entry.length != 2) {
				getLogger().log(Level.WARNING, "Malformed Entry: " + param);
				continue;
			}
			// only set values of non-set existing keys
			if(!entries.containsKey(entry[0]) || !entries.get(entry[0]).isEmpty()) {
				getLogger().log(Level.WARNING, "Unexpected Entry: " + param);
				continue;
			}
			
			entries.put(entry[0], entry[1]);
		}
		
		// fail if at least one entry is not set
		if(entries.entrySet().stream().anyMatch(e -> e.getValue().isEmpty())) {
			session.sendStatus(HttpStatus.BAD_REQUEST);
			getLogger().log(Level.WARNING, "400 Bad Request: Malformed Body: " + body);
			return;
		}
		
		var sniper = new Sniper(entries.get("player"), entries.get("clan"));
		int total = sniper.start(null);
		boolean refreshAll = "all".equals(session.getParameters().get("refresh"));
		
		var file = read("frontend/clans.html", Map.of(
			"total", () -> "" + total,
			"clans", () -> createClanSpans(refreshAll ? sniper.getClans(true).values() : sniper.getChange(true)).orElse("")
		), session);
		
		session.sendHeader("Set-Cookie", "uuid=" + sniper.uuid() + "; Max-Age=36000");
		session.sendBody(file);
	}
	
	// 	expected:	GET /clan?clan=<CLAN_TAG>
	public static void clan(Session session) throws IOException {
		
		var sniper = getSniper(session);
		if(sniper.isEmpty()) return;
		
		var tag = session.getParameters().get("clan");
		if(tag == null) {
			getLogger().log(Level.WARNING, "307 Temporary Redirect: clan tag parameter not set");
			session.sendStatus(HttpStatus.TEMPORARY_REDIRECT);
			session.sendHeader("Location", "/clans?refresh=all");
			return;
		}
		
		var clan = sniper.get().getClans(false).get(tag);
		if(clan == null) {
			session.sendBody(error(HttpStatus.NOT_FOUND, "Clan not found", "Clan with Tag " + tag + " not found", false, "", session));
			return;
		}
		var players = clan.filterPlayers(sniper.get().searchedPlayer(), false);
		
		session.sendBody(
			read("frontend/clan.html", Map.of(
				"players", () -> createPlayerSpans(tag, players).orElse("")
			), session)
		);
	}
	
	// 	expected:	GET /player?clan=<CLAN_TAG>&player=<PLAYER_TAG>
	public static void player(Session session) throws IOException {
		var sniper = getSniper(session);
		if(sniper.isEmpty()) return;
		
		var ptag = session.getParameters().get("player");
		var ctag = session.getParameters().get("clan");
		if(ptag == null || ctag == null) {
			getLogger().log(Level.WARNING, "307 Temporary Redirect: player or clan tag parameter not set");
			session.sendStatus(HttpStatus.TEMPORARY_REDIRECT);
			session.sendHeader("Location", "/clans?refresh=all");
			return;
		}
		
		var clan = sniper.get().getClans(false).get(ctag);
		var playerOpt = clan.getPlayers()
				.stream()
				.filter(p -> p.getTag().equals(ptag))
				.findFirst();
		if(playerOpt.isEmpty()) {
			getLogger().log(Level.DEBUG, "Player not found");
			session.sendStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			return;
		}
		var player = playerOpt.get();
		
		session.sendBody(
			read("frontend/player.html", Map.of(
				"name", player::getName,
				"tag", player::getTag,
				"trophies", () -> "" + player.getTrophies().orElse(-1),
				"maindeck", () -> createPlayerCards(player.getMainDeckURLs()).orElse(""),
				"battledeck", () -> createPlayerCards(player.getBattleDeckURLs()).orElse("")
			), session)
		);
	}
	// expected:	GET /change[c]?uuid=<UUID>
	public static void change(String method, String resource, Session session) throws IOException {
		if(!method.equals("GET")) {
			getLogger().log(Level.WARNING, "405 " + method + " Not Allowed");
			session.sendStatus(HttpStatus.METHOD_NOT_ALLOWED);
			return;
		}
		
		var sniper = getSniper(session);
		if(sniper.isEmpty()) return;
		
		switch(resource.substring(1)) {
			case "change" -> {
				var clans = sniper.get().getChange(true);
				var spans = createClanSpans(clans);
				if(spans.isEmpty()) {
					getLogger().log(Level.ERROR, "501 No clan-span template implemented");
					session.sendStatus(HttpStatus.NOT_IMPLEMENTED);
					return;
				}
				
				session.sendStatus(HttpStatus.OK);
				session.sendBody(spans.get().toString());
			}
			case "changec" -> {
				session.sendStatus(HttpStatus.OK);
				session.sendBody("" + sniper.get().checkedClans());
			}
			default -> {
				session.sendStatus(HttpStatus.NOT_FOUND);
			}
		}
	}
	
	/**
	 * parses the value of a cookie without throwing an exception
	 * 
	 * @return the value of the cookie, or empty if it is not set
	 */
	public static Optional<String> getCookie(String cookie, Session session) {
		if(!session.getRequestHeaders().containsKey("Cookie"))
			return Optional.empty();
		var cookies = session.requestHeader("Cookie").get();
		var key = cookie + "=";
		if(!cookies.contains(key))
			return Optional.empty();
		
		int start = cookies.indexOf(key) + key.length();
		int end = cookies.indexOf(';', start);
		return Optional.of(cookies.substring(start, end > 0 ? end : cookies.length()));
	}
	/**
	 * return the sniper object from the uuid cookie if set,
	 * 	sends redirect to client and returns empty otherwise
	 */
	public static Optional<Sniper> getSniper(Session session) throws IOException {
		var uuidCookie = getCookie("uuid", session);
		if(uuidCookie.isEmpty()) {
			getLogger().log(Level.WARNING, "307 Temporary Redirect: UUID cookie not set");
			session.sendStatus(HttpStatus.TEMPORARY_REDIRECT);
			session.sendHeader("Location", "/");
			return Optional.empty();
		}
		var sniper = Sniper.getByUUID(UUID.fromString(uuidCookie.get()));
		if(sniper.isEmpty()) {
			getLogger().log(Level.WARNING, "307 Temporary Redirect: Invalid UUID");
			session.sendStatus(HttpStatus.TEMPORARY_REDIRECT);
			session.sendHeader("Location", "/");
			return Optional.empty();
		}
		return sniper;
	}
	
	public static Optional<Sniper> getSniperByParam(Session session) throws IOException {
		var uuidParam = session.getParameters().get("uuid");
		if(uuidParam == null) {
			getLogger().log(Level.WARNING, "307 Temporary Redirect: UUID cookie not set");
			session.sendStatus(HttpStatus.TEMPORARY_REDIRECT);
			session.sendHeader("Location", "/");
			return Optional.empty();
		}
		var sniper = Sniper.getByUUID(UUID.fromString(uuidParam));
		if(sniper.isEmpty()) {
			getLogger().log(Level.WARNING, "307 Temporary Redirect: Invalid UUID");
			session.sendStatus(HttpStatus.TEMPORARY_REDIRECT);
			session.sendHeader("Location", "/");
			return Optional.empty();
		}
		return sniper;
	}
	
	
	/**
	 * loads the clan-span template and renders it with every clans' name, tag and badge
	 * 
	 * @return the rendered template, or empty if frontend/clan-span.html couldn't be found
	 */
	public static Optional<String> createClanSpans(Collection<Clan> clans) {
		var spans = new StringBuilder();
		var clan_span = FileUtil.read("frontend/clan-span.html");
		if(clan_span.isEmpty())
			return Optional.empty();
		for(var clan : clans) {
			spans.append(
				FileUtil.renderTemplate(clan_span.get(), Map.of(
					"name", () -> clan.getName(),
					"tag", () -> clan.getTag(),
					"badgeURL", () -> clan.getBadgeURL().orElse("/unknown.png")
				))
			);
		}
		return Optional.of(spans.toString());
	}
	/**
	 * loads the player-span template and renders it with every players' name, tag and trophy count
	 * 
	 * @return the rendered template, or empty if frontend/player-span.html couldn't be found
	 */
	public static Optional<String> createPlayerSpans(String clanTag, Collection<Player> players) {
		var spans = new StringBuilder();
		var player_span = FileUtil.read("frontend/player-span.html");
		if(player_span.isEmpty())
			return Optional.empty();
		
		var duplicates = getDuplicateNames(players);
		for(var player : players) {
			spans.append(
				FileUtil.renderTemplate(player_span.get(), Map.of(
					"name", () -> player.getName(),
					"ctag", () -> clanTag,
					"ptag", () -> player.getTag(),
					"trophies", () -> "" + (duplicates.contains(player) ? player.getTrophies().orElse(-1) : "")
				))
			);
		}
		
		return Optional.of(spans.toString());
	}
	
	/**
	 * loads the player-card template and renders for every card in the deck
	 * 
	 * @return the rendered template, or empty if frontend/player-card.html couldn't be found 
	 */
	public static Optional<String> createPlayerCards(Map<String, Integer> deck) {
		var cards = new StringBuilder();
		var player_card = FileUtil.read("frontend/card-span.html");
		if(player_card.isEmpty())
			return Optional.empty();
		for(var card : deck.entrySet()) {
			cards.append(
				FileUtil.renderTemplate(player_card.get(), Map.of(
					"url", () -> card.getKey(),
					"level", () -> "" + card.getValue()
				))
			);
		}
		return Optional.of(cards.toString());
	}
	
	public static Collection<Player> getDuplicateNames(Collection<Player> players) {
		var pl = new HashMap<String, Player>();
		var duplicates = new HashSet<Player>();
		for(var player : players) {
			if(pl.containsKey(player.getName())) {
				duplicates.add(player);
				duplicates.add(pl.get(player.getName()));
			} else
				pl.put(player.getName(), player);
		}
		
		return duplicates;
	}

}
