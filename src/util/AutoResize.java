package util;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

import javax.swing.JFrame;

public class AutoResize {
	public static void resize(JFrame frame) {
		new Thread(() -> {
			try {
				Thread.sleep(250);
				while(!frame.isEnabled()) {
					Thread.sleep(10);
				}
				
				Robot robot = new Robot();
				
				Point p = MouseInfo.getPointerInfo().getLocation();
				
				int x = frame.getX() + frame.getWidth(),
					y = frame.getY() + frame.getHeight();
				
				frame.setResizable(true);
				robot.mouseMove(x, y);
				robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
				robot.mouseMove(x + 1, y + 1);
				robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
				robot.mouseMove(p.x, p.y);
//				frame.setResizable(false);
			} catch (AWTException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}
}
