package burp;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class SearchMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;
	SearchMenu(){
//
//        try{
//            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
//            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
//        }catch (Exception e){
//            stdout = new PrintWriter(System.out, true);
//            stderr = new PrintWriter(System.out, true);
//        }

//		JMenuItem webpackItem = new JMenuItem(new AbstractAction("webpack  (app|pc)\\.([0-9a-z])*\\.js") {
//			@Override
//			public void actionPerformed(ActionEvent actionEvent) {
//				//app\.(.)*\.js 
//				//app\.([0-9a-zA-Z])*\.js
//				//以上2个是可以在burp的搜索框中验证成功
//				//final String webpack_PATTERN = "app\\.([0-9a-zA-Z])*\\.js";
//				String webpack_PATTERN = "(app|pc)\\.([0-9a-z])*\\.js";//后文有小写转换
//				TitlePanel.getTitleTable().searchRegex(webpack_PATTERN);
//			}
//		});
//		this.add(webpackItem);
		
		
		JMenuItem webpackItemPc = new JMenuItem(new AbstractAction("webpack-pc:   pc\\.([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String webpack_PATTERN = "pc\\.([0-9a-z])*\\.js";//后文有小写转换
				TitlePanel.getTitleTable().searchRegex(webpack_PATTERN);
			}
		});
		this.add(webpackItemPc);
		
		
		JMenuItem webpackItemApp = new JMenuItem(new AbstractAction("webpack-app:   app\\.([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String webpack_PATTERN = "app\\.([0-9a-z])*\\.js";//后文有小写转换
				TitlePanel.getTitleTable().searchRegex(webpack_PATTERN);
			}
		});
		this.add(webpackItemApp);
		
		JMenuItem webpackItemIndex = new JMenuItem(new AbstractAction("webpack-index:   index\\.([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String webpack_PATTERN = "index\\.([0-9a-z])*\\.js";//后文有小写转换
				TitlePanel.getTitleTable().searchRegex(webpack_PATTERN);
			}
		});
		this.add(webpackItemIndex);
		
		this.addSeparator();//分割线
		
		JMenuItem webpackItemAll = new JMenuItem(new AbstractAction("All hash JS:   ([a-z])*\\.([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				//根据webpack知识，文件名是可以变的，但是通常会在文件名中加入hash字段（文件名.hash.js），所以用这个正则 
				String webpack_PATTERN = "([a-z])*\\.([0-9a-z])*\\.js";//后文有小写转换
				TitlePanel.getTitleTable().searchRegex(webpack_PATTERN);
			}
		});
		this.add(webpackItemAll);
		
		JMenuItem AllJS = new JMenuItem(new AbstractAction("All JS:   ([0-9a-z])*\\.js") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				//根据webpack知识，文件名是可以变的，但是通常会在文件名中加入hash字段（文件名.hash.js），所以用这个正则 
				String webpack_PATTERN = "([0-9a-z])*\\.js";//后文有小写转换
				TitlePanel.getTitleTable().searchRegex(webpack_PATTERN);
			}
		});
		this.add(AllJS);
	}
}
