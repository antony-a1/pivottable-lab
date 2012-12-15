/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import java.util.List;

import org.olap4j.metadata.Member;
import org.zkoss.pivot.PivotField;
import org.zkoss.pivot.PivotHeaderNode;
import org.zkoss.pivot.impl.util.IndexLinkedList;

/**
 * 
 * @author simonpai
 */
public class OlapPivotHeaderNode implements PivotHeaderNode {
	
	private final OlapPivotHeaderTree _tree;
	private final OlapPivotHeaderNode _parent;
	private IndexLinkedList<Object, OlapPivotHeaderNode> _children;
	private final int _depth;
	private final Member _member;
	private transient int _size = -1;
	private transient int _sizeAsIfOpen = -1;
	
	private boolean _childrenLoaded;
	private boolean _open; // default close
	
	/*package*/ OlapPivotHeaderNode(OlapPivotHeaderTree tree) {
		_parent = null;
		_tree = tree;
		_depth = 0;
		_member = null;
		_open = true;
		// append root members
		for (Member m : tree.getField().getRootMembers())
			append(m);
	}
	
	protected OlapPivotHeaderNode(Member member, OlapPivotHeaderNode parent) {
		_parent = parent;
		if (parent != null)
			parent.initChildren().add(member, this); // append, sort later
		_tree = parent.getTree();
		_depth = parent.getDepth() + 1;
		_member = member;
	}
	
	/**
	 * 
	 * @param member
	 * @return
	 */
	public OlapPivotHeaderNode append(Member member) {
		return new OlapPivotHeaderNode(member, this);
	}
	
	public boolean isChildrenLoaded() {
		return _childrenLoaded;
	}
	
	public void setChildrenLoaded(boolean loaded) {
		_childrenLoaded = loaded;
	}
	
	@Override
	public OlapPivotHeaderTree getTree() {
		return _tree;
	}
	
	@Override
	public OlapPivotHeaderNode getParent() {
		return _parent;
	}
	
	@Override
	public List<OlapPivotHeaderNode> getChildren() {
		return initChildren();
	}
	
	@Override
	public int getDepth() {
		return _depth;
	}
	
	@Override
	public boolean isLeaf() {
		return _depth == _tree.getDepth() - 1;
	}
	
	public boolean isRoot() {
		return _depth == 0;
	}
	
	@Override
	public OlapPivotHeaderNode getChild(Object key) {
		return _children == null ? null : _children.query(key);
	}
	
	public Member getMember() {
		return _member;
	}
	
	@Override
	public Object getKey() {
		return _member;
	}
	
	@Override
	public PivotField getField() {
		return _depth == 0 ? null : _tree.getField().getLevelFields().get(_depth - 1);
	}
	
	@Override
	public boolean isOpen() {
		return _open;
	}

	@Override
	public void setOpen(boolean open) {
		_open = open;
		// clear size cache
		for (OlapPivotHeaderNode p = getParent(); p != null; p = p.getParent())
			p._size = -1;
		getTree()._listener.onOpen(this, open);
	}
	
	@Override
	public int getSubtotalCount(boolean asIfOpen) {
		return 0;
	}
	
	@Override
	public int getSize(boolean asIfOpen) {
		if (isLeaf() || (!_open && !asIfOpen))
			return 1;
		if (asIfOpen) {
			if (_sizeAsIfOpen < 0) {
				_sizeAsIfOpen = 0;
				for (OlapPivotHeaderNode n : getChildren())
					_sizeAsIfOpen += n.getSize(true)/* + n.getSubtotalCount(true)*/;
			}
			return _sizeAsIfOpen;
		} else {
			if (_size < 0) {
				_size = 0;
				for (OlapPivotHeaderNode n : getChildren())
					_size += n.getSize(false)/* + n.getSubtotalCount(false)*/;
			}
			return _size;
		}
	}
	
	protected IndexLinkedList<Object, OlapPivotHeaderNode> initChildren() {
		if (_children == null)
			_children = new IndexLinkedList<Object, OlapPivotHeaderNode>();
		return _children;
	}
	
	public String getHash() {
		return isRoot() ? "[]" : _member.getUniqueName();
	}
	
}
