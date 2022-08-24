package util;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class ImageUtil {
	private static Map<String, Image> fileImages = new HashMap<>();
	private static Map<URL, Image> urlImages = new HashMap<>();
	
	private Image img;
	private JComponent container, repaintComponent;
	private AtomicReference<Image> reference;
	private Thread thread;
	private boolean syncRequested;
	private Consumer<Exception> errHandler;
	private Consumer<Image> imageHandler;
	public String uri;
	
	private ImageUtil(File file) {
		uri = file.getAbsolutePath();
		thread = new Thread(() -> {
			if(fileImages.containsKey(file.getAbsolutePath())) {
				img = fileImages.get(file.getAbsolutePath());
				set();
				return;
			}
			
			try {
				img = ImageIO.read(file);
				fileImages.put(file.getAbsolutePath(), img);
				set();
			} catch (IOException e) {
				if(errHandler != null)
					errHandler.accept(e);
				else
					e.printStackTrace();
			}
			
		}, "imageutil-load");
		thread.start();
	}
	private ImageUtil(URL url) {
		uri = url.getPath();
		thread = new Thread(() -> {
			if(urlImages.containsKey(url)) {
				img = urlImages.get(url);
				set();
				return;
			}
			
			try {
				img = ImageIO.read(url);
				urlImages.put(url, img);
				set();
			} catch (IOException e) {
				e.printStackTrace();
				if(errHandler != null)
					errHandler.accept(e);
//				else
//					e.printStackTrace();
			}
		}, "imageutil-load");
		thread.start();
	}
	
	private void set() {
		// wait until 'container' or 'reference' was set or sync was requested
		while(container == null && reference == null && imageHandler == null)
			if(syncRequested) return;
		
		if(container != null) {
			JLabel icon = new JLabel();
			icon.setSize(container.getSize());
			icon.setIcon(new ImageIcon(resize(img, icon.getWidth(), icon.getHeight())));
			container.add(icon);
			container.repaint();
			container.revalidate();
		}
		if(reference != null)
			reference.set(img);
		if(imageHandler != null)
			imageHandler.accept(img);
		
		if(repaintComponent != null) {
			repaintComponent.repaint();
			repaintComponent.revalidate();
		}
	}
	
	public static ImageUtil loadFile(String path) {
		return loadFile(new File(path));
	}
	public static ImageUtil loadFile(File file) {
		return new ImageUtil(file);
	}
	public static ImageUtil loadURL(String url) throws MalformedURLException {
		return loadURL(new URL(url));
	}
	public static ImageUtil loadURL(URL url) {
		return new ImageUtil(url);
	}
	
	public ImageUtil to(JComponent container) {
		this.container = container;
		
		return this;
	}
	public ImageUtil to(AtomicReference<Image> reference) {
		this.reference = reference;
		
		return this;
	}
	public ImageUtil to(Consumer<Image> handler) {
		this.imageHandler = handler;
		
		return this;
	}
	public ImageUtil to(JComponent container, AtomicReference<Image> reference) {
		this.container = container;
		this.reference = reference;
		
		return this;
	}
	public ImageUtil repaint(JComponent component) {
		this.repaintComponent = component;
		
		return this;
	}
	public AtomicReference<Image> getReference() {
		return reference;
	}
	
	public ImageUtil catchErr(Consumer<Exception> errHandler) {
		this.errHandler = errHandler;
		
		return this;
	}
	
	public Image sync() throws InterruptedException {
		syncRequested = true;
		thread.join();
		return img;
	}
	
	public static Image resize(Image img, int width, int height) {
		var scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		BufferedImage bufferedImage= new BufferedImage(scaledImg.getWidth(null), scaledImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
		var g = bufferedImage.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
		g.drawImage(img, 0, 0, scaledImg.getWidth(null), scaledImg.getHeight(null), null);
		
		return bufferedImage;
	}
}
