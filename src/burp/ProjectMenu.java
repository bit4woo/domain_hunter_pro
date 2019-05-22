package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

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
        JMenu menuButton = new JMenu("hunter");

        JMenuItem newMenu = new JMenuItem(new AbstractAction("New")
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                DomainObject domainResult = DomainPanel.getDomainResult();
                if (domainResult != null){
                    //saveDialog(true);//save old project
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
                //openDialog();
            }
        });
        openMenu.setToolTipText("Open Domain Hunter Project File");
        menuButton.add(openMenu);


        JMenuItem saveMenu = new JMenuItem(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //saveDialog();
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
