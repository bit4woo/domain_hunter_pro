package burp;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;

public class LineTable extends JTable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private LineTableModel lineTableModel;
	private TableRowSorter<LineTableModel> rowSorter;//TableRowSorter vs. RowSorter

	private IMessageEditor requestViewer;
	private IMessageEditor responseViewer;
	PrintWriter stdout;
	PrintWriter stderr;

	private JSplitPane tableAndDetailSplitPane;//table area + detail area
	public JSplitPane getTableAndDetailSplitPane() {
		return tableAndDetailSplitPane;
	}


	private int selectedRow = this.getSelectedRow();//to identify the selected row after search or hide lines

	public LineTable(LineTableModel lineTableModel)
	{
		//super(lineTableModel);//这个方法创建的表没有header
        try{
            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
        }catch (Exception e){
            stdout = new PrintWriter(System.out, true);
            stderr = new PrintWriter(System.out, true);
        }

		this.lineTableModel = lineTableModel;
		this.setFillsViewportHeight(true);//在table的空白区域显示右键菜单
		//https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
		this.setModel(lineTableModel);

		tableinit();
		//FitTableColumns(this);
		addClickSort();
		registerListeners();

		tableAndDetailSplitPane = tableAndDetailPanel();
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
		//return (LineTableModel) super.getModel();
		return lineTableModel;
	}


	public JSplitPane tableAndDetailPanel(){
		JSplitPane splitPane = new JSplitPane();//table area + detail area
		splitPane.setResizeWeight(0.5);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		//TitlePanel.add(splitPane, BorderLayout.CENTER); // getTitlePanel to get it

		JScrollPane scrollPaneRequests = new JScrollPane(this,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);//table area
		//允许横向滚动条
		//scrollPaneRequests.setViewportView(titleTable);//titleTable should lay here.
		splitPane.setLeftComponent(scrollPaneRequests);

		JSplitPane RequestDetailPanel = new JSplitPane();//request and response
		RequestDetailPanel.setResizeWeight(0.5);
		splitPane.setRightComponent(RequestDetailPanel);

		JTabbedPane RequestPanel = new JTabbedPane();
		RequestDetailPanel.setLeftComponent(RequestPanel);

		JTabbedPane ResponsePanel = new JTabbedPane();
		RequestDetailPanel.setRightComponent(ResponsePanel);

		requestViewer = BurpExtender.getCallbacks().createMessageEditor(this.getModel(), false);
		responseViewer = BurpExtender.getCallbacks().createMessageEditor(this.getModel(), false);
		RequestPanel.addTab("Request", requestViewer.getComponent());
		ResponsePanel.addTab("Response", responseViewer.getComponent());
		
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);//配合横向滚动条

		return splitPane;
	}

	public void tableinit(){
		//Font f = new Font("Arial", Font.PLAIN, 12);
		Font f = this.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		int width = fm.stringWidth("A");//一个字符的宽度
		this.getColumnModel().getColumnIndex("#");

		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("#")).setPreferredWidth(width*5);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("#")).setMaxWidth(width*8);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("URL")).setPreferredWidth(width*25);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("URL")).setMaxWidth(width*50);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Title")).setPreferredWidth(width*30);
		//this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Title")).setMaxWidth(width*50);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("IP")).setPreferredWidth(width*30);
		//this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("IP")).setMaxWidth(width*50);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("CDN")).setPreferredWidth(width*30);
		//this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("CDN")).setMaxWidth(width*50);
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Comments")).setPreferredWidth(width*30);
		
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
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Time")).setPreferredWidth(width*("2019-05-28-14-13-16".length()));
		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Time")).setMaxWidth(width*25);
//		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Text")).setPreferredWidth(width*0);//response text,for search
//		this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex("Text")).setMaxWidth(width*0);//response text,for search
		//this.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);//配合横向滚动条

	}
	
	@Deprecated//据说自动调整行宽度，测试了一下没用啊
	public void FitTableColumns(JTable myTable){
		  JTableHeader header = myTable.getTableHeader();
		     int rowCount = myTable.getRowCount();
		     Enumeration columns = myTable.getColumnModel().getColumns();
		     while(columns.hasMoreElements()){
		         TableColumn column = (TableColumn)columns.nextElement();
		         int col = header.getColumnModel().getColumnIndex(column.getIdentifier());
		         int width = (int)myTable.getTableHeader().getDefaultRenderer()
		                 .getTableCellRendererComponent(myTable, column.getIdentifier()
		                         , false, false, -1, col).getPreferredSize().getWidth();
		         for(int row = 0; row<rowCount; row++){
		             int preferedWidth = (int)myTable.getCellRenderer(row, col).getTableCellRendererComponent(myTable,
		               myTable.getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth();
		             width = Math.max(width, preferedWidth);
		         }
		         header.setResizingColumn(column); // 此行很重要
		         column.setWidth(width+myTable.getIntercellSpacing().width);
		     }
	}


	public void addClickSort() {//双击header头进行排序

		rowSorter = new TableRowSorter<LineTableModel>(lineTableModel);//排序和搜索
		LineTable.this.setRowSorter(rowSorter);

		JTableHeader header = this.getTableHeader();
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					LineTable.this.getRowSorter().getSortKeys().get(0).getColumn();
					////当Jtable中无数据时，jtable.getRowSorter()是nul
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		});
	}

	public void search(String keywork) {
		//rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + keywork));

		final RowFilter filter = new RowFilter() {
			@Override
			public boolean include(Entry entry) {
				//entry --- a non-null object that wraps the underlying object from the model
				int row = (int) entry.getIdentifier();
				LineEntry line = rowSorter.getModel().getLineEntries().get(row);

				if (GUI.getTitlePanel().getSitemapTree().getCurrentSelected().size() > 0){
					for (String item:GUI.getTitlePanel().getSitemapTree().getCurrentSelected()){
						if (line.getUrl().contains(item)){
							return true;
						}
					}
				}

				if (GUI.getTitlePanel().rdbtnHideCheckedItems.isSelected()&& line.isChecked()) {//to hide checked lines
					if (selectedRow == row) {
						selectedRow = row+1;
					}
					return false;
				}

				if (keywork.trim().length() == 0) {
					return true;
				} else {
					if (new String(line.getRequest()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getResponse()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getUrl()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getIP()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getCDN()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (new String(line.getComment()).toLowerCase().contains(keywork.toLowerCase())) {
						return true;
					}
					if (selectedRow== row) {
						selectedRow = row+1;
					}
					return false;
				}
			}
		};
		rowSorter.setRowFilter(filter);

		try {
			this.setRowSelectionInterval(selectedRow,selectedRow);
		} catch (Exception e) {
			//e.printStackTrace(stderr);//java.lang.IllegalArgumentException: Row index out of range
		}

	}

	public void registerListeners(){
		LineTable.this.setRowSelectionAllowed(true);
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

					//int row = ((LineTable) e.getSource()).rowAtPoint(e.getPoint()); // 获得行位置
					int col = ((LineTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置


					if ((col < LineTable.this.getColumnModel().getColumnIndex("Comments"))) {//last column----comments
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

							new LineEntryMenu(LineTable.this, rows).show(e.getComponent(), e.getX(), e.getY());
						}else{//在table的空白处显示右键菜单
							//https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
							//new LineEntryMenu(_this).show(e.getComponent(), e.getX(), e.getY());
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
