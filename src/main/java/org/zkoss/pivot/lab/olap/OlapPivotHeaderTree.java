/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import org.olap4j.Position;
import org.olap4j.metadata.Member;
import org.zkoss.pivot.PivotHeaderTree;

/**
 * 
 * @author simonpai
 */
public class OlapPivotHeaderTree implements PivotHeaderTree {
	
	private final OlapPivotModel _model;
	private final OlapHierarchyField _field;
	protected final OlapNodeEventListener _listener;
	
	private final OlapPivotHeaderNode _root;
	private final int _depth;
	
	public OlapPivotHeaderTree(OlapPivotModel model, OlapHierarchyField field, 
			OlapNodeEventListener olapNodeEventListener) {
		_model = model;
		_field = field;
		_listener = olapNodeEventListener;
		_root = new OlapPivotHeaderNode(this);
		_depth = field.getLevelFields().size() + 1;
	}
	
	public OlapPivotModel getModel() {
		return _model;
	}
	
	public OlapHierarchyField getField() {
		return _field;
	}
	
	@Override
	public OlapPivotHeaderNode getRoot() {
		return _root;
	}
	
	@Override
	public int getDepth() {
		return _depth;
	}
	
	public OlapPivotHeaderNode getNode(Position position) {
		OlapPivotHeaderNode n = _root;
		// as we only allow 1 dimension on each Axis for now
		Member m0 = position.getMembers().get(0);
		// trace down, may use tree-wide node map to save time
		for (Member m : getMemberChain(m0))
			n = n.getChild(m);
		return n;
	}
	
	protected static Member[] getMemberChain(Member member) {
		Member[] chain = new Member[member.getDepth() + 1];
		Member m = member;
		for (int i = chain.length - 1; i > -1; i--) {
			chain[i] = m;
			m = m.getParentMember();
		}
		return chain;
	}
	
	public interface OlapNodeEventListener {
		
		public void onOpen(OlapPivotHeaderNode node, boolean open);
		
	}
	
	// debugger //
	public void print() {
		System.out.println("* Field: " + _field);
		printNode(_root);
	}
	
	protected void printNode(OlapPivotHeaderNode node) {
		if (!node.isRoot())
			System.out.println(StringUtil.space(node.getDepth() * 2) + node.getMember().getName());
		for (OlapPivotHeaderNode c : node.getChildren())
			printNode(c);
	}
	
}
