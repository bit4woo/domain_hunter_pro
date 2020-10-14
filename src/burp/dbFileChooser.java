package burp;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class dbFileChooser extends JFileChooser {

    JFileChooser fc;
    private static final Logger log=LogManager.getLogger(dbFileChooser.class);

    public dbFileChooser() {
        this.fc =  new JFileChooser();
    }

    /*
使用数据模型监听后，不需再自行单独保存当前项目了。
但是需要用于另存为，单独保存域名(和saveDomainOnly) 2个功能。
都需要文件选择对话框
 */
    public File dialog(boolean isOpen) {
        try {
            if (fc.getCurrentDirectory() != null) {
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
                if (!isOpen){
                    if(!(file.getName().toLowerCase().endsWith(".db"))){
                        file=new File(fc.getCurrentDirectory(),file.getName()+".db");
                    }
                    if(file.exists()){
                        int result = JOptionPane.showConfirmDialog(null,"Are you sure to overwrite this file ?");
                        if (result == JOptionPane.YES_OPTION) {
                            file.delete();
                            file.createNewFile();
                        }
                    }else {
                        file.createNewFile();
                    }
                }
                return file;
            }
            return null;
        }catch (Exception e){
            e.printStackTrace();
            log.error(e);
            return null;
        }
    }
    
    public File saveDialog() {
        try {
            if (fc.getCurrentDirectory() != null) {
                fc = new JFileChooser(fc.getCurrentDirectory());
            }else {
                fc = new JFileChooser();
            }

            JsonFileFilter jsonFilter = new JsonFileFilter(); //文件扩展名过滤器
            fc.addChoosableFileFilter(jsonFilter);
            fc.setFileFilter(jsonFilter);
            fc.setDialogType(JFileChooser.CUSTOM_DIALOG);

            int action = fc.showSaveDialog(null);

            if(action==JFileChooser.APPROVE_OPTION){
                File file=fc.getSelectedFile();
                fc.setCurrentDirectory(new File(file.getParent()));//save latest used dir.
                return file;
            }
            return null;
        }catch (Exception e){
            e.printStackTrace();
            log.error(e);
            return null;
        }
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
