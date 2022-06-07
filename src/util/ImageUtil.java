package util;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ImageUtil {
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
