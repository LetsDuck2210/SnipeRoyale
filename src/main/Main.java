package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import util.AutoResize;
import util.Clan;

public class Main {
	private static JFrame frame;
	private static JLabel debugLabel;
	
	public static void showFrame() {
		frame = new JFrame("SnipeRoyale");
		frame.setSize(600, 400);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(3);
		frame.setResizable(false);
		frame.setLayout(null);
		frame.setVisible(true);
		
		debugLabel = new JLabel("", 0);
		debugLabel.setSize(frame.getWidth(), 50);
		frame.add(debugLabel);
		
		var inSize = new Dimension(200, 40);
		var clanInput = new JTextField();
		clanInput.setSize(inSize);
		clanInput.setLocation(
			frame.getWidth() / 2 - inSize.width / 2,
			frame.getHeight() / 2 - inSize.height - 16
		);
		var clanInputLabel = new JLabel("Clan: ", SwingConstants.RIGHT);
		clanInputLabel.setSize(clanInput.getX(), inSize.height);
		clanInputLabel.setLocation(0, clanInput.getY());
		frame.add(clanInput);
		frame.add(clanInputLabel);
		
		var playerInput = new JTextField();
		playerInput.setSize(inSize);
		playerInput.setLocation(
			frame.getWidth() / 2 - inSize.width / 2,
			frame.getHeight() / 2 - 16
		);
		var playerInputLabel = new JLabel("Player: ", SwingConstants.RIGHT);
		playerInputLabel.setSize(playerInput.getX(), inSize.height);
		playerInputLabel.setLocation(0, playerInput.getY());
		frame.add(playerInputLabel);
		frame.add(playerInput);
		
		
		var exactSearch = new JCheckBox("exact match");
		exactSearch.setSize((int) Math.round(inSize.width / 1.5), inSize.height);
		exactSearch.setLocation(
			playerInput.getX() - 10,
			frame.getHeight() / 2 + inSize.height
		);
		frame.add(exactSearch);
		
		var searchButton = new JButton("search");
		searchButton.setSize(inSize.width / 2, inSize.height);
		searchButton.setLocation(
			frame.getWidth() / 2,
			frame.getHeight() / 2 + inSize.height
		);
		searchButton.addActionListener(a -> {
			try {
				clanInput.setVisible(false);
				clanInputLabel.setVisible(false);
				playerInput.setVisible(false);
				playerInputLabel.setVisible(false);
				searchButton.setVisible(false);
				exactSearch.setVisible(false);
				
				showClans(search(clanInput.getText(), playerInput.getText(), exactSearch.isSelected()));
			} catch (IOException e) {
				System.out.println("I/O Exception: " + e.getMessage());
				debug("Couldn't find clan: " + e.getMessage(), Color.RED);
			}
		});
		frame.add(searchButton);
		
		AutoResize.resize(frame);
		
		frame.repaint();
	}
	public static void showClans(Clan[] clans) {
		var container = new JLabel("");
		container.setSize(frame.getSize());
		var scrollPane = new JScrollPane(container);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		for(int i = 0; i < clans.length; i++) {
			final var clan = clans[i];
			final var clanLabel = new JLabel() {
				private static final long serialVersionUID = 8499764229216881906L;
				
				@Override
				public void paintComponent(Graphics g) {
					super.paintComponent(g);
					
					System.out.println("painting clan " + clan);
					
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
					bufferedImage.getGraphics().drawImage(img, 0, 0, scaledImg.getWidth(null), scaledImg.getHeight(null), null);
					
					return bufferedImage;
				}

			};
			clanLabel.setSize(500, 100);
			clanLabel.setLocation(50, i * clanLabel.getHeight());
			clanLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
			clanLabel.setVisible(true);
			container.add(clanLabel);
		}
		container.repaint();
		
		frame.add(container);
		frame.repaint();
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
