package burp;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;


public class dbFileChooser extends JFileChooser {

    JFileChooser fc;

    public dbFileChooser() {
        this.fc =  new JFileChooser();
    }

    public File dialog(boolean isOpen) {
        if (fc.getCurrentDirectory() != null) {
            File xxx = fc.getCurrentDirectory();
            fc = new JFileChooser(fc.getCurrentDirectory());
        }else {
            fc = new JFileChooser();
        }

        JsonFileFilter jsonFilter = new JsonFileFilter(); //文件扩展名过滤器
        fc.addChoosableFileFilter(jsonFilter);
        fc.setFileFilter(jsonFilter);
        fc.setDialogType(JFileChooser.CUSTOM_DIALOG);

        int action;
        if (isOpen) {
            action = fc.showOpenDialog(null);
        }else {
            action = fc.showSaveDialog(null);
        }

        if(action==JFileChooser.APPROVE_OPTION){
            File file=fc.getSelectedFile();
            fc.setCurrentDirectory(new File(file.getParent()));//save latest used dir.
            return file;
        }
        return null;
    }

    class JsonFileFilter extends FileFilter {
        public String getDescription() {
            return "*.db";
        }//sqlite
        public boolean accept(File file) {
            String name = file.getName();
            return file.isDirectory() || name.toLowerCase().endsWith(".db");  // 仅显示目录和json文件
        }
    }
}
