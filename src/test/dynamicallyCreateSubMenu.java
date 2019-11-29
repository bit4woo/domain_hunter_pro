package test;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

public class dynamicallyCreateSubMenu extends JFrame {
   private JPopupMenu popup;
   private JMenu subMenu;
   public dynamicallyCreateSubMenu() {
      setTitle("JPopupMenuwithSubMenu Test");
      popup = new JPopupMenu();
      subMenu = new JMenu("Course");
      subMenu.addMouseListener(
    		  new MouseAdapter() {
    		         public void mouseEntered(MouseEvent me) {
    		        	 System.out.println(subMenu.getItemCount());
    		        	 if (subMenu.getItemCount() ==0) {
        		             subMenu.add("Java");
        		             subMenu.add("Python");
        		             subMenu.add("Scala");
        		             System.out.println("1111");
        		             //subMenu.update(null);
        		             //subMenu.updateUI();
    		        	 }
    		         }
    		  }
    		         
    		  );
      
 
      popup.add(new JMenuItem("First Name"));
      popup.add(new JMenuItem("Last Name"));
      popup.addSeparator();
      popup.add(subMenu);

      addMouseListener(new MouseAdapter() {
         public void mouseReleased(MouseEvent me) {
            showPopup(me);
         }
      }) ;
      setSize(400, 275);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setLocationRelativeTo(null);
      setVisible(true);
   }
   void showPopup(MouseEvent me) {
      if(me.isPopupTrigger())
         popup.show(me.getComponent(), me.getX(), me.getY());
   }
   public static void main(String args[]) {
      new dynamicallyCreateSubMenu();
   }
}