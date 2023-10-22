import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import net.sf.image4j.codec.ico.ICODecoder;
import title.FaviconTableCellRenderer;

public class displayIcon {
    public static void main(String[] args)throws IOException {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    JFrame frame = new JFrame();
                    DefaultTableModel model = new DefaultTableModel();
                    model.addColumn("111");
                    model.addColumn("222");

                    String[] data = {"aa", "bb"};
                    model.addRow(data);
                    JTable table = new JTable(model);
                    table.getColumnModel().getColumn(1).setCellRenderer(new FaviconTableCellRenderer());

                    // 设置第二列的单元格值为ImageIcon
                    byte[] imageData = imageToByteArray("C:\\Users\\xxx\\Downloads\\tab_logo.png");
                    
                    imageData = convertIcoToPng1(imageData);
//                    Image[] images = readAllIconsFromICO(imageData);
                    ImageIcon icon = new ImageIcon(imageData); // 修正文件路径
                    model.setValueAt(icon, 0, 1);

                    table.setFillsViewportHeight(true);
                    JScrollPane scrollPane = new JScrollPane(table);

                    frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

                    JButton button = new JButton("Change Data");
                    button.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            // 修改数据示例
                            try {
								model.setValueAt(new ImageIcon(imageToByteArray("G:/222.png")), 0, 1);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} // 修正文件路径
                        }
                    });

                    frame.getContentPane().add(button, BorderLayout.NORTH);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setSize(400, 200);
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    
    public static byte[] imageToByteArray(String imagePath) throws IOException {
        File file = new File(imagePath);
        byte[] imageData = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(imageData);
        }

        return imageData;
    }


    public static byte[] convertIcoToPng1(byte[] icoBytes) throws Exception {
        try {
			List<BufferedImage> images = ICODecoder.read(new ByteArrayInputStream(icoBytes));

			if (images.size() > 0) {
			    BufferedImage bi = images.get(0);
			    ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
			    ImageIO.write(bi, "PNG", pngOutputStream);
			    return pngOutputStream.toByteArray();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        return icoBytes;
    }
}
