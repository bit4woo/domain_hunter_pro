package burp;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import Config.Dork;

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
		LineEntry Entry = this.lineTableModel.getLineEntries().getValueAtIndex(super.convertRowIndexToModel(row));

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


		Map<String,Integer> preferredWidths = new HashMap<String,Integer>();
		preferredWidths.put("#",5);
		preferredWidths.put("URL",25);
		preferredWidths.put("Status",6);
		preferredWidths.put("Length",10);
		preferredWidths.put("Title",30);
		preferredWidths.put("Comments",30);
		preferredWidths.put("Time","2019-05-28-14-13-16".length());
		preferredWidths.put("isChecked","isChecked".length());
		preferredWidths.put("IP",30);
		preferredWidths.put("CDN",30);
		preferredWidths.put("Server",10);
		for(String header:LineTableModel.getTitletList()){
			try{//避免动态删除表字段时，出错
				int multiNumber = preferredWidths.get(header);
				this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex(header)).setPreferredWidth(width*multiNumber);
			}catch (Exception e){

			}
		}
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

	//TODO,还没弄明白
	public void setColor(int inputRow) {
		try {
			DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer() {
				//重写getTableCellRendererComponent 方法
				@Override
				public Component getTableCellRendererComponent(JTable table,Object value, boolean isSelected, boolean hasFocus,int row, int column) {
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					if (row == 1) {
						c.setBackground(Color.RED);
					}
					return c;
				}
			};
			//对每行的每一个单元格
			int columnCount = this.getColumnCount();
			for (int i = 0; i < columnCount; i++) {
				this.getColumn(this.getColumnName(i)).setCellRenderer(dtcr);
			}

		} catch (Exception e) {
			e.printStackTrace();
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

	public static boolean entryNeedToShow(LineEntry entry) {

		if (TitlePanel.rdbtnCheckedItems.isSelected()&& entry.statusIsChecked()) {
			return true;
		}

		if (TitlePanel.rdbtnCheckingItems.isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_Checking)) {
			return true;//小心 == 和 equals的区别，之前这里使用 ==就导致了checking状态的条目的消失。
		}

		if (TitlePanel.rdbtnUnCheckedItems.isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_UnChecked)) {
			return true;
		}

		return false;
	}

	//dork搜索和全数据包字符串搜索
	public void search(String keyword) {
		keyword = keyword.trim().toLowerCase();
		if (Dork.isDorkString(keyword)) {
			dorkSearch(keyword);
		}else {
			fullSearch(keyword);
		}
	}

	public void fullSearch(String Inputkeyword) {
		//rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + keyword));

		Inputkeyword = Inputkeyword.trim().toLowerCase();
		if (Inputkeyword.contains("\"") || Inputkeyword.contains("\'")){
			//为了处理输入是"dork:12345"的情况，下面的这种写法其实不严谨，中间也可能有引号，不过应付一般的搜索足够了。
			Inputkeyword = Inputkeyword.replaceAll("\"", "");
			Inputkeyword = Inputkeyword.replaceAll("\'", "");
		}
		final String keyword = Inputkeyword;
		final RowFilter filter = new RowFilter() {
			@Override
			public boolean include(Entry entry) {
				//entry --- a non-null object that wraps the underlying object from the model
				int row = (int) entry.getIdentifier();
				LineEntry line = rowSorter.getModel().getLineEntries().getValueAtIndex(row);

				//第一层判断，根据按钮状态进行判断，如果为true，进行后面的逻辑判断，false直接返回。
				if (!entryNeedToShow(line)) {
					if (selectedRow == row) {
						selectedRow = row+1;
					}
					return false;
				}

				if (keyword.length() == 0) {
					return true;
				}else {//全局搜索
					if (new String(line.getRequest()).toLowerCase().contains(keyword)) {
						return true;
					}
					if (new String(line.getResponse()).toLowerCase().contains(keyword)) {
						return true;
					}
					if (new String(line.getUrl()).toLowerCase().contains(keyword)) {
						return true;
					}
					if (new String(line.getIP()).toLowerCase().contains(keyword)) {
						return true;
					}
					if (new String(line.getCDN()).toLowerCase().contains(keyword)) {
						return true;
					}
					if (new String(line.getComment()).toLowerCase().contains(keyword)) {
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

	//支持部分类似google dork的搜索语法
	//Host url header body request response comment
	public void dorkSearch(String dorkString) {

		dorkString = dorkString.toLowerCase().trim();

		String[] arr = dorkString.split(":",2);//limit =2 分割成2份
		String dork = arr[0].trim();
		String keyword =  arr[1].trim();

		final RowFilter filter = new RowFilter() {
			@Override
			public boolean include(Entry entry) {
				//entry --- a non-null object that wraps the underlying object from the model
				int row = (int) entry.getIdentifier();
				LineEntry line = rowSorter.getModel().getLineEntries().getValueAtIndex(row);

				if (!entryNeedToShow(line)) {
					if (selectedRow == row) {
						selectedRow = row+1;
					}
					return false;
				}

				if (dork.equalsIgnoreCase(Dork.HOST)) {
					if (line.getHost().toLowerCase().contains(keyword)) {
						return true;
					}else {
						return false;
					}
				}

				if (dork.equalsIgnoreCase(Dork.URL)) {
					if (line.getUrl().toLowerCase().contains(keyword)) {
						return true;
					}else {
						return false;
					}
				}

				if (dork.equalsIgnoreCase(Dork.REQUEST)) {
					if (new String(line.getRequest()).toLowerCase().contains(keyword)) {
						return true;
					}else {
						return false;
					}
				}

				if (dork.equalsIgnoreCase(Dork.RESPONSE)) {
					if (new String(line.getResponse()).toLowerCase().contains(keyword)) {
						return true;
					}else {
						return false;
					}
				}

				if (dork.equalsIgnoreCase(Dork.COMMENT)) {
					if (line.getComment().toLowerCase().contains(keyword)) {
						return true;
					}else {
						return false;
					}
				}
				stderr.println("Unsupported dork: "+dork);
				return false;
			}
		};
		rowSorter.setRowFilter(filter);

		try {
			this.setRowSelectionInterval(selectedRow,selectedRow);
		} catch (Exception e) {
			//e.printStackTrace(stderr);//java.lang.IllegalArgumentException: Row index out of range
		}
	}

	public void searchRegex(String regex) {		

		final RowFilter filter = new RowFilter() {
			@Override
			public boolean include(Entry entry) {
				//entry --- a non-null object that wraps the underlying object from the model
				int row = (int) entry.getIdentifier();
				LineEntry line = rowSorter.getModel().getLineEntries().getValueAtIndex(row);

				if (!entryNeedToShow(line)) {
					if (selectedRow == row) {
						selectedRow = row+1;
					}
					return false;
				}

				Pattern pRegex = Pattern.compile(regex);

				if (regex.trim().length() == 0) {
					return true;
				} else {
					if (pRegex.matcher(new String(line.getRequest()).toLowerCase()).find()) {
						return true;
					}
					if (pRegex.matcher(new String(line.getResponse()).toLowerCase()).find()) {
						return true;
					}
					if (pRegex.matcher(new String(line.getUrl()).toLowerCase()).find()) {
						return true;
					}
					if (pRegex.matcher(new String(line.getIP()).toLowerCase()).find()) {
						return true;
					}
					if (pRegex.matcher(new String(line.getCDN()).toLowerCase()).find()) {
						return true;
					}
					if (pRegex.matcher(new String(line.getComment()).toLowerCase()).find()) {
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

					LineEntry selecteEntry = LineTable.this.lineTableModel.getLineEntries().getValueAtIndex(rows[0]);
					if ((col==0 )) {//双击index在google中搜索host。
						String host = selecteEntry.getHost();
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
					}else if(col==1) {//双击url在浏览器中打开
						try{
							String url = selecteEntry.getUrl();
							Commons.browserOpen(url,ToolPanel.getLineConfig().getBrowserPath());
						}catch (Exception e1){
							e1.printStackTrace(stderr);
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
