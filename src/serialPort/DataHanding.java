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
		long startTime = System.currentTimeMillis();//获取当前时间
		double area=0;  // 体积
//		for(int i=0;i<list.size();i++)
//		{		
//			double finalAngle =getAngle(list.get(i));  		     		
//			double finaDistance=getDistance(list.get(i));  
//			System.out.println("[角度:"+finalAngle+","+"距离:"+finaDistance+"]");
//		}
		for(int i=0;i<list.size();i++)
		{
			double deltaAngel;
			double angle1;  //存放前一个点的角度
			double angle2;  //存放后一点个点角度
			double distance1;  //存放前一个点的距离
			double distance2;  //存放后一个点的距离
			
		  if(i+1<list.size())  //当前点不是最后一个点，直接取下一个点的数据
		  {
			angle1=getAngle(list.get(i));  
			angle2=getAngle(list.get(i+1));  
			distance1=getDistance(list.get(i));  
			distance2=getDistance(list.get(i+1)); 
		  }
		  else  //当前点是最后一个点，下一个点既为第一个点
		  {
			angle1=getAngle(list.get(i));  
			angle2=getAngle(list.get(0));  
			distance1=getDistance(list.get(i));  
			distance2=getDistance(list.get(0)); 			  
		  }
			
			if(angle1>angle2)  //一圈的数据不是严格地从小到大，后面的点角度值可能比前面小
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
//			System.out.println("角度差："+deltaAngel);
//			System.out.println("距离1："+distance1);
//			System.out.println("距离2："+distance2);
			//开始用获取到的值计算面积
			area=area+(0.5*distance1*distance2*Math.sin(Math.toRadians(deltaAngel)))/1000000;  //距离单位是毫米，转换成米
//			System.out.println("面积："+area);
			}
		  
		if(model==1) //如果是正常工作模式，则每7圈输出一个数据
		 {
			  double finalArea=RecieveFromPort.initdata-area;
			  if(finalArea<0)
			  {
				  finalArea=0; 
			  }
			  result.add(finalArea); //将单圈的面积加入list中
			  System.out.println("圈数："+result.size());
		      System.out.println("点数："+list.size());
		      System.out.println("面积："+finalArea);		
		  if(result.size()==7)  //根据扫描频率决定校验值，相当于一秒转7圈
		    {
			  double total = 0;
			  double average;
			  for(int i=0;i<7;i++)
			  {
				total=total+result.get(i) ;
			  }
			  average=(double)Math.round((total/7)*100000)/100000;  //求得一秒的面积平均值,保留五位小数
			  //System.out.println(result);
			  System.out.println("渣土面积："+average);
			  RecieveFromPort.cachedThreadPool.execute(new WriteToDb(String.valueOf(average),1)); //将一秒的测量均值写入数据库
			  result.clear();//清空list，循环使用
		    }
		  }
		  else  //如果是初始化模式，则算70圈的测量平均值
		  {   
			  double finalArea=area;
		      //finalArea=(double)Math.round(area*100000)/100000;  //保留五位小数
		      result.add(finalArea); //将单圈的面积加入list中
			  System.out.println("圈数："+result.size());
		      System.out.println("点数："+list.size());
		      System.out.println("面积："+finalArea);				  
			  if(result.size()==70)  //根据扫描频率决定校验值，相当于一秒转7圈
			    {
				  double total = 0;
				  double average;
				  for(int i=0;i<70;i++)
				  {
					total=total+result.get(i) ;
				  }
				  average=(double)Math.round((total/70)*100000)/100000;  //求得面积平均值,保留五位小数
				  RecieveFromPort.cachedThreadPool.execute(new WriteToDb(String.valueOf(average),4)); //将测量均值写入数据库
				  result.clear();//清空list，循环使用
				  try {					     
					  SerialTool.stopMeasure();// 停止雷达扫描
						 } catch (Exception e) {
								e.printStackTrace();
							}		
			    }
		  }
	      long endTime = System.currentTimeMillis();
	      System.out.println("程序运行时间："+(endTime-startTime)+"ms");		
		}

	private double getAngle(String str)
	{
		String angle=str.substring(4,6)+str.substring(2,4);
		double finalAngle = (Integer.parseInt(angle, 16)>>1)/64.0;  //十六进制转换成二进制后右移一位
		finalAngle = (double)Math.round(finalAngle*100)/100;   //角度保留两位小数	
		return finalAngle;
	}
	private double getDistance(String str)
	{
		String distance=str.substring(8,10)+str.substring(6,8);
		double finaDistance= (Integer.parseInt(distance, 16))/4; 
		return finaDistance;
	}

}
