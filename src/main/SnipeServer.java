package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SnipeServer {
	public static void host(int port) throws IOException {
		ServerSocket server = new ServerSocket(port);
		
		boolean running = true;
		while(running) {
			Socket client = server.accept();
			
			new Thread(() -> {
				try {
					var reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
//					var writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
					
					String line = "";
					while(!line.contains("\"clan:\"") || !line.contains("\"player\":\"")) {
						line += reader.readLine();
					}
					
//					var cStart = line.indexOf("\"clan\":\"") + 6;
//					var cEnd = line.indexOf("\"", cStart);
//					var clan = line.substring(cStart, cEnd);
					
//					var pStart = line.indexOf("\"player\":\"") + 10;
//					var pEnd = line.indexOf("\"", pStart);
//					var player = line.substring(pStart, pEnd);
					
					// TODO search
				} catch(IOException e) {
					e.printStackTrace();
				}
			}).start();
		}
		
		server.close();
	}
}
