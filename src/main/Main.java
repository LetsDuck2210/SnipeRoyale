package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import util.AutoResize;
import util.Clan;
import util.Player;

public class Main {
	private static JFrame frame;
	private static JPanel root;
	private static JLabel debugLabel;
	
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
				for(int i = 0; i < root.getComponentCount(); i++)
					root.remove(root.getComponent(i));
				
				showClans(search(clanInput.getText(), playerInput.getText(), exactSearch.isSelected()));
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
		var container = new JLabel();
		container.setSize(root.getWidth(), 100 * clans.length);
		var scrollPane = new JScrollPane(container);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		for(int i = 0; i < clans.length; i++) {
			final var clan = clans[i];
			
			container.add(getClanLabel(clan, i, () -> {
				for(int j = 0; j < root.getComponentCount(); j++)
					root.remove(root.getComponent(j));
				
				var playerContainer = new JLabel();
				playerContainer.setSize(root.getWidth(), 80 * clan.getPlayers().length);
				var playerScrollPane = new JScrollPane(playerContainer);
				playerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				playerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
				
				for(int j = 0; j < clan.getPlayers().length; j++) {
					Player p = clan.getPlayers()[j];
					playerContainer.add(getPlayerLabel(p, j, () -> {}));
				}
				root.add(playerScrollPane);
				root.repaint();
			}));
		}
		container.repaint();
		
		root.add(container);
		root.repaint();
	}
	public static void debug(String message, Color color) {
		debugLabel.setForeground(color);
		debugLabel.setText(message);
	}
	
	public static void main(String[] args) throws IOException {
		
		showFrame();
		
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
	
	
	public static JLabel getClanLabel(Clan clan, int i, Runnable onClick) {
		final var clanLabel = new JLabel() {
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
				
				var badge = resize(clan.getBadge(), -1, getHeight());
				g.drawImage(badge, getWidth() - badge.getWidth(null), 0, null);
			}
			
			public static Image resize(Image img, int width, int height) {
				var scaledImg = img.getScaledInstance(width, height, Image.SCALE_FAST);
				BufferedImage bufferedImage= new BufferedImage(scaledImg.getWidth(null), scaledImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
				var g = bufferedImage.getGraphics();
				g.setColor(frame.getBackground());
				g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
				g.drawImage(img, 0, 0, scaledImg.getWidth(null), scaledImg.getHeight(null), null);
				
				return bufferedImage;
			}

		};
		clanLabel.setSize(500, 100);
		clanLabel.setLocation(50, i * clanLabel.getHeight());
		clanLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		clanLabel.setVisible(true);
		
		clanLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				final int x = e.getX(),
						  y = e.getY();
				if(x > clanLabel.getX() && x < clanLabel.getX() + clanLabel.getWidth()
						&& y > clanLabel.getY() && y < clanLabel.getY() + clanLabel.getHeight() + 16)
					onClick.run();
			}
		});
		
		return clanLabel;
	}
	public static JLabel getPlayerLabel(Player p, int i, Runnable onClick) {
		var label = new JLabel() {
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
			}

		};
		
		
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
