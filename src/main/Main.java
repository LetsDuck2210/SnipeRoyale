package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
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
	private static JLabel debugLabel;
	private static boolean matchExact;
	private static String searchedPlayer;
	private static boolean stopThread;
	private static List<Thread> threads;
	
	private static JButton homeButton;
	
	public static void setupFrame() {
		frame = new JFrame("SnipeRoyale");
		frame.setSize(600, 400);
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
		try {
			homeButton.setIcon(new ImageIcon(ImageUtil.resize(ImageUtil.load("assets/Home.png"), buttonSize, buttonSize)));
		} catch (IOException e) {
			debug("Couldn't read image(assets/Home.png): " + e.getMessage(), Color.RED);
			e.printStackTrace();
		}
		homeButton.setSize(buttonSize, buttonSize);
		homeButton.setLocation(4, root.getHeight() - buttonSize * 2);
		homeButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		homeButton.addActionListener(a -> {
			root.removeAll();
			showFrame();
			stopThread = true;
		});
		
		threads = new ArrayList<>();
	}
	public static void showFrame() {
		if(frame == null)
			setupFrame();
		
		var titleImage = new JLabel();
		titleImage.setSize(600, 140);
		try {
			titleImage.setIcon(new ImageIcon(ImageUtil.resize(ImageUtil.load("assets/SnipeRoyaleTitle.jpg"), 600, titleImage.getHeight())));
		} catch (IOException e) {
			debug("Couldn't load title-image: " + e.getMessage(), Color.RED);
			e.printStackTrace();
		}
		root.add(titleImage);
		
		var inSize = new Dimension(200, 40);
		var clanInput = new JTextField();
		clanInput.setSize(inSize);
		clanInput.setLocation(
			root.getWidth() / 2 - inSize.width / 2,
			root.getHeight() / 2 - inSize.height - 16
		);
		var clanInputLabel = new JLabel("Clan: ", SwingConstants.RIGHT);
		clanInputLabel.setSize(clanInput.getX(), inSize.height);
		clanInputLabel.setLocation(0, clanInput.getY());
		root.add(clanInput);
		root.add(clanInputLabel);
		
		var playerInput = new JTextField();
		playerInput.setSize(inSize);
		playerInput.setLocation(
			root.getWidth() / 2 - inSize.width / 2,
			root.getHeight() / 2 - 16
		);
		var playerInputLabel = new JLabel("Player: ", SwingConstants.RIGHT);
		playerInputLabel.setSize(playerInput.getX(), inSize.height);
		playerInputLabel.setLocation(0, playerInput.getY());
		root.add(playerInputLabel);
		root.add(playerInput);
		
		var exactSearch = new JCheckBox("exact match");
		exactSearch.setSize((int) Math.round(inSize.width / 1.5), inSize.height);
		exactSearch.setLocation(
			playerInput.getX() - 10,
			root.getHeight() / 2 + inSize.height
		);
		root.add(exactSearch);
		
		var searchButton = new JButton("search");
		searchButton.setSize(inSize.width / 2, inSize.height);
		searchButton.setLocation(
			root.getWidth() / 2,
			root.getHeight() / 2 + inSize.height
		);
		searchButton.addActionListener(a -> {
			try {
				root.removeAll();
				matchExact = exactSearch.isSelected();
				searchedPlayer = playerInput.getText();
				
				showAndSearchClans(clanInput.getText(), playerInput.getText(), matchExact);
			} catch (IOException e) {
				e.printStackTrace();
				debug("Connect exception: " + e.getMessage(), Color.RED);
			}
		});
		root.add(searchButton);
		
		AutoResize.resize(frame);
		
		frame.repaint();
	}
	public static void showPlayers(Clan clan) {
		if(clan.getPlayers().length == 1) {
			showPlayer(clan.getPlayers()[0]);
			return;
		}
		
		root.removeAll();
		
		clan.onlyPlayersByName(searchedPlayer, matchExact);
		
		var clanName = new JLabel(clan.getName() + " (" + clan.getTag() + ")", SwingConstants.CENTER);
		clanName.setSize(root.getWidth(), 50);
		clanName.setFont(new Font("sans-serif", Font.BOLD, 20));
		
		var playerContainer = new JLabel();
		playerContainer.setPreferredSize(new Dimension(root.getWidth(), 80 * clan.getPlayers().length + clanName.getHeight() + 28 * 2));
		
		playerContainer.add(clanName);
		var playerScrollPane = new JScrollPane(playerContainer);
		playerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		playerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		playerScrollPane.setSize(root.getWidth(), root.getHeight() - homeButton.getHeight() * 2);
		for(int j = 0; j < clan.getPlayers().length; j++) {
			Player p = clan.getPlayers()[j];
			playerContainer.add(getPlayerLabel(p, j + 1, () -> showPlayer(p)));
		}
		root.add(playerScrollPane);
		root.add(homeButton);
		playerScrollPane.revalidate();
	}
	public static void showPlayer(Player player) {
		root.removeAll();
		root.repaint();
		stopThread = true;
		
		System.out.println("loading deck " + player + "...");
		
		try {
			var doc = Jsoup.connect("https://royaleapi.com/player/" + player.getTag()).get();
			var cards = doc.select("img.deck_card");
			var levels = doc.select("h5.cardlevel");
			for(int i = 0; i < 2; i++) {
				for(int j = 0; j < 4; j++) {
					var card = cards.get(j + (i * 4)).toString();
					var level = levels.get(j + (i * 4)).toString();
					
					var urlPrefix = "src=\""; // src="
					var urlStart = card.indexOf(urlPrefix) + urlPrefix.length();
					var urlEnd = card.indexOf('"', urlStart);
					
					var levelStart = level.indexOf('>') + 1;
					var levelEnd = level.indexOf('<', levelStart);
					var levelStr = level.substring(levelStart, levelEnd);
					
					String imgURL = card.substring(urlStart, urlEnd);
					Image img = ImageUtil.resize(
						ImageIO.read(new URL(imgURL)),
						500 / 4,
						-1
					);
					JLabel cardLabel = new JLabel() {
						private static final long serialVersionUID = 8499764229216881906L;
						
						@Override
						public void paintComponent(Graphics g) {
							g.setFont(new Font("sans-serif", Font.BOLD, 20));
							g.drawImage(img, 0, 0, null);
							g.setColor(Color.CYAN);
							var lvlStr = levelStr;
							g.drawString(lvlStr, getWidth() / 2 - (lvlStr.length() * getFont().getSize()) / 2, getHeight() - getFont().getSize());
						}
					};
					cardLabel.setSize(img.getWidth(null), img.getHeight(null));
					cardLabel.setLocation(j * cardLabel.getWidth(), i * cardLabel.getHeight());
					root.add(cardLabel);
					root.add(homeButton);
					root.repaint();
				}
			}
		} catch(IOException e) {
			debug("Connect exception: " + e.getMessage(), Color.RED);
			e.printStackTrace();
		}
	}
	
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
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			showFrame();
		});
	}
	
	private static AtomicInteger foundClans;
	public static void showAndSearchClans(String clan, String player, boolean exactMatchOnly) throws IOException {
		stopThread = false;
		var cs = sanitizeForURL(clan);
		Document doc = Jsoup.connect("https://royaleapi.com/clans/search?name=" + cs + "&exactNameMatch=on").get();
		foundClans = new AtomicInteger();
		
		root.removeAll();
		
		var container = new JLabel();
		var scrollPane = new JScrollPane(container);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setSize(root.getWidth(), root.getHeight() - 64);
		
		root.add(scrollPane);
		root.add(homeButton);
		root.repaint();	
		scrollPane.revalidate();
		
		var clanResults = doc.select("div.card");
		var amountResults = doc.select("div.ui.segment.attached.top").select("strong").get(0).html();
		var num = Integer.parseInt(amountResults.substring("Found".length(), amountResults.length() - "clans".length()).trim());
		System.out.println(clanResults.size() + " clans found...");
		
		evalSearch(clan, player, exactMatchOnly, clanResults, container);
		scrollPane.revalidate();
		
		thread("showandsearch-root", () -> {
			for(int i = 2; i <= Math.floor(num / 60); i++) {
				if(stopThread) return;
				final int j = i;
				thread("showandsearch-" + i, () -> {
					try {
						var docP = Jsoup.connect("https://royaleapi.com/clans/search?name=" + cs + "&exactNameMatch=on&page=" + j).get();
						var clanResultsP = docP.select("div.card");
						evalSearch(clan, player, exactMatchOnly, clanResultsP, container);
						scrollPane.revalidate();
					} catch (IOException e) {
						debug("Connect exception: " + e.getMessage(), Color.RED);
						e.printStackTrace();
					}
				}).start();
				try {
					Thread.sleep(750 * i);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	public static void evalSearch(String clan, String player, boolean exactMatchOnly, Elements clans, JLabel container) {
		if(stopThread) return;
		
		container.setPreferredSize(new Dimension(root.getWidth(), 100 * clans.size()));
		
		for(int i = 0; i < clans.size(); i++) {
			if(stopThread) return;
			
			final int j = i;
			thread("evalsearch-" + i, () -> {
				if(stopThread) return;
				try {
					Clan clanRes = new Clan(clans.get(j).toString());
					boolean add = false;
					
					for(var playerRes : clanRes.getPlayers()) {
						if(stopThread) return;
						
						if(exactMatchOnly) {
							if(playerRes.getName().equals(player)) {
								add = true;
								break;
							}
						} else if(playerRes.getName().toLowerCase().contains(player.toLowerCase())) {
							add = true;
							break;
						}
					}
					if(add)
						container.add(getClanLabel(clanRes, foundClans.getAndIncrement(), () -> showPlayers(clanRes)));
					else
						container.setPreferredSize(new Dimension(root.getWidth(), 100 * foundClans.get()));
					
					container.repaint();
					container.revalidate();
					container.getParent().revalidate();
				} catch (IOException e) {
					debug("Connect exception: " + e.getMessage(), Color.RED);
					e.printStackTrace();
				}
			}).start();
		}
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
				
				var badge = ImageUtil.resize(clan.getBadge(), -1, getHeight());
				g.drawImage(badge, getWidth() - badge.getWidth(null), 0, null);
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
				String trophyStr = "-1";
				try {
					trophyStr = "" + p.getTrophies();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
