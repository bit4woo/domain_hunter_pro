package burp;

import java.util.HashSet;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class TargetMapTreeModel extends DefaultTreeModel  {
	
	HashSet<String> targetSet = new HashSet<String>();

	public TargetMapTreeModel(TreeNode root) {
		super(root);
	}

}
