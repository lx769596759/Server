package serialPort;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import ServiceClient.ServiceClient;

/*
 * ��������Socket���ⷢ������
 */
public class SendToSocket extends Thread {
	private String message;

	public SendToSocket(String message) {
		this.message = message;
		this.start();
	}

	// ѭ����ÿ�������ϵ�socket��������
	@Override
	public void run() {
		if (ServiceClient.socketList.isEmpty()) {
			return;
		}
		try {
			for (Socket socket : ServiceClient.socketList) {
				// ��ͻ��˴��ݵ���Ϣ
				OutputStream os = socket.getOutputStream();
				PrintWriter pw = new PrintWriter(os);// ��װ��ӡ��
				String test = message + "\n";
				System.out.println(test);
				pw.write(test);
				pw.flush();
			}
			// �ر���Դ
			// printWriter.close();
			// ots.close();
		} catch (IOException e) {
			ServiceClient.logger.error("Socket��Ϣ����ʧ��", e);
		}
	}
}
