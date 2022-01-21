package title;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import GUI.LineEntryMenuForBurp;
import GUI.RunnerGUI;
import GUI.GUI;
import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.Getter;
import burp.IBurpExtenderCallbacks;
import burp.IHttpRequestResponse;
import domain.DomainPanel;
import title.search.SearchDork;

public class GetTitleMenu extends JPopupMenu {


	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	JMenuItem getTitleItem;
	JMenuItem GetExtendtitleItem;
	JMenuItem GettitleOfJustNewFoundItem;
	JMenuItem CopySubnetItem;
	private JMenuItem StopItem;

	GetTitleMenu(){
		
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
						GUI.getTitlePanel().getAllTitle();
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
						GUI.getTitlePanel().getExtendTitle();
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
						GUI.getTitlePanel().getTitleOfNewDomain();
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

						String subnetsString = GUI.getTitlePanel().getSubnet(result == JOptionPane.YES_OPTION?true:false,publicSubnets == JOptionPane.YES_OPTION?true:false);

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
		
		
		StopItem = new JMenuItem(new AbstractAction("Stop Threads") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to [Force Stop] all theads ?");
				if (TitlePanel.threadGetTitle != null && result == JOptionPane.YES_OPTION){
					TitlePanel.threadGetTitle.forceStopThreads();
				}
			}
		});
		
		JMenuItem doGateWayByPassCheck = new JMenuItem(new AbstractAction("Do GateWay ByPass Check For All") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.
					@Override
					protected Map doInBackground() throws Exception {

						RunnerGUI runnergui = new RunnerGUI();
						runnergui.begainGatewayBypassCheck();
						runnergui.setVisible(true);
						return null;
					}
					@Override
					protected void done() {
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
		//this.add(doGateWayByPassCheck);
	}
}
