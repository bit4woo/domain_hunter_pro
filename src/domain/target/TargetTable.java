package domain.target;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.TableColumn;

import GUI.GUIMain;
import burp.BurpExtender;
import title.LineTableModel;

public class TargetTable extends JTable{

	private PrintWriter stderr;
	private PrintWriter stdout;
	private GUIMain guiMain;

	public TargetTable(GUIMain guiMain) {
		this.guiMain = guiMain;
		try {
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		} catch (Exception e) {
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setBorder(new LineBorder(new Color(0, 0, 0)));
		//tableHeaderLengthInit();//这个时候还没有设置model，其中的默认model类型是javax.swing.table.DefaultTableModel

		getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				SortOrder sortedMethod;
				try {
					getRowSorter().getSortKeys().get(0).getColumn();
					//System.out.println(sortedColumn);
					sortedMethod = getRowSorter().getSortKeys().get(0).getSortOrder();
					System.out.println(sortedMethod); //ASCENDING   DESCENDING
				} catch (Exception e1) {
					sortedMethod = null;
					e1.printStackTrace(stderr);
				}
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override//表格中的鼠标右键菜单
			public void mouseReleased(MouseEvent e) {//在windows中触发,因为isPopupTrigger在windows中是在鼠标释放是触发的，而在mac中，是鼠标点击时触发的。
				//https://stackoverflow.com/questions/5736872/java-popup-trigger-in-linux
				if (SwingUtilities.isRightMouseButton(e)) {
					if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
						//getSelectionModel().setSelectionInterval(rows[0], rows[1]);
						int[] rows = getSelectedRows();
						int col = ((JTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
						if (rows.length > 0) {
							rows = SelectedRowsToModelRows(getSelectedRows());
							new TargetEntryMenu(guiMain,TargetTable.this, rows, col).show(e.getComponent(), e.getX(), e.getY());
						} else {//在table的空白处显示右键菜单
							//https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
							//new LineEntryMenu(_this).show(e.getComponent(), e.getX(), e.getY());
						}
					}
				}
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				//双击进行google搜索、双击浏览器打开url、双击切换Check状态
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
					int[] rows = SelectedRowsToModelRows(getSelectedRows());

					int col = ((TargetTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
					int modelCol = TargetTable.this.convertColumnIndexToModel(col);

					TargetEntry selecteEntry = getTargetModel().getTargetEntries().get(rows[0]);
					if (modelCol == TargetTableModel.getTitleList().indexOf("Black")) {
						selecteEntry.setBlack(!selecteEntry.isBlack());
						guiMain.getDomainPanel().getTargetDao().addOrUpdateTarget(selecteEntry);
						getTargetModel().fireTableRowsUpdated(rows[0], rows[0]);
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) { //在mac中触发
				mouseReleased(e);
			}
		});

		setAutoCreateRowSorter(true);
		setColumnSelectionAllowed(true);
		setCellSelectionEnabled(true);
		setSurrendersFocusOnKeystroke(true);
		setFillsViewportHeight(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
	}

	/**
	 * 需要在数据加载后，即setModel后才有效果!
	 */
	public void tableHeaderLengthInit(){
		Font f = this.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		int width = fm.stringWidth("A");//一个字符的宽度
		//"Domain/Subnet", "Keyword", "Comment","Black"
		
		for (int index=0;index<this.getColumnCount();index++) {
			TableColumn column = this.getColumnModel().getColumn(index);
			if (column.getIdentifier().equals("Black")) {
				column.setMaxWidth(width*"Black++".length());
				//需要预留排序时箭头符合的位置，2个字符宽度
			}
			
			if (column.getIdentifier().equals("Domain/Subnet")) {
				column.setPreferredWidth(width*"Domain/Subnet".length());
			}
			
			if (column.getIdentifier().equals("Keyword")) {
				column.setPreferredWidth(width*"Keyword".length());
			}
		}
		//this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);//配合横向滚动条
	}

	public int[] SelectedRowsToModelRows(int[] SelectedRows) {
		int[] rows = SelectedRows;
		for (int i = 0; i < rows.length; i++) {
			rows[i] = convertRowIndexToModel(rows[i]);//转换为Model的索引，否则排序后索引不对应〿
		}
		Arrays.sort(rows);//升序
		return rows;
	}


	/**
	 * setModel和getModel是JTable本来就实现了的函数。但是其中Model的类型是DefaultTableModel,
	 * DefaultTableModel extends AbstractTableModel。而我们自己实现的model虽然也是继承于AbstractTableModel，
	 * 但是其中有一些自己实现的方法，想要方便地进行其中方法的调用，就不能使用原本的setModel和getModel方法。
	 * @return
	 */
	public TargetTableModel getTargetModel() {
		return (TargetTableModel)getModel();
	}
}