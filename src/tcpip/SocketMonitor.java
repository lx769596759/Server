package tcpip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import org.apache.log4j.Logger;

import ServiceClient.ServiceClient;

public class SocketMonitor implements Runnable {
	private Socket socket;
	public static Logger logger = Logger.getLogger(SocketMonitor.class);

	public SocketMonitor(Socket s) {
		this.socket = s;
	}

	public void run() {
		while (true) {
			try {
				InputStream is = socket.getInputStream();// 字节输入流
				InputStreamReader isr = new InputStreamReader(is);// 将字节流转为字符流
				BufferedReader br = new BufferedReader(isr);// 为输入流添加缓冲
				String info = null;
				if ((info = br.readLine()) != null) {
					System.out.println("接收命令:" + info);
				}
			} catch (IOException e) {
				logger.error("检测到Socket断开："
						+ socket.getInetAddress().getHostAddress());
				ServiceClient.socketList.remove(socket);
				socket = null;
				break;
			}
		}
	}
}
