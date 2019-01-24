package burp;

import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IMessageEditorController;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class LineTableModel extends AbstractTableModel implements IMessageEditorController,Serializable {
	//https://stackoverflow.com/questions/11553426/error-in-getrowcount-on-defaulttablemodel
	//when use DefaultTableModel, getRowCount encounter NullPointerException. why?
    /**
	 * LineTableModel中数据如果类型不匹配，或者有其他问题，可能导致图形界面加载异常！
	 */
	private static final long serialVersionUID = 1L;
	private IHttpRequestResponse currentlyDisplayedItem;
    private List<LineEntry> lineEntries =new ArrayList<LineEntry>();
    private IBurpExtender burp;
    private static final String[] titles = new String[] {
    		"#", "URL", "Status", "Length", "MIME Type", "Title", "IP", "Time","isNew"
    	};

    public LineTableModel(final BurpExtender burp){
        this.burp = burp;

    }
    
    public List<LineEntry> getLineEntries() {
		return lineEntries;
	}

	public void setLineEntries(List<LineEntry> lineEntries) {
		this.lineEntries = lineEntries;
	}
	
	public List<String> getbodyTexts(){
		List<String> result = new ArrayList<String>();
		for(LineEntry line:lineEntries) {
			String linetext = line.getBodyText();
			result.add(linetext);
		}
		return result;
	}
    
    
    ////////////////////// extend AbstractTableModel////////////////////////////////

    @Override
    public int getColumnCount()
    {
        return titles.length;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {	switch(columnIndex) 
    	{
    	case 0: 
    		return Integer.class;//id
    	case 2: 
    		return Integer.class;//Status
    	case 3: 
    		return Integer.class;//Length
    	case 8:
    		return boolean.class;
    	default:
    		return String.class;
    	}

    }

    @Override
    public int getRowCount()
    {
        return lineEntries.size();
    }
    
    //define header of table???
    @Override
    public String getColumnName(int columnIndex) {
		if (columnIndex >= 0 && columnIndex <= titles.length) {
			return titles[columnIndex];
		}else {
			return "";
		}
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    
    public void removeRows(int[] rows) {
        synchronized (lineEntries) {
        	//because thread let the delete action not in order, so we must loop in here.
        	//list length and index changed after every remove.the origin index not point to right item any more.
        	Arrays.sort(rows); //升序
        	for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
            	lineEntries.remove(rows[i]);
                this.fireTableRowsDeleted(rows[i], rows[i]);
        	}
        }
    }
    

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        LineEntry entry = lineEntries.get(rowIndex);
        entry.parse();
        switch (columnIndex)
        {
            case 0:
                return rowIndex;
            case 1:
                return entry.getUrl();
            case 2:
                return entry.getStatuscode();
            case 3:
                return entry.getContentLength();
            case 4:
                return entry.getMIMEtype();
            case 5:
                return entry.getTitle();
            case 6:
                return entry.getIP();
            case 7:
                return entry.getTime();
            case 8:
            	return entry.isNew();
            default:
                return "";
        }
    }
    
    //////////////////////extend AbstractTableModel////////////////////////////////

    public void addNewLineEntry(LineEntry lineEntry){
        synchronized (lineEntries) {
            while(lineEntries.size() >= (new LineConfig()).getMaximumEntries()){
                final LineEntry removed = lineEntries.remove(0);
            }
    		
            lineEntries.add(lineEntry);
            int row = lineEntries.size();
            //fireTableRowsInserted(row, row);
            //need to use row-1 when add setRowSorter to table. why??
            //https://stackoverflow.com/questions/6165060/after-adding-a-tablerowsorter-adding-values-to-model-cause-java-lang-indexoutofb
            fireTableRowsInserted(row-1, row-1);
        }
    }
    
    
    public IHttpRequestResponse getCurrentlyDisplayedItem() {
        return this.currentlyDisplayedItem;
    }

    public void setCurrentlyDisplayedItem(IHttpRequestResponse currentlyDisplayedItem) {
        this.currentlyDisplayedItem = currentlyDisplayedItem;
    }
    
    //
    // implement IMessageEditorController
    // this allows our request/response viewers to obtain details about the messages being displayed
    //

    @Override
    public byte[] getRequest()
    {
        if(getCurrentlyDisplayedItem()==null) {
        	return "".getBytes();
        }
        return getCurrentlyDisplayedItem().getRequest();
    }

    @Override
    public byte[] getResponse()
    {
        if(getCurrentlyDisplayedItem()==null) {
        	return "".getBytes();
        }
        return getCurrentlyDisplayedItem().getResponse();
    }

    @Override
    public IHttpService getHttpService()
    {
        if(getCurrentlyDisplayedItem()==null) {
        	return null;
        }
        return getCurrentlyDisplayedItem().getHttpService();
    }
    
    
/*    public class LineTable extends JTable
    {	
    	*//**
    	 * 
    	 *//*
    	private static final long serialVersionUID = 1L;
        public LineTable(LineTableModel lineTableModel)
        {
            super(lineTableModel);
        }
        
        @Override
        public void changeSelection(int row, int col, boolean toggle, boolean extend)
        {
            // show the log entry for the selected row
        	LineEntry Entry = lineEntries.get(super.convertRowIndexToModel(row));
            requestViewer.setMessage(Entry.messageinfo.getRequest(), true);
            responseViewer.setMessage(Entry.messageinfo.getResponse(), false);
            currentlyDisplayedItem = Entry.messageinfo;
            super.changeSelection(row, col, toggle, extend);
        }
    }*/
}