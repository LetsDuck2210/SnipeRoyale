package main;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import util.AutoResize;
import util.Clan;
import util.ImageUtil;
import util.Player;

public class Main {
	private static JFrame frame;
	private static JPanel root;
	private static JLabel debugLabel, info;
	private static boolean exactPlayerSearch, exactClanSearch;
	private static boolean stopThread;
	private static List<Thread> threads;
	
	private static JButton homeButton;
	private static JButton backButton;
	
	private static String searchedClan;
	private static String searchedPlayer;
	private static Clan currentClan;
	
	private static final int TIMEOUT = 30 * 1000;

	public static void debug(String message, Color color) {
		boolean contains = false;
		for(var comp : root.getComponents())
			if(comp.equals(debugLabel))
				contains = true;
		
		if(!contains)
			root.add(debugLabel);
		debugLabel.setForeground(color);
		debugLabel.setText(message);
	}
	
	public static void main(String[] args) throws AWTException, InterruptedException, IOException {
		Toolkit.getDefaultToolkit().beep();
		SwingUtilities.invokeLater(() -> {
			showFrame();
		});
	}
	public static void setupFrame() {
		frame = new JFrame("SnipeRoyale");
		frame.setSize(600, 416);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(3);
		frame.setResizable(false);
		frame.setLayout(null);
		frame.setVisible(true);
		
		root = new JPanel();
		root.setSize(frame.getSize());
		root.setLayout(null);
		frame.add(root);
		
		debugLabel = new JLabel("", 0);
		debugLabel.setSize(root.getWidth(), 50);
		root.add(debugLabel);
		
		final var buttonSize = 32;
		homeButton = new JButton();
		homeButton.setSize(buttonSize, buttonSize);
		ImageUtil.loadFile("assets/Home.png").to(homeButton).catchErr(e -> {
			debug("Couldn't read image(assets/Home.png): " + e.getMessage(), Color.RED);
			e.printStackTrace();
		});
		homeButton.setLocation(4 + buttonSize, root.getHeight() - buttonSize * 2);
		homeButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		homeButton.addActionListener(a -> {
			root.removeAll();
			showFrame();
			stopThread = true;
		});
		
		backButton = new JButton();
		backButton.setSize(buttonSize, buttonSize);
		ImageUtil.loadFile("assets/Back.png").to(backButton).catchErr(e -> {
			debug("Couldn't read image(assets/Home.png): " + e.getMessage(), Color.RED);
			e.printStackTrace();
		});
		backButton.setLocation(homeButton.getX() - buttonSize, homeButton.getY());
		backButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		backButton.addActionListener(a -> {
			switch(a.getActionCommand()) {
				case "clans" -> {
					try {
						showAndSearchClans(searchedClan, searchedPlayer, false);
					} catch (IOException e1) {
						e1.printStackTrace();
						debug("Connect exception: " + e1.getMessage(), Color.RED);
					}
					break;
				}
				case "players" -> {
					showPlayers(currentClan, false);
					break;
				}
			}
		});
		
		threads = new ArrayList<>();
	}
	public static void showFrame() {
		if(frame == null)
			setupFrame();
		
		var titleImage = new JLabel();
		titleImage.setSize(600, 140);
		ImageUtil.loadFile("assets/SnipeRoyaleTitle.jpg").to(titleImage).catchErr(e -> {
			debug("Couldn't load title-image: " + e.getMessage(), Color.RED);
			e.printStackTrace();
		});
		root.add(titleImage);
		
		var inSize = new Dimension(200, 40);
		var playerInput = new JTextField();
		playerInput.setSize(inSize);
		playerInput.setLocation(
			root.getWidth() / 2 - inSize.width / 2,
			root.getHeight() / 2 - inSize.height - 16
		);
		var playerInputLabel = new JLabel("Player: ", SwingConstants.RIGHT);
		playerInputLabel.setSize(playerInput.getX(), inSize.height);
		playerInputLabel.setLocation(0, playerInput.getY());
		root.add(playerInputLabel);
		root.add(playerInput);
		
		var clanInput = new JTextField();
		clanInput.setSize(inSize);
		clanInput.setLocation(
			root.getWidth() / 2 - inSize.width / 2,
			root.getHeight() / 2 - 16
		);
		var clanInputLabel = new JLabel("Clan: ", SwingConstants.RIGHT);
		clanInputLabel.setSize(clanInput.getX(), inSize.height);
		clanInputLabel.setLocation(0, clanInput.getY());
		root.add(clanInput);
		root.add(clanInputLabel);
		
		
		var exactPlayerSearch = new JCheckBox("exact p");
		exactPlayerSearch.setSize((int) Math.round(inSize.width / 1.5), inSize.height);
		exactPlayerSearch.setLocation(
			playerInput.getX() + playerInput.getWidth(),
			playerInput.getY()
		);
		root.add(exactPlayerSearch);
		var exactClanSearch = new JCheckBox("exact c");
		exactClanSearch.setSize((int) Math.round(inSize.width / 1.5), inSize.height);
		exactClanSearch.setLocation(
			clanInput.getX() + clanInput.getWidth(),
			clanInput.getY()
		);
		root.add(exactClanSearch);
		
		var searchButton = new JButton("snipe");
		searchButton.setSize(inSize.width, inSize.height);
		searchButton.setLocation(
			root.getWidth() / 2 - searchButton.getWidth() / 2,
			root.getHeight() / 2 + inSize.height
		);
		searchButton.addActionListener(a -> {
			try {
				root.removeAll();
				Main.exactPlayerSearch = exactPlayerSearch.isSelected();
				Main.exactClanSearch = exactClanSearch.isSelected();
				searchedPlayer = playerInput.getText();
				searchedClan = clanInput.getText();
				
				showAndSearchClans(searchedClan, searchedPlayer, true);
			} catch (IOException e) {
				e.printStackTrace();
				debug("Connect exception: " + e.getMessage(), Color.RED);
			}
		});
		root.add(searchButton);
		
		AutoResize.resize(frame);
		
		frame.repaint();
	}
	public static void showPlayers(Clan clan, boolean loadWhenSingle) {
		root.removeAll();
		
		clan.filterPlayers(searchedPlayer, exactPlayerSearch);
		
		if(clan.getPlayers().size() == 1 && loadWhenSingle) {
			showPlayer(clan, clan.getPlayers().get(0));
			return;
		}
		
		var clanName = new JLabel(clan.getName() + " (" + clan.getTag() + ")", SwingConstants.CENTER);
		clanName.setSize(root.getWidth(), 50);
		clanName.setFont(new Font("sans-serif", Font.BOLD, 20));
		
		var playerContainer = new JLabel();
		playerContainer.setPreferredSize(new Dimension(root.getWidth(), 80 * clan.getPlayers().size() + clanName.getHeight() + 28 * 2));
		
		playerContainer.add(clanName);
		var playerScrollPane = new JScrollPane(playerContainer);
		playerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		playerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		playerScrollPane.setSize(root.getWidth(), root.getHeight() - homeButton.getHeight() * 2);
		for(int j = 0; j < clan.getPlayers().size(); j++) {
			Player p = clan.getPlayers().get(j);
			playerContainer.add(getPlayerLabel(p, j + 1, () -> showPlayer(clan, p)));
		}
		backButton.setActionCommand("clans");
		
		root.add(playerScrollPane);
		root.add(backButton);
		root.add(homeButton);
		playerScrollPane.revalidate();
	}
	
