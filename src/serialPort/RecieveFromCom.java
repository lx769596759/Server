package serialPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import ServiceClient.ServiceClient;

public class RecieveFromCom implements Runnable {
	
	public static boolean isRun = true;
	public static boolean isBengin = true;
	public static boolean isRound = false;

	public void run() {

		out: while (isRun == true) {
			while (isBengin == true) {
				try {
					InputStream inputStream = RecieveFromPort.serialCom[0]
							.getInputStream();
					int count = 12;
					byte[] b = new byte[count];
					int readCount = 0; // �Ѿ��ɹ���ȡ���ֽڵĸ���
					while (readCount < count) {
						readCount += inputStream.read(b, readCount, count
								- readCount);
					}
				} catch (IOException e) {
					ServiceClient.logger.error("��ȡ��һȦ���ݴ���", e);
				}
				isBengin = false;
			}
			System.out.println("��ʼ�ռ�����");
			ArrayList<String> list = new ArrayList<String>();
			while (true) {
				InputStream inputStream = null;
				try {
					inputStream = RecieveFromPort.serialCom[0].getInputStream();
					int count = 5; // �ܹ�Ҫȡ���ֽڵĸ���
					byte[] b = new byte[count];
					int readCount = 0; // �Ѿ��ɹ���ȡ���ֽڵĸ���
					while (readCount < count) {
						if (isRun == false) {
							break out;
						}
						readCount += inputStream.read(b, readCount, count
								- readCount);
					}
					StringBuilder buf = new StringBuilder(b.length * 2);
					StringBuffer result = new StringBuffer();
					if ((b[0] & 0x1) == 1) // ���һλΪ1�������µ�һȦ360��ɨ��Ŀ�ʼ
					{
						if (isRound == false) {
							isRound = true;
						}
						if (list.isEmpty() == false)// ���list��Ϊ��
						{
							new Thread((new DataHanding(new ArrayList<String>(
									list), 2))).start(); // ���̳߳ش���list
						}
						list.clear(); // ���list
						for (byte bb : b) { // ʹ��String��format��������ת��
							result.append(Long.toString(bb & 0xff, 2) + ",");
							buf.append(String.format("%02x", new Integer(
									bb & 0xff)));
						}
						if (b[3] != 0 || b[4] != 0) {
							list.add(buf.toString()); // ��5��16���Ƶ��ַ����뵽list
							// System.out.println(result.toString().substring(0,
							// result.length()-1));
							// System.out.println(buf);
						}
					} else {
						if (isRound == false) // ����������µ�һ�����������ݣ���֤ȡ������һ�ܵ�����
						{
							continue; // �����κζ�����ֱ�Ӽ���ȡ����
						} else {
							for (byte bb : b) { // ʹ��String��format��������ת��
								result.append(Long.toString(bb & 0xff, 2) + ",");
								buf.append(String.format("%02x", new Integer(
										bb & 0xff)));
							}
							if (b[3] != 0 || b[4] != 0) {
								list.add(buf.toString());
								// System.out.println(result.toString().substring(0,
								// result.length()-1));
								// System.out.println(buf);
							}
						}
					}
				} catch (IOException e1) {
					ServiceClient.logger.error(e1.getMessage(), e1);
				}

			}
		}
	}
}
