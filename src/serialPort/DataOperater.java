package serialPort;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import ServiceClient.ServiceClient;
import dbUtility.WriteToDb;
import dbUtility.dbTools;

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
			ServiceClient.logger.error("数据库连接失败", e2);
		}
		try {
			while (true) {

				if (exit) {
					conn.close();
					break;
				}
				String sql1 = "Select top 1 * from table_1 where READFLAG=0 order by 记录时间";// 只取READFLAG=0的数据
				String sql2 = "Select top 1 * from table_3 where READFLAG=0 order by 记录时间";// 只取READFLAG=0的数据
				try {

					String result1 = db.ddlSelect(sql1, conn, "测量均值", "记录时间");
					String result2 = db.ddlSelect(sql2, conn, "速度", "记录时间");

					if (result1 == null) {
						if (result2 == null)// 两个结果都为空，则继续循环
						{
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								ServiceClient.logger.error(e.getMessage(), e);
								Thread.currentThread().interrupt();
							}
							continue;
						} else // 速度有值，面积无值
						{
							String[] array = result2.split(",");
							String time = array[0];
							String sql = "update table_3 set READFLAG=2 where 记录时间="
									+ "'" + time + "'";
							try {
								db.modify(sql, conn);
							} catch (Exception e1) {
								ServiceClient.logger.error(e1.getMessage(), e1);
							}
							continue;
						}
					} else {
						if (result2 == null)// 速度无值，面积有值，则进入下一个循环，直到速度有值
						{
							continue;
						}

						else// 两个均有值，对比两个时间差是否在2S内，如Y则进行进一步计算，如N则舍弃时间小的值
						{
							String[] array1 = result1.split(",");
							String time1 = array1[0];
							String[] array2 = result2.split(",");
							String time2 = array2[0];
							DateFormat df = new SimpleDateFormat(
									"yyyy-MM-dd HH:mm:ss.SSS");
							Date d1 = df.parse(time1);
							Date d2 = df.parse(time2);
							long diff = d1.getTime() - d2.getTime();// 差值是毫秒级别
							if (Math.abs(diff) > 2000)// 相差大于2s，则舍去时间更小的那个
							{
								if (diff > 0) {
									String sql = "update table_3 set READFLAG=2 where 记录时间="
											+ "'" + time2 + "'";
									try {
										db.modify(sql, conn);
									} catch (Exception e1) {
										ServiceClient.logger.error(
												e1.getMessage(), e1);
									}
									continue;
								} else {
									String sql = "update table_1 set READFLAG=2 where 记录时间="
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
					// 获取面积值
					String[] array = result1.split(",");
					String time = array[0];
					double value = Double.parseDouble(array[1]);

					// 获取速度值
					String[] array2 = result2.split(",");
					String time2 = array2[0];
					double speed = Double.parseDouble(array2[1]);

					// 算出出土速度
					double finalValue = value * speed;
					finalValue = (double) Math.round(finalValue * 100000) / 100000; // 保留五位小数
					// 将出土速度存入数据库中
					new Thread(new WriteToDb(String.valueOf(finalValue), 3))
							.start();

					// 前端实时显示
					String recordTime = sdf.format(new Date());
					ServiceClient.tf_speed.setText(String.valueOf(speed));
					ServiceClient.tf_speed2.setText(String.valueOf(finalValue));
					ServiceClient.recordTime.setText("记录时间：" + recordTime);

					// 通过Socket向外发送消息
					String message = "speed=" + finalValue + ";" + "time="
							+ recordTime;
					new SendToSocket(message);

					try {
						String sql3 = "update table_1 set READFLAG=1 where 记录时间="
								+ "'" + time + "'";
						String sql4 = "update table_3 set READFLAG=1 where 记录时间="
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
}
