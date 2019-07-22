package target;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TargetMapTreeModel implements TreeModel  {
	
	Set<TargetEntry> targetSet = new HashSet<TargetEntry>();
    private Vector<TreeModelListener> treeModelListeners =
            new Vector<TreeModelListener>();
    TargetEntry rootNode;
	public TargetMapTreeModel(){
	    rootNode=new TargetEntry("ROOT");
	}
	
	public void init() {
		for (TargetEntry entry: targetSet) {
			rootNode.getChildren().add(entry);
		}
	}
	
	
	public Set<TargetEntry> getTargetSet() {
		return targetSet;
	}

	public void setTargetSet(Set<TargetEntry> targetSet) {
		this.targetSet = targetSet;
	}
	
	
	/////////////////////////////////////////
	@Override
	public Object getRoot() {
		return rootNode;
	}

	@Override
	public Object getChild(Object parent, int index) {
		TargetEntry p = (TargetEntry)parent;
        return p.getChildAt(index);
	}

	@Override
	public int getChildCount(Object parent) {
		TargetEntry p = (TargetEntry)parent;
        return p.getChildren().size();
	}

	@Override
	public boolean isLeaf(Object node) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListeners.removeElement(l);
	}

}
