package config;

import javax.swing.JScrollPane;

import GUI.GUIMain;

public class ConfigJScrollPane extends JScrollPane {
	//将 JPanel 放入一个 JScrollPane 中，这样当内容过大时，自动显示滚动条，解决显示不全的问题
    public ConfigJScrollPane(GUIMain gui) {
        super(new ConfigPanel(gui)); // 推荐写法
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }
}

