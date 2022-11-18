package test;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;


public class JTableModelTest{
	
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JFrame frame = new JFrameTest();
					
					JTable table = new JTable(new MyTableModel());
					table.setFillsViewportHeight(true);
					JScrollPane scrollPane = new JScrollPane(table);
					
					frame.getContentPane().add(scrollPane,BorderLayout.CENTER);
					
					JButton button = new JButton("change Data");
					button.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							Object[][]  data = {
									{"aaa", "Smith",
										"Snowboarding", new Integer(5), new Boolean(false)},
									{"bbb", "Doe",
											"Rowing", new Integer(3), new Boolean(true)},
									{"ccc", "Black",
												"Knitting", new Integer(2), new Boolean(false)},
									};
							//可以确定，这种更换数据的方式，不会触发监听器。后续加载数据库可以用这种方式。
							table.setModel(new MyTableModel(data));
						}
						
					});
					
					frame.getContentPane().add(button,BorderLayout.NORTH);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}

/**
 * https://docs.oracle.com/javase/tutorial/uiswing/components/table.html#data
 *
 */
class MyTableModel extends AbstractTableModel {
	private String[] columnNames = {"First Name",
			"Last Name",
			"Sport",
			"# of Years",
	"Vegetarian"};
	Object[][] data = {
			{"Kathy", "Smith",
				"Snowboarding", new Integer(5), new Boolean(false)},
			{"John", "Doe",
					"Rowing", new Integer(3), new Boolean(true)},
			{"Sue", "Black",
						"Knitting", new Integer(2), new Boolean(false)},
			{"Jane", "White",
							"Speed reading", new Integer(20), new Boolean(true)},
			{"Joe", "Brown",
								"Pool", new Integer(10), new Boolean(false)}
	};
	
	public void addListener(){
		/**
		 *
		 * 注意，所有直接对TargetTableModel中数据的修改，都不会触发该tableChanged监听器。
		 * 除非操作的逻辑中包含了firexxxx来主动通知监听器。
		 */
		addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				System.out.println("tableChanged");
				JOptionPane.showMessageDialog(null, "tableChanged ", " tableChanged", JOptionPane.ERROR_MESSAGE);
				
				int type = e.getType();//获取事件类型(增、删、改等)
				int rowstart = e.getFirstRow();//获取触发事件的行索引，即是fireTableRowxxx中的2个参数。
				int rowend = e.getLastRow();
				
				if (type == TableModelEvent.INSERT) {//插入事件使用批量方法好像不行，都是一个个插入的，每次都会触发
				
				} else if (type == TableModelEvent.UPDATE) {
			
				} else if (type == TableModelEvent.DELETE) {//可以批量操作

				} else {
					//System.out.println("此事件是由其他原因触发");
				}
			}
		});
	}

	
	public MyTableModel() {
		addListener();
	}

	public MyTableModel(Object[][] data) {
		addListener();
		this.data = data;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.length;
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	/*
	 * Don't need to implement this method unless your table's
	 * editable.
	 */
	public boolean isCellEditable(int row, int col) {
		//Note that the data/cell address is constant,
		//no matter where the cell appears onscreen.
		if (col < 2) {
			return false;
		} else {
			return true;
		}
	}

	/*
	 * Don't need to implement this method unless your table's
	 * data can change.
	 */
	public void setValueAt(Object value, int row, int col) {
		data[row][col] = value;
		fireTableCellUpdated(row, col);
	}
}