package test;

import java.util.List;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import burp.BurpExtender;
import burp.IBurpExtender;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IMessageEditor;
import burp.IMessageEditorController;
import burp.LineEntry;


public class LineTableModel_backup extends DefaultTableModel implements IMessageEditorController {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private IHttpRequestResponse currentlyDisplayedItem;
    private LineManager lineManager;
    private IMessageEditor requestViewer;
    private IMessageEditor responseViewer;
    private IBurpExtender burp;

    public LineTableModel_backup(final BurpExtender burp, LineManager lineManager){
        this.lineManager = lineManager;
        this.burp = burp;
        requestViewer = burp.callbacks.createMessageEditor(LineTableModel_backup.this, false);
        responseViewer = burp.callbacks.createMessageEditor(LineTableModel_backup.this, false);
        burp.RequestPanel.addTab("Request", requestViewer.getComponent());
        burp.ResponsePanel.addTab("Response", responseViewer.getComponent());
		
        //create table and add to GUI Panel
		LineTable table_1 = new LineTable(this);
		burp.scrollPaneRequests.setViewportView(table_1);
    }
    
    
    ////////////////////// extend AbstractTableModel////////////////////////////////

    @Override
    public int getColumnCount()
    {
        return 11;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return String.class;
    }

    @Override
    public int getRowCount()
    {
        // To delete the Request/Response logTable the log section is empty (after deleting the logs when an item is already selected)
        if(currentlyDisplayedItem!=null && lineManager.getLineEntries().size() <= 0){
            currentlyDisplayedItem = null;
        }
        //DefaultTableModel calls this before we can set the entries list.
        if(lineManager == null || lineManager.getLineEntries()==null) return 0;
        return lineManager.getLineEntries().size();
    }
    
    //define header of table???
    @Override
    public String getColumnName(int columnIndex) {
		String[] titles = new String[] {
		"#", "URL", "Status", "Length", "MIME Type", "Title", "IP", "Time", "New column", "New column", "New column", "New column", "New column", "New column", "New column", "New column"
	};
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


    @Override
    public void removeRow(int row) {
        synchronized (lineManager.getLineEntries()) {
        	lineManager.getLineEntries().remove(row);
            this.fireTableRowsDeleted(row, row);
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if(rowIndex >= lineManager.getLineEntries().size()) return null;
        if(columnIndex == 0) return rowIndex+1;
        return lineManager.getLineEntries().get(rowIndex).getValue(columnIndex);
    }
    
    //////////////////////extend AbstractTableModel////////////////////////////////

    
    
    
    public IHttpRequestResponse getCurrentlyDisplayedItem() {
        return this.currentlyDisplayedItem;
    }

    public void setCurrentlyDisplayedItem(IHttpRequestResponse currentlyDisplayedItem) {
        this.currentlyDisplayedItem = currentlyDisplayedItem;
    }

    public List<LineEntry> getData() {
        return this.lineManager.getLineEntries();
    }

    public LineEntry getRow(int row) {return this.lineManager.getLineEntries().get(row);}

    
    
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
    
    
    public class LineTable extends JTable
    {	
    	/**
    	 * 
    	 */
    	private static final long serialVersionUID = 1L;
    	private TableModel tableModel;
        public LineTable(TableModel tableModel)
        {
            super(tableModel);
            this.tableModel = tableModel;
            
        }
        
        @Override
        public void changeSelection(int row, int col, boolean toggle, boolean extend)
        {
            // show the log entry for the selected row
    
        	LineEntry Entry = getData().get(super.convertRowIndexToModel(row));
            requestViewer.setMessage(Entry.getMessageinfo().getRequest(), true);
            responseViewer.setMessage(Entry.getMessageinfo().getResponse(), false);
            currentlyDisplayedItem = Entry.getMessageinfo();
            super.changeSelection(row, col, toggle, extend);
        }
    }
}