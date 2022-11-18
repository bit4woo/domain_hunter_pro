package title;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import GUI.GUIMain;
import burp.BurpExtender;

public class GetTitleMenu extends JPopupMenu {


	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	JMenuItem getTitleItem;
	JMenuItem GetExtendtitleItem;
	JMenuItem GettitleOfJustNewFoundItem;
	JMenuItem CopySubnetItem;
	JMenuItem StopItem;
	JMenuItem FreshASNInfo;
	private GUIMain guiMain;


	public GetTitleMenu(GUIMain guiMain){
		this.guiMain = guiMain;

		getTitleItem = new JMenuItem(new AbstractAction("Get Title") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				//https://stackabuse.com/how-to-use-threads-in-java-swing/

				//method one: // don't need to wait threads in getAllTitle to exits
				//but hard to know the finish time of task
				//// Runs inside of the Swing UI thread
				/*			    SwingUtilities.invokeLater(new Runnable() {
			        public void run() {// don't need to wait threads in getAllTitle to exits
			        	btnGettitle.setEnabled(false);
			        	getAllTitle();
			        	btnGettitle.setEnabled(true);
			        	//domainResult.setLineEntries(TitletableModel.getLineEntries());
			        }
			    });*/
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						getTitleItem.setEnabled(false);
						guiMain.getTitlePanel().getAllTitle();
						//btnGettitle.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							getTitleItem.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		getTitleItem.setToolTipText("A fresh start for all domain name");


		GetExtendtitleItem = new JMenuItem(new AbstractAction("Get Extend Title") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						GetExtendtitleItem.setEnabled(false);
						guiMain.getTitlePanel().getExtendTitle();
						//btnGetExtendtitle.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							GetExtendtitleItem.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		GetExtendtitleItem.setToolTipText("Get title of the host that in same subnet,you should do this after get domain title done!");

		GettitleOfJustNewFoundItem = new JMenuItem(new AbstractAction("Get Title For New Found Domain") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						GettitleOfJustNewFoundItem.setEnabled(false);
						guiMain.getTitlePanel().getTitleOfNewDomain();
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							GettitleOfJustNewFoundItem.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		GettitleOfJustNewFoundItem.setToolTipText("Just get title of new found subdomains");

		CopySubnetItem = new JMenuItem(new AbstractAction("Copy Subnet") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				this.setEnabled(false);
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {

						CopySubnetItem.setEnabled(false);
						int result = JOptionPane.showConfirmDialog(null,"Just get IP Subnets of [Current] lines ?");

						int publicSubnets = JOptionPane.showConfirmDialog(null,"Just get [Pulic] IP Subnets ?");

						String subnetsString = guiMain.getTitlePanel().getSubnet(result == JOptionPane.YES_OPTION?true:false,publicSubnets == JOptionPane.YES_OPTION?true:false);

						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection selection = new StringSelection(subnetsString);
						clipboard.setContents(selection, null);
						stdout.println(subnetsString);
						CopySubnetItem.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							CopySubnetItem.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});


		StopItem = new JMenuItem(new AbstractAction("Force Stop Get Title Threads") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (guiMain.getTitlePanel().getThreadGetTitle() != null && 
						guiMain.getTitlePanel().getThreadGetTitle().isAlive() ){
					int result = JOptionPane.showConfirmDialog(null,"Are You Sure To [Force Stop] All Get Title Threads ?");
					if (result == JOptionPane.YES_OPTION){
						guiMain.getTitlePanel().getThreadGetTitle().forceStopThreads();
					}
				}
			}
		});


		FreshASNInfo = new JMenuItem(new AbstractAction("Fresh ASN Info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.
					@Override
					protected Map doInBackground() throws Exception {
						FreshASNInfo.setEnabled(false);

						try {
							//https://api.shadowserver.org/net/asn?origin=116.198.3.100/24
							//Set<String> IPSet = TitlePanel.getTitleTableModel().getPublicIPSetFromTitle();

							/**之前只使用网络接口查询，是为了最开始缓存一点基础数据，减少查询次数。
							 * 现在有了本地数据，不需要了
							Set<String> publicSubnets = TitlePanel.getTitleTableModel().getPublicSubnets();
							for (List<String> partition : Iterables.partition(publicSubnets, 200)) {
								ASNQuery.batchQueryFromApi(partition);//接口有限制，请求过快，过频繁会被封。查网段是不错的选择
							}
							 */
							guiMain.getTitlePanel().getTitleTable().getLineTableModel().freshAllASNInfo();
						} catch (Exception e) {
							e.printStackTrace();
						}
						FreshASNInfo.setEnabled(true);
						return null;
					}
				};
				worker.execute();
			}
		});

		this.add(getTitleItem);
		this.add(GetExtendtitleItem);
		this.add(GettitleOfJustNewFoundItem);
		this.add(CopySubnetItem);
		this.add(StopItem);
		this.add(FreshASNInfo);
		//this.add(doGateWayByPassCheck);
	}
}
