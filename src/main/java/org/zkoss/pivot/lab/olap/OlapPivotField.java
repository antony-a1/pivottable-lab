/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import java.util.LinkedList;
import java.util.List;

import org.olap4j.metadata.MetadataElement;
import org.zkoss.pivot.Calculator;
import org.zkoss.pivot.GroupHandler;
import org.zkoss.pivot.PivotField;
import org.zkoss.pivot.event.FieldDataListener;

/**
 * 
 * @author simonpai
 */
public abstract class OlapPivotField<M extends MetadataElement> implements PivotField {
	
	protected final M _element;
	protected Type _type;
	
	protected OlapPivotField(M element) {
		_element = element;
	}
	
	@Override
	public String getTitle() {
		return _element.getName();
	}
	
	@Override
	public String getFieldName() {
		return _element.getName();
	}
	
	@Override
	public Type getType() {
		return _type;
	}
	
	/*package*/ void setType(Type type) {
		_type = type;
	}
	
	public String getElementUniqueName() {
		return _element.getUniqueName();
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + _element.getUniqueName();
	}
	
	
	
	// event listener //
	private List<FieldDataListener> _listeners = new LinkedList<FieldDataListener>();
	@Override
	public void addFieldDataListener(FieldDataListener f) {
		_listeners.add(f);
	}
	@Override
	public void removeFieldDataListener(FieldDataListener f) {
		_listeners.remove(f);
	}
	
	
	
	// unsupported //
	@Override
	public GroupHandler getGroupHandler() {
		return null;
	}
	@Override
	public Calculator getSummary() {
		return OlapPivotModel.DEF;
	}
	@Override
	public Calculator[] getSubtotals() {
		return new Calculator[0];
	}
	@Override
	public Calculator getSubtotal(int index) {
		return null;
	}
	
}
