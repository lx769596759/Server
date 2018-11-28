package ServiceClient;

import gnu.io.SerialPort;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import serialPort.AnswerCmd;
import serialPort.DataOperater;
import serialPort.GetSpeed;
import serialPort.RecieveFromPort;
import serialPort.SerialTool;
import tcpip.SocketMonitor;
import dbUtility.dbTools;

public class ServiceClient extends JFrame implements Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static JTextArea textArea;
	public static SimpleDateFormat df = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss:SSS");
	public static Logger logger = Logger.getLogger(ServiceClient.class);
	private static ServiceClient frame;
	public static JTextField tf_speed; // 显示皮带线速度
	public static JTextField tf_speed2; // 显示出土量
	public static JTextPane recordTime; // 显示记录时间
	public static volatile ArrayList<Socket> socketList;
	private static int runModel;  //运行模式  0-速度由计数器获取，1-速度由导向系统传入

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		// 获取运行模式
		if (args.length == 0) {
			logger.error("未指定运行模式");
			return;
		} else {
			runModel = Integer.valueOf(args[0]);
		}
		
		// 开启界面
		try {
			frame = new ServiceClient();
			frame.setVisible(true);
		} catch (Exception e) {
			logger.error("界面启动失败", e);
		}
		
		// 停止雷达
		try {
			stopLidar();
		} catch (Exception e) {
			logger.error("雷达初始化失败", e);
			showMessage("error","启动失败：雷达初始化失败！");
			return;
		}

		// 检测是否有校准值
		if (!getInitResult()) { // 没有记录
			int result = JOptionPane.showConfirmDialog(frame,
					"检测到没有校准值，是否立即开始校准？", "提示", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				InitDialog dialog = new InitDialog(frame, "校准", false);
				dialog.setVisible(true);
				AnswerCmd.controlPort("start校准");
				for (int i = 0; i < 100; i++) {
					dialog.getProgressBar().setValue(i + 1);
					try {
						Thread.sleep(150);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				dialog.dispose();
				// 校验是否初始化成功
				if (!getInitResult()) { // 校准失败
					JOptionPane.showMessageDialog(frame, "校准失败！请重新校准！", "校准",
							JOptionPane.ERROR_MESSAGE);
					showMessage("error","启动失败：校准失败！");
					return;
				} else {
					JOptionPane.showMessageDialog(frame, "校准成功！", "校准",
							JOptionPane.INFORMATION_MESSAGE);
				}
			} else {
				showMessage("error","启动失败：未进行校准！");
				return;
			}
		}
		
		//开启数据处理线程
		Thread t1 = new Thread(new DataOperater());
		t1.setPriority(10);
		t1.start();
		
		// 如果运行模式为0模式，则获取滚轮直径，并开启计数器数据收集线程
		if (runModel == 0) {
			String filePath = "c:\\param.txt";
			File file = new File(filePath);
			if (!file.exists()) { // 未设置滚轮直径，弹出提示框要求设置
				String inputValue = (String) JOptionPane.showInputDialog(frame,
						"设置滚轮直径(单位：cm)", "参数设置", JOptionPane.WARNING_MESSAGE, null,
						null, "10");
				try {
					FileUtils.writeStringToFile(file, inputValue, "UTF-8", false);
				} catch (IOException e) {
					logger.error("写入参数失败", e);
				}
			}
			String diameter = ""; // 滚轮直径
			try {
				diameter = FileUtils.readFileToString(file, "UTF-8");
				if (diameter.equals("")) {
					showMessage("error","启动失败：未获取到滚轮直径！");
					return;
				}
			} catch (IOException e) {
				logger.error("读取参数失败", e);
			}
			showMessage("normal", "启动成功！");
			AnswerCmd.controlPort("开始监控,滚轮直径:" + diameter);
		} else {
			showMessage("normal", "启动成功！");
		}

		// 开启Socket
		try {
	        ServerSocket server =  new ServerSocket(8888);
	            logger.info("Server Listenning...");
	            socketList = new ArrayList<Socket>(); 
	            while(true){//不断循环随时等待新的客户端接入服务器
	                Socket clientSocket = server.accept();
	                socketList.add(clientSocket);
	                logger.info(clientSocket.getInetAddress().getHostAddress() + " connected...");
	                new Thread(new SocketMonitor(clientSocket)).start(); //为每个Socket开启一个接收线程
	            }
	        } catch (IOException e) {
	        	logger.error("Socket异常", e);
	        }
	}

	private static void stopLidar() throws Exception {
		SerialPort port = null;
		if (RecieveFromPort.serialCom != null) {
			port = RecieveFromPort.serialCom[0];
		} else {
			port = SerialTool.openPort("COM3", 115200);
		}
		port.setDTR(false);// 设置或清除DTR位
		// 发送停止扫描命令
		SerialTool.sendToPort(port, new byte[] { (byte) 0xA5, 0x25 });
		Thread.sleep(100);
		// 发送关闭电机转动命令
		SerialTool.sendToPort(port, new byte[] { (byte) 0xA5, (byte) 0xF0,
				0x02, 0x00, 0x00, 0x57 });
		SerialTool.closePort(port);
	}

	private static boolean getInitResult() {
		try {
			ResultSet rs = dbTools.sqlSelect("select * from table_2");
			rs.last(); // 移到最后一行
			if (rs.getRow() == 1) {
				return true;
			}
		} catch (Exception e) {
			logger.error("查询校准值失败", e);
		}
		return false;
	}

	/**
	 * Create the frame.
	 */
	public ServiceClient() {
		final Container contentPane = this.getContentPane();
		setTitle("数据采集服务");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 509, 462);
		getContentPane().setLayout(null);
		getContentPane().setBackground(Color.WHITE);
		this.addWindowListener(new CloseWindowsListener());// 处理界面关闭事件
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getVerticalScrollBar().setValue(
				scrollPane.getVerticalScrollBar().getMaximum());
		scrollPane.setBounds(0, 0, 505, 308);
		scrollPane.setBackground(Color.WHITE);
		getContentPane().add(scrollPane);

		textArea = new JTextArea();
		textArea.setEditable(false);
		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		textArea.setFont(new Font("宋体", Font.PLAIN, 17));
		textArea.setBackground(Color.BLACK);
		textArea.setBounds(new Rectangle(0, 0, 505, 305));
		textArea.setForeground(Color.GREEN);
		scrollPane.setViewportView(textArea);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setBackground(Color.WHITE);
		scrollPane.setColumnHeaderView(menuBar);

		JMenu mnNewMenu = new JMenu("设置(F)");
		mnNewMenu.setFont(new Font("宋体", Font.BOLD, 13));
		mnNewMenu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(mnNewMenu);

		// 校准
		JMenuItem initializeMenu = new JMenuItem("校准");
		initializeMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int result = JOptionPane.showConfirmDialog(contentPane,
						"校准需要重启客户端,是否继续？", "提示", JOptionPane.YES_NO_OPTION);

				if (result == JOptionPane.OK_OPTION) {
					new Thread(new ServiceClient()).start();
				}
			}
		});
		mnNewMenu.add(initializeMenu);
		initializeMenu.setAccelerator(KeyStroke.getKeyStroke('I',
				InputEvent.ALT_MASK));

		// 设置半径
		JMenuItem setParamMenu = new JMenuItem("滚轮直径");
		setParamMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String path = System.getProperty("user.dir");
				String filePath = path + File.separator + "param.txt";
				File file = new File(filePath);
				try {
					String initialValue = "20";
					if (file.exists()) {
						initialValue = FileUtils
								.readFileToString(file, "UTF-8");
					}
					String inputValue = JOptionPane.showInputDialog(
							contentPane, "设置滚轮直径(单位：cm)", initialValue);
					if (inputValue == null) {
						inputValue = initialValue;
					}
					FileUtils.writeStringToFile(file, inputValue, "UTF-8",
							false);
				} catch (IOException e1) {
					logger.error("参数模块错误", e1);
				}
			}
		});
		mnNewMenu.add(setParamMenu);
		setParamMenu.setAccelerator(KeyStroke.getKeyStroke('P',
				InputEvent.ALT_MASK));

		// 关闭
		JMenuItem mntmS = new JMenuItem("关闭");
		mntmS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		mnNewMenu.add(mntmS);
		mntmS.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.ALT_MASK));

		JPanel dataPanel = new JPanel();
		dataPanel.setFont(new Font("微软雅黑", Font.BOLD, 12));
		dataPanel.setBackground(Color.WHITE);
		dataPanel.setBounds(145, 325, 200, 87);
		getContentPane().add(dataPanel);
		dataPanel.setBorder(new TitledBorder(UIManager
				.getBorder("TitledBorder.border"), "实时数据",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dataPanel.setLayout(null);

		JLabel speed = new JLabel("带速(m/s)");
		speed.setFont(new Font("微软雅黑", Font.BOLD, 12));
		speed.setBounds(21, 26, 81, 15);
		dataPanel.add(speed);

		JLabel speed1 = new JLabel("出土量(m^3/s)");
		speed1.setFont(new Font("微软雅黑", Font.BOLD, 12));
		speed1.setBounds(21, 57, 90, 15);
		dataPanel.add(speed1);

		tf_speed = new JTextField();
		tf_speed.setBounds(120, 23, 60, 21);
		tf_speed.setEditable(false);
		dataPanel.add(tf_speed);

		tf_speed2 = new JTextField();
		tf_speed2.setBounds(120, 54, 60, 21);
		tf_speed2.setEditable(false);
		dataPanel.add(tf_speed2);

		recordTime = new JTextPane();
		recordTime.setBounds(310, 413, 193, 21);
		recordTime.setFont(new Font("微软雅黑", Font.BOLD, 12));
		getContentPane().add(recordTime);
	}

	class CloseWindowsListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			exit();
		}
	}
	
	private void exit () {
		if (GetSpeed.timer != null) {
			GetSpeed.timer.cancel();// 取消计时器
		}
		if (RecieveFromPort.serialCom != null) {
			SerialTool.stopMeasure();// 停止雷达扫描
		}
		System.exit(0);
	}

	@Override
	public void run() {
		Connection conn = null;
		try {
			conn = dbTools.getConn(dbTools.driverName, dbTools.dbUrl,
					dbTools.us, dbTools.pw);
		} catch (Exception e1) {
			logger.error("连接数据库失败", e1);
		}
		Statement sta;
		try {
			sta = conn.createStatement();
			sta.executeUpdate("delete from table_2");
		} catch (SQLException e2) {
			logger.error("删除初始值失败", e2);
		}
		exit();
	}
	
	public static void showMessage(String type, String message) {
		if ("normal".equals(type)) {
			textArea.setForeground(Color.GREEN);
		} else {
			textArea.setForeground(Color.RED);
		}
		textArea.append(ServiceClient.df.format(new Date()) + " "
				+ message);
		textArea.append("\r\n");
	}
}
