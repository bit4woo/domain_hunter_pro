package InternetSearch;

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
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;

import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.SystemUtils;
import com.bit4woo.utilbox.utils.TextUtils;

import GUI.GUIMain;
import base.Commons;
import burp.BurpExtender;
import config.ConfigManager;
import config.ConfigName;


public class SearchTable extends JTable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	PrintWriter stdout;
	PrintWriter stderr;
	GUIMain guiMain;
	public static final List<String> HeadList = SearchTableHead.getTableHeadList();

	public SearchTable(GUIMain guiMain)
	{
		//super(lineTableModel);//这个方法创建的表没有header
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		this.guiMain = guiMain;
	}

	public SearchTable(GUIMain guiMain,SearchTableModel model)
	{
		this(guiMain);
		
		//实现点击排序
		TableRowSorter<SearchTableModel> tableRowSorter = new TableRowSorter<SearchTableModel>(model);
		setRowSorter(tableRowSorter);
		
		setModel(model);
		tableHeaderWidthinit();
		registerListeners();
	}

	public SearchTableModel getSearchTableModel(){
		return (SearchTableModel)getModel();
	}


	/**
	 * 必须在model设置过后调用才有效
	 */
	public void tableHeaderWidthinit(){
		Font f = this.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		int width = fm.stringWidth("A");//一个字符的宽度

		Map<String,Integer> preferredWidths = new HashMap<String,Integer>();
		preferredWidths.put(SearchTableHead.Index,5);
		preferredWidths.put(SearchTableHead.URL,30);
		preferredWidths.put(SearchTableHead.Host,25);
		preferredWidths.put(SearchTableHead.Source,25);
		preferredWidths.put(SearchTableHead.Title,30);
		preferredWidths.put(SearchTableHead.RootDomain,20);
		preferredWidths.put(SearchTableHead.IP,30);
		preferredWidths.put(SearchTableHead.CertInfo,30);
		preferredWidths.put(SearchTableHead.Server,20);
		preferredWidths.put(SearchTableHead.ASNInfo,30);
		preferredWidths.put(SearchTableHead.IconHash, "-17480088888".length());
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
		//this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS); //全自动调整列表，就用这个
	}

	public int[] SelectedRowsToModelRows(int[] SelectedRows) {

		for (int i = 0; i < SelectedRows.length; i++){
			SelectedRows[i] = convertRowIndexToModel(SelectedRows[i]);//转换为Model的索引，否则排序后索引不对应〿
		}
		Arrays.sort(SelectedRows);//升序
		return SelectedRows;
	}


	/**
	 * 鼠标事件
	 */
	public void registerListeners(){
		this.setRowSelectionAllowed(true);
		this.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e) {
				//双击进行google搜索、双击浏览器打开url、双击切换Check状态
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
					int[] rows = SelectedRowsToModelRows(getSelectedRows());

					//int row = ((SearchTable) e.getSource()).rowAtPoint(e.getPoint()); // 获得行位置
					int col = ((SearchTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
					int modelCol = convertColumnIndexToModel(col);

					SearchResultEntry selecteEntry = getSearchTableModel().getRowAt(rows[0]);
					if ((modelCol == HeadList.indexOf(SearchTableHead.Index) )) {//双击index在google中搜索host。
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
					}else if(modelCol==HeadList.indexOf(SearchTableHead.URL)) {//双击url在浏览器中打开
						try{
							String url = selecteEntry.getUri();
							if (StringUtils.isEmpty(url)) {
								return;
							}
							String lowerUrl = url.toLowerCase();
							if (lowerUrl.contains("://")) {
								String protocol = lowerUrl.substring(0,lowerUrl.indexOf("://"));
								List<String> protocols = TextUtils.textToLines("http\r\n"
										+ "https\r\n"
										+ "ftp\r\n"
										+ "sftp");
								if (!protocols.contains(protocol)) {
									url = url.substring(url.indexOf("://"));
									url = "http://"+url;
								}
							}else {
								url = "http://"+url;//针对DNS记录中URL字段是host的情况
							}
							SystemUtils.browserOpen(url,ConfigManager.getStringConfigByKey(ConfigName.BrowserPath));
						}catch (Exception e1){
							e1.printStackTrace(stderr);
						}
					}else if (modelCol == HeadList.indexOf(SearchTableHead.ASNInfo)) {

					}else if (modelCol == HeadList.indexOf(SearchTableHead.Favicon)) {
						try {
							SystemUtils.browserOpen(selecteEntry.getIcon_url(),ConfigManager.getStringConfigByKey(ConfigName.BrowserPath));
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					} else{
						String value = getSearchTableModel().getValueAt(rows[0],modelCol).toString();
						SystemUtils.writeToClipboard(value);
					}
				}
			}

			@Override//表格中的鼠标右键菜单
			public void mouseReleased( MouseEvent e ){//在windows中触发,因为isPopupTrigger在windows中是在鼠标释放是触发的，而在mac中，是鼠标点击时触发的。
				//https://stackoverflow.com/questions/5736872/java-popup-trigger-in-linux
				if ( SwingUtilities.isRightMouseButton( e )){
					if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {
						int[] rows = getSelectedRows();
						int col = ((SearchTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
						int modelCol = convertColumnIndexToModel(col);
						if (rows.length>0){
							int[] modelRows = SelectedRowsToModelRows(rows);
							new SearchResultEntryMenu(guiMain,SearchTable.this, modelRows, modelCol).show(e.getComponent(), e.getX(), e.getY());
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
		});
	}
}