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
	public static JTextField tf_speed; // ��ʾƤ�����ٶ�
	public static JTextField tf_speed2; // ��ʾ������
	public static JTextPane recordTime; // ��ʾ��¼ʱ��
	public static volatile ArrayList<Socket> socketList;
	private static int runModel;  //����ģʽ  0-�ٶ��ɼ�������ȡ��1-�ٶ��ɵ���ϵͳ����

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		// ��ȡ����ģʽ
		if (args.length == 0) {
			logger.error("δָ������ģʽ");
			return;
		} else {
			runModel = Integer.valueOf(args[0]);
		}
		
		// ��������
		try {
			frame = new ServiceClient();
			frame.setVisible(true);
		} catch (Exception e) {
			logger.error("��������ʧ��", e);
		}
		
		// ֹͣ�״�
		try {
			stopLidar();
		} catch (Exception e) {
			logger.error("�״��ʼ��ʧ��", e);
			showMessage("error","����ʧ�ܣ��״��ʼ��ʧ�ܣ�");
			return;
		}

		// ����Ƿ���У׼ֵ
		if (!getInitResult()) { // û�м�¼
			int result = JOptionPane.showConfirmDialog(frame,
					"��⵽û��У׼ֵ���Ƿ�������ʼУ׼��", "��ʾ", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				InitDialog dialog = new InitDialog(frame, "У׼", false);
				dialog.setVisible(true);
				AnswerCmd.controlPort("startУ׼");
				for (int i = 0; i < 100; i++) {
					dialog.getProgressBar().setValue(i + 1);
					try {
						Thread.sleep(150);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				dialog.dispose();
				// У���Ƿ��ʼ���ɹ�
				if (!getInitResult()) { // У׼ʧ��
					JOptionPane.showMessageDialog(frame, "У׼ʧ�ܣ�������У׼��", "У׼",
							JOptionPane.ERROR_MESSAGE);
					showMessage("error","����ʧ�ܣ�У׼ʧ�ܣ�");
					return;
				} else {
					JOptionPane.showMessageDialog(frame, "У׼�ɹ���", "У׼",
							JOptionPane.INFORMATION_MESSAGE);
				}
			} else {
				showMessage("error","����ʧ�ܣ�δ����У׼��");
				return;
			}
		}
		
		//�������ݴ����߳�
		Thread t1 = new Thread(new DataOperater());
		t1.setPriority(10);
		t1.start();
		
		// �������ģʽΪ0ģʽ�����ȡ����ֱ���������������������ռ��߳�
		if (runModel == 0) {
			String filePath = "c:\\param.txt";
			File file = new File(filePath);
			if (!file.exists()) { // δ���ù���ֱ����������ʾ��Ҫ������
				String inputValue = (String) JOptionPane.showInputDialog(frame,
						"���ù���ֱ��(��λ��cm)", "��������", JOptionPane.WARNING_MESSAGE, null,
						null, "10");
				try {
					FileUtils.writeStringToFile(file, inputValue, "UTF-8", false);
				} catch (IOException e) {
					logger.error("д�����ʧ��", e);
				}
			}
			String diameter = ""; // ����ֱ��
			try {
				diameter = FileUtils.readFileToString(file, "UTF-8");
				if (diameter.equals("")) {
					showMessage("error","����ʧ�ܣ�δ��ȡ������ֱ����");
					return;
				}
			} catch (IOException e) {
				logger.error("��ȡ����ʧ��", e);
			}
			showMessage("normal", "�����ɹ���");
			AnswerCmd.controlPort("��ʼ���,����ֱ��:" + diameter);
		} else {
			showMessage("normal", "�����ɹ���");
		}

		// ����Socket
		try {
	        ServerSocket server =  new ServerSocket(8888);
	            logger.info("Server Listenning...");
	            socketList = new ArrayList<Socket>(); 
	            while(true){//����ѭ����ʱ�ȴ��µĿͻ��˽��������
	                Socket clientSocket = server.accept();
	                socketList.add(clientSocket);
	                logger.info(clientSocket.getInetAddress().getHostAddress() + " connected...");
	                new Thread(new SocketMonitor(clientSocket)).start(); //Ϊÿ��Socket����һ�������߳�
	            }
	        } catch (IOException e) {
	        	logger.error("Socket�쳣", e);
	        }
	}

	private static void stopLidar() throws Exception {
		SerialPort port = null;
		if (RecieveFromPort.serialCom != null) {
			port = RecieveFromPort.serialCom[0];
		} else {
			port = SerialTool.openPort("COM3", 115200);
		}
		port.setDTR(false);// ���û����DTRλ
		// ����ֹͣɨ������
		SerialTool.sendToPort(port, new byte[] { (byte) 0xA5, 0x25 });
		Thread.sleep(100);
		// ���͹رյ��ת������
		SerialTool.sendToPort(port, new byte[] { (byte) 0xA5, (byte) 0xF0,
				0x02, 0x00, 0x00, 0x57 });
		SerialTool.closePort(port);
	}

	private static boolean getInitResult() {
		try {
			ResultSet rs = dbTools.sqlSelect("select * from table_2");
			rs.last(); // �Ƶ����һ��
			if (rs.getRow() == 1) {
				return true;
			}
		} catch (Exception e) {
			logger.error("��ѯУ׼ֵʧ��", e);
		}
		return false;
	}

	/**
	 * Create the frame.
	 */
	public ServiceClient() {
		final Container contentPane = this.getContentPane();
		setTitle("���ݲɼ�����");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 509, 462);
		getContentPane().setLayout(null);
		getContentPane().setBackground(Color.WHITE);
		this.addWindowListener(new CloseWindowsListener());// �������ر��¼�
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
		textArea.setFont(new Font("����", Font.PLAIN, 17));
		textArea.setBackground(Color.BLACK);
		textArea.setBounds(new Rectangle(0, 0, 505, 305));
		textArea.setForeground(Color.GREEN);
		scrollPane.setViewportView(textArea);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setBackground(Color.WHITE);
		scrollPane.setColumnHeaderView(menuBar);

		JMenu mnNewMenu = new JMenu("����(F)");
		mnNewMenu.setFont(new Font("����", Font.BOLD, 13));
		mnNewMenu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(mnNewMenu);

		// У׼
		JMenuItem initializeMenu = new JMenuItem("У׼");
		initializeMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int result = JOptionPane.showConfirmDialog(contentPane,
						"У׼��Ҫ�����ͻ���,�Ƿ������", "��ʾ", JOptionPane.YES_NO_OPTION);

				if (result == JOptionPane.OK_OPTION) {
					new Thread(new ServiceClient()).start();
				}
			}
		});
		mnNewMenu.add(initializeMenu);
		initializeMenu.setAccelerator(KeyStroke.getKeyStroke('I',
				InputEvent.ALT_MASK));

		// ���ð뾶
		JMenuItem setParamMenu = new JMenuItem("����ֱ��");
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
							contentPane, "���ù���ֱ��(��λ��cm)", initialValue);
					if (inputValue == null) {
						inputValue = initialValue;
					}
					FileUtils.writeStringToFile(file, inputValue, "UTF-8",
							false);
				} catch (IOException e1) {
					logger.error("����ģ�����", e1);
				}
			}
		});
		mnNewMenu.add(setParamMenu);
		setParamMenu.setAccelerator(KeyStroke.getKeyStroke('P',
				InputEvent.ALT_MASK));

		// �ر�
		JMenuItem mntmS = new JMenuItem("�ر�");
		mntmS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		mnNewMenu.add(mntmS);
		mntmS.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.ALT_MASK));

		JPanel dataPanel = new JPanel();
		dataPanel.setFont(new Font("΢���ź�", Font.BOLD, 12));
		dataPanel.setBackground(Color.WHITE);
		dataPanel.setBounds(145, 325, 200, 87);
		getContentPane().add(dataPanel);
		dataPanel.setBorder(new TitledBorder(UIManager
				.getBorder("TitledBorder.border"), "ʵʱ����",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dataPanel.setLayout(null);

		JLabel speed = new JLabel("����(m/s)");
		speed.setFont(new Font("΢���ź�", Font.BOLD, 12));
		speed.setBounds(21, 26, 81, 15);
		dataPanel.add(speed);

		JLabel speed1 = new JLabel("������(m^3/s)");
		speed1.setFont(new Font("΢���ź�", Font.BOLD, 12));
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
		recordTime.setFont(new Font("΢���ź�", Font.BOLD, 12));
		getContentPane().add(recordTime);
	}

	class CloseWindowsListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			exit();
		}
	}
	
	private void exit () {
		if (GetSpeed.timer != null) {
			GetSpeed.timer.cancel();// ȡ����ʱ��
		}
		if (RecieveFromPort.serialCom != null) {
			SerialTool.stopMeasure();// ֹͣ�״�ɨ��
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
			logger.error("�������ݿ�ʧ��", e1);
		}
		Statement sta;
		try {
			sta = conn.createStatement();
			sta.executeUpdate("delete from table_2");
		} catch (SQLException e2) {
			logger.error("ɾ����ʼֵʧ��", e2);
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
