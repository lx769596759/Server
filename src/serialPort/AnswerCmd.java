package serialPort;

import gnu.io.SerialPort;

import java.util.Date;

import org.apache.log4j.Logger;

import serialException.SendDataToSerialPortFailure;
import serialException.SerialPortOutputStreamCloseFailure;
import ServiceClient.ServiceClient;

public class AnswerCmd {

	static Thread reciveFromPort;
	private static Logger logger = Logger.getLogger(AnswerCmd.class);

	public static void controlPort(String cmd) {
		
		if (cmd.contains("start校准")) {
			String[] comPorts1 = new String[] { "COM3" };
			if (RecieveFromPort.serialCom == null) {
				RecieveFromPort.serialCom = new SerialPort[1];
				for (int i = 0; i < comPorts1.length; i++) {
					try {
						RecieveFromPort.serialCom[i] = SerialTool.openPort(
								comPorts1[i], 115200);
						RecieveFromPort.serialCom[i].setDTR(false);// 设置或清除DTR位
					} catch (Exception e) {
						logger.error("雷达串口打开失败",e);
						return;
					}
				}
			}

			// 启动接收线程
			reciveFromPort = new Thread(new RecieveFromCom());
			RecieveFromCom.isRun = true;
			RecieveFromCom.isBengin = true;
			RecieveFromCom.isRound = false;
			reciveFromPort.setName("串口监听");
			reciveFromPort.setPriority(10);
			reciveFromPort.start();

			// 发送数据
			byte[] startMotor = new byte[] { (byte) 0xA5, (byte) 0xF0, 0x02,
					(byte) 0xa9, 0x01, (byte) 0xff };
			byte[] startScan = new byte[] { (byte) 0xA5, 0x20 };
			try {
				for (int j = 0; j < RecieveFromPort.serialCom.length; j++) {
					SerialTool.sendToPort(RecieveFromPort.serialCom[j],
							startMotor);
					Thread.sleep(100);
					SerialTool.sendToPort(RecieveFromPort.serialCom[j],
							startScan);
				}

			} catch (SendDataToSerialPortFailure
					| SerialPortOutputStreamCloseFailure | InterruptedException e) {
				ServiceClient.logger.error(e.getMessage(), e);
			}
			ServiceClient.showMessage("normal", "开始校准...");
		}

		if (cmd.contains("stop校准")) {
			ServiceClient.showMessage("normal", "停止校准...");
		}
		if (cmd.contains("开始")) {
			StringBuffer sb = new StringBuffer();
			String diameter = sb.append(cmd).substring(10, cmd.length());
			GetSpeed.diameter = Double.parseDouble(diameter)/100;  //传过来是cm,转成m
			new Thread(new GetSpeed()).start();// 开启测速线程
		}
		if (cmd.contains("停止")) {
			if (GetSpeed.timer != null) {
				GetSpeed.timer.cancel();// 取消计时器
			}
			if (RecieveFromPort.serialCom != null) {
				SerialTool.stopMeasure();// 停止雷达扫描
			}
			ServiceClient.showMessage("normal", "停止监控传送带速度...");
		}
	}
}
