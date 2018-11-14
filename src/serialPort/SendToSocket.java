package serialPort;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import ServiceClient.ServiceClient;

/*
 * 该类用于Socket对外发送数据
 */
public class SendToSocket extends Thread {
	private String message;

	public SendToSocket(String message) {
		this.message = message;
		this.start();
	}

	// 循环向每个连接上的socket发送数据
	@Override
	public void run() {
		if (ServiceClient.socketList.isEmpty()) {
			return;
		}
		try {
			for (Socket socket : ServiceClient.socketList) {
				// 向客户端传递的信息
				OutputStream os = socket.getOutputStream();
				PrintWriter pw = new PrintWriter(os);// 包装打印流
				String test = message + "\n";
				System.out.println(test);
				pw.write(test);
				pw.flush();
			}
			// 关闭资源
			// printWriter.close();
			// ots.close();
		} catch (IOException e) {
			ServiceClient.logger.error("Socket消息发送失败", e);
		}
	}
}
