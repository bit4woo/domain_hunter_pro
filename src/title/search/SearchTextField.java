package title.search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import burp.BurpExtender;

/**
 * 这个类主要是为了创建搜索框，并为搜索框添加各种监听事件：
 * 右键菜单、上线翻动历史记录（鼠标滚轮翻动和上下键翻动）、enter事件、
 */
public class SearchTextField extends JTextField{

	PrintWriter stdout;
	PrintWriter stderr;
	boolean caseSensitive;

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public SearchTextField(String name,JButton SearchButton){
		super(name);

		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		//enter键触发
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SearchButton.doClick();
				//searchHistory.addRecord(keyword);//记录搜索历史
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
				}
				if (e.getButton() == MouseEvent.BUTTON3) {//鼠标右键
					// 弹出菜单
					SearchMenu sm = new SearchMenu();
					sm.show(SearchTextField.this, e.getX(), e.getY());
				}
			}
		});


		addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode()==KeyEvent.VK_KP_UP || e.getKeyCode() == KeyEvent.VK_UP)//上键
				{
					try {
						History searchHistory = History.getInstance();
						String record = searchHistory.moveUP();
						if (record != null) {
							setText(record);
						}
					} catch (Exception ex) {
						ex.printStackTrace(stderr);
					}
				}

				if (e.getKeyCode() == KeyEvent.VK_KP_DOWN || e.getKeyCode() == KeyEvent.VK_DOWN){
					try {
						History searchHistory = History.getInstance();
						String record = searchHistory.moveDown();
						if (record != null) {
							setText(record);
						}
					} catch (Exception ex) {
						ex.printStackTrace(stderr);
					}
				}

			}
		});

		addMouseWheelListener(new MouseWheelListener(){

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(e.getWheelRotation()==1){
					try {
						History searchHistory = History.getInstance();
						String record = searchHistory.moveUP();
						if (record != null) {
							setText(record);
						}
					} catch (Exception ex) {
						ex.printStackTrace(stderr);
					}
					//System.out.println("滑轮向前。。。。");
				}
				if(e.getWheelRotation()==-1){
					try {
						History searchHistory = History.getInstance();
						String record = searchHistory.moveDown();
						if (record != null) {
							setText(record);
						}
					} catch (Exception ex) {
						ex.printStackTrace(stderr);
					}
					//System.out.println("滑轮向后....");
				}
			}

		});
		setColumns(30);
	}
}
