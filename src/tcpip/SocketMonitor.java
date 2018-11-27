package tcpip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import org.apache.log4j.Logger;

import serialPort.SpeedController;
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
				InputStream is = socket.getInputStream();// �ֽ�������
				InputStreamReader isr = new InputStreamReader(is);// ���ֽ���תΪ�ַ���
				BufferedReader br = new BufferedReader(isr);// Ϊ��������ӻ���
				String info = null;
				if ((info = br.readLine()) != null) {
					System.out.println("��������:" + info);
					String speed = info.split("=")[1];
					new SpeedController(speed);  //�ٶȴ����߳�
				}
			} catch (IOException e) {
				logger.info("��⵽Socket�Ͽ���"
						+ socket.getInetAddress().getHostAddress());
				ServiceClient.socketList.remove(socket);
				try {
					socket.close();
				} catch (IOException e1) {
					logger.error("Socket�ر�ʧ��",e1);					
				} finally {
					socket = null;
				}
				break;
			}
		}
	}
}
