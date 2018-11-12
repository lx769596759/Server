package serialPort;

import java.util.ArrayList;

import dbUtility.WriteToDb;

public class DataHanding implements Runnable {
	ArrayList<String> list;
	private static ArrayList<Double> result=new ArrayList<Double>();
	int model;
	public DataHanding(ArrayList<String> list,int model)
	{
		this.list= list; 
		this.model=model;
	}

	public void run() {	    
		long startTime = System.currentTimeMillis();//��ȡ��ǰʱ��
		double area=0;  // ���
//		for(int i=0;i<list.size();i++)
//		{		
//			double finalAngle =getAngle(list.get(i));  		     		
//			double finaDistance=getDistance(list.get(i));  
//			System.out.println("[�Ƕ�:"+finalAngle+","+"����:"+finaDistance+"]");
//		}
		for(int i=0;i<list.size();i++)
		{
			double deltaAngel;
			double angle1;  //���ǰһ����ĽǶ�
			double angle2;  //��ź�һ�����Ƕ�
			double distance1;  //���ǰһ����ľ���
			double distance2;  //��ź�һ����ľ���
			
		  if(i+1<list.size())  //��ǰ�㲻�����һ���㣬ֱ��ȡ��һ���������
		  {
			angle1=getAngle(list.get(i));  
			angle2=getAngle(list.get(i+1));  
			distance1=getDistance(list.get(i));  
			distance2=getDistance(list.get(i+1)); 
		  }
		  else  //��ǰ�������һ���㣬��һ�����Ϊ��һ����
		  {
			angle1=getAngle(list.get(i));  
			angle2=getAngle(list.get(0));  
			distance1=getDistance(list.get(i));  
			distance2=getDistance(list.get(0)); 			  
		  }
			
			if(angle1>angle2)  //һȦ�����ݲ����ϸ�ش�С���󣬺���ĵ�Ƕ�ֵ���ܱ�ǰ��С
			 {
				if ((angle1-angle2)>300)
				{
				    deltaAngel=angle2+360-angle1;
				}
				else
				{
					deltaAngel=angle1-angle2;	
				}
			 }
			else
			{
				deltaAngel=angle2-angle1;
			}
//			System.out.println("�ǶȲ"+deltaAngel);
//			System.out.println("����1��"+distance1);
//			System.out.println("����2��"+distance2);
			//��ʼ�û�ȡ����ֵ�������
			area=area+(0.5*distance1*distance2*Math.sin(Math.toRadians(deltaAngel)))/1000000;  //���뵥λ�Ǻ��ף�ת������
//			System.out.println("�����"+area);
			}
		  
		if(model==1) //�������������ģʽ����ÿ7Ȧ���һ������
		 {
			  double finalArea=RecieveFromPort.initdata-area;
			  if(finalArea<0)
			  {
				  finalArea=0; 
			  }
			  result.add(finalArea); //����Ȧ���������list��
			  System.out.println("Ȧ����"+result.size());
		      System.out.println("������"+list.size());
		      System.out.println("�����"+finalArea);		
		  if(result.size()==7)  //����ɨ��Ƶ�ʾ���У��ֵ���൱��һ��ת7Ȧ
		    {
			  double total = 0;
			  double average;
			  for(int i=0;i<7;i++)
			  {
				total=total+result.get(i) ;
			  }
			  average=(double)Math.round((total/7)*100000)/100000;  //���һ������ƽ��ֵ,������λС��
			  //System.out.println(result);
			  System.out.println("���������"+average);
			  RecieveFromPort.cachedThreadPool.execute(new WriteToDb(String.valueOf(average),1)); //��һ��Ĳ�����ֵд�����ݿ�
			  result.clear();//���list��ѭ��ʹ��
		    }
		  }
		  else  //����ǳ�ʼ��ģʽ������70Ȧ�Ĳ���ƽ��ֵ
		  {   
			  double finalArea=area;
		      //finalArea=(double)Math.round(area*100000)/100000;  //������λС��
		      result.add(finalArea); //����Ȧ���������list��
			  System.out.println("Ȧ����"+result.size());
		      System.out.println("������"+list.size());
		      System.out.println("�����"+finalArea);				  
			  if(result.size()==70)  //����ɨ��Ƶ�ʾ���У��ֵ���൱��һ��ת7Ȧ
			    {
				  double total = 0;
				  double average;
				  for(int i=0;i<70;i++)
				  {
					total=total+result.get(i) ;
				  }
				  average=(double)Math.round((total/70)*100000)/100000;  //������ƽ��ֵ,������λС��
				  RecieveFromPort.cachedThreadPool.execute(new WriteToDb(String.valueOf(average),4)); //��������ֵд�����ݿ�
				  result.clear();//���list��ѭ��ʹ��
				  try {					     
					  SerialTool.stopMeasure();// ֹͣ�״�ɨ��
						 } catch (Exception e) {
								e.printStackTrace();
							}		
			    }
		  }
	      long endTime = System.currentTimeMillis();
	      System.out.println("��������ʱ�䣺"+(endTime-startTime)+"ms");		
		}

	private double getAngle(String str)
	{
		String angle=str.substring(4,6)+str.substring(2,4);
		double finalAngle = (Integer.parseInt(angle, 16)>>1)/64.0;  //ʮ������ת���ɶ����ƺ�����һλ
		finalAngle = (double)Math.round(finalAngle*100)/100;   //�Ƕȱ�����λС��	
		return finalAngle;
	}
	private double getDistance(String str)
	{
		String distance=str.substring(8,10)+str.substring(6,8);
		double finaDistance= (Integer.parseInt(distance, 16))/4; 
		return finaDistance;
	}

}
