/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import org.olap4j.OlapConnection;

/**
 * 
 * @author simonpai
 */
public interface OlapConnectionFactory {
	
	OlapConnection create() throws Exception;
	
}
