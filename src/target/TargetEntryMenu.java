package target;

import burp.BurpExtender;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class TargetEntryMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;
	TargetEntryMenu(final TargetMapTree tree){

		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		TreePath[] paths = tree.getSelectionModel().getSelectionPaths();
		tree.getCurrentSelected().clear();
		for(TreePath path:paths){
			tree.getCurrentSelected().add(path.getLastPathComponent().toString());
		}
		BurpExtender.getStdout().println(tree.getCurrentSelected().toString());

		JMenuItem itemNumber = new JMenuItem(new AbstractAction(paths.length+" Items Selected") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
			}
		});

		this.add(itemNumber);

		JMenuItem checkedItem = new JMenuItem(new AbstractAction("Mark As Checked") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int[] paths = tree.getSelectionModel().getSelectionRows();
				for (int path:paths) {
					TargetEntry target = (TargetEntry) tree.getModel().getChild(tree.getModel().getRoot(), path);
					target.setChecked(true);
				}
			}
		});
		this.add(checkedItem);

		this.add(new LevelMenu(tree));


		JMenuItem linkTogether = new JMenuItem(new AbstractAction("Link To ") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String domain = JOptionPane.showInputDialog("domain", null).trim();
				while (domain.trim().equals("")) {
					domain = JOptionPane.showInputDialog("domain", null).trim();
				}
				if (domain == null || domain.equals("")) return;
				TargetEntry newParent = ((TargetMapTreeModel) tree.getModel()).getTargetbyDomain(domain);
				if (newParent == null) return;

				int[] paths = tree.getSelectionModel().getSelectionRows();
				for (int path : paths) {
					TargetEntry target = (TargetEntry) tree.getModel().getChild(tree.getModel().getRoot(), path);
					((TargetMapTreeModel) tree.getModel()).linkTogether(newParent,target);
				}
			}
		});
		this.add(linkTogether);



		this.addSeparator();

		JMenuItem removeItem = new JMenuItem(new AbstractAction("Delete") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items ?");
				if (result == JOptionPane.YES_OPTION) {
					int[] paths = tree.getSelectionModel().getSelectionRows();
					Set<TargetEntry> itemsToDel = new HashSet<>();
					for (int path : paths) {
						TargetEntry target = (TargetEntry) tree.getModel().getChild(tree.getModel().getRoot(), path);
						itemsToDel.add(target);
					}
					((TargetMapTreeModel) tree.getModel()).removeTargets(itemsToDel);
				}else {
					return;
				}
			}
		});
		this.add(removeItem);

		JMenuItem blackListItem = new JMenuItem(new AbstractAction("Add To Black List") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items and Add To BLACK LIST ?");
//				if (result == JOptionPane.YES_OPTION) {
//					lineTable.getModel().addBlackList(rows);
//				}else {
//					return;
//				}
//				BurpExtender.getGui().titlePanel.digStatus();
			}
		});
		blackListItem.setToolTipText("will not get title from next time");
		this.add(blackListItem);
	}
}


class LevelMenu extends JMenu{
	public String[] MainMenu = {"HIGH 3", "MIDDLE 2", "LOW 3"};

	public LevelMenu(TargetMapTree tree){
		this.setText("Set Level As");

		for(int i = 0; i < MainMenu.length; i++){
			JMenuItem item = new JMenuItem(MainMenu[i]);
			item.addActionListener((ActionListener) new LevelItemListener(tree));
			this.add(item);
		}
	}

	class LevelItemListener implements ActionListener {

		TargetMapTree tree;
		LevelItemListener(TargetMapTree tree) {
			this.tree = tree;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int[] paths = tree.getSelectionModel().getSelectionRows();
			for (int path:paths) {
				TargetEntry target = (TargetEntry) tree.getModel().getChild(tree.getModel().getRoot(), path);
				target.setValueLevel(Integer.parseInt(e.getActionCommand().split(" ")[1]));
			}
		}
	}
}



