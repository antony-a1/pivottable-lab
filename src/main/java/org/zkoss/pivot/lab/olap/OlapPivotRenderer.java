/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import org.olap4j.metadata.Member;
import org.zkoss.pivot.PivotField;
import org.zkoss.pivot.PivotField.Type;
import org.zkoss.pivot.Pivottable;
import org.zkoss.pivot.impl.SimplePivotRenderer;

/**
 * 
 * @author simonpai
 */
public class OlapPivotRenderer extends SimplePivotRenderer {

	@Override
	public String renderField(Object data, Pivottable table, PivotField field) {
		return (field.getType() != Type.DATA && data instanceof Member) ? 
				((Member) data).getName() : super.renderField(data, table, field);
	}
	
}
