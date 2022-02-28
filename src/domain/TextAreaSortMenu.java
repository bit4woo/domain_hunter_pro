package domain;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

import Tools.DomainComparator;
import Tools.LengthComparator;
import burp.BurpExtender;

public class TextAreaSortMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;
	TextAreaSortMenu(final JTextArea TextArea){

		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		List<String> selectedItems = Arrays.asList(TextArea.getText().split(System.lineSeparator()));

		JMenuItem Sort = new JMenuItem(new AbstractAction("Sort") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				Collections.sort(selectedItems);
				TextArea.setText(String.join(System.lineSeparator(), selectedItems));
			}
		});
		this.add(Sort);

		JMenuItem SortByLength = new JMenuItem(new AbstractAction("Sort By Length") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				Collections.sort(selectedItems,new LengthComparator());
				TextArea.setText(String.join(System.lineSeparator(), selectedItems));
			}
		});
		this.add(SortByLength);

		JMenuItem SortDomain = new JMenuItem(new AbstractAction("Sort Domain") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				Collections.sort(selectedItems,new DomainComparator());
				TextArea.setText(String.join(System.lineSeparator(), selectedItems));
			}
		});
		this.add(SortDomain);


		//https://blog.csdn.net/opshres169/article/details/51913713
		JMenuItem SearchDomain = new JMenuItem(new AbstractAction("Search") {
			int searchBegin = 0;

			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String keyword = JOptionPane.showInputDialog("Find What?");
				searchBegin = search(keyword,searchBegin);

				/**
				try {
					JTextArea parent = (JTextArea)actionEvent.getSource();

					boolean enterListenerAdded = false;
					KeyListener[] keyListeners = parent.getKeyListeners();
					for (KeyListener item: keyListeners) {
						if (item.getClass() == EnterListener.class) {
							enterListenerAdded = true;
						}
					}
					if (!enterListenerAdded) {
						parent.addKeyListener(new EnterListener());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}*/
			}

			/**
			 * 从特定的位置开始搜索
			 * @param searchBegin
			 * @return
			 */
			public int search(String keyword, int searchBegin) {
				if (searchBegin >= TextArea.getText().length()) {
					searchBegin = 0;
				}

				int offset = TextArea.getText().toLowerCase().indexOf(keyword.toLowerCase(),searchBegin);
				int length = keyword.length();

				if (offset != -1) {
					TextArea.setSelectionStart(offset);
					TextArea.setSelectionEnd(offset + length);
					TextArea.requestFocus();
					//offset = TextArea.getText().indexOf(search, offset + 1);//查找下一个
				}
				return offset + 1;//下一次查找开始的位置
			}

			/**
			 * 会和文本编辑冲突
			 * @author bit4woo
			 *
			 */
			class EnterListener extends KeyAdapter{
				@Override
				public void keyPressed(KeyEvent evt){
					if (evt.getKeyCode() == KeyEvent.VK_N) {
						String keyword = TextArea.getSelectedText();
						if (keyword == null || "".equals(keyword)) {
							searchBegin = search(keyword,searchBegin);
						}
					}
				}
			}
		});

		this.add(SearchDomain);
		SortDomain.setToolTipText("search something");
	}
}
