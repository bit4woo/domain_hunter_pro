package test;

//-*- mode:java; encoding:utf-8 -*-
//vim:set fileencoding=utf-8:
//https://ateraimemo.com/Swing/PointInsidePrefSize.html

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public final class linkincell extends JPanel {
private final String[] columnNames = {"No.", "Name", "URL"};
private final DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
 @Override public Class<?> getColumnClass(int column) {
   switch (column) {
     case 0: return Integer.class;
     case 1: return String.class;
     case 2: return URL.class;
     default: return super.getColumnClass(column);
   }
 }

 @Override public boolean isCellEditable(int row, int col) {
   return false;
 }
};

private linkincell() {
 super(new BorderLayout());
 try {
   model.addRow(new Object[] {0, "FrontPage", new URL("https://ateraimemo.com/")});
   model.addRow(new Object[] {1, "Java Swing Tips", new URL("https://ateraimemo.com/Swing.html")});
   model.addRow(new Object[] {2, "Example", new URL("http://www.example.com/")});
   model.addRow(new Object[] {3, "Example.jp", new URL("http://www.example.jp/")});
 } catch (MalformedURLException ex) {
   ex.printStackTrace();
 }

 JTable table = new JTable(model) {
   private final Color evenColor = new Color(250, 250, 250);
   @Override public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
     Component c = super.prepareRenderer(tcr, row, column);
     c.setForeground(getForeground());
     c.setBackground(row % 2 == 0 ? evenColor : getBackground());
     return c;
   }
 };
 table.setRowSelectionAllowed(true);
 table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
 table.setIntercellSpacing(new Dimension());
 table.setShowGrid(false);
 table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
 table.setAutoCreateRowSorter(true);

 TableColumn col = table.getColumnModel().getColumn(0);
 col.setMinWidth(50);
 col.setMaxWidth(50);
 col.setResizable(false);

 UrlRenderer renderer = new UrlRenderer();
 table.setDefaultRenderer(URL.class, renderer);
 table.addMouseListener(renderer);
 table.addMouseMotionListener(renderer);

 col = table.getColumnModel().getColumn(1);
 col.setPreferredWidth(1000);

 col = table.getColumnModel().getColumn(2);
 // col.setCellRenderer(renderer);
 col.setPreferredWidth(2000);

 JScrollPane scrollPane = new JScrollPane(table);
 scrollPane.getViewport().setBackground(Color.WHITE);
 add(scrollPane);
 setPreferredSize(new Dimension(320, 240));
}

public static void main(String... args) {
 EventQueue.invokeLater(new Runnable() {
   @Override public void run() {
     createAndShowGui();
   }
 });
}

public static void createAndShowGui() {
 try {
   UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
 } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
   ex.printStackTrace();
 }
 JFrame frame = new JFrame("PointInsidePrefSize");
 frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
 frame.getContentPane().add(new linkincell());
 frame.pack();
 frame.setLocationRelativeTo(null);
 frame.setVisible(true);
}
}

class UrlRenderer extends DefaultTableCellRenderer implements MouseListener, MouseMotionListener {
private static Rectangle lrect = new Rectangle();
private static Rectangle irect = new Rectangle();
private static Rectangle trect = new Rectangle();
private int vrow = -1; // viewRowIndex
private int vcol = -1; // viewColumnIndex
private boolean isRollover;

@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
 super.getTableCellRendererComponent(table, value, isSelected, false, row, column);

 // TEST: this.setBorder(BorderFactory.createMatteBorder(0, 16, 0, 16, Color.RED));
 Insets i = this.getInsets();
 lrect.x = i.left;
 lrect.y = i.top;
 lrect.width = table.getColumnModel().getColumn(column).getWidth() - table.getColumnModel().getColumnMargin() - i.right - lrect.x;
 lrect.height = table.getRowHeight(row) - table.getRowMargin() - i.bottom - lrect.y;
 irect.setBounds(0, 0, 0, 0); // .x = irect.y = irect.width = irect.height = 0;
 trect.setBounds(0, 0, 0, 0); // .x = trect.y = trect.width = trect.height = 0;

