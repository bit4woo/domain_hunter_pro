package test;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class TestFilter {
	public static void main(String args[]) {
		JFrame frame = new JFrame("JTable的过滤测试");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Object rows[][] = { { "王明", "中国", 44 }, { "姚明", "中国", 25 },
				{ "赵子龙", "西蜀", 1234 }, { "曹操", "北魏", 2112 },
				{ "Bill Gates", "美国", 45 }, { "Mike", "英国", 33 } };
		String columns[] = { "姓名", "国籍", "年龄" };
		TableModel model = new DefaultTableModel(rows, columns) {
			public Class getColumnClass(int column) {
				Class returnValue;
				if ((column >= 0) && (column < getColumnCount())) {
					returnValue = getValueAt(0, column).getClass();
				} else {
					returnValue = Object.class;
				}
				return returnValue;
			}
		};
		final JTable table = new JTable(model);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(
				model);
		table.setRowSorter(sorter);
		JScrollPane pane = new JScrollPane(table);
		frame.add(pane, BorderLayout.CENTER);
		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel("过滤");
		panel.add(label, BorderLayout.WEST);
		final JTextField filterText = new JTextField("");
		panel.add(filterText, BorderLayout.CENTER);
		frame.add(panel, BorderLayout.NORTH);
		JButton button = new JButton("过滤");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = filterText.getText();
				if (text.length() == 0) {
					sorter.setRowFilter(null);
				} else {
					sorter.setRowFilter(RowFilter.regexFilter(text));
				}
			}
		});
		frame.add(button, BorderLayout.SOUTH);
		frame.setSize(300, 250);
		frame.setVisible(true);
	}
}
