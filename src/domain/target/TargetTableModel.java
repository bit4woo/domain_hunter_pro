package domain.target;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import burp.BurpExtender;
import domain.DomainPanel;
import title.IndexedLinkedHashMap;
import title.LineEntry;

public class TargetTableModel extends AbstractTableModel {

	private IndexedLinkedHashMap<String,TargetEntry> targetEntries =new IndexedLinkedHashMap<String,TargetEntry>();
	private boolean ListenerIsOn = true;

	PrintWriter stdout;
	PrintWriter stderr;

	private static final String[] standardTitles = new String[] {
			"Domain/Subnet/IP", "Keyword", "Comments"};
	private static List<String> titletList = new ArrayList<>(Arrays.asList(standardTitles));
	//为了实现动态表结构
	public static List<String> getTitletList() {
		return titletList;
	}
	public TargetTableModel(){
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}


		/**
		 * 
		 * 注意，所有直接对TargetTableModel中数据的修改，都不会触发该tableChanged监听器。
		 * 除非操作的逻辑中包含了firexxxx来主动通知监听器。
		 * DomainPanel.domainTableModel.fireTableChanged(null);
		 */
		addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (ListenerIsOn) {
					DomainPanel.getDomainResult().setTargetEntries(targetEntries);
					DomainPanel.autoSave();
				}
			}
		});
	}

	@Override
	public int getRowCount() {
		return targetEntries.size();
	}

	@Override
	public int getColumnCount() {
		return standardTitles.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		TargetEntry entry = targetEntries.get(rowIndex);
		if (columnIndex == titletList.indexOf("Domain/Subnet/IP")) {
			return entry.getTarget();
		}
		if (columnIndex == titletList.indexOf("Keyword")) {
			return entry.getKeyword();
		}
		if (columnIndex == titletList.indexOf("Comments")) {
			return entry.getComment();
		}
		return "";
	}

	public void addRow(String key,TargetEntry entry) {
		int oldsize = targetEntries.size();
		targetEntries.put(key,entry);
		int newsize = targetEntries.size();
		if (oldsize ==newsize) {
			int rowIndex = targetEntries.IndexOfKey(key);
			fireTableRowsUpdated(rowIndex,rowIndex);
		}else {
			int rowIndex = getRowCount();
			fireTableRowsInserted(rowIndex, rowIndex);
		}
	}

	public void removeRow(int rowIndex) {
		targetEntries.remove(rowIndex);
		fireTableRowsInserted(rowIndex, rowIndex);
	}

	public void removeRow(String key) {
		int rowIndex = targetEntries.IndexOfKey(key);
		targetEntries.remove(key);
		fireTableRowsInserted(rowIndex, rowIndex);
	}
	
	public TargetEntry getValueAt(int rowIndex) {
		TargetEntry entry = targetEntries.get(rowIndex);
		return entry;
	}
	
	public void updateRow(TargetEntry entry) {
		String key = entry.getTarget();
		addRow(key,entry);
	}

}