 String str = SwingUtilities.layoutCompoundLabel(
     this,
     this.getFontMetrics(this.getFont()),
     Objects.toString(value, ""), // this.getText(),
     this.getIcon(),
     this.getVerticalAlignment(),
     this.getHorizontalAlignment(),
     this.getVerticalTextPosition(),
     this.getHorizontalTextPosition(),
     lrect,
     irect, // icon
     trect, // text
     this.getIconTextGap());

 if (isRolloverCell(table, row, column)) {
   setText("<html><u><font color='blue'>" + str);
 } else {
   setText(str);
 }
 return this;
}

protected boolean isRolloverCell(JTable table, int row, int column) {
 return !table.isEditing() && this.vrow == row && this.vcol == column && this.isRollover;
}

// @see SwingUtilities2.pointOutsidePrefSize(...)
private static boolean pointInsidePrefSize(JTable table, Point p) {
 int row = table.rowAtPoint(p);
 int col = table.columnAtPoint(p);
 TableCellRenderer tcr = table.getCellRenderer(row, col);
 Object value = table.getValueAt(row, col);
 Component cell = tcr.getTableCellRendererComponent(table, value, false, false, row, col);
 Dimension itemSize = cell.getPreferredSize();
 Insets i = ((JComponent) cell).getInsets();
 Rectangle cellBounds = table.getCellRect(row, col, false);
 cellBounds.width = itemSize.width - i.right - i.left;
 cellBounds.translate(i.left, i.top);
 return cellBounds.contains(p);
}

private static boolean isUrlColumn(JTable table, int column) {
 return column >= 0 && table.getColumnClass(column).equals(URL.class);
}

@Override public void mouseMoved(MouseEvent e) {
 JTable table = (JTable) e.getComponent();
 Point pt = e.getPoint();
 final int prevRow = vrow;
 final int prevCol = vcol;
 final boolean prevRollover = isRollover;
 vrow = table.rowAtPoint(pt);
 vcol = table.columnAtPoint(pt);
 isRollover = isUrlColumn(table, vcol) && pointInsidePrefSize(table, pt);
 if (vrow == prevRow && vcol == prevCol && isRollover == prevRollover) {
   return;
 }
 if (!isRollover && !prevRollover) {
   return;
 }
 // >>>> HyperlinkCellRenderer.java
 // @see http://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/demos/table/HyperlinkCellRenderer.java
 Rectangle repaintRect;
 if (isRollover) {
   Rectangle r = table.getCellRect(vrow, vcol, false);
   repaintRect = prevRollover ? r.union(table.getCellRect(prevRow, prevCol, false)) : r;
 } else { // if (prevRollover) {
   repaintRect = table.getCellRect(prevRow, prevCol, false);
 }
 table.repaint(repaintRect);
 // <<<<
 // table.repaint();
}

@Override public void mouseExited(MouseEvent e) {
 JTable table = (JTable) e.getComponent();
 if (isUrlColumn(table, vcol)) {
   table.repaint(table.getCellRect(vrow, vcol, false));
   vrow = -1;
   vcol = -1;
   isRollover = false;
 }
}

@Override public void mouseClicked(MouseEvent e) {
 JTable table = (JTable) e.getComponent();
 Point pt = e.getPoint();
 int ccol = table.columnAtPoint(pt);
 if (isUrlColumn(table, ccol) && pointInsidePrefSize(table, pt)) {
   int crow = table.rowAtPoint(pt);
   URL url = (URL) table.getValueAt(crow, ccol);
   System.out.println(url);
   try {
     // Web Start
     // BasicService bs = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
     // bs.showDocument(url);
     if (Desktop.isDesktopSupported()) { // JDK 1.6.0
       Desktop.getDesktop().browse(url.toURI());
     }
   } catch (URISyntaxException | IOException ex) {
     ex.printStackTrace();
   }
 }
}

@Override public void mouseDragged(MouseEvent e) { /* not needed */ }

@Override public void mouseEntered(MouseEvent e) { /* not needed */ }

@Override public void mousePressed(MouseEvent e) { /* not needed */ }

@Override public void mouseReleased(MouseEvent e) { /* not needed */ }
}
