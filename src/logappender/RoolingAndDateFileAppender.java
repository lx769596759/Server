package logappender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.CountingQuietWriter;
import org.apache.log4j.helpers.LogLog;
 
 
public class RoolingAndDateFileAppender extends RollingFileAppender {
 
    private String datePattern;//���ڸ�ʽ
    private String dateStr = "";//�ļ����������
    private String expirDays = "1";//�����������
    private String isCleanLog = "true";//�Ƿ�����־
    private String maxIndex = "100";//һ����༸���ļ�
    private File rootDir;//��Ŀ¼�ĳ���·����
    private String gzFormat = "gz";//ѹ����ʽ
 
 
    /**
     * �������ڸ�ʽ
     *
     * @param datePattern
     */
    public void setDatePattern(String datePattern) {
        if (null != datePattern && !"".equals(datePattern)) {
            this.datePattern = datePattern;
        }
    }
 
    /**
     * ��ȡ���ڸ�ʽ
     *
     * @return
     */
    public String getDatePattern() {
        return this.datePattern;
    }
 
    public void rollOver() {
        //�ļ����������
        dateStr = new SimpleDateFormat(this.datePattern).format(new Date(System.currentTimeMillis()));
        File file = null;
        if (qw != null) {
            //�õ�д����ֽ���
            long size = ((CountingQuietWriter) this.qw).getCount();
            LogLog.debug("rolling over count=" + size);
        }
        //Ĭ���������һ�������ļ�
        LogLog.debug("maxBackupIndex=" + this.maxBackupIndex);
        //���maxIndex<=0��������
        if (maxIndex != null && Integer.parseInt(maxIndex) > 0) {
            //logRecoed.log.2018-08-24.5
            //ɾ�����ļ�
            file = new File(this.fileName + '.' + dateStr + '.' + Integer.parseInt(this.maxIndex) + '.' + gzFormat);
            if (file.exists()) {//�������������·������ʾ���ļ���Ŀ¼�Ƿ���ڡ�
                //���������־�ﵽ���������������ɾ�������һ����־��������־Ϊβ�ż�һ
                Boolean boo = reLogNum();
                if (!boo) {
                    LogLog.debug("��־����������ʧ�ܣ�");
                }
            }
        }
        //��ȡ���������ļ�����
        int count = cleanLog();
        //�������ļ�
        //target=new File(fileName+"."+dateStr+"."+(count+1));
        this.closeFile();//�ر���ǰ�򿪵��ļ���
        file = new File(fileName);
 
        //creat zip output stream to build zip file
        GZIPOutputStream gzout = null;
        FileInputStream fin = null;
        byte[] buf = new byte[1024];
 
        //file -> gz
        try {
            fin = new FileInputStream(file);
            gzout = new GZIPOutputStream(new FileOutputStream(fileName + "." + dateStr + "." + (count + 1) + '.' + gzFormat));
 
            int num;
            while ((num = fin.read(buf, 0, buf.length)) != -1) {
                gzout.write(buf, 0, num);
            }
            gzout.flush();
            gzout.finish();
 
            LogLog.debug(fileName + " -> " + fileName + "." + dateStr + "." + (count + 1) + '.' + gzFormat + " successful!");
        } catch (IOException e) {
            LogLog.error("add gz file(" + fileName + "." + dateStr + "." + (count + 1) + '.' + gzFormat + ") failed.");
        } finally {
            try {
                if (gzout != null) {
                    gzout.close();
                }
                if (fin != null)
                    fin.close();
            } catch (IOException e) {
                LogLog.error("close Stream failed");
            }
        }
 
        //delete old file
        file.delete();
 
 
        //LogLog.debug("Renaming file"+file+"to"+target);
        //file.renameTo(target);//������file�ļ�
        try {
            setFile(this.fileName, false, this.bufferedIO, this.bufferSize);
        } catch (IOException e) {
            LogLog.error("setFile(" + this.fileName + ",false)call failed.", e);
        }
    }
 
    /**
     * ��ȡ���������ļ�����
     *
     * @return
     */
    public int cleanLog() {
        int count = 0;//��¼�����ļ�����
        if (Boolean.parseBoolean(isCleanLog)) {
            File f = new File(fileName);
            //�����������·�����ĸ�Ŀ¼�ĳ���·����
            rootDir = f.getParentFile();
            //Ŀ¼�������ļ���
            File[] listFiles = rootDir.listFiles();
            for (File file : listFiles) {
                if (file.getName().contains(dateStr)) {
                    count = count + 1;//�ǵ�����־����+1
                } else {
                    //���ǵ�����־��Ҫ�ж��Ƿ���ɾ��
                    if (Boolean.parseBoolean(isCleanLog)) {
                        //���������־
                        String[] split = file.getName().split("\\\\")[0].split("\\.");
                        //У����־���֣���ȡ�����ڣ��жϹ���ʱ��
                        if (split.length == 4 && isExpTime(split[2])) {
                            file.delete();
                        }
                    }
                }
            }
        }
        return count;
    }
 
    /**
     * �жϹ���ʱ��
     *
     * @param time
     * @return
     */
    public Boolean isExpTime(String time) {
        SimpleDateFormat format = new SimpleDateFormat(this.datePattern);
        try {
            Date logTime = format.parse(time);
            Date nowTime = format.parse(format.format(new Date()));
            //�����־�뵱ǰ��������
            int days = (int) (nowTime.getTime() - logTime.getTime()) / (1000 * 3600 * 24);
            if (Math.abs(days) >= Integer.parseInt(expirDays)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            LogLog.error(e.toString());
            return false;
        }
    }
 
    /**
     * ���������־�ﵽ���������������ÿ��ɾ��β��Ϊ1����־��
     * ������־������μ�ȥ1��������
     *
     * @return
     */
    public Boolean reLogNum() {
        boolean renameTo = false;
        File startFile = new File(this.fileName + '.' + dateStr + '.' + "1" + '.' + gzFormat);
        if (startFile.exists() && startFile.delete()) {//�Ƿ�沢ɾ��
            for (int i = 2; i <= Integer.parseInt(maxIndex); i++) {
                File target = new File(this.fileName + '.' + dateStr + '.' + (i - 1) + '.' + gzFormat);
                this.closeFile();
                File file = new File(this.fileName + '.' + dateStr + '.' + i + '.' + gzFormat);
                renameTo = file.renameTo(target);//������file�ļ�
            }
        }
        return renameTo;
    }
 
    public String getDateStr() {
        return dateStr;
    }
 
    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }
 
    public String getExpirDays() {
        return expirDays;
    }
 
    public void setExpirDays(String expirDays) {
        this.expirDays = expirDays;
    }
 
    public String getIsCleanLog() {
        return isCleanLog;
    }
 
    public void setIsCleanLog(String isCleanLog) {
        this.isCleanLog = isCleanLog;
    }
 
    public String getMaxIndex() {
        return maxIndex;
    }
 
    public void setMaxIndex(String maxIndex) {
        this.maxIndex = maxIndex;
    }
 
 
}

