package tcpip;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import serialPort.AnswerCmd;
import ServiceClient.ServiceClient;



public class RecieveData  implements Runnable{
	   Socket socket=null;
	   boolean isRun=true;
	   AnswerCmd answerCmd=new AnswerCmd();
	   public RecieveData(Socket s)
	   {
		   this.socket=s;
	   }
	   
	public void run() {
		while (isRun)
		{
		try {
		 InputStream is = socket.getInputStream();// �ֽ�������
         InputStreamReader isr = new InputStreamReader(is);// ���ֽ���תΪ�ַ���
         BufferedReader br = new BufferedReader(isr);// Ϊ��������ӻ���
         String info = null;
		   if ((info = br.readLine()) != null) {
			     System.out.println("��������:" + info);
			     answerCmd.controlPort(info);
			 }
		} catch (IOException e) {
			ServiceClient.logger.error(e.getMessage(),e);
			if(socket==null)
			{
				isRun=false;
			}
		}
		 try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			ServiceClient.logger.error(e.getMessage(),e);
		}
		
	}  
 }
}		


