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
					int readCount = 0; // 已经成功读取的字节的个数
					while (readCount < count) {
						readCount += inputStream.read(b, readCount, count
								- readCount);
					}
				} catch (IOException e) {
					ServiceClient.logger.error("读取第一圈数据错误", e);
				}
				isBengin = false;
			}
			System.out.println("开始收集数据");
			ArrayList<String> list = new ArrayList<String>();
			while (true) {
				InputStream inputStream = null;
				try {
					inputStream = RecieveFromPort.serialCom[0].getInputStream();
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
							new Thread((new DataHanding(new ArrayList<String>(
									list), 2))).start(); // 用线程池处理list
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
