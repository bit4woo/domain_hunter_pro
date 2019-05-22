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

    public ProjectMenu(GUI gui)
    {
        this.gui = gui;
        try{
            JMenuBar menuBar = getBurpFrame().getJMenuBar();
            JMenu hunterMenu = Menu();
            menuBar.add(hunterMenu, menuBar.getMenuCount() - 1);
        }catch (Exception e){

        }
    }

    public void remove(){
            JMenuBar menuBar = getBurpFrame().getJMenuBar();
            int num = menuBar.getMenuCount();
            for (int n=0 ; n < num; n++){
                if (menuBar.getMenu(n).getAction().equals("Domainhunter")){
                    menuBar.remove(menuBar.getMenu(n));
                }
            }
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
                DomainObject domainResult = DomainPanel.getDomainResult();
                if (domainResult != null){
                    gui.saveDialog(true);//save old project
                    int result = JOptionPane.showConfirmDialog(null,"Save Current Project?");

                    /*     是:   JOptionPane.YES_OPTION
                     *     否:   JOptionPane.NO_OPTION
                     *     取消: JOptionPane.CANCEL_OPTION
                     *     关闭: JOptionPane.CLOSED_OPTION*/
                    if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
                        return;
                    }else if (result == JOptionPane.YES_OPTION) {
                        gui.saveDialog(true);
                    }else if (result == JOptionPane.NO_OPTION) {
                        // nothing to do
                    }
                }

                domainResult = new DomainObject("");
                TitlePanel.getTitleTableModel().clear();
                gui.currentDBFile = null;
                gui.saveDialog(false);
                gui.getDomainPanel().showToDomainUI();
            }
        }
        );
        newMenu.setToolTipText("New A Project File");
        menuButton.add(newMenu);

        JMenuItem openMenu = new JMenuItem(new AbstractAction("Open") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                gui.openDialog();
            }
        });
        openMenu.setToolTipText("Open Domain Hunter Project File");
        menuButton.add(openMenu);


        JMenuItem saveMenu = new JMenuItem(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                gui.saveDialog(true);
            }
        });
        openMenu.setToolTipText("Save Domain Hunter Project File");
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
