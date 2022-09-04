package test;

import domain.DomainPanel;
import domain.target.TargetControlPanel;
import domain.target.TargetTable;
import domain.target.TargetTableModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import GUI.RunnerGUI;

import java.awt.*;

public class TargetModelTest {

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
//					JSplitPane TargetPane = new JSplitPane();//中间的大模块，一分为二
//					TargetPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
//					TargetPane.setResizeWeight(1);
//
//					//RunnerPanel.add(splitPane, BorderLayout.CENTER);
//
//					JScrollPane PanelWest1 = new JScrollPane();
//					TargetPane.setLeftComponent(PanelWest1);
//					TargetTable targetTable = new TargetTable();
//					targetTable.loadData(new TargetTableModel());
//					PanelWest1.setViewportView(targetTable);
//
//					TargetControlPanel ControlPanel = new TargetControlPanel();
//					TargetPane.setRightComponent(ControlPanel);

					JFrame frame = new FrameUseForTest();
					frame.setContentPane(new DomainPanel());

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		

	}
}
