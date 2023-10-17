import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import title.FaviconTableCellRenderer;

import com.aspose.imaging.Image;
import com.aspose.imaging.ImageOptionsBase;
import com.aspose.imaging.fileformats.jpeg2000.Jpeg2000Codec;
import com.aspose.imaging.fileformats.png.PngColorType;
import com.aspose.imaging.fileformats.tiff.enums.TiffExpectedFormat;
import com.aspose.imaging.imageoptions.*;


public class DisplayIcon {
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
                    //ImageIcon icon = new ImageIcon("C:/Users/dell/Downloads/favicon_v3.ico"); // 修正文件路径
                    
                    byte[] imageData = imageToByteArray("G:/111.ico");
                    
                    imageData = convertIcoToPng(imageData);
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
								model.setValueAt(new ImageIcon(imageToByteArray("G:/111.png")), 0, 1);
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

   

    		
    private static byte[] convertIcoToPng(byte[] icoBytes) throws IOException, ImageReadException, ImageWriteException {
        // Use Imaging library to read and write the image
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        Imaging.writeImage(Imaging.getBufferedImage(icoBytes), pngOutputStream, ImageFormats.PNG);

        return pngOutputStream.toByteArray();
    }
    private static byte[] convertIcoToPng1(byte[] icoBytes) throws IOException, ImageReadException, ImageWriteException {
    	List<BufferedImage> image = ICODecoder.read(new File("input.ico"));
    }
    
}
