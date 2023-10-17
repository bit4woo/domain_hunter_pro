import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;


/**
 *
 *
 */
public class DisplayIcon3 extends JFrame{
     
    public DisplayIcon3() {
        
             setSize(800,300);
             int x=(getScreenWidth()-getWidth())/2;
             int y=(getScreenHeight()-getHeight())/2;
             setLocation(x, y);
             setTitle("表格显示image");
             setResizable(false); 
             
             
             String[] tableHeads = new String[]{"图片显示"};
             DefaultTableModel tableModel=new DefaultTableModel();
             tableModel.setColumnIdentifiers(tableHeads);
             
             
             
             
             JTable  table = new JTable(tableModel);   
             table.setRowHeight(200);
             table.getTableHeader().setPreferredSize(new Dimension(50, 30));
             
             
             JScrollPane scrollPane=new JScrollPane(table);
             
              
             ImgTableRender imgRender=new ImgTableRender();
             TableColumn  col1=table.getColumn("图片显示");
             col1.setPreferredWidth(100);
             col1.setCellRenderer(imgRender);
             
             
             
             Object[] objects=new Object[] {"D:/1.jpg"};
             tableModel.addRow(objects);
             
             JPanel jPanel=new JPanel();
             jPanel.setPreferredSize(new Dimension(800,300));
             jPanel.add(scrollPane);
         
             getContentPane().add(jPanel);
             setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
             setVisible(true);
        
        
    }
    
    public static void main(String[] args) {
        
        new DisplayIcon3();
    }
    private static int getScreenWidth() {
        return (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    }

    private static int getScreenHeight() {
        return (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    }
    
    class ImgTableRender  implements  TableCellRenderer{
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {             
            System.out.println(value);
    
            JLabel lable=new JLabel();
            JLabel rLabel=new JLabel();
            try {
                BufferedImage bufImg=ImageIO.read(new File("C:/1.png"));
                int type=bufImg.getType()==0?BufferedImage.TYPE_INT_ARGB :bufImg.getType();
                BufferedImage resizeImg=resizeImage(bufImg, 16, 16, type);
                ImageIcon icon=new ImageIcon(resizeImg);
                lable.setPreferredSize(new Dimension(200, 200));
                lable.setIcon(icon); 
                
                BufferedImage buImage1=ImageIO.read(new File("C:/1.png"));
                type=buImage1.getType()==0?BufferedImage.TYPE_INT_ARGB :buImage1.getType();
                
                BufferedImage resizeImg1=resizeImage(buImage1, 16, 16, type);
                ImageIcon icon1=new ImageIcon(resizeImg1);
                rLabel.setPreferredSize(new Dimension(200, 200));
                rLabel.setIcon(icon1); 
                                
            
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            JPanel panel=new JPanel();            
            panel.setPreferredSize(new Dimension(200, 200));
            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            panel.add(lable);
             panel.add(rLabel);
            return panel;
        }
        
        private  BufferedImage resizeImage(BufferedImage originalImage,Integer width,Integer heigth, int type){
            BufferedImage resizedImage = new BufferedImage(width,heigth,type);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0,width,heigth, null);
            g.dispose();

            return resizedImage;
            }        
        
    }
           
    
}

