package target;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import burp.*;
import com.google.gson.Gson;

public class TargetMapTreeModel implements TreeModel  {

	private Vector<TreeModelListener> treeModelListeners = new Vector<TreeModelListener>();
	//关于监听器，官方例子中，应该是一个节点对应一个监听器。在我自己的这段代码中，就只关注这个root节点，后续的操作也是以root节点为核心的。
	//故只需要一个监听器就可以了??----似乎必须用监听器的集合，有一些默认的监听器会被自动添加，debug时可以看到！！！
	static  TargetEntry rootNode;

	public static TargetEntry getRootNode() {
		return rootNode;
	}

	public static void setRootNode(TargetEntry rootNode) {
		TargetMapTreeModel.rootNode = rootNode;
	}

	public String rootNodeToJson(){
		return new Gson().toJson(rootNode);
	}

	public static TargetEntry restoreRootNodeFromJson (String modelJson){
		return new Gson().fromJson(modelJson,TargetEntry.class);
	}

	public TargetMapTreeModel() {
		super();
		rootNode = new TargetEntry("Targets");

//		addTargetFromDomain("test");
//		addTargetFromDomain("www.baidu.com");
		this.addTreeModelListener(new MyTreeModelListener());//最后再添加这个监听器，避免事件触发
	}

	/*
	获取某个节点下的所有子节点
	 */
	public static Set<TargetEntry> getSubTargetEntrysOfNode(TargetEntry node){
		Set<TargetEntry> result = new HashSet<>();
		if (node == null || node.getChildren() == null) return null;
		for (TargetEntry child:node.getChildren()){
			result.add(child);
			if (child.getChildren().size() > 0){
				result.addAll(getSubTargetEntrysOfNode(child));
			}
		}
		return  result;
	}

	/*
	获取root节点下的所有节点
	 */
	public Set<TargetEntry> getAllTargetEntries(){
		Set<TargetEntry> results = getSubTargetEntrysOfNode(rootNode);
		results.add(rootNode);//需包含节点本身
		return results;
	}

	@Deprecated
	public Set<TargetEntry> getTargetsbyDomains(Set<String> domains) {
		Set<TargetEntry> result = new HashSet<>();
		for (String domain: domains) {
			for (TargetEntry entry:rootNode.getChildren()){
				if (entry.getDomain().equalsIgnoreCase(domain)){
					result.add(entry);
				}
			}
		}
		return result;
	}

	/*
	根据域名在root节点下查找
	 */
	public TargetEntry getTargetbyDomain(String domain) {
		Set<TargetEntry> enties = getAllTargetEntries();
		for (TargetEntry entry:getAllTargetEntries()){
			if (entry.getDomain().equalsIgnoreCase(domain)){
				return entry;
			}
		}
		return null;
	}


	///////各种add target的方法 ////////////////
	public boolean isexist(TargetEntry target){
		for (TargetEntry entry:getAllTargetEntries()){
			if (entry.getDomain().equalsIgnoreCase(target.getDomain())){
				return true;
			}
		}
		return false;
	}

	/*
	添加子节点，不调用fire函数，批量添加时避免频繁fire！！！
	 */
	private void justAddTarget(TargetEntry target) {
		//if (isexist(target)) return;
		if (!isexist(target)) {
			rootNode.getChildren().add(target);
			target.setParentName(rootNode.getDomain());
		}
	}
	/*
	向root节点添加子节点
	 */
	public void addTarget(TargetEntry target) {
		justAddTarget(target);
		fireTreeStructureChanged();
	}

	/*
	向root节点添加子节点
	 */
	public void addTargets(Set<TargetEntry> targets) {
		for (TargetEntry entry: targets) {
			justAddTarget(entry);
		}
		//fireTreeStructureChanged(rootNode);
	}

	/*
	向root节点添加子节点
	*/
	public void addTargetsFromDomains(Set<String> domains) {
		for (String domain: domains) {
			TargetEntry newEntry = new TargetEntry(domain);
			justAddTarget(newEntry);
		}
		fireTreeStructureChanged();
	}

	/*
	向root节点添加子节点
	 */
	public void addTargetFromDomain(String domain) {
		TargetEntry newEntry = new TargetEntry(domain);
		justAddTarget(newEntry);
		fireTreeStructureChanged();
	}

	/*
	从root节点删除子节点,
	可以从set中删除不存在的对象，不会报错
	 */
	public void removeTargets(Set<TargetEntry> targets) {
		for (TargetEntry entry: targets) {
			rootNode.getChildren().remove(entry);
			TitlePanel.getTitleTableModel().removeLinesByDomain(entry.getDomain());
		}
		fireTreeStructureChanged();
	}

