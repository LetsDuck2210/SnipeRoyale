package util;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class ImageUtil {
	private static Map<String, Image> images = new HashMap<>();
	public static Image load(String path) throws IOException {
		return load(new File(path));
	}
	public static Image load(File file) throws IOException {
		if(images.containsKey(file.getAbsolutePath()))
			return images.get(file.getAbsolutePath());
		
		Image img = ImageIO.read(file);
		images.put(file.getAbsolutePath(), img);
		
		return img;
	}
	
	public static Image resize(Image img, int width, int height) {
		var scaledImg = img.getScaledInstance(width, height, Image.SCALE_FAST);
		BufferedImage bufferedImage= new BufferedImage(scaledImg.getWidth(null), scaledImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
		var g = bufferedImage.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
		g.drawImage(img, 0, 0, scaledImg.getWidth(null), scaledImg.getHeight(null), null);
		
		return bufferedImage;
	}
}
