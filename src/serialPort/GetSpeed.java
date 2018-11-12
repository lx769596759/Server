package serialPort;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TooManyListenersException;

import dbUtility.WriteToDb;
import ServiceClient.ServiceClient;
import serialException.NoSuchPort;
import serialException.NotASerialPort;
import serialException.PortInUse;
import serialException.SendDataToSerialPortFailure;
import serialException.SerialPortOutputStreamCloseFailure;
import serialException.SerialPortParameterFailure;

public class GetSpeed implements SerialPortEventListener,Runnable {
    public static SerialPort serialCom;  
    public static Timer timer;
    public static double diameter;
	public void run() {	
		if(serialCom==null)
		{
		try {
			serialCom = SerialTool.openPort("COM2", 9600);
			try {
				serialCom.addEventListener(new GetSpeed());
			} catch (TooManyListenersException e) {
				e.printStackTrace();
			}
			serialCom.notifyOnDataAvailable(true);
		} catch (SerialPortParameterFailure | NotASerialPort | NoSuchPort
				| PortInUse e) {
			e.printStackTrace();
		}
		}
		 timer=new Timer();
		 GetSpeed gs=new GetSpeed();
		 GetSpeed.GetSpeedTask mt=gs.new GetSpeedTask(serialCom);
	     timer.schedule(mt,1,1000);
	}	
	   
	class GetSpeedTask extends TimerTask {			  
		   private SerialPort serialCom;
		   byte[] cmd=new byte[]{0x01,0x03,0x00,0x62,0x00,0x02,0x65,(byte) 0xD5};
		   public GetSpeedTask(SerialPort serialCom){
			   this.serialCom=serialCom;
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
		 switch(event.getEventType()) {
	        case SerialPortEvent.BI:/*Break interrupt,ͨѶ�ж�*/
	        case SerialPortEvent.OE:/*Overrun error����λ����*/
	        case SerialPortEvent.FE:/*Framing error����֡����*/
	        case SerialPortEvent.PE:/*Parity error��У�����*/
	        case SerialPortEvent.CD:/*Carrier detect���ز����*/
	        case SerialPortEvent.CTS:/*Clear to send���������*/
	        case SerialPortEvent.DSR:/*Data set ready�������豸����*/
	        case SerialPortEvent.RI:/*Ring indicator������ָʾ*/
	        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:/*Output buffer is empty��������������*/
	            break;
	        case SerialPortEvent.DATA_AVAILABLE:/*Data available at the serial port���˿��п������ݡ������������飬������ն�*/	    			
			InputStream inputStream = null;
			try {
				inputStream = serialCom.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
					int count = 9;  
					byte[] b = new byte[count];  
					int readCount = 0; // �Ѿ��ɹ���ȡ���ֽڵĸ���  
					while (readCount < count) {  
					    try {
							readCount += inputStream.read(b, readCount, count - readCount);
						} catch (IOException e) {
							e.printStackTrace();
						}  					    
					}
		           	//ת����ʮ������
					StringBuilder buf = new StringBuilder(b.length * 2);
			        for(byte bb : b) { // ʹ��String��format��������ת��
			            buf.append(String.format("%02x", new Integer(bb & 0xff)));
			        }
					//System.out.println(buf);
					StringBuffer sb = new StringBuffer();
					String data1 =sb.append(buf).substring(6,10);
					int speed1=Integer.parseInt(data1,16);//ʮ������ת����ʮ����
					double finalSpeed=(double)speed1*3.14*diameter/60;//ת�ٳ����ܳ��õ����ٶ�,ת�ٵ�λΪn/min�����Գ���60�õ�n/s
					BigDecimal bg = new BigDecimal(finalSpeed);   
					double lineSpeed = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();  					
					System.out.println("���ٶ�="+lineSpeed);
					if(lineSpeed>0)
					{
						if(RecieveFromPort.isRun==false)
						{
						 //�����������ݽ����߳�
						 RecieveFromPort.isRun=true;
						 RecieveFromPort.isBengin=true;
						 RecieveFromPort.isRound=false;
						 Thread reciveFromPort=new Thread(new RecieveFromPort());
						 reciveFromPort.setName("�������ݽ���");
						 ServiceClient.textArea.append(ServiceClient.df.format(new Date())+" "+"��ʼ��������...");
						 ServiceClient.textArea.append("\r\n");
						 reciveFromPort.setPriority(10);
						 reciveFromPort.start();
						}				 
					}
					else
					{
						if(RecieveFromPort.isRun==true)
						{
							SerialTool.stopMeasure();//ֹͣ�״����
							ServiceClient.textArea.append(ServiceClient.df.format(new Date())+" "+"ֹͣ��������...");
							ServiceClient.textArea.append("\r\n");
						}
					}
                    //���ٶ�д�����ݿ�
					new Thread(new WriteToDb(String.valueOf(lineSpeed),2)).start();
					  }					
	                }
                  }
