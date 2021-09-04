package title;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.IMessageEditor;
import title.search.History;
import title.search.LineSearch;
import title.search.SearchDork;
import title.search.SearchTextField;


public class LineTable extends JTable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private LineTableModel lineTableModel;
	private TableRowSorter<LineTableModel> tableRowSorter;//TableRowSorter vs. RowSorter

	private IMessageEditor requestViewer;
	private IMessageEditor responseViewer;
	PrintWriter stdout;
	PrintWriter stderr;

	private JSplitPane tableAndDetailSplitPane;//table area + detail area
	public JSplitPane getTableAndDetailSplitPane() {
		return tableAndDetailSplitPane;
	}

	@Override//参考javax.swing.JTable中的函数，每次都有主动进行转换
	public Object getValueAt(int row, int column) {
		return getModel().getValueAt(convertRowIndexToModel(row),
				convertColumnIndexToModel(column));
	}

	public LineEntry getRowAt(int row) {
		return getModel().getLineEntries().getValueAtIndex(convertRowIndexToModel(row));
	}

	//将选中的行（图形界面的行）转换为Model中的行数（数据队列中的index）.因为图形界面排序等操作会导致图像和数据队列的index不是线性对应的。
	public int[] SelectedRowsToModelRows(int[] SelectedRows) {

		int[] rows = SelectedRows;
		for (int i=0; i < rows.length; i++){
			rows[i] = convertRowIndexToModel(rows[i]);//转换为Model的索引，否则排序后索引不对应〿
		}
		Arrays.sort(rows);//升序

		return rows;
	}

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
		tableRowSorter = new TableRowSorter<LineTableModel>(lineTableModel);
		setRowSorter(tableRowSorter);
		//addClickSort();
		//FitTableColumns(this);
		//this.setAutoCreateRowSorter(true);

		registerListeners();

		tableAndDetailSplitPane = tableAndDetailPanel();
	}

	@Override
	public void changeSelection(int row, int col, boolean toggle, boolean extend)
	{
		// show the log entry for the selected row
		//LineEntry Entry = this.lineTableModel.getLineEntries().getValueAtIndex(super.convertRowIndexToModel(row));
		LineEntry Entry = this.getRowAt(row);

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
		preferredWidths.put("CheckDoneTime","2019-05-28-14-13-16".length());
		preferredWidths.put("isChecked"," isChecked ".length());
		preferredWidths.put("IP",30);
		preferredWidths.put("CDN|CertInfo",30);
		preferredWidths.put("Server",10);
		preferredWidths.put("IconHash", "-17480088888".length());
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
	@Deprecated
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

	@Deprecated //还是没能解决添加数据时排序报错的问题
	public void addClickSort() {//双击header头进行排序
		tableRowSorter = new TableRowSorter<LineTableModel>(lineTableModel);
		setRowSorter(tableRowSorter);

		JTableHeader header = this.getTableHeader();
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					if (LineTable.this.getModel() != null) {////当Jtable中无数据时，jtable.getRowSorter()是nul
						//https://bugs.openjdk.java.net/browse/JDK-6386900
						//当model中还在添加数据时，如果进行排序，就会导致出错
						int col = ((LineTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
						List<RowSorter.SortKey> keys = (List<RowSorter.SortKey>) LineTable.this.getRowSorter().getSortKeys();
						keys.add(new RowSorter.SortKey(col, SortOrder.ASCENDING));
						tableRowSorter.setSortKeys(keys);
					}
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		});
	}

	/**
	 * 搜索功能，自动获取caseSensitive的值
	 * @param keyword
	 */
	public void search(String keyword) {
		SearchTextField searchTextField = (SearchTextField)TitlePanel.getTextFieldSearch();
		boolean caseSensitive = searchTextField.isCaseSensitive();
		search(keyword,caseSensitive);
	}

	/**
	 * 搜索功能
	 * @param caseSensitive
	 */
	public void search(String Input,boolean caseSensitive) {
		//rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + keyword));
		History.getInstance().addRecord(Input);//记录搜索历史,单例模式

		final RowFilter filter = new RowFilter() {
			@Override
			public boolean include(Entry entry) {
				//entry --- a non-null object that wraps the underlying object from the model
				int row = (int) entry.getIdentifier();
				LineEntry line = LineTable.this.getModel().getLineEntries().getValueAtIndex(row);

				//第一层判断，根据按钮状态进行判断，如果为true，进行后面的逻辑判断，false直接返回。
				if (!LineSearch.entryNeedToShow(line)) {
					return false;
				}
				//目前只处理&&（and）逻辑的表达式
				if (Input.contains("&&")) {
					String[] searchConditions = Input.split("&&");
					for (String condition:searchConditions) {
						if (oneCondition(condition,line)) {
							continue;
						}else {
							return false;
						}
					}
					return true;
				}else {
					return oneCondition(Input,line);
				}
			}
			public boolean oneCondition(String Input,LineEntry line) {
				if (SearchDork.isDork(Input)) {
					//stdout.println("do dork search,dork:"+dork+"   keyword:"+keyword);
					return LineSearch.dorkFilter(line,Input,caseSensitive);
				}else {
					return LineSearch.textFilter(line,Input,caseSensitive);
				}
			}
		};
		tableRowSorter.setRowFilter(filter);
	}


	public void registerListeners(){
		LineTable.this.setRowSelectionAllowed(true);
		this.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e) {
				//双击进行google搜索、双击浏览器打开url、双击切换Check状态
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
					int[] rows = SelectedRowsToModelRows(getSelectedRows());

					//int row = ((LineTable) e.getSource()).rowAtPoint(e.getPoint()); // 获得行位置
					int col = ((LineTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
					int modelCol = LineTable.this.convertColumnIndexToModel(col);

					LineEntry selecteEntry = LineTable.this.lineTableModel.getLineEntries().getValueAtIndex(rows[0]);
					if ((modelCol == LineTableModel.getTitletList().indexOf("#") )) {//双击index在google中搜索host。
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
					}else if(modelCol==LineTableModel.getTitletList().indexOf("URL")) {//双击url在浏览器中打开
						try{
							String url = selecteEntry.getUrl();
							if (url != null && !url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
								url = "http://"+url;//针对DNS记录中URL字段是host的情况
							}
							Commons.browserOpen(url,ToolPanel.getLineConfig().getBrowserPath());
						}catch (Exception e1){
							e1.printStackTrace(stderr);
						}
					}else if (modelCol == LineTableModel.getTitletList().indexOf("isChecked")) {
						try{
							//LineTable.this.lineTableModel.updateRowsStatus(rows,LineEntry.CheckStatus_Checked);//处理多行
							String currentStatus= selecteEntry.getCheckStatus();
							List<String> tmpList = Arrays.asList(LineEntry.CheckStatusArray);
							int index = tmpList.indexOf(currentStatus);
							String newStatus = tmpList.get((index+1)%LineEntry.CheckStatusArray.length);
							selecteEntry.setCheckStatus(newStatus);
							if (newStatus.equalsIgnoreCase(LineEntry.CheckStatus_Checked)) {
								selecteEntry.setTime(Commons.getNowTimeString());
							}
							stdout.println("$$$ "+selecteEntry.getUrl()+" status has been set to "+newStatus);
							LineTable.this.lineTableModel.fireTableRowsUpdated(rows[0], rows[0]);
						}catch (Exception e1){
							e1.printStackTrace(stderr);
						}
					}else if (modelCol == LineTableModel.getTitletList().indexOf("AssetType")) {
						String currentLevel = selecteEntry.getAssetType();
						List<String> tmpList = Arrays.asList(LineEntry.AssetTypeArray);
						int index = tmpList.indexOf(currentLevel);
						String newLevel = tmpList.get((index+1)%3);
						selecteEntry.setAssetType(newLevel);
						stdout.println(String.format("$$$ %s updated [AssetType-->%s]",selecteEntry.getUrl(),newLevel));
						LineTable.this.lineTableModel.fireTableRowsUpdated(rows[0], rows[0]);
					}else{//LineTableModel.getTitletList().indexOf("CDN|CertInfo")
						//String value = TitlePanel.getTitleTable().getValueAt(rows[0], col).toString();//rows[0]是转换过的，不能再转换
						//调用的是原始Jtable中的getValueAt，它本质上也是调用model中的getValueAt，但是有一次转换的过程！！！
						String value = LineTable.this.lineTableModel.getValueAt(rows[0],modelCol).toString();
						//调用的是我们自己实现的TableModel类中的getValueAt,相比Jtable类中的同名方法，就少了一次转换的过程！！！
						//String CDNAndCertInfo = selecteEntry.getCDN();
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection selection = new StringSelection(value);
						clipboard.setContents(selection, null);
					}
				}
			}

			@Override//title表格中的鼠标右键菜单
			public void mouseReleased( MouseEvent e ){//在windows中触发,因为isPopupTrigger在windows中是在鼠标释放是触发的，而在mac中，是鼠标点击时触发的。
				//https://stackoverflow.com/questions/5736872/java-popup-trigger-in-linux
				if ( SwingUtilities.isRightMouseButton( e )){
					if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {
						//getSelectionModel().setSelectionInterval(rows[0], rows[1]);
						int[] rows = getSelectedRows();
						int col = ((LineTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
						int modelCol = LineTable.this.convertColumnIndexToModel(col);
						if (rows.length>0){
							int[] modelRows = SelectedRowsToModelRows(rows);
							new LineEntryMenu(LineTable.this, modelRows, modelCol).show(e.getComponent(), e.getX(), e.getY());
						}else{//在table的空白处显示右键菜单
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

			@Override
			public void mouseEntered(MouseEvent e){
				//displayCDNAndCertInfo(e);
			}

			@Override
			public void mouseMoved(MouseEvent evt) {
				//displayCDNAndCertInfo(evt);
			}

			//鼠标移动到证书信息时，浮动显示完整内容
			@Deprecated //效果不是很好，弃用
			public void displayCDNAndCertInfo(MouseEvent evt){
				int row = TitlePanel.getTitleTable().rowAtPoint(evt.getPoint());
				int modelRow = TitlePanel.getTitleTable().convertRowIndexToModel(row);

				int colunm = TitlePanel.getTitleTable().columnAtPoint(evt.getPoint());
				int modelColunm = TitlePanel.getTitleTable().convertColumnIndexToModel(colunm);

				int headerIndex = LineTableModel.getTitletList().indexOf("CDN|CertInfo");

				if (modelColunm == headerIndex) {
					String informations = TitlePanel.getTitleTable().getValueAt(row, colunm).toString();
					//调用的是原始Jtable中的getValueAt，有一次自动转换行列index的过程！
					//String value = LineTable.this.lineTableModel.getValueAt(modelRow,modelColunm).toString();
					//调用的是我们自己实现TableModel类中的getValueAt,没有行列index自动转换！！！
					if  (informations.length()>=15) {
						TitlePanel.getTitleTable().setToolTipText(informations);
						ToolTipManager.sharedInstance().setDismissDelay(5000);// 设置为5秒
					}
				}
			}
		});
	}
}