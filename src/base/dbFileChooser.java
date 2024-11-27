package base;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import burp.BurpExtender;


public class dbFileChooser extends JFileChooser {

	JFileChooser fc;
	private static final Logger log=LogManager.getLogger(dbFileChooser.class);

	public dbFileChooser() {
		this.fc =  new JFileChooser();
	}

	/**
	使用数据模型监听后，不需再自行单独保存当前项目了。
	但是需要用于另存为，单独保存域名(和saveDomainOnly) 2个功能。
	都需要文件选择对话框
	 */
	public File dialog(boolean isOpen,String fileExtension) {
		try {
			File curr_db = BurpExtender.getDataLoadManager().getCurrentDBFile();
			if (curr_db != null && curr_db.exists()) {
				fc = new JFileChooser(curr_db.getParentFile());
			}else if (fc.getCurrentDirectory() != null) {
				fc = new JFileChooser(fc.getCurrentDirectory());
			}else{
				fc = new JFileChooser();
			}

			myFileFilter filter = new myFileFilter(fileExtension); //文件扩展名过滤器
			fc.addChoosableFileFilter(filter);
			fc.setFileFilter(filter);
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
					if(!(file.getName().toLowerCase().endsWith(fileExtension))){
						file=new File(fc.getCurrentDirectory(),file.getName()+fileExtension);
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

			myFileFilter dbFilter = new myFileFilter(".db"); //文件扩展名过滤器
			fc.addChoosableFileFilter(dbFilter);
			fc.setFileFilter(dbFilter);
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

	class myFileFilter extends FileFilter {
		private String fileExtension;
		myFileFilter(String fileExtension){
			if (!fileExtension.startsWith(".")) {
				fileExtension = "."+fileExtension;
			}
			this.fileExtension = fileExtension;
		}
		public String getDescription() {
			return fileExtension;
		}
		public boolean accept(File file) {
			String name = file.getName();
			return file.isDirectory() || name.toLowerCase().endsWith(fileExtension.toLowerCase());  // 仅显示目录和xxx文件
		}
	}
}
