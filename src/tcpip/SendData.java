package tcpip;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;


public class SendData { 	  	  
			
	public SendData(){};
	//���������������
	public static void sendData(Socket socket) {	 		   
			try {
			   //Socket socket=new Socket(Client.textField_1.getText(),8888);
			   OutputStream os = socket.getOutputStream();			
	           PrintWriter pw = new PrintWriter(os);// ��װ��ӡ��
	           pw.write(2+"\n");
	           pw.flush();
	           try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	           //socket.shutdownOutput();
	           //pw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	  	}
	}

		  	       
      


	
 
 
    