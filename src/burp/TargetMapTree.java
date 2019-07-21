package burp;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class TargetMapTree extends JTree{

	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DefaultMutableTreeNode topNode;	
	private TargetMapTreeModel targetTreeModel;

	public TargetMapTree(TargetMapTreeModel targetTreeModel) {
		this.targetTreeModel = targetTreeModel;
		this.setModel(this.targetTreeModel);
		topNode = (DefaultMutableTreeNode) targetTreeModel.getRoot();
	}


	public void createNodes() {
	    DefaultMutableTreeNode category = null;
	    DefaultMutableTreeNode book = null;
	    

	    
	    category = new DefaultMutableTreeNode("Books for Java Programmers");
	    topNode.add(category);
	    
	    targetTreeModel.insertNodeInto(category,topNode, topNode.getChildCount());
	    //original Tutorial
	    book = new DefaultMutableTreeNode(new TargetEntry
	        ("The Java Tutorial: A Short Course on the Basics"));
	    category.add(book);
	    
	    //Tutorial Continued
	    book = new DefaultMutableTreeNode(new TargetEntry
	        ("The Java Tutorial Continued: The Rest of the JDK"));
	    category.add(book);
	    
	    //Swing Tutorial
	    book = new DefaultMutableTreeNode(new TargetEntry
	        ("The Swing Tutorial: A Guide to Constructing GUIs"));
	    category.add(book);

	    //...add more books for programmers...

	    category = new DefaultMutableTreeNode("Books for Java Implementers");
	    topNode.add(category);

	    //VM
	    book = new DefaultMutableTreeNode(new TargetEntry
	        ("The Java Virtual Machine Specification"));
	    category.add(book);

	    //Language Spec
	    book = new DefaultMutableTreeNode(new TargetEntry
	        ("The Java Language Specification"));
	    category.add(book);
	}
}
