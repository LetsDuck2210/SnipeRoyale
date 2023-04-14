package main;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import util.Clan;

public class Sniper {
	private static final int TIMEOUT = 30 * 1000;
	private static Map<UUID, Sniper> snipers = new HashMap<>();
	private UUID uuid;
	private String player, clan;
	private Map<String, Clan> clans;
	private List<Clan> change;
	
	public Sniper(String player, String clan) {
		uuid = UUID.randomUUID();
		snipers.put(uuid, this);
		this.player = player;
		this.clan = clan;
		clans = new LinkedHashMap<>();
		change = new LinkedList<>();
		threads = new LinkedList<>();
	}
	
	public UUID uuid() {
		return uuid;
	}
	
	public Map<String, Clan> getClans(boolean reset) {
		if(reset)
			change.clear();
		return Map.copyOf(clans);
	}
	public List<Clan> getChange(boolean reset) {
		var l = List.copyOf(change);
		if(reset)
			change.clear();
		return l;
	}
	public int checkedClans() {
		return checkedClans.get();
	}
	
	private boolean stopThread;
	private AtomicInteger foundClans, searchingThreads, checkedClans;
	private int searchResults;
	public int start(Consumer<Clan> handler) {
		stopThread = false;
		SnipeServer.getLogger().log(Level.INFO, "starting search for " + uuid + "...");
		Document doc;
		try {
			doc = load("https://royaleapi.com/clans/search?name=" + clan + "&exactNameMatch=on");
		} catch (IOException e) {
			SnipeServer.getLogger().log(Level.ERROR, "I/O Exception: " + e.getMessage());
			return 0;
		}
		System.out.println("page 0   OK");
		foundClans = new AtomicInteger();
		searchingThreads = new AtomicInteger();
		checkedClans = new AtomicInteger();
		
		
		var clanResults = doc.select("div.card");
		var arElem = doc.select("div.ui.segment.attached.top").select("strong");
		if(arElem.size() == 0) {
			SnipeServer.getLogger().log(Level.WARNING, "invalid search query: ");
			System.out.println(clan);
			return 0;
		}
		var amountResults = arElem.get(0).html();
		var num = Integer.parseInt(amountResults.substring("Found".length(), amountResults.length() - "clans".length()).trim());
		searchResults = num;
		
		thread("search-superroot", () -> {
			checkClans(this.player, this.clan, clanResults, handler);
				
			thread("search-root", () -> {
				searchingThreads.getAndIncrement();
				for(int i = 2; i <= Math.ceil(num / 60.0); i++) {
					if(stopThread) return;
					final int j = i;
					thread("search-" + i, () -> {
						try {
							var docP = load("https://royaleapi.com/clans/search?name=" + clan + "&exactNameMatch=on&page=" + j);
							SnipeServer.getLogger().log(Level.DEBUG, "page " + j + "   OK");
							var clanResultsP = docP.select("div.card");
							checkClans(clan, player, clanResultsP, handler);
						} catch (IOException e) {
							SnipeServer.getLogger().log(Level.WARNING, "Connect exception: " + e.getMessage());
							e.printStackTrace();
						}
						
						threads.remove(Thread.currentThread());
					}).start();
					
					try {
						Thread.sleep(150 * j);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				searchingThreads.getAndDecrement();
				threads.remove(Thread.currentThread());
			}).start();
		}).start();
		
		return searchResults;
	}
	
	public void checkClans(String player, String clan, Elements clans, Consumer<Clan> handler) {
		if(stopThread) return;
		
		SnipeServer.getLogger().log(Level.DEBUG, "searching for " + clan + "...");
		
		for(int i = 0; i < clans.size(); i++) {
			if(stopThread) return;
			
			final int j = i;
			thread("check-clan-" + i, () -> {
				if(stopThread) return;
				
				searchingThreads.getAndIncrement();
				
				try {
					Clan clanRes = new Clan(clans.get(j).toString(), true);
					boolean add = false;
					
					SnipeServer.getLogger().log(Level.DEBUG, "searching " + player + " in " + clanRes);
					for(var playerRes : clanRes.getPlayers()) {
						if(stopThread) return;
						
						if(playerRes.getName().toLowerCase().contains(player.toLowerCase())) {
							add = true;
							break;
						}
					}
					if(add)
						foundClan(clanRes, handler);
					
					checkedClans.getAndIncrement();
					SnipeServer.getLogger().log(Level.DEBUG, clanRes + " checked");
				} catch (IOException e) {
					SnipeServer.getLogger().log(Level.ERROR, "Connect exception: " + e.getMessage());
					e.printStackTrace();
				}
				searchingThreads.getAndDecrement();
			}).start();
		}
		
		SnipeServer.getLogger().log(Level.DEBUG, "searching threads running");
	}
	
	private void foundClan(Clan clan, Consumer<Clan> handler) {
		if(handler != null)
			handler.accept(clan);
		
		this.clans.put(clan.getTag(), clan);
		this.change.add(clan);
		
		foundClans.getAndIncrement();
		SnipeServer.getLogger().log(Level.DEBUG, "found player in " + clan);
	}
	
	public String searchedPlayer() {
		return player;
	}
	public String searchedClan() {
		return clan;
	}
	
	public void stopThread() {
		stopThread = true;
	}
	
	private static Map<String, Document> docCache = new ConcurrentHashMap<>();
	public static Document load(String url) throws IOException {
		if(!docCache.containsKey(url))
			docCache.put(url, Jsoup.connect(url).timeout(TIMEOUT).get());
		
		return docCache.get(url);
	}
	
	public static String sanitizeForURL(String text) {
		String res = "";
		
		for(int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if(!Character.isAlphabetic(c))
				res += "%" + Integer.toHexString(c);
			else
				res += c;
		}
		
		return res;
	}

	private static List<Thread> threads;
	public static Thread thread(Runnable r) {
		Thread thr = new Thread(r);
		threads.add(thr);
		return thr;
	}
	public static Thread thread(String name, Runnable r) {
		Thread thr = new Thread(r, name);
		threads.add(thr);
		return thr;
	}
	
	public static Optional<Sniper> getByUUID(UUID uuid) {
		return Optional.ofNullable(snipers.get(uuid));
	}
}
