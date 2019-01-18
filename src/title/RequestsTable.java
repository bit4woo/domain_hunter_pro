package title;

import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.TableModel;

public class RequestsTable extends JTable
{	
	private TableModel tableModel;
    public RequestsTable(TableModel tableModel)
    {
        super(tableModel);
        this.tableModel = tableModel;
    }
    
    @Override
    public void changeSelection(int row, int col, boolean toggle, boolean extend)
    {
        // show the log entry for the selected row
/*	    
	    RequestEntry RequestEntry = TableModel.get(row);
        requestViewer.setMessage(RequestEntry.requestResponse.getRequest(), true);
        responseViewer.setMessage(RequestEntry.requestResponse.getResponse(), false);
        currentlyDisplayedItem = RequestEntry.requestResponse;*/
        
        super.changeSelection(row, col, toggle, extend);
    }        
}
