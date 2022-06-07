package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

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
	
	public static void showFrame() {
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
				
				showClans(search(clanInput.getText(), playerInput.getText(), matchExact));
			} catch (IOException e) {
				e.printStackTrace();
				debug("Couldn't find clan: " + e.getMessage(), Color.RED);
			}
		});
		root.add(searchButton);
		
		AutoResize.resize(frame);
		
		frame.repaint();
	}
	public static void showClans(Clan[] clans) {
		if(clans.length == 1) {
			showPlayers(clans[0]);
			return;
		}
		
		root.removeAll();
		
		var container = new JLabel();
		container.setPreferredSize(new Dimension(root.getWidth(), 100 * clans.length));
		var scrollPane = new JScrollPane(container);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setSize(root.getSize());
		
		for(int i = 0; i < clans.length; i++) {
			final var clan = clans[i];
			
			container.add(getClanLabel(clan, i, () -> showPlayers(clan)));
		}
		
		root.add(scrollPane);
		scrollPane.revalidate();
	}
	public static void showPlayers(Clan clan) {
		root.removeAll();
		
		clan.onlyPlayersByName(searchedPlayer, matchExact);
		
		var clanName = new JLabel(clan.getName() + "(" + clan.getTag() + ")", SwingConstants.CENTER);
		clanName.setSize(root.getWidth(), 50);
		clanName.setFont(new Font("sans-serif", Font.BOLD, 20));
		
		var playerContainer = new JLabel();
		playerContainer.setPreferredSize(new Dimension(root.getWidth(), 80 * clan.getPlayers().length + clanName.getHeight() + 28 * 2));
		
		playerContainer.add(clanName);
		var playerScrollPane = new JScrollPane(playerContainer);
		playerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		playerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		playerScrollPane.setSize(root.getSize());
		for(int j = 0; j < clan.getPlayers().length; j++) {
			Player p = clan.getPlayers()[j];
			playerContainer.add(getPlayerLabel(p, j + 1, () -> showPlayer(p)));
		}
		root.add(playerScrollPane);
		playerScrollPane.revalidate();
	}
	public static void showPlayer(Player player) {
		root.removeAll();
		root.repaint();
		
		System.out.println("loading deck " + player + "...");
		
		try {
			var doc = Jsoup.connect("https://royaleapi.com/player/" + player.getTag()).get();
			var cards = doc.select("img.deck_card");
			for(int i = 0; i < 2; i++) {
				for(int j = 0; j < 4; j++) {
					var card = cards.get(j + (i * 4)).toString();
					
					var urlPrefix = "src=\""; // src="
					var urlStart = card.indexOf(urlPrefix) + urlPrefix.length();
					var urlEnd = card.indexOf('"', urlStart);
					
					String imgURL = card.substring(urlStart, urlEnd);
					Image img = ImageUtil.resize(
						ImageIO.read(new URL(imgURL)),
						500 / 4,
						-1
					);
					JLabel cardLabel = new JLabel();
					cardLabel.setIcon(new ImageIcon(img));
					cardLabel.setSize(img.getWidth(null), img.getHeight(null));
					cardLabel.setLocation(j * cardLabel.getWidth(), i * cardLabel.getHeight());
					root.add(cardLabel);
					root.repaint();
				}
			}
		} catch(IOException e) {
			debug("Couldn't load deck: " + e.getMessage(), Color.RED);
			e.printStackTrace();
		}
	}
	
	public static void debug(String message, Color color) {
		debugLabel.setForeground(color);
		debugLabel.setText(message);
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			showFrame();
		});
	}
	
	public static Clan[] search(String clan, String player, boolean exactMatchOnly) throws IOException {
		var cs = sanitizeForURL(clan);
		Document doc = Jsoup.connect("https://royaleapi.com/clans/search?name=" + cs).get();
		
		Elements clanResults = doc.select("div.card");
		var clans = new ArrayList<Clan>();
		System.out.println(clanResults.size() + " clans found...");
		for(int i = 0; i < clanResults.size(); i++) {
			var clanRes = new Clan(clanResults.get(i).toString()); 
			for(var playerRes : clanRes.getPlayers()) {
				if(exactMatchOnly) {
					if(playerRes.getName().equals(player)) {
						clans.add(clanRes);
						break;
					}
				} else if(playerRes.getName().toLowerCase().contains(player.toLowerCase())) {
					clans.add(clanRes);
					break;
				}
			}
		}
		
		return clans.toArray(new Clan[0]);
	}
	
	
	public static JButton getClanLabel(Clan clan, int i, Runnable onClick) {
		final var clanLabel = new JButton() {
			private static final long serialVersionUID = 8499764229216881906L;
			
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				g.setFont(new Font("sans-serif", 0, 20));
				g.drawString(
					clan.getName() + "(" + clan.getTag() + ")",
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
	public static JButton getPlayerLabel(Player p, int i, Runnable onClick) {
		var label = new JButton() {
			private static final long serialVersionUID = 8499764229216881906L;
			
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				g.setFont(new Font("sans-serif", 0, 20));
				g.drawString(
					p.getName() + "(" + p.getTag() + ")",
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
