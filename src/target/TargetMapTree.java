package target;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import burp.BurpExtender;
import burp.DomainPanel;
import burp.LineEntry;
import burp.LineEntryMenu;
import burp.LineTable;
import burp.TitlePanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TargetMapTree extends JTree{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static Set<String> currentSelected =  new HashSet<>();

	public static Set<String> getCurrentSelected() {
		return currentSelected;
	}

	public TargetMapTree(TargetMapTreeModel targetTreeModel) {
		super(targetTreeModel);

		getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		Icon personIcon = null;
		renderer.setLeafIcon(personIcon);
		renderer.setClosedIcon(personIcon);
		renderer.setOpenIcon(personIcon);
		setCellRenderer(renderer);
		this.setRootVisible(false);

		addListener();
		//addRender();//有错误。导致显示异常
	}

	public void addListener() {
		this.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				TargetEntry SelectedTarget = (TargetEntry)TargetMapTree.this.getLastSelectedPathComponent();
//				if (SelectedTarget.getDomain().equals("Targets")){
//					//TODO 是否有必要在某些情况下让currentSelected为空，以显示全部呢？
//				}

				TreePath[] paths = TargetMapTree.this.getSelectionModel().getSelectionPaths();
				currentSelected.clear();
				for(TreePath path:paths){
					currentSelected.add(((TargetEntry)path.getLastPathComponent()).getDomain());
				}
				BurpExtender.getStdout().println(currentSelected.toString());

				TitlePanel.getTitleTable().search("");
			}

		});



		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if ( SwingUtilities.isRightMouseButton( e )){
					if (e.isPopupTrigger() && e.getComponent() instanceof JTree ) {
						new TargetEntryMenu(TargetMapTree.this).show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}
		});
	}

	public void addRender() {
		this.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value,
					boolean arg2, boolean arg3, boolean arg4, int arg5, boolean arg6) {

				Component c = super.getTreeCellRendererComponent(tree, value, arg2, arg3, arg4, arg5, arg6);

				LineEntry node = (LineEntry) value;
				if (node.isChecked()) {
					c.setForeground(Color.GREEN);
					return c;
				}
				else if (node.isNew()) {
					c.setForeground(Color.RED);
					return c;
				}
				else {
					return c;
				}
			}
		});
	}
}
