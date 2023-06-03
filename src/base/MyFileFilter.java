package base;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class MyFileFilter extends FileFilter {

	private String extension;

	public MyFileFilter(String extension){
		if (!extension.startsWith(".")) {
			extension = "."+extension;
		}
		this.extension = extension;
	}

	@Override
	public String getDescription() {  
		return "*"+extension;  
	}

	@Override
	public boolean accept(File file) {
		String name = file.getName();
		return file.isDirectory() || name.toLowerCase().endsWith(extension);  // 仅显示目录和json文件
	}
}