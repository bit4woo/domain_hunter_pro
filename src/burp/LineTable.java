package burp;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

public class LineTable extends JTable
{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LineTableModel lineTableModel;
	private IMessageEditor requestViewer;
    private IMessageEditor responseViewer;
    private BurpExtender burp;
	private RowSorter<LineTableModel> sorter;
    
    
    public LineTable(LineTableModel lineTableModel,BurpExtender burp)
    {
        super(lineTableModel);
        this.lineTableModel = lineTableModel;
        this.burp = burp;
        
        requestViewer = burp.callbacks.createMessageEditor(lineTableModel, false);
        responseViewer = burp.callbacks.createMessageEditor(lineTableModel, false);
        burp.RequestPanel.addTab("Request", requestViewer.getComponent());
        burp.ResponsePanel.addTab("Response", responseViewer.getComponent());
        addClickSort();
        registerListeners(); 
    }
    
    @Override
    public void changeSelection(int row, int col, boolean toggle, boolean extend)
    {
        // show the log entry for the selected row
    	LineEntry Entry = this.lineTableModel.getLineEntries().get(super.convertRowIndexToModel(row));
        requestViewer.setMessage(Entry.messageinfo.getRequest(), true);
        responseViewer.setMessage(Entry.messageinfo.getResponse(), false);
        this.lineTableModel.setCurrentlyDisplayedItem(Entry.messageinfo);
        super.changeSelection(row, col, toggle, extend);
    }
    
    @Override
    public LineTableModel getModel(){
        return (LineTableModel) super.getModel();
    }
    
    private void addClickSort() {
    	sorter = new TableRowSorter<LineTableModel>(lineTableModel);
		LineTable.this.setRowSorter(sorter);
		
    	JTableHeader header = this.getTableHeader();
    	header.addMouseListener(new MouseAdapter() {
    		@Override
    		public void mouseClicked(MouseEvent e) {
    			try {
    				LineTable.this.getRowSorter().getSortKeys().get(0).getColumn();
					////当Jtable中无数据时，jtable.getRowSorter()是nul
				} catch (Exception e1) {
					e1.printStackTrace(LineTable.this.burp.stderr);
				}
    		}
    	});
    }
    
    private void addContentFilter() {
        
        List<RowFilter<LineTableModel,Object>> filters = new ArrayList<RowFilter<LineTableModel,Object>>();
        
        filters.add(RowFilter.regexFilter("TD001"));
        filters.add(RowFilter.regexFilter("TD002"));

/*        RowFilter<LineTableModel,Object> arregloFiltros = RowFilter.orFilter(filters);      
        sorter.setRowFilter(arregloFiltros);  */
     
        setSize(200, 150);
        setVisible(true);
    }
    
    private void registerListeners(){
        final LineTable _this = this;
        this.addMouseListener( new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e) {
                onMouseEvent(e);
            }

            @Override
            public void mouseReleased( MouseEvent e ){
                onMouseEvent(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                onMouseEvent(e);
            }

            private void onMouseEvent(MouseEvent e){
                if ( SwingUtilities.isRightMouseButton( e )){
                    if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {
                        //getSelectionModel().setSelectionInterval(rows[0], rows[1]);
                    	int[] rows = getSelectedRows();
                    	
        				for (int i=0; i < rows.length; i++){
        					rows[i] = convertRowIndexToModel(rows[i]);//转换为Model的索引，否则排序后索引不对应〿
        				}
        				Arrays.sort(rows);//升序
        				
                    	LineTable.this.burp.stdout.println(rows.length+" items deleted");
                    	new LineEntryMenu(_this, rows).show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }
}
