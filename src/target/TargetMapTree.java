package target;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

public class TargetMapTree extends JTree{

	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DefaultMutableTreeNode topNode;	
	private TargetMapTreeModel targetTreeModel;

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
        //this.setRootVisible(false);
        
        Set<String> test = new HashSet<String>();
        List<String> testdomain = Arrays.asList("www.baidu.com,www.jd.com,www.sf.com".split(","));
        test.addAll(testdomain);
        
        targetTreeModel.setTargetSet(domainToTargetEntry(test));
        targetTreeModel.init();
	}
	
	public Set<TargetEntry> domainToTargetEntry(Set<String> domains) {
		if (domains == null) return null;
		Set<TargetEntry> targetSet = new HashSet<TargetEntry>();
		for (String domain:domains) {
			TargetEntry target = new TargetEntry(domain);
			targetSet.add(target);
		}
		return targetSet;
		
	}
}
