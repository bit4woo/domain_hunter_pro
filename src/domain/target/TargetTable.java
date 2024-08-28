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
import javax.swing.table.TableModel;

import burp.BurpExtender;
import domain.DomainManager;
import domain.DomainPanel;
import title.IndexedLinkedHashMap;
import title.LineTableModel;

public class TargetTable extends JTable{

	private PrintWriter stderr;
	private PrintWriter stdout;

	public TargetTable() {

		try {
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		} catch (Exception e) {
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setBorder(new LineBorder(new Color(0, 0, 0)));
		//tableHeaderLengthInit();

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
							new TargetEntryMenu(TargetTable.this, rows, col).show(e.getComponent(), e.getX(), e.getY());
						} else {//在table的空白处显示右键菜单
							//https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
							//new LineEntryMenu(_this).show(e.getComponent(), e.getX(), e.getY());
						}
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

	public void tableHeaderLengthInit(){
		Font f = this.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		int width = fm.stringWidth("A");//一个字符的宽度

		Map<String,Integer> preferredWidths = new HashMap<String,Integer>();
		preferredWidths.put("Comments",20);
		preferredWidths.put("Black"," Black".length());
		for(String header:LineTableModel.getTitletList()){
			try{//避免动态删除表字段时，出错
				int multiNumber = preferredWidths.get(header);
				this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex(header)).setPreferredWidth(width*multiNumber);
			}catch (Exception e){

			}
		}
	}

	public int[] SelectedRowsToModelRows(int[] SelectedRows) {
		int[] rows = SelectedRows;
		for (int i = 0; i < rows.length; i++) {
			rows[i] = convertRowIndexToModel(rows[i]);//转换为Model的索引，否则排序后索引不对应〿
		}
		Arrays.sort(rows);//升序
		return rows;
	}

	public TargetTableModel getTargetModel() {
		return (TargetTableModel) getModel();
	}

	/**
	 * 这里加载数据的过程中使用了setModel来设置新的model对象，这个操作会导致原来的listener失效！
	 * 而titlePanel中的loadData是修改model中的数据对象，而不是直接更换model。
	 * @param targetTableModel
	 */
	public void loadData(TargetTableModel targetTableModel){
		if (targetTableModel == null) {//兼容旧版本
			DomainPanel.backupDB();
			targetTableModel = new TargetTableModel();
			IndexedLinkedHashMap<String, TargetEntry> entries = rootDomianToTarget(DomainPanel.getDomainResult());
			targetTableModel.setData(entries);
		}
		setModel(targetTableModel);//这句很关键，否则无法显示整个表的头和内容
	}

	public static IndexedLinkedHashMap<String, TargetEntry> rootDomianToTarget(DomainManager domainManager){
		IndexedLinkedHashMap<String, TargetEntry> entries = new IndexedLinkedHashMap<String, TargetEntry>();
		//兼容旧版本
		if (domainManager.getRootDomainMap().size() >0 ) {
			for (Map.Entry<String, String> entry : DomainPanel.getDomainResult().getRootDomainMap().entrySet()) {
				String key = entry.getKey();
				if (key.startsWith("[exclude]")) {
					key = key.replaceFirst("\\[exclude\\]", "");
					TargetEntry targetEntry = new TargetEntry(key, false);
					targetEntry.setBlack(true);
					entries.put(key,targetEntry);
				} else {
					entries.put(key, new TargetEntry(key, false));//直接操作entries，避免触发监听器，频繁操作数据库
				}
			}
		}

		//兼容旧版本
		if (domainManager.getSubnetSet().size() >0 ) {
			for (String IPorSubnet : DomainPanel.getDomainResult().getSubnetSet()) {
				entries.put(IPorSubnet,new TargetEntry(IPorSubnet));
			}
		}

		return entries;
	}
}
