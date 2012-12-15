/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.olap4j.OlapException;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

/**
 * 
 * @author simonpai
 */
public class OlapHierarchyField extends OlapPivotField<Hierarchy> {
	
	protected final List<OlapLevelField> _levelFields = new LinkedList<OlapLevelField>();
	protected final List<Member> _rootMembers;
	
	public OlapHierarchyField(Hierarchy hierarchy) {
		super(hierarchy);
		for (Level lv : hierarchy.getLevels())
			_levelFields.add(new OlapLevelField(this, lv));
		try {
			_rootMembers = new ArrayList<Member>(hierarchy.getRootMembers());
		} catch (OlapException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Hierarchy getHierarchy() {
		return _element;
	}
	
	public List<Member> getRootMembers() {
		return Collections.unmodifiableList(_rootMembers);
	}
	
	public List<OlapLevelField> getLevelFields() {
		return Collections.unmodifiableList(_levelFields);
	}
	
}
