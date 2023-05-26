package title.search;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import title.LineEntry;

public class SearchMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;

	private SearchTextField searchField;

	public SearchMenu(SearchTextField searchField){
		this.searchField = searchField;

		JMenuItem webpackItemPc = new JMenuItem(new AbstractAction("webpack-pc:   pc\\.([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String webpack_PATTERN = "pc\\.([0-9a-z])*\\.js";//后文有小写转换
				searchField.setText(SearchRegex.REGEX+":"+webpack_PATTERN);
				//TitlePanel.getTitleTable().search(searchField.getText());
			}
		});

		JMenuItem webpackItemApp = new JMenuItem(new AbstractAction("webpack-app:   app\\.([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String webpack_PATTERN = "app\\.([0-9a-z])*\\.js";//后文有小写转换
				//searchField.setText("regex:app\\.([0-9a-z])*\\.js");
				//stdout.println("regex:app\\.([0-9a-z])*\\.js");
				//stdout.println(SearchDork.REGEX.toString()+":"+webpack_PATTERN);
				searchField.setText(SearchRegex.REGEX+":"+webpack_PATTERN);
				//TitlePanel.getTitleTable().search(searchField.getText());
			}
		});

		JMenuItem webpackItemIndex = new JMenuItem(new AbstractAction("webpack-index:   index\\.([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String webpack_PATTERN = "index\\.([0-9a-z])*\\.js";//后文有小写转换
				searchField.setText(SearchRegex.REGEX+":"+webpack_PATTERN);
				//TitlePanel.getTitleTable().search(searchField.getText());
			}
		});

		JMenuItem webpackItemNoScript = new JMenuItem(new AbstractAction("webpack-noscript:   <noscript>(.*?)</noscript>") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				//</noscript> doesn't work properly without JavaScript </noscript>
				String webpack_PATTERN = "<noscript>(.*?)</noscript>";//后文有小写转换
				searchField.setText(SearchRegex.REGEX+":"+webpack_PATTERN);
				//TitlePanel.getTitleTable().search(searchField.getText());
			}
		});

		JMenuItem findAdminByTableTag = new JMenuItem(new AbstractAction("Table:   <table(.*?)</table>") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				//<title(.*?)</title>
				String webpack_PATTERN = "<table(.*?)</table>";
				searchField.setText(SearchRegex.REGEX+":"+webpack_PATTERN);
				//TitlePanel.getTitleTable().search(searchField.getText());
			}
		});


		JMenuItem webpackItemAll = new JMenuItem(new AbstractAction("All hash JS:   ([a-z])*\\.([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				//根据webpack知识，文件名是可以变的，但是通常会在文件名中加入hash字段（文件名.hash.js），所以用这个正则 
				String webpack_PATTERN = "([a-z])*\\.([0-9a-z])*\\.js";//后文有小写转换
				searchField.setText(SearchRegex.REGEX+":"+webpack_PATTERN);
				//TitlePanel.getTitleTable().search(searchField.getText());
			}
		});

		JMenuItem AllJS = new JMenuItem(new AbstractAction("All JS:   ([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				//根据webpack知识，文件名是可以变的，但是通常会在文件名中加入hash字段（文件名.hash.js），所以用这个正则 
				String webpack_PATTERN = "([0-9a-z])*\\.js";//后文有小写转换
				searchField.setText(SearchRegex.REGEX+":"+webpack_PATTERN);
				//TitlePanel.getTitleTable().search(searchField.getText());
			}
		});


		JMenuItem ItemsWithResponse = new JMenuItem(new AbstractAction("Items With Response:   status>=1") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				searchField.setText(SearchNumbericDork.STATUS+">= 1");
			}
		});


		JMenuItem UrlRedirection = new JMenuItem(new AbstractAction("URL Redirection:   status>=300 && status<400") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String expresstion = SearchNumbericDork.STATUS+">=300 && "+SearchNumbericDork.STATUS+"<400";
				searchField.setText(expresstion);
			}
		});


		JMenuItem ManualSaved = new JMenuItem(new AbstractAction("Manual Saved Items:   source:saved") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String expresstion = SearchStringDork.SOURCE+":"+LineEntry.Source_Manual_Saved;
				searchField.setText(expresstion);
			}
		});
		
		
		JMenuItem DNSRecord = new JMenuItem(new AbstractAction("DNS Records Items:   SOURCE:certain && status <=0") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String expresstion = SearchStringDork.SOURCE+":"+LineEntry.Source_Certain +"&& status <=0";
				searchField.setText(expresstion);
			}
		});

		JMenuItem caseSensitive = new JMenuItem(new AbstractAction("Case Sensitive") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (actionEvent.getActionCommand().equals("Disable Case Sensitive")) {
					((SearchTextField)searchField).setCaseSensitive(false);
				}else {
					((SearchTextField)searchField).setCaseSensitive(true);
				}
			}
		});

		SearchTextField searchTextField = ((SearchTextField)searchField);
		if (searchTextField.isCaseSensitive()) {
			caseSensitive.setText("Disable Case Sensitive");
		}else {
			caseSensitive.setText("Enable Case Sensitive");
		}


		this.add(webpackItemPc);
		this.add(webpackItemApp);
		this.add(webpackItemIndex);
		this.add(webpackItemNoScript);
		this.add(findAdminByTableTag);
		this.addSeparator();//分割线
		this.add(webpackItemAll);
		this.add(AllJS);
		this.addSeparator();//分割线
		this.add(ItemsWithResponse);
		this.add(UrlRedirection);
		this.add(ManualSaved);
		this.add(DNSRecord);
		this.addSeparator();//分割线
		this.add(caseSensitive);
	}
}
