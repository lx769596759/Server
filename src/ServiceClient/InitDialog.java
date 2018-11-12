package ServiceClient;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Color;

public class InitDialog extends JDialog {
	

    private JProgressBar progressBar;


	public JProgressBar getProgressBar() {
		return progressBar;
	}


	public void setProgressBar(JProgressBar progressBar) {
		this.progressBar = progressBar;
	}


	/**
	 * Create the dialog.
	 */
	public InitDialog(JFrame frame,String name,boolean modal) {
		super(frame,name,modal);
		JPanel contentPanel = new JPanel();
		setBounds(frame.getX()+35, frame.getY()+83, 437, 166);
		contentPanel.setBackground(Color.WHITE);
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		JProgressBar progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setBounds(20, 80, 376, 27);
		setProgressBar(progressBar);
		contentPanel.add(progressBar);
		JLabel label = new JLabel("\u6B63\u5728\u521D\u59CB\u5316\uFF0C\u8BF7\u7A0D\u540E...");
		label.setFont(new Font("Î¢ÈíÑÅºÚ", Font.BOLD, 12));
		label.setBounds(10, 29, 140, 27);
		contentPanel.add(label);
	}
	
	

}
