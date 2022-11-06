package title;

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
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.Commons;
import burp.IMessageEditor;
import burp.SystemUtils;
import config.ConfigPanel;
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
	//private TableRowSorter<LineTableModel> tableRowSorter;//TableRowSorter vs. RowSorter


	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain guiMain;

	@Override//参考javax.swing.JTable中的函数，每次都有主动进行转换
	public Object getValueAt(int row, int column) {
		return getLineTableModel().getValueAt(convertRowIndexToModel(row),
				convertColumnIndexToModel(column));
	}

	public LineEntry getRowAt(int row) {
		return getLineTableModel().getLineEntries().get(convertRowIndexToModel(row));
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

	public LineTable(GUIMain guiMain)
	{
		//super(lineTableModel);//这个方法创建的表没有header
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		this.setFillsViewportHeight(true);//在table的空白区域显示右键菜单
		//https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
		//this.setLineTableModel(lineTableModel);
		this.guiMain = guiMain;

		//FitTableColumns(this);
		//this.setAutoCreateRowSorter(true);

		registerListeners();

		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);//配合横向滚动条
	}

	@Override
	public void changeSelection(int row, int col, boolean toggle, boolean extend)
	{
		// show the log entry for the selected row
		//LineEntry Entry = this.lineTableModel.getLineEntries().get(super.convertRowIndexToModel(row));
		LineEntry Entry = this.getRowAt(row);

		guiMain.getTitlePanel().getRequestViewer().setMessage(Entry.getRequest(), true);
		guiMain.getTitlePanel().getResponseViewer().setMessage(Entry.getResponse(), false);
		getLineTableModel().setCurrentlyDisplayedItem(Entry);
		super.changeSelection(row, col, toggle, extend);
	}

	public LineTableModel getLineTableModel(){
		return (LineTableModel)super.getModel();
	}

	public void setLineTableModel(LineTableModel lineTableModel) {
		//this.lineTableModel = lineTableModel;//自己保存一份，避免调用getModel后进行类型转换失败。
		super.setModel(lineTableModel);
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
		preferredWidths.put("CNAME|CertInfo",30);
		preferredWidths.put("Server",10);
		preferredWidths.put("IconHash", "-17480088888".length());
		preferredWidths.put("ASNInfo","HUAWEI CLOUD SERVICE DATA CENTER".length());
		for(String header:LineTableModel.getTitletList()){
			try{//避免动态删除表字段时，出错
				if (preferredWidths.keySet().contains(header)){
					int multiNumber = preferredWidths.get(header);
					this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex(header)).setPreferredWidth(width*multiNumber);
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}
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

	/**
	 * 搜索功能，自动获取caseSensitive的值
	 * @param keyword
	 */
	public void search(String keyword) {
		SearchTextField searchTextField = (SearchTextField)guiMain.getTitlePanel().getTextFieldSearch();
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
				LineEntry line = getLineTableModel().getLineEntries().get(row);

				//第一层判断，根据按钮状态进行判断，如果为true，进行后面的逻辑判断，false直接返回。
				if (!new LineSearch(guiMain.getTitlePanel()).entryNeedToShow(line)) {
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
				Input = Input.trim();//应该去除空格，符合java代码编写习惯
				if (SearchDork.isDork(Input)) {
					//stdout.println("do dork search,dork:"+dork+"   keyword:"+keyword);
					return LineSearch.dorkFilter(line,Input,caseSensitive);
				}else {
					return LineSearch.textFilter(line,Input,caseSensitive);
				}
			}
		};
		((TableRowSorter)LineTable.this.getRowSorter()).setRowFilter(filter);
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

					LineEntry selecteEntry = getLineTableModel().getLineEntries().get(rows[0]);
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
							Commons.browserOpen(url,guiMain.getConfigPanel().getLineConfig().getBrowserPath());
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
							getLineTableModel().fireTableRowsUpdated(rows[0], rows[0]);
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
						getLineTableModel().fireTableRowsUpdated(rows[0], rows[0]);
					}else if (modelCol == LineTableModel.getTitletList().indexOf("ASNInfo")) {
						if (selecteEntry.getASNInfo().equals("")){
							selecteEntry.freshASNInfo();
						}else {
							SystemUtils.writeToClipboard(selecteEntry.getASNInfo());
						}
					} else{//LineTableModel.getTitletList().indexOf("CDN|CertInfo")
						//String value = guiMain.getTitlePanel().getTitleTable().getValueAt(rows[0], col).toString();//rows[0]是转换过的，不能再转换
						//调用的是原始Jtable中的getValueAt，它本质上也是调用model中的getValueAt，但是有一次转换的过程！！！
						String value = getLineTableModel().getValueAt(rows[0],modelCol).toString();
						//调用的是我们自己实现的TableModel类中的getValueAt,相比Jtable类中的同名方法，就少了一次转换的过程！！！
						//String CDNAndCertInfo = selecteEntry.getCDN();
						SystemUtils.writeToClipboard(value);
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
							new LineEntryMenu(guiMain, modelRows, modelCol).show(e.getComponent(), e.getX(), e.getY());
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
				int row = guiMain.getTitlePanel().getTitleTable().rowAtPoint(evt.getPoint());
				int modelRow = guiMain.getTitlePanel().getTitleTable().convertRowIndexToModel(row);

				int colunm = guiMain.getTitlePanel().getTitleTable().columnAtPoint(evt.getPoint());
				int modelColunm = guiMain.getTitlePanel().getTitleTable().convertColumnIndexToModel(colunm);

				int headerIndex = LineTableModel.getTitletList().indexOf("CDN|CertInfo");

				if (modelColunm == headerIndex) {
					String informations = guiMain.getTitlePanel().getTitleTable().getValueAt(row, colunm).toString();
					//调用的是原始Jtable中的getValueAt，有一次自动转换行列index的过程！
					//String value = LineTable.this.lineTableModel.getValueAt(modelRow,modelColunm).toString();
					//调用的是我们自己实现TableModel类中的getValueAt,没有行列index自动转换！！！
					if  (informations.length()>=15) {
						guiMain.getTitlePanel().getTitleTable().setToolTipText(informations);
						ToolTipManager.sharedInstance().setDismissDelay(5000);// 设置为5秒
					}
				}
			}
		});
	}
}