package GUI;

import java.awt.GridBagConstraints;
import java.awt.Insets;

public class MyGridBagLayout extends GridBagConstraints {
	/**
	 * 采用普通的行列计数，从1开始
	 * @param row
	 * @param colum
	 */
	public MyGridBagLayout(int row,int column){
		this.fill = GridBagConstraints.BOTH;
		this.insets = new Insets(0, 0, 5, 5);
		this.gridx = column-1;
		this.gridy = row-1;
	}
}