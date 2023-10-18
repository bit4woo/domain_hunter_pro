package title;

import java.awt.Component;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class FaviconTableCellRenderer extends DefaultTableCellRenderer {

	private int desiredWidth = 16;
    private int desiredHeight = 16;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof ImageIcon) {
            // 获取原始图标
            ImageIcon originalIcon = (ImageIcon) value;
            // 获取原始图标的图像
            Image originalImage = originalIcon.getImage();
            
            // 缩放图像以适应所需的大小
            Image resizedImage = originalImage.getScaledInstance(desiredWidth, desiredHeight, Image.SCALE_SMOOTH);
            
            // 创建新的ImageIcon
            ImageIcon resizedIcon = new ImageIcon(resizedImage);
            
            // 显示调整后的图标
            setIcon(resizedIcon);
            setText(null); // 清空文本
        } else {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
        return this;
    }
    
    
    public Component getTableCellRendererComponent111(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof ImageIcon) {
            // 获取原始图标
            ImageIcon originalIcon = (ImageIcon) value;
            setIcon(originalIcon);
            setText(null); // 清空文本
        } else {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
        return this;
    }
}
