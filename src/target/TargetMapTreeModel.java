package target;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import burp.BurpExtender;

public class TargetMapTreeModel implements TreeModel  {

	private Set<TargetEntry> targetSet = new HashSet<TargetEntry>();
    private Vector<TreeModelListener> treeModelListeners =
            new Vector<TreeModelListener>();
    TargetEntry rootNode;

	
	public TargetMapTreeModel() {
		super();
		rootNode = new TargetEntry("root");
		this.addTreeModelListener(new MyTreeModelListener());
	}

	public void init() {
		for (TargetEntry entry: targetSet) {
			rootNode.getChildren().add(entry);
		}
		fireTreeStructureChanged(rootNode);
	}

	///////各种add target的方法 ////////////////
	public boolean isexist(TargetEntry target){
		for (TargetEntry entry:rootNode.getChildren()){
			if (entry.getDomain().equalsIgnoreCase(target.getDomain())){
				return true;
			}
		}
		return false;
	}

	public void addTargets(Set<TargetEntry> targets) {
		for (TargetEntry entry: targets) {
			if (isexist(entry)) return;
			rootNode.getChildren().add(entry);
		}
		fireTreeStructureChanged(rootNode);
	}

	public void addTarget(TargetEntry target) {
		if (isexist(target)) return;
		rootNode.getChildren().add(target);
		fireTreeStructureChanged(rootNode);
	}


	public void addTargetsFromDomains(Set<String> domains) {
		for (String domain: domains) {
			if (isexist(new TargetEntry(domain))) return;
			rootNode.getChildren().add(new TargetEntry(domain));
		}
		fireTreeStructureChanged(rootNode);
	}

	public void addTargetFromDomain(String domain) {
		if (isexist(new TargetEntry(domain))) return;
		rootNode.getChildren().add(new TargetEntry(domain));
		fireTreeStructureChanged(rootNode);
	}

	///////各种add target的方法 ////////////////
    /**
     * The only event raised by this model is TreeStructureChanged with the
     * root as path, i.e. the whole tree has changed.
     */
    protected void fireTreeStructureChanged(TargetEntry oldRoot) {
        int len = treeModelListeners.size();
        TreeModelEvent e = new TreeModelEvent(this, 
                                              new Object[] {oldRoot});
        for (TreeModelListener tml : treeModelListeners) {
            tml.treeStructureChanged(e);
        }
    }



	public Set<TargetEntry> getTargetSet() {
		return targetSet;
	}

	public void setTargetSet(Set<TargetEntry> targetSet) {
		this.targetSet = targetSet;
	}


	public void showToTargetUI(List<TargetEntry> targetEntries) {
		//titleTableModel.setLineEntries(new ArrayList<LineEntry>());//clear
		//这里没有fire delete事件，会导致排序号加载文件出错，但是如果fire了又会触发tableModel的删除事件，导致数据库删除。改用clear()
		this.targetSet.clear();//clear
//		titleTableModel.setListenerIsOn(false);
//		titleTableModel.setListenerIsOn(true);
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
		TargetEntry p = (TargetEntry)node;
        return p.getChildren().size() == 0;
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		//需要编辑节点信息的时候才需要
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		TargetEntry p = (TargetEntry)parent;
        return p.getIndexOfChild((TargetEntry)child);
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		treeModelListeners.addElement(l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListeners.removeElement(l);
	}

}



class MyTreeModelListener implements TreeModelListener{

	@Override
	public void treeNodesChanged(TreeModelEvent e) {
		Object[] urltest = e.getPath();
		BurpExtender.getStdout().println("getPath: "+urltest);
		TreePath tp = e.getTreePath();
        Object[] children = e.getChildren();
        BurpExtender.getStdout().println("getTreePath: "+tp);
        BurpExtender.getStdout().println("getChildren: "+children);
	}

	@Override
	public void treeNodesInserted(TreeModelEvent e) {

	}

	@Override
	public void treeNodesRemoved(TreeModelEvent e) {

	}

	@Override
	public void treeStructureChanged(TreeModelEvent e) {

	}

}
