package serialPort;

import gnu.io.SerialPort;
import java.util.Date;
import serialException.SendDataToSerialPortFailure;
import serialException.SerialPortOutputStreamCloseFailure;
import ServiceClient.ServiceClient;

public class AnswerCmd {

	static Thread reciveFromPort;

	public static void controlPort(String cmd) {
		
		if (cmd.contains("startУ׼")) {
			String[] comPorts1 = new String[] { "COM3" };
			if (RecieveFromPort.serialCom == null) {
				RecieveFromPort.serialCom = new SerialPort[1];
				for (int i = 0; i < comPorts1.length; i++) {
					try {
						RecieveFromPort.serialCom[i] = SerialTool.openPort(
								comPorts1[i], 115200);
						RecieveFromPort.serialCom[i].setDTR(false);// ���û����DTRλ
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
			}

			// ���������߳�
			reciveFromPort = new Thread(new RecieveFromCom());
			RecieveFromCom.isRun = true;
			RecieveFromCom.isBengin = true;
			RecieveFromCom.isRound = false;
			reciveFromPort.setName("���ڼ���");
			reciveFromPort.setPriority(10);
			reciveFromPort.start();

			// ��������
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

			ServiceClient.textArea.append(ServiceClient.df.format(new Date())
					+ " " + "��ʼУ׼...");
			ServiceClient.textArea.append("\r\n");
		}

		if (cmd.contains("stopУ׼")) {
			ServiceClient.textArea.append(ServiceClient.df.format(new Date())
					+ " " + "ֹͣУ׼...");
			ServiceClient.textArea.append("\r\n");
		}
		if (cmd.contains("��ʼ")) {
			StringBuffer sb = new StringBuffer();
			String diameter = sb.append(cmd).substring(10, cmd.length());
			GetSpeed.diameter = Double.parseDouble(diameter)/100;  //��������cm,ת��m
			new Thread(new GetSpeed()).start();// ���������߳�
			ServiceClient.textArea.append(ServiceClient.df.format(new Date())
					+ " " + "��ʼ��ش��ʹ��ٶ�...");
			ServiceClient.textArea.append("\r\n");
		}
		if (cmd.contains("ֹͣ")) {
			if (GetSpeed.timer != null) {
				GetSpeed.timer.cancel();// ȡ����ʱ��
			}
			if (RecieveFromPort.serialCom != null) {
				SerialTool.stopMeasure();// ֹͣ�״�ɨ��
			}
			ServiceClient.textArea.append(ServiceClient.df.format(new Date())
					+ " " + "ֹͣ��ش��ʹ��ٶ�...");
			ServiceClient.textArea.append("\r\n");
		}
	}
}