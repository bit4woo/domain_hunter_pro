package test;
import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

public class RoundColorTable extends JFrame {
	private String[] colname = {"第1列","第2列","第3列","第4列","第5列"}; //表头信息
	private String[][] data = new String[10][5]; //表内容 
	private JTable table;
	public RoundColorTable() {
		//表内容赋值
		for(int i = 0; i < 10; i++) {
			for(int j = 0; j < 5; j++) {
				data[i][j] = "( " + (j+1) + ", " + (i+1) + " )";
			}
		}
		table = new JTable(new DefaultTableModel(data,colname));
		TableCellRenderer tcr = new ColorTableCellRenderer();
		table.setDefaultRenderer(Object.class,tcr);//为JTable增加渲染器，因为是针对于表格中的所有单元格，所有用Object.class
		add(new JScrollPane(table),BorderLayout.CENTER);
		setVisible(true);
		setSize(500,300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	public static void main(String args[]) {
		new RoundColorTable();
	}
}

class ColorTableCellRenderer extends DefaultTableCellRenderer
{
	DefaultTableCellRenderer renderer=new DefaultTableCellRenderer();
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if(row%2 == 0){
			//调用基类方法
			return super.getTableCellRendererComponent(table, value, isSelected,hasFocus, row, column);
		}else{
			return renderer.getTableCellRendererComponent(table, value, isSelected,hasFocus, row, column);
		}
	}
	//该类继承与JLabel，Graphics用于绘制单元格,绘制红线
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D g2=(Graphics2D)g;
		final BasicStroke stroke=new BasicStroke(2.0f);
		g2.setColor(Color.RED);
		g2.setStroke(stroke);
		g2.drawLine(0,getHeight()/2,getWidth(),getHeight()/2);
	}
}