/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import org.olap4j.OlapConnection;
import org.olap4j.OlapStatement;

/**
 * 
 * @author simonpai
 */
public class OlapUtil {
	
	/**
	 * 
	 * @param factory
	 * @param handler
	 */
	public static void runConnection(OlapConnectionFactory factory, 
			Handler<OlapConnection> handler) throws RuntimeException {
		OlapConnection conn = null;
		try {
			handler.handle(conn = factory.create());
			conn.close();
		} catch (Exception e) {
			try {
				if (conn != null && !conn.isClosed()) conn.close();
			} catch (Exception e2) {}
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 * @param factory
	 * @param handler
	 */
	public static void runStatement(final OlapConnectionFactory factory, 
			final Handler<OlapStatement> handler) throws RuntimeException {
		runConnection(factory, new Handler<OlapConnection>() {
			public void handle(OlapConnection conn) throws Exception {
				OlapStatement stat = null;
				try {
					handler.handle(stat = conn.createStatement());
					stat.close();
				} catch (Exception e) {
					try {
						if (stat != null && !stat.isClosed()) stat.close();
					} catch (Exception e2) {}
					throw e;
				}
			}
		});
	}
	
	public interface Handler<T> {
		public void handle(T item) throws Exception;
	}
	
}

