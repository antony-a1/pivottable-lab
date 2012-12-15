/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import org.olap4j.metadata.Measure;

/**
 * 
 * @author simonpai
 */
public class OlapMeasureField extends OlapPivotField<Measure> {
	
	public OlapMeasureField(Measure measure) {
		super(measure);
	}
	
	public Measure getMeasure() {
		return _element;
	}
	
}
