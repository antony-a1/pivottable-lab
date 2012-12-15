/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import org.olap4j.metadata.Level;

/**
 * 
 * @author simonpai
 */
public class OlapLevelField extends OlapPivotField<Level> {
	
	protected final OlapHierarchyField _hierarchyField;
	
	public OlapLevelField(OlapHierarchyField hierarchyField, Level level) {
		super(level);
		_hierarchyField = hierarchyField;
	}
	
	public Level getLevel() {
		return _element;
	}
	
	public OlapHierarchyField getHierarchyField() {
		return _hierarchyField;
	}
	
}
