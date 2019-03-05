package burp;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
        this.setFillsViewportHeight(true);//在table的空白区域显示右键菜单
        //https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
        
        requestViewer = burp.callbacks.createMessageEditor(lineTableModel, false);
        responseViewer = burp.callbacks.createMessageEditor(lineTableModel, false);
        burp.RequestPanel.addTab("Request", requestViewer.getComponent());
        burp.ResponsePanel.addTab("Response", responseViewer.getComponent());
        addClickSort();
        registerListeners();
        tableinit();
    }

	public BurpExtender getBurp() {
		return burp;
	}

    public void tableinit(){
    	//Font f = new Font("Arial", Font.PLAIN, 12);
    	Font f = this.getFont();
    	FontMetrics fm = this.getFontMetrics(f);
    	int width = fm.stringWidth("A");//一个字符的宽度
    	
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("#")).setPreferredWidth(width*5);
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("#")).setMaxWidth(width*8);
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Status")).setPreferredWidth(width*"Status".length());
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Status")).setMaxWidth(width*("Status".length()+3));
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("isNew")).setPreferredWidth(width*"isNew".length());
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("isNew")).setMaxWidth(width*("isNew".length()+3));
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("isChecked")).setPreferredWidth(width*"isChecked".length());
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("isChecked")).setMaxWidth(width*("isChecked".length()+3));
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Length")).setPreferredWidth(width*10);
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Length")).setMaxWidth(width*15);
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("MIME Type")).setPreferredWidth(width*"MIME Type".length());
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("MIME Type")).setMaxWidth(width*("MIME Type".length()+3));
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Time")).setPreferredWidth(width*22);
    	this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Time")).setMaxWidth(width*25);
    	this.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }
    
    @Override
    public void changeSelection(int row, int col, boolean toggle, boolean extend)
    {
        // show the log entry for the selected row
    	LineEntry Entry = this.lineTableModel.getLineEntries().get(super.convertRowIndexToModel(row));
        requestViewer.setMessage(Entry.getRequest(), true);
        responseViewer.setMessage(Entry.getResponse(), false);
        this.lineTableModel.setCurrentlyDisplayedItem(Entry);
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
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
                	int[] rows = getSelectedRows();
                	
    				for (int i=0; i < rows.length; i++){
    					rows[i] = convertRowIndexToModel(rows[i]);//转换为Model的索引，否则排序后索引不对应〿
    				}
    				Arrays.sort(rows);//升序
    				
    				String host = LineTable.this.lineTableModel.getLineEntries().get(rows[0]).getHost();
    				String url= "https://www.google.com/search?q=site%3A"+host;
    				try {
    					URI uri = new URI(url);
    					Desktop desktop = Desktop.getDesktop();
    					if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
    						desktop.browse(uri);
    					}
    				} catch (Exception e2) {
    					e2.printStackTrace();
    				}
                }
            }

            @Override
            public void mouseReleased( MouseEvent e ){
                if ( SwingUtilities.isRightMouseButton( e )){
                    if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {
                        //getSelectionModel().setSelectionInterval(rows[0], rows[1]);
                    	int[] rows = getSelectedRows();
                    	if (rows.length>0){
							for (int i=0; i < rows.length; i++){
								rows[i] = convertRowIndexToModel(rows[i]);//转换为Model的索引，否则排序后索引不对应〿
							}
							Arrays.sort(rows);//升序

							new LineEntryMenu(_this, rows).show(e.getComponent(), e.getX(), e.getY());
						}else{//在table的空白处显示右键菜单
                    		//https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
							new LineEntryMenu(_this).show(e.getComponent(), e.getX(), e.getY());
						}
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                //no need
            }
          
        });
    }
}
