package test;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
public class rightclick extends JFrame implements ActionListener
{
	JButton btnTest=new JButton("Test");
	JTextArea textArea=new JTextArea();
	PopupMenu pMenu=new PopupMenu();    //创建弹出式菜单，下面三项是菜单项
	MenuItem mItemCopy=new MenuItem("复制");
	MenuItem mItemPaste=new MenuItem("粘贴");
	MenuItem mItemCut=new MenuItem("剪切");
	MouseAdapter mouseAdapter=new MouseAdapter()//监听鼠标事件
	{
		public void mouseClicked(MouseEvent event)
		{
			if(event.getButton()==MouseEvent.BUTTON3)//只响应鼠标右键单击事件
			{
				pMenu.show(textArea,event.getX(),event.getY());//在鼠标位置显示弹出式菜单
			}
		}
	};
	ActionListener menuAction=new ActionListener()//响应单击菜单项的事件，只是示例，
	{//具体内容可自己编写
		public void actionPerformed(ActionEvent e)
		{
			MenuItem item=(MenuItem)e.getSource();
			if(item==mItemCopy) //单击了“复制”菜单项
			{
				JOptionPane.showMessageDialog(null,"复制");
			}
			else if(item==mItemPaste)  //“粘贴”菜单项
			{
				JOptionPane.showMessageDialog(null,"粘贴");
				
			}
			else
			{
				JOptionPane.showMessageDialog(null,"剪切");    //“剪切”菜单项
			}
		}
	};
	public rightclick()
	{
		setTitle("Test");
		setSize(300,300);
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(btnTest,BorderLayout.NORTH);
		add(textArea,BorderLayout.CENTER);
		textArea.add(pMenu);    //弹出式菜单加入到文本框中，否则不能显示
		textArea.addMouseListener(mouseAdapter);  //文本框加入鼠标监听器
		pMenu.add(mItemCopy);  //菜单项的单击事件监听器
		mItemCopy.addActionListener(menuAction);
		pMenu.add(mItemPaste);
		mItemPaste.addActionListener(menuAction);
		pMenu.add(mItemCut);
		mItemCut.addActionListener(menuAction);
		
		btnTest.addActionListener(this);
	}
	public static void main(String... args)
	{
		new rightclick().setVisible(true);
	}
	public void actionPerformed(ActionEvent e)
	{
		JOptionPane.showMessageDialog(null,"测试功能");
	}
	
}