	/*
	从root节点删除子节点
	 */
	public void removeTarget(TargetEntry target) {
		rootNode.getChildren().remove(target);
		fireTreeStructureChanged();
	}

	/*
	修改2个节点的父子关系
	 */
	public void linkTogether(TargetEntry newParent,TargetEntry child) {

		TargetEntry oldParent = getTargetbyDomain(child.getParentName());
		oldParent.getChildren().remove(child);
		//child.getParent().getChildren().remove(child); //从原始父节点移除
		child.setParentName(newParent.getDomain());
		newParent.getChildren().add(child); //添加到新的父节点下
		fireTreeStructureChanged();
	}



	///////各种add target的方法 ////////////////
	/**
	 * The only event raised by this model is TreeStructureChanged with the
	 * root as path, i.e. the whole tree has changed.
	 */

//	protected void fireTreeStructureChanged(TargetEntry oldRoot) {
//		int len = treeModelListeners.size();
//		TreeModelEvent e = new TreeModelEvent(this,
//				new Object[] {oldRoot});
//		for (TreeModelListener tml : treeModelListeners) {
//			tml.treeStructureChanged(e);
//		}
//	}
//
	public void fireTreeStructureChanged() {
		int len = treeModelListeners.size();
		TreeModelEvent e = new TreeModelEvent(this,
				new Object[] {rootNode});
		for (TreeModelListener tml : treeModelListeners) {
			tml.treeStructureChanged(e);
		}
	}

//	//root节点的监听器处理,但是居然不生效！！！why? 一定要用多个listener？
//	protected void fireTreeStructureChanged() {
//		TreeModelEvent e = new TreeModelEvent(this,
//				new Object[] {rootNode});
//		treeModelListener.treeStructureChanged(e);
//	}

	public void showToTargetUI(List<TargetEntry> targetEntries) {
		//titleTableModel.setLineEntries(new ArrayList<LineEntry>());//clear
		//这里没有fire delete事件，会导致排序号加载文件出错，但是如果fire了又会触发tableModel的删除事件，导致数据库删除。改用clear()
		//this.targetSet.clear();//clear
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

	public Object getChildOfRoot(int index) {
		TargetEntry p = (TargetEntry)rootNode;
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
		//当使用多个监听器时需要以上代码，这里只有一个监听器，不需要了
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListeners.removeElement(l);
		//当使用多个监听器时需要以上代码，这里只有一个监听器，不需要了
	}






	class MyTreeModelListener implements TreeModelListener{

		@Override
		public void treeNodesChanged(TreeModelEvent e) {
			BurpExtender.getStdout().println("treeNodesChanged: ");
//		Object[] urltest = e.getPath();
//		TreePath tp = e.getTreePath();
//		Object[] children = e.getChildren();
//		BurpExtender.getStdout().println("getTreePath: "+tp);
//		BurpExtender.getStdout().println("getChildren: "+children);
//		DBHelper dbHelper = new DBHelper(GUI.getCurrentDBFile().toString());
//		dbHelper.updateTargets(getRelatedItems(e));

		}

		@Override
		public void treeNodesInserted(TreeModelEvent e) {
			BurpExtender.getStdout().println("treeNodesInserted: ");
//		DBHelper dbHelper = new DBHelper(GUI.getCurrentDBFile().toString());
//		dbHelper.addTargets(getRelatedItems(e));
		}

		@Override
		public void treeNodesRemoved(TreeModelEvent e) {
			BurpExtender.getStdout().println("treeNodesRemoved: ");
//		DBHelper dbHelper = new DBHelper(GUI.getCurrentDBFile().toString());
//		dbHelper.deleteTargets(getRelatedItems(e));
		}

		@Override
		public void treeStructureChanged(TreeModelEvent e) {
			BurpExtender.getStdout().println("treeStructureChanged: ");
			DBHelper dbHelper = new DBHelper(GUI.getCurrentDBFile().toString());
			dbHelper.saveRootNode(TargetMapTreeModel.getRootNode());
//			//TODO 数据库操作同步过程还是有问题，关键要理解监听器的工作
//			//目前采用全量删除+全量添加的方法来更新所有target。但这个方法肯定是效率低的！！！

		}

		@Deprecated
		public Set<TargetEntry> getRelatedItems(TreeModelEvent e){
			Object[] modifiedItems = e.getChildren();//获取相关的，发生了改变的节点
			Set<TargetEntry> Targets= new HashSet<>();
			for(Object item: modifiedItems){
				TargetEntry entry = (TargetEntry)item;
				Targets.add(entry);
			}
			return Targets;
		}

	}

	public static void main(String[] args){
		TargetMapTreeModel aaa = new TargetMapTreeModel();
		System.out.print(aaa.rootNodeToJson());
	}

}



