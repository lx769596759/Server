package serialPort;

import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import serialException.NoSuchPort;
import serialException.NotASerialPort;
import serialException.PortInUse;
import serialException.SendDataToSerialPortFailure;
import serialException.SerialPortOutputStreamCloseFailure;
import serialException.SerialPortParameterFailure;
import ServiceClient.ServiceClient;
import dbUtility.dbTools;

public class RecieveFromPort implements Runnable {

	public static boolean isRun = false;
	public static ExecutorService cachedThreadPool = Executors
			.newCachedThreadPool();
	static boolean isBengin = true;
	static boolean isRound = false;
	public static double initdata;
	public static SerialPort[] serialCom;

	public void run() {
		System.out.println("���������ռ��߳�");
		// �������ݿ��ȡ�����ʼֵ

		try {
			ResultSet rs = dbTools.sqlSelect("select * from table_2");
			while (rs.next()) {
				String value = rs.getString("У׼ֵ");
				RecieveFromPort.initdata = Double.parseDouble(value);
			}
		} catch (Exception e1) {
			ServiceClient.logger.error("��ȡУ׼ֵʧ��", e1);
		}

		String[] comPorts = new String[] { "COM3" };
		if (serialCom == null) {
			serialCom = new SerialPort[1];
			for (int i = 0; i < comPorts.length; i++) {
				try {
					serialCom[i] = SerialTool.openPort(comPorts[i], 115200);
					serialCom[i].setDTR(false);// ���û����DTRλ
				} catch (SerialPortParameterFailure | NotASerialPort
						| NoSuchPort | PortInUse e) {
					ServiceClient.logger.error(e.getMessage(), e);
				}
			}
			if (serialCom != null) {
				ServiceClient.textArea.append(ServiceClient.df
						.format(new Date()) + " " + "�ѳɹ��򿪴���,���ݽ�����...");
				ServiceClient.textArea.append("\r\n");
			} else {
				ServiceClient.textArea.append(ServiceClient.df
						.format(new Date()) + " " + "�򿪴���ʧ��...");
				ServiceClient.textArea.append("\r\n");
			}
		}
		// ��ʼ��ÿ�����ڵ�����
		else {
			try {
				for (int j = 0; j < serialCom.length; j++) {
					byte[] b1 = new byte[10240];
					int temp = 0;
					while ((temp = (RecieveFromPort.serialCom[j]
							.getInputStream()).read(b1)) != 0) {
						String str = new String(b1);
						System.out.println(str);
						System.out.println(temp);
						b1 = new byte[10240];
					}
					b1 = null;
					RecieveFromPort.serialCom[j].getInputStream().close();
					RecieveFromPort.serialCom[j].getOutputStream().close();
				}
			} catch (Exception e) {
				ServiceClient.logger.error("��ʼ����������ʧ��", e);
			}
		}
		// ��������
		 byte[] startMotor=new byte[]{(byte) 0xA5,(byte) 0xF0,0x02,(byte)
		 0xb8, 0x01, (byte) 0xee}; //440
		// byte[] startMotor=new byte[]{(byte) 0xA5,(byte) 0xF0,0x02,(byte)
		// 0xa4, 0x01, (byte) 0xf2}; //420
		//byte[] startMotor = new byte[] { (byte) 0xA5, (byte) 0xF0, 0x02,
				//(byte) 0x90, 0x01, (byte) 0xc6 }; // 400
		byte[] startScan = new byte[] { (byte) 0xA5, 0x20 };
		try {
			for (int j = 0; j < serialCom.length; j++) {
				SerialTool.sendToPort(serialCom[j], startMotor);
				Thread.sleep(100);
				SerialTool.sendToPort(serialCom[j], startScan);
			}

		} catch (SendDataToSerialPortFailure
				| SerialPortOutputStreamCloseFailure | InterruptedException e) {
			ServiceClient.logger.error(e.getMessage(), e);
		}

		// ѭ���������Դ��ڵ�����
		out: while (isRun == true) {
			while (isBengin == true) {
				try {
					InputStream inputStream = serialCom[0].getInputStream();
					int count = 12;
					byte[] b = new byte[count];
					int readCount = 0; // �Ѿ��ɹ���ȡ���ֽڵĸ���
					while (readCount < count) {
						if (isRun == false) {
							break out;
						}
						readCount += inputStream.read(b, readCount, count
								- readCount);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				isBengin = false;
			}
			System.out.println("��ʼ�ռ�����");
			ArrayList<String> list = new ArrayList<String>();
			while (true) {
				InputStream inputStream = null;
				try {
					inputStream = serialCom[0].getInputStream();
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
							cachedThreadPool.execute(new DataHanding(
									new ArrayList<String>(list), 1)); // ���̳߳ش���list
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
