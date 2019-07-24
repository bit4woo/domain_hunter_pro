package target;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import burp.BurpExtender;
import burp.DomainPanel;
import burp.TitlePanel;

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
		Set<String> test = new HashSet<String>();
		List<String> testdomain = Arrays.asList("www.baidu.com,www.jd.com,www.sf.com".split(","));
		test.addAll(testdomain);

		targetTreeModel.addTargetsFromDomains(test);
		targetTreeModel.init();

	}

	public void addListener() {
		this.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				TargetEntry SelectedTarget = (TargetEntry)TargetMapTree.this.getLastSelectedPathComponent();
				TreePath[] paths = TargetMapTree.this.getSelectionModel().getSelectionPaths();
				currentSelected.clear();
				for(TreePath path:paths){
					currentSelected.add(path.getLastPathComponent().toString());
				}
				BurpExtender.getStdout().println(currentSelected.toString());

				TitlePanel.getTitleTable().search("111");
			}

		});
	}
}
