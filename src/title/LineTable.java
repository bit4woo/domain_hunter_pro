package title;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.table.TableRowSorter;

import GUI.GUIMain;
import base.Commons;
import burp.BurpExtender;
import com.bit4woo.utilbox.utils.SystemUtils;
import config.ConfigManager;
import config.ConfigName;
import org.apache.commons.lang3.StringUtils;
import title.search.History;
import title.search.SearchManager;
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
	public static final List<String> HeadList = LineTableHead.getTableHeadList();


	public LineEntry getRowAt(int row) {
		return ((LineTableModel) getModel()).getRowAt(convertRowIndexToModel(row));
	}

	//将选中的行（图形界面的行）转换为Model中的行数（数据队列中的index）.因为图形界面排序等操作会导致图像和数据队列的index不是线性对应的。
	public int[] SelectedRowsToModelRows(int[] SelectedRows) {

		for (int i = 0; i < SelectedRows.length; i++){
			SelectedRows[i] = convertRowIndexToModel(SelectedRows[i]);//转换为Model的索引，否则排序后索引不对应〿
		}
		Arrays.sort(SelectedRows);//升序

		return SelectedRows;
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
		this.guiMain = guiMain;
		registerListeners();
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);//配合横向滚动条
	}

	@Override
	public void changeSelection(int row, int col, boolean toggle, boolean extend)
	{
		// show the log entry for the selected row
		//LineEntry Entry = this.lineTableModel.getLineEntries().get(super.convertRowIndexToModel(row));
		LineEntry Entry = this.getRowAt(row);
		((LineTableModel) getModel()).setCurrentlyDisplayedItem(Entry);
		guiMain.getTitlePanel().getRequestViewer().setMessage(Entry.getRequest(), true);
		guiMain.getTitlePanel().getResponseViewer().setMessage(Entry.getResponse(), false);

		super.changeSelection(row, col, toggle, extend);
	}


	public LineTableModel getLineTableModel(){
		return (LineTableModel)getModel();
	}


	/**
	 * 必须在model设置过后调用才有效
	 */
	public void tableHeaderWidthinit(){
		//Font f = new Font("Arial", Font.PLAIN, 12);
		Font f = this.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		int width = fm.stringWidth("A");//一个字符的宽度


		Map<String,Integer> preferredWidths = new HashMap<String,Integer>();
		preferredWidths.put(LineTableHead.Index,5);
		preferredWidths.put(LineTableHead.URL,25);
		preferredWidths.put(LineTableHead.Status,6);
		preferredWidths.put(LineTableHead.Length,10);
		preferredWidths.put(LineTableHead.Title,30);
		preferredWidths.put(LineTableHead.Comments,30);
		preferredWidths.put(LineTableHead.CheckDoneTime,"2019-05-28-14-13-16".length());
		preferredWidths.put(LineTableHead.isChecked," isChecked ".length());
		preferredWidths.put(LineTableHead.IP,30);
		preferredWidths.put(LineTableHead.CNAMEAndCertInfo,30);
		preferredWidths.put(LineTableHead.Server,10);
		preferredWidths.put(LineTableHead.IconHash, "-17480088888".length());
		preferredWidths.put(LineTableHead.ASNInfo,"HUAWEI CLOUD SERVICE DATA CENTER".length());
		for(String header:HeadList){
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

		int index = HeadList.indexOf(LineTableHead.IconHash);
		getColumnModel().getColumn(index).setCellRenderer(new FaviconTableCellRenderer()); // 第二列显示图片,必须在setModel之后
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
				LineEntry line = getLineTableModel().getRowAt(row);

				return new SearchManager(guiMain.getTitlePanel()).include(line,Input,caseSensitive);
			}

		};
		((TableRowSorter)LineTable.this.getRowSorter()).setRowFilter(filter);
		//(LineTable.this.getRowSorter()).modelStructureChanged();这会导致排序结果被重置，恢复排序前的状态
	}

	/**
	 * 鼠标事件
	 */
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

					LineEntry selecteEntry = getLineTableModel().getRowAt(rows[0]);
					if ((modelCol == HeadList.indexOf(LineTableHead.Index) )) {//双击index在google中搜索host。
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
					}else if(modelCol==HeadList.indexOf(LineTableHead.URL)) {//双击url在浏览器中打开
						try{
							String url = selecteEntry.getUrl();
							if (url != null && !url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
								url = "http://"+url;//针对DNS记录中URL字段是host的情况
							}
							SystemUtils.browserOpen(url,ConfigManager.getStringConfigByKey(ConfigName.BrowserPath));
						}catch (Exception e1){
							e1.printStackTrace(stderr);
						}
					}else if (modelCol == HeadList.indexOf(LineTableHead.isChecked)) {
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
					}else if (modelCol == HeadList.indexOf(LineTableHead.AssetType)) {
						String currentLevel = selecteEntry.getAssetType();
						List<String> tmpList = Arrays.asList(LineEntry.AssetTypeArray);
						int index = tmpList.indexOf(currentLevel);
						String newLevel = tmpList.get((index+1)%3);
						selecteEntry.setAssetType(newLevel);
						stdout.println(String.format("$$$ %s updated [AssetType-->%s]",selecteEntry.getUrl(),newLevel));
						getLineTableModel().fireTableRowsUpdated(rows[0], rows[0]);
					}else if (modelCol == HeadList.indexOf(LineTableHead.ASNInfo)) {
						if (StringUtils.isEmpty(selecteEntry.getASNInfo())){
							selecteEntry.freshASNInfo();
						}else {
							SystemUtils.writeToClipboard(selecteEntry.getASNInfo());
						}
					}else if (modelCol == HeadList.indexOf(LineTableHead.Favicon)) {
						try {
							SystemUtils.browserOpen(selecteEntry.getIcon_url(),ConfigManager.getStringConfigByKey(ConfigName.BrowserPath));
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					} else{
						//HeadList.indexOf("CDN|CertInfo")
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

				int headerIndex = HeadList.indexOf(LineTableHead.CNAMEAndCertInfo);

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