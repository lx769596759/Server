package serialPort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import dbUtility.WriteToDb;
import dbUtility.dbTools;
import ServiceClient.ServiceClient;

public class DataOperater implements Runnable {

	dbTools db = new dbTools();
	public static boolean exit = false;
	private static Connection conn;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public void run() {
		try {
			conn = dbTools.getConn(dbTools.driverName, dbTools.dbUrl,
					dbTools.us, dbTools.pw);
		} catch (Exception e2) {
			ServiceClient.logger.error("���ݿ�����ʧ��", e2);
		}
		try {
			while (true) {

				if (exit == true) {
					conn.close();
					break;
				}
				String sql1 = "Select top 1 * from table_1 where READFLAG=0 order by ��¼ʱ��";// ֻȡREADFLAG=0������
				String sql2 = "Select top 1 * from table_3 where READFLAG=0 order by ��¼ʱ��";// ֻȡREADFLAG=0������
				try {

					String result1 = db.ddlSelect(sql1, conn, "������ֵ", "��¼ʱ��");
					String result2 = db.ddlSelect(sql2, conn, "�ٶ�", "��¼ʱ��");

					if (result1 == null) {
						if (result2 == null)// ���������Ϊ�գ������ѭ��
						{
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								ServiceClient.logger.error(e.getMessage(), e);
							}
							continue;
						} else if (result2 != null)// �ٶ���ֵ��������ֵ
						{
							if (result2.split(",")[1].equals("0.00")) // �ٶ����Ϊ0
							{
								String time = result2.split(",")[0];
								// ǰ��ʵʱ��ʾ
								String recordTime = sdf.format(new Date());
								ServiceClient.tf_speed.setText("0");
								ServiceClient.tf_speed2.setText("0");
								ServiceClient.recordTime.setText("��¼ʱ�䣺"
										+ recordTime);
								
								//ͨ��Socket���ⷢ����Ϣ
								String message = "speed=0" + ";" + "time=" + recordTime;
								new SendToSocket(message);
								
								//�������ݿ�״̬
								String sql = "update table_3 set READFLAG=10 where ��¼ʱ��="
										+ "'" + time + "'";
								try {
									db.modify(sql, conn);
								} catch (Exception e1) {
									ServiceClient.logger.error(e1.getMessage(),e1);
								}
								continue;
							} else // �ٶȲ�Ϊ0
							{
								String[] array = result2.split(",");
								String time = array[0];
								String sql = "update table_3 set READFLAG=2 where ��¼ʱ��="
										+ "'" + time + "'";
								try {
									db.modify(sql, conn);
								} catch (Exception e1) {
									ServiceClient.logger.error(e1.getMessage(),e1);
								}
								continue;
							}
						}
					} else {
						if (result2 == null)// �ٶ���ֵ��������ֵ���������һ��ѭ����ֱ���ٶ���ֵ
						{
							continue;
						}

						else// ��������ֵ���Ա�����ʱ����Ƿ���2S�ڣ���Y����н�һ�����㣬��N������ʱ��С��ֵ
						{
							String[] array1 = result1.split(",");
							String time1 = array1[0];
							String[] array2 = result2.split(",");
							String time2 = array2[0];
							DateFormat df = new SimpleDateFormat(
									"yyyy-MM-dd HH:mm:ss.SSS");
							Date d1 = df.parse(time1);
							Date d2 = df.parse(time2);
							long diff = d1.getTime() - d2.getTime();// ��ֵ�Ǻ��뼶��
							if (Math.abs(diff) > 2000)// ������2s������ȥʱ���С���Ǹ�
							{
								if (diff > 0) {
									String sql = "update table_3 set READFLAG=2 where ��¼ʱ��="
											+ "'" + time2 + "'";
									try {
										db.modify(sql, conn);
									} catch (Exception e1) {
										ServiceClient.logger.error(
												e1.getMessage(), e1);
									}
									continue;
								} else {
									String sql = "update table_1 set READFLAG=2 where ��¼ʱ��="
											+ "'" + time1 + "'";
									try {
										db.modify(sql, conn);
									} catch (Exception e1) {
										ServiceClient.logger.error(
												e1.getMessage(), e1);
									}
									continue;
								}
							}
						}
					}
					// ��ȡ���ֵ
					String[] array = result1.split(",");
					String time = array[0];
					double value = Double.parseDouble(array[1]);

					// ��ȡ�ٶ�ֵ
					String[] array2 = result2.split(",");
					String time2 = array2[0];
					double speed = Double.parseDouble(array2[1]);

					// ��������ٶ�
					double finalValue = value * speed;
					finalValue = (double) Math.round(finalValue * 100000) / 100000; // ������λС��
					// �������ٶȴ������ݿ���
					new Thread(new WriteToDb(String.valueOf(finalValue), 3))
							.start();

					// ǰ��ʵʱ��ʾ
					String recordTime = sdf.format(new Date());
					ServiceClient.tf_speed.setText(String.valueOf(speed));
					ServiceClient.tf_speed2.setText(String.valueOf(finalValue));
					ServiceClient.recordTime.setText("��¼ʱ�䣺"
							+ recordTime);
					
					//ͨ��Socket���ⷢ����Ϣ
					String message = "speed=" + String.valueOf(finalValue) + ";" + "time=" + recordTime;
					new SendToSocket(message); 

					try {
						String sql3 = "update table_1 set READFLAG=1 where ��¼ʱ��="
								+ "'" + time + "'";
						String sql4 = "update table_3 set READFLAG=1 where ��¼ʱ��="
								+ "'" + time2 + "'";
						db.modify(sql3, conn);
						db.modify(sql4, conn);
						Thread.sleep(500);
					} catch (Exception e) {
						ServiceClient.logger.error(e.getMessage(), e);
					}

				} catch (SQLException e) {
					ServiceClient.logger.error(e.getMessage(), e);
					exit = true;
				}
			}

		} catch (Exception e) {
			ServiceClient.logger.error(e.getMessage(), e);
		}
	}

	class SendToSocket extends Thread {
		private String message;

		public SendToSocket(String message) {
			this.message = message;
			this.start();
		}

		//ѭ����ÿ�������ϵ�socket��������
		public void run() {
			if (ServiceClient.socketList.size() == 0) {
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
//				printWriter.close();
//				ots.close();
			} catch (IOException e) {
				ServiceClient.logger.error("Socket��Ϣ����ʧ��", e);
			}
		}

	}
}
