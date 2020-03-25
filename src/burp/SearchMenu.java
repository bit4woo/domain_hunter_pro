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

		JMenuItem webpackItem = new JMenuItem(new AbstractAction("webpack") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				//app\.(.)*\.js 
				//app\.([0-9a-zA-Z])*\.js
				//以上2个是可以在burp的搜索框中验证成功
				//final String webpack_PATTERN = "app\\.([0-9a-zA-Z])*\\.js";
				String webpack_PATTERN = "(app|pc)\\.([0-9a-z])*\\.js";//后文有小写转换
				TitlePanel.getTitleTable().searchRegex(webpack_PATTERN);
			}
		});
		this.add(webpackItem);
		
		
		JMenuItem webpackItemPc = new JMenuItem(new AbstractAction("webpack-pc") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String webpack_PATTERN = "pc\\.([0-9a-z])*\\.js";//后文有小写转换
				TitlePanel.getTitleTable().searchRegex(webpack_PATTERN);
			}
		});
		this.add(webpackItemPc);
		
		
		JMenuItem webpackItemApp = new JMenuItem(new AbstractAction("webpack-app") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String webpack_PATTERN = "app\\.([0-9a-z])*\\.js";//后文有小写转换
				TitlePanel.getTitleTable().searchRegex(webpack_PATTERN);
			}
		});
		this.add(webpackItemApp);
	}
}