	private static boolean showMainDeck = true;
	public static void showPlayer(Clan clan, Player player) {
		root.removeAll();
		root.repaint();
		stopThread = true;
		currentClan = clan;
		
		
		try {
			JLabel container = new JLabel();
			container.setSize(root.getSize());
			container.setBackground(Color.WHITE);
			container.setOpaque(true);
			root.add(container);
			
			JLabel deckContainer = new JLabel();
			deckContainer.setLocation(50, 50);
			deckContainer.setSize(root.getWidth() - 50, root.getHeight() - 120);
			deckContainer.setBackground(Color.WHITE);
			deckContainer.setOpaque(true);
			container.add(deckContainer);
			
			drawMainDeck(deckContainer, player);
			showMainDeck = true;
			
			String nameLabelStr = "\u1f464" + player.getName() + "  \u1f6e1" + clan.getName();
			JLabel nameLabel = new JLabel(nameLabelStr);
			nameLabel.setFont(new Font("sans-serif", Font.BOLD, 16));
			nameLabel.setSize(root.getWidth(), 40);
			nameLabel.setLocation(50, 10);
			
			JLabel trophyLabel = new JLabel("\u1f3c6" + player.getTrophies());
			trophyLabel.setFont(nameLabel.getFont());
			trophyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			trophyLabel.setSize(root.getWidth() - 50, 40);
			trophyLabel.setLocation(0, 10);
			
			JButton swapDecks = new JButton("\u2694");
			swapDecks.setSize(homeButton.getSize());
			swapDecks.setLocation(container.getWidth() - swapDecks.getWidth(), container.getHeight() - 2 * swapDecks.getHeight());
			swapDecks.setFont(new Font("sans-serif", 0, 20));
			swapDecks.addActionListener(a -> {
				deckContainer.removeAll();
				showMainDeck = !showMainDeck;
				
				if(showMainDeck) {
					try {
						drawMainDeck(deckContainer, player);
						swapDecks.setText("\u2694");
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					try {
						if(drawBattleDeck(deckContainer, player) < 0) {
							deckContainer.removeAll();
							drawMainDeck(deckContainer, player);
							swapDecks.setVisible(false);
						} else
							swapDecks.setText("\u1f5e1");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			
			backButton.setActionCommand("players");
			
			container.add(homeButton);
			container.add(backButton);
			container.add(swapDecks);
			container.add(nameLabel);
			container.add(trophyLabel);
			container.repaint();
		} catch(IOException e) {
			debug("Connect exception: " + e.getMessage(), Color.RED);
			e.printStackTrace();
		}
	}
	public static int drawMainDeck(JLabel container, Player player) throws IOException {
		container.removeAll();
		var deck = player.getMainDeck();
		var iter = deck.keySet().iterator();
		System.out.println("main deck");
		
		for(int i = 0; i < 2; i++) {
			for(int j = 0; j < 4; j++) {
				var loader = iter.next();
				
				var cardLabel = getCardLabel(loader, loader.getReference(), deck.get(loader));
				cardLabel.setLocation(j * cardLabel.getWidth(), i * cardLabel.getHeight());
				
				container.add(cardLabel);
			}
		}
		container.repaint();
		return 0;
	}
	public static int drawBattleDeck(JLabel container, Player player) throws IOException {
		container.removeAll();
		var deck = player.getBattleDeck();
		var iter = deck.keySet().iterator();
		System.out.println("battle deck");
		
		for(int i = 0; i < 2; i++) {
			for(int j = 0; j < 4; j++) {
				if(iter.hasNext()) {
					var loader = iter.next();
					
					var cardLabel = getCardLabel(loader, loader.getReference(), deck.get(loader));
					cardLabel.setLocation(j * cardLabel.getWidth(), i * cardLabel.getHeight());
					
					container.add(cardLabel);
				} else {
					return -1;
				}
			}
		}
		container.repaint();
		return 0;
	}
	
	private static AtomicInteger foundClans, checkedClans, searchingThreads;
	private static int searchResults;
	public static void showAndSearchClans(String clan, String player, boolean loadWhenSingle) throws IOException {
		stopThread = false;
		var cs = sanitizeForURL(clan);
		System.out.println("starting search...");
		Document doc = load("https://royaleapi.com/clans/search?name=" + cs + "&exactNameMatch=on");
		System.out.println("page 0   OK");
		foundClans = new AtomicInteger();
		searchingThreads = new AtomicInteger();
		checkedClans = new AtomicInteger();
		
		root.removeAll();
		
		var container = new JLabel();
		var scrollPane = new JScrollPane(container);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setSize(root.getWidth(), root.getHeight() - 64);
		
		var clanResults = doc.select("div.card");
		var arElem = doc.select("div.ui.segment.attached.top").select("strong");
		if(arElem.size() == 0) {
			System.out.println("invalid search query");
			showFrame();
			var errMsg = new JLabel("Invalid search query", SwingConstants.CENTER);
			errMsg.setForeground(new Color(138, 70, 70));
			errMsg.setSize(root.getWidth(), 30);
			errMsg.setLocation(0, root.getHeight() - errMsg.getHeight() - 64);
			root.add(errMsg);
			root.repaint();
			return;
		}
		var amountResults = arElem.get(0).html();
		var num = Integer.parseInt(amountResults.substring("Found".length(), amountResults.length() - "clans".length()).trim());
		info = new JLabel("0/" + num + " clan" + (num != 1 ? "s" : ""));
		info.setSize(100, 32);
		info.setHorizontalAlignment(SwingConstants.RIGHT);
		info.setLocation(root.getWidth() - info.getWidth() - 4, root.getHeight() - info.getHeight() * 2);
		root.add(info);
		root.add(scrollPane);
		root.add(homeButton);
		root.repaint();
		scrollPane.revalidate();
		searchResults = num;
		
		System.out.println("searching for player in page 0...");
		
		evalSearch(clan, player, clanResults, container);
		
		scrollPane.revalidate();
		
		thread("showandsearch-root", () -> {
			searchingThreads.getAndIncrement();
			for(int i = 2; i <= Math.ceil(num / 60.0); i++) {
				if(stopThread) return;
				final int j = i;
				thread("showandsearch-" + i, () -> {
					try {
						var docP = load("https://royaleapi.com/clans/search?name=" + cs + "&exactNameMatch=on&page=" + j);
						System.out.println("page " + j + "   OK");
						var clanResultsP = docP.select("div.card");
						evalSearch(clan, player, clanResultsP, container);
						scrollPane.revalidate();
					} catch (IOException e) {
						debug("Connect exception: " + e.getMessage(), Color.RED);
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
		 
		if(loadWhenSingle)
			thread("check-one-clan", () -> {
				while(searchingThreads.get() > 0 && !stopThread) { }
				if(foundClans.get() == 1 && !stopThread) {
					if(container.getComponent(0) instanceof JButton button) button.doClick();
				}
			}).start();
	}
	public static void evalSearch(String clan, String player, Elements clans, JLabel container) {
		if(stopThread) return;
		
		container.setPreferredSize(new Dimension(root.getWidth(), 100 * clans.size()));
		
		System.out.println("searching for " + clan + "...");
		
		for(int i = 0; i < clans.size(); i++) {
			if(stopThread) return;
			
			final int j = i;
			thread("evalsearch-" + i, () -> {
				if(stopThread) return;
				
				searchingThreads.getAndIncrement();
				
				try {
					Clan clanRes = new Clan(clans.get(j).toString(), !exactClanSearch);
					if(exactClanSearch) 
						if(clanRes.getName().equalsIgnoreCase(clan)) {
							System.out.println("Loading players for " + clanRes);
							clanRes.loadPlayers();
						} else {
							container.setPreferredSize(new Dimension(root.getWidth(), 100 * foundClans.get()));
							checkedClans.getAndIncrement();
							return;
						}
					;
					boolean add = false;
					
					System.out.println("searching " + player + " in " + clanRes);
					for(var playerRes : clanRes.getPlayers()) {
						if(stopThread) return;
						
						if(exactPlayerSearch) {
							if(playerRes.getName().equalsIgnoreCase(player)) {
								add = true;
								break;
							}
						} else if(playerRes.getName().toLowerCase().contains(player.toLowerCase())) {
							add = true;
							break;
						}
					}
					if(add) {
						container.add(getClanLabel(clanRes, foundClans.getAndIncrement(), () -> {
							showPlayers(clanRes, true);
						}));
						System.out.println("found player in " + clanRes);
					} else
						container.setPreferredSize(new Dimension(root.getWidth(), 100 * foundClans.get()));
					
					checkedClans.getAndIncrement();
					info.setText(checkedClans.get() + "/" + searchResults + " clan" + (searchResults != 1 ? "s" : ""));
					info.repaint();
					container.repaint();
					container.revalidate();
					container.getParent().revalidate();
					System.out.println(clanRes + " checked");
				} catch (IOException e) {
					debug("Connect exception: " + e.getMessage(), Color.RED);
					e.printStackTrace();
				}
				searchingThreads.getAndDecrement();
			}).start();
		}
		
		System.out.println("searching threads running");
	}
	
	/**
	 * creates a button to display a clan, used to display multiple clans on a stack
	 * 
	 *  @return a button, representing a clan label 
	 */
	public static JButton getClanLabel(Clan clan, int i, Runnable onClick) {
		final var clanLabel = new JButton() {
			private static final long serialVersionUID = 8499764229216881906L;
			
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				g.setFont(new Font("sans-serif", 0, 20));
				g.drawString(
					clan.getName() + " (" + clan.getTag() + ")",
					getHeight() / 2 - getFont().getSize() / 2,
					getHeight() / 2 + 8
				);
				
				try {
					var badge = ImageUtil.resize(clan.getBadge(), -1, getHeight());
					g.drawImage(badge, getWidth() - badge.getWidth(null), 0, null);
				} catch(IOException e) {
					debug("Connect exception: " + e.getMessage(), Color.RED);
					e.printStackTrace();
				}
			}
		};
		clanLabel.setSize(500, 100);
		clanLabel.setLocation(50, i * clanLabel.getHeight());
		clanLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		clanLabel.setVisible(true);
		
		clanLabel.addActionListener(a -> onClick.run());
		
		return clanLabel;
	}
	/**
	 * creates a button to display a player, used to display multiple players on a stack
	 * 
	 *  @return a button, representing a player label 
	 */
	public static JButton getPlayerLabel(Player p, int i, Runnable onClick) {
		var label = new JButton() {
			private static final long serialVersionUID = 8499764229216881906L;
			
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				g.setFont(new Font("sans-serif", 0, 20));
				g.drawString(
					p.getName() + " (" + p.getTag() + ")",
					getHeight() / 2 - getFont().getSize() / 2,
					getHeight() / 2 + 8
				);
				String trophyStr = "" + p.getTrophies().orElse(-1);
				g.drawString(
					trophyStr,
					getWidth() - getFont().getSize() * trophyStr.length() - 10,
					getHeight() / 2 + 8
				);
			}
		};
		label.setSize(500, 80);
		label.setLocation(50, i * label.getHeight());
		label.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		label.setVisible(true);
		
		label.addActionListener(a -> onClick.run());
		
		return label;
	}
	
	public static JLabel getCardLabel(ImageUtil loader, AtomicReference<Image> imgRef, int level) {
		JLabel cardLabel = new JLabel() {
			private static final long serialVersionUID = 8499764229216881906L;
			
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				g.setFont(new Font("sans-serif", Font.BOLD, 20));
				if(imgRef.get() != null) {
					Image img = ImageUtil.resize(
						imgRef.get(),
						500 / 4,
						-1
					);
					g.drawImage(img, 0, 0, null);
					setSize(img.getWidth(null), img.getHeight(null));
				}
				g.setColor(Color.CYAN);
				var lvlStr = "Lvl " + level;
				g.drawString(lvlStr, getWidth() / 2 - (lvlStr.length() * getFont().getSize()) / 2, getHeight() - getFont().getSize());
			}
		};
		cardLabel.setSize(125, 150);
		loader.repaint(cardLabel);
		cardLabel.repaint();
		
		return cardLabel;
	}
	
	/**
	 * will create and register a new thread 
	 */
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
}
