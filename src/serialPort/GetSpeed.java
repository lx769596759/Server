package serialPort;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TooManyListenersException;

import org.apache.log4j.Logger;

import dbUtility.WriteToDb;
import ServiceClient.ServiceClient;
import serialException.NoSuchPort;
import serialException.NotASerialPort;
import serialException.PortInUse;
import serialException.SendDataToSerialPortFailure;
import serialException.SerialPortOutputStreamCloseFailure;
import serialException.SerialPortParameterFailure;

public class GetSpeed implements SerialPortEventListener, Runnable {
	public static SerialPort serialCom;
	public static Timer timer;
	public static double diameter;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static Logger logger = Logger.getLogger(GetSpeed.class);

	public void run() {
		if (serialCom == null) {
			try {
				serialCom = SerialTool.openPort("COM2", 9600);
				serialCom.addEventListener(new GetSpeed());
				serialCom.notifyOnDataAvailable(true);
			} catch (Exception e) {
				logger.error("计数器串口打开失败",e);
				return;
			}
		}
		timer = new Timer();
		GetSpeed gs = new GetSpeed();
		GetSpeed.GetSpeedTask mt = gs.new GetSpeedTask(serialCom);
		timer.schedule(mt, 1, 1000);
		ServiceClient.showMessage("normal", "开始监控传送带速度...");
	}

	class GetSpeedTask extends TimerTask {
		private SerialPort serialCom;
		byte[] cmd = new byte[] { 0x01, 0x03, 0x00, 0x62, 0x00, 0x02, 0x65,
				(byte) 0xD5 };

		public GetSpeedTask(SerialPort serialCom) {
			this.serialCom = serialCom;
		}

		public void run() {
			try {
				SerialTool.sendToPort(serialCom, cmd);
			} catch (SendDataToSerialPortFailure
					| SerialPortOutputStreamCloseFailure e) {
				e.printStackTrace();
			}
		}
	}

	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
		case SerialPortEvent.BI:/* Break interrupt,通讯中断 */
		case SerialPortEvent.OE:/* Overrun error，溢位错误 */
		case SerialPortEvent.FE:/* Framing error，传帧错误 */
		case SerialPortEvent.PE:/* Parity error，校验错误 */
		case SerialPortEvent.CD:/* Carrier detect，载波检测 */
		case SerialPortEvent.CTS:/* Clear to send，清除发送 */
		case SerialPortEvent.DSR:/* Data set ready，数据设备就绪 */
		case SerialPortEvent.RI:/* Ring indicator，响铃指示 */
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:/*
												 * Output buffer is
												 * empty，输出缓冲区清空
												 */
			break;
		case SerialPortEvent.DATA_AVAILABLE:/*
											 * Data available at the serial
											 * port，端口有可用数据。读到缓冲数组，输出到终端
											 */
			InputStream inputStream = null;
			try {
				inputStream = serialCom.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			int count = 9;
			byte[] b = new byte[count];
			int readCount = 0; // 已经成功读取的字节的个数
			while (readCount < count) {
				try {
					readCount += inputStream.read(b, readCount, count
							- readCount);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// 转换成十六进制
			StringBuilder buf = new StringBuilder(b.length * 2);
			for (byte bb : b) { // 使用String的format方法进行转换
				buf.append(String.format("%02x", new Integer(bb & 0xff)));
			}
			// System.out.println(buf);
			StringBuffer sb = new StringBuffer();
			String data1 = sb.append(buf).substring(6, 10);
			int speed1 = Integer.parseInt(data1, 16);// 十六进制转化成十进制
			double finalSpeed = (double) speed1 * 3.14 * diameter / 60;// 转速乘以周长得到线速度,转速单位为n/min，所以除以60得到n/s
			BigDecimal bg = new BigDecimal(finalSpeed);
			double lineSpeed = bg.setScale(2, BigDecimal.ROUND_HALF_UP)
					.doubleValue();
			System.out.println("线速度=" + lineSpeed);
			
			// 速度控制雷达
			controlLidar(lineSpeed);  

		}
	}
	
	public void controlLidar (double lineSpeed) {
		if (lineSpeed > 0) {
			if (RecieveFromPort.isRun == false) {
				// 启动串口数据接收线程
				RecieveFromPort.isRun = true;
				RecieveFromPort.isBengin = true;
				RecieveFromPort.isRound = false;
				Thread reciveFromPort = new Thread(new RecieveFromPort());
				reciveFromPort.setName("串口数据接收");
				ServiceClient.showMessage("normal", "开始接收数据...");
				reciveFromPort.setPriority(10);
				reciveFromPort.start();
			}
			// 将速度写入数据库
			new Thread(new WriteToDb(String.valueOf(lineSpeed), 2)).start();
		} else {  //速度为0，不写入数据库，界面展示+Socket发送
			if (RecieveFromPort.isRun == true) {
				SerialTool.stopMeasure();// 停止雷达测量
				ServiceClient.showMessage("normal", "停止接收数据...");
			}
			
			// 前端实时显示
			String recordTime = sdf.format(new Date());
			ServiceClient.tf_speed.setText("0");
			ServiceClient.tf_speed2.setText("0");
			ServiceClient.recordTime.setText("记录时间："
					+ recordTime);
			
			//通过Socket向外发送消息
			String message = "speed=0" + ";" + "time=" + recordTime;
			new SendToSocket(message);
		}
	}
}
