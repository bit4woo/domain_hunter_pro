package title.search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.PrintWriter;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import burp.BurpExtender;
import title.TitlePanel;

/*
 * 这个类主要是为了创建搜索框，并为搜索框添加各种监听事件：
 * 右键菜单、上线翻动历史记录（鼠标滚轮翻动和上下键翻动）、enter事件、
 */
public class SearchTextField extends JTextField{

	PrintWriter stdout;
	PrintWriter stderr;
	History searchHistory = History.getInstance();

	public JTextField Create(String name){
		
		JTextField textFieldSearch = new JTextField(name);

		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}


		textFieldSearch.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (textFieldSearch.getText().equals("Input text to search")) {
					textFieldSearch.setText("");
				}
			}
			@Override
			public void focusLost(FocusEvent e) {
				/*
				 * if (textFieldSearch.getText().equals("")) {
				 * textFieldSearch.setText("Input text to search"); }
				 */

			}
		});

		//enter键触发
		textFieldSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText().trim();
				TitlePanel.getTitleTable().search(keyword);
				//searchHistory.addRecord(keyword);//记录搜索历史
			}
		});

		textFieldSearch.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
				}
				if (e.getButton() == MouseEvent.BUTTON3) {//鼠标右键
					// 弹出菜单
					SearchMenu sm = new SearchMenu();
					sm.show(textFieldSearch, e.getX(), e.getY());
				}
			}
		});


		textFieldSearch.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode()==KeyEvent.VK_KP_UP || e.getKeyCode() == KeyEvent.VK_UP)//上键
				{
					try {
						String record = searchHistory.moveUP();
						if (record != null) {
							textFieldSearch.setText(record);
						}
					} catch (Exception ex) {
						ex.printStackTrace(stderr);
					}
				}

				if (e.getKeyCode() == KeyEvent.VK_KP_DOWN || e.getKeyCode() == KeyEvent.VK_DOWN){
					try {
						String record = searchHistory.moveDown();
						if (record != null) {
							textFieldSearch.setText(record);
						}
					} catch (Exception ex) {
						ex.printStackTrace(stderr);
					}
				}

			}
		});

		textFieldSearch.addMouseWheelListener(new MouseWheelListener(){

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(e.getWheelRotation()==1){
					try {
						String record = searchHistory.moveUP();
						if (record != null) {
							textFieldSearch.setText(record);
						}
					} catch (Exception ex) {
						ex.printStackTrace(stderr);
					}
					//System.out.println("滑轮向前。。。。");
				}
				if(e.getWheelRotation()==-1){
					try {
						String record = searchHistory.moveDown();
						if (record != null) {
							textFieldSearch.setText(record);
						}
					} catch (Exception ex) {
						ex.printStackTrace(stderr);
					}
					//System.out.println("滑轮向后....");
				}
			}

		});
		textFieldSearch.setColumns(30);
		return textFieldSearch;
	}
}
