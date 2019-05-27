package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
            public void actionPerformed(ActionEvent actionEvent) {//实质就是save一个空的项目
                File file = gui.dbfc.dialog(false);//通过保存对话指定文件，这会是一个空文件。
                DomainPanel.setDomainResult(new DomainObject(file.getName()));
                gui.saveData(file.toString(),true);
                gui.LoadData(file.toString());//然后加载，就是一个新的空项目了。
                GUI.setCurrentDBFile(file);
            }
        }
        );
        newMenu.setToolTipText("Create A New Project File");
        menuButton.add(newMenu);

        JMenuItem openMenu = new JMenuItem(new AbstractAction("Open") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                File file = gui.dbfc.dialog(true);
                gui.LoadData(file.toString());
                GUI.setCurrentDBFile(file);
            }
        });
        openMenu.setToolTipText("Open Domain Hunter Project File");
        menuButton.add(openMenu);


        JMenuItem saveMenu = new JMenuItem(new AbstractAction("Save as") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    @Override
                    protected Map doInBackground() throws Exception {
                        File file = gui.dbfc.dialog(false);
                        gui.saveData(file.toString(),false);
                        return new HashMap<String, String>();
                        //no use ,the return.
                    }
                    @Override
                    protected void done() {
                    }
                };
                worker.execute();
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
