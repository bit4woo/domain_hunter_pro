package burp;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

public class ProjectMenu{
	GUI gui;
	JMenu hunterMenu;
	
	ProjectMenu(GUI gui){
		this.gui = gui;
		hunterMenu = Menu();
	}
    
    public void Add() {
        try{
            JMenuBar menuBar = getBurpFrame().getJMenuBar();
            menuBar.add(hunterMenu, menuBar.getMenuCount() - 1);
        }catch (Exception e){

        }
	}

    public void remove(){
        JMenuBar menuBar = getBurpFrame().getJMenuBar();
        menuBar.remove(hunterMenu);
        menuBar.repaint();
    }

    static JFrame getBurpFrame()
    {
        for(Frame f : Frame.getFrames())
        {
            if(f.isVisible() && f.getTitle().startsWith(("Burp Suite")))
            {
                return (JFrame) f;
            }
        }
        return null;
    }

    public JMenu Menu() {
        JMenu menuButton = new JMenu("Domainhunter");

        JMenuItem newMenu = new JMenuItem(new AbstractAction("New")
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
//                TitlePanel.getTitleTableModel().clear();
//                gui.currentDBFile = null;
//                gui.saveDialog(false);
//                gui.getDomainPanel().showToDomainUI();
                //TODO
            }
        }
        );
        newMenu.setToolTipText("Create A New Project File");
        menuButton.add(newMenu);

        JMenuItem openMenu = new JMenuItem(new AbstractAction("Open") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                gui.openDialog();
            }
        });
        openMenu.setToolTipText("Open Domain Hunter Project File");
        menuButton.add(openMenu);


        JMenuItem saveMenu = new JMenuItem(new AbstractAction("Save as") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //gui.saveDialog(true); //TODO
            }
        });
        openMenu.setToolTipText("Save All Domains And Titles To Another File");
        menuButton.add(saveMenu);


        InputMap inputMap = saveMenu.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke sav = KeyStroke.getKeyStroke(KeyEvent.VK_S, 2); //2 --ctrl;  Ctrl+S
        inputMap.put(sav, "Save");

        saveMenu.getActionMap().put("Save", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                //saveDialog(false);
            }
        });

        return menuButton;
    }
}
