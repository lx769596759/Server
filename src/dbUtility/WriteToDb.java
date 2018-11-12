package dbUtility;

import java.text.MessageFormat;

import ServiceClient.ServiceClient;
import dbUtility.dbTools;

public class WriteToDb implements Runnable {

	String value;
	dbTools db = new dbTools();
	int model;

	public WriteToDb(String value, int model) {
		this.value = value;
		this.model = model;
	}

	public void run() {
		try {
			String sql = null;
			if (model == 1) {
				sql = MessageFormat.format(
						"insert into table_1 (������ֵ,READFLAG) values({0},0)",value);
			} else if (model == 2) {
				sql = MessageFormat.format(
						"insert into table_3 (�ٶ�,READFLAG) values({0},0)",value);
			} else if (model == 3) {
				sql = MessageFormat.format(
						"insert into table_4 (�����ٶ�) values({0})", value);
			} else {
				sql = MessageFormat.format(
						"insert into table_2 (У׼ֵ) values({0})", value);
			}
			db.insert(sql);
		} catch (Exception e) {
			ServiceClient.logger.error(e.getMessage());
		}
	}
}
