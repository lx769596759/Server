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
		System.out.println("开启数据收集线程");
		// 连接数据库获取检测点初始值

		try {
			ResultSet rs = dbTools.sqlSelect("select * from table_2");
			while (rs.next()) {
				String value = rs.getString("校准值");
				RecieveFromPort.initdata = Double.parseDouble(value);
			}
		} catch (Exception e1) {
			ServiceClient.logger.error("获取校准值失败", e1);
		}

		String[] comPorts = new String[] { "COM3" };
		if (serialCom == null) {
			serialCom = new SerialPort[1];
			for (int i = 0; i < comPorts.length; i++) {
				try {
					serialCom[i] = SerialTool.openPort(comPorts[i], 115200);
					serialCom[i].setDTR(false);// 设置或清除DTR位
				} catch (SerialPortParameterFailure | NotASerialPort
						| NoSuchPort | PortInUse e) {
					ServiceClient.logger.error(e.getMessage(), e);
				}
			}
			if (serialCom != null) {
				ServiceClient.textArea.append(ServiceClient.df
						.format(new Date()) + " " + "已成功打开串口,数据接收中...");
				ServiceClient.textArea.append("\r\n");
			} else {
				ServiceClient.textArea.append(ServiceClient.df
						.format(new Date()) + " " + "打开串口失败...");
				ServiceClient.textArea.append("\r\n");
			}
		}
		// 初始化每个串口的数据
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
				ServiceClient.logger.error("初始化串口数据失败", e);
			}
		}
		// 发送数据
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

		// 循环接收来自串口的数据
		out: while (isRun == true) {
			while (isBengin == true) {
				try {
					InputStream inputStream = serialCom[0].getInputStream();
					int count = 12;
					byte[] b = new byte[count];
					int readCount = 0; // 已经成功读取的字节的个数
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
			System.out.println("开始收集数据");
			ArrayList<String> list = new ArrayList<String>();
			while (true) {
				InputStream inputStream = null;
				try {
					inputStream = serialCom[0].getInputStream();
					int count = 5; // 总共要取的字节的个数
					byte[] b = new byte[count];
					int readCount = 0; // 已经成功读取的字节的个数
					while (readCount < count) {
						if (isRun == false) {
							break out;
						}
						readCount += inputStream.read(b, readCount, count
								- readCount);
					}
					StringBuilder buf = new StringBuilder(b.length * 2);
					StringBuffer result = new StringBuffer();
					if ((b[0] & 0x1) == 1) // 最后一位为1，代表新的一圈360°扫描的开始
					{
						if (isRound == false) {
							isRound = true;
						}
						if (list.isEmpty() == false)// 如果list不为空
						{
							cachedThreadPool.execute(new DataHanding(
									new ArrayList<String>(list), 1)); // 用线程池处理list
						}
						list.clear(); // 清空list
						for (byte bb : b) { // 使用String的format方法进行转换
							result.append(Long.toString(bb & 0xff, 2) + ",");
							buf.append(String.format("%02x", new Integer(
									bb & 0xff)));
						}
						if (b[3] != 0 || b[4] != 0) {
							list.add(buf.toString()); // 将5个16进制的字符加入到list
							// System.out.println(result.toString().substring(0,
							// result.length()-1));
							// System.out.println(buf);
						}
					} else {
						if (isRound == false) // 如果不是在新的一周里，则放弃数据，保证取到完整一周的数据
						{
							continue; // 不做任何动作，直接继续取数据
						} else {
							for (byte bb : b) { // 使用String的format方法进行转换
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
