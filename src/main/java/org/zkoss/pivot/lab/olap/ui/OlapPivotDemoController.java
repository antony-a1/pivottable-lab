/*
 * 
 */
package org.zkoss.pivot.lab.olap.ui;

import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.util.Properties;

import org.olap4j.OlapConnection;
import org.olap4j.OlapWrapper;
import org.zkoss.pivot.Pivottable;
import org.zkoss.pivot.PivotField.Type;
import org.zkoss.pivot.lab.olap.OlapConnectionFactory;
import org.zkoss.pivot.lab.olap.OlapPivotModel;
import org.zkoss.pivot.lab.olap.OlapPivotRenderer;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Radio;

/**
 * 
 * @author simonpai
 */
public class OlapPivotDemoController extends GenericForwardComposer<Component> {
	
	private static final long serialVersionUID = 1L;
	
	private String _catalogRealPath;
	protected String getCatalogRealPath() {
		if (_catalogRealPath == null)
			_catalogRealPath = 
				application.getServletContext().getRealPath("/WEB-INF/FoodMart.xml");
		return _catalogRealPath;
	}
	
	private String _jdbcString;
	protected String getJdbcString() {
		if (_jdbcString == null) {
			Properties props = new Properties();
			InputStream in = null;
	        try {
	        	in = application.getServletContext()
	        			.getResourceAsStream("/WEB-INF/olap-pivot-demo.properties");
	        	if (in != null)
	        		props.load(in);
	        	else
	        		System.out.println("olap-pivot-demo.properties not found, using default configuration.");
			} catch (IOException e) {
				System.out.println("error reading olap-pivot-demo.properties, using default configuration.");
			} finally {
				if (in != null)
					try { in.close(); } catch (IOException e) {}
			}
	        String jdbc = props.getProperty("jdbc_url", "jdbc:mysql://localhost:3306");
	        String dbname = props.getProperty("db_name", "foodmart");
	        String account = props.getProperty("db_account", "foodmart");
	        String password = props.getProperty("db_password", "foodmart");
	        _jdbcString = "Jdbc=" + jdbc + "/" + dbname + "?user=" + account + "&password=" + password;
		}
		return _jdbcString;
	}
	
	protected String getMondrianConnectionString() {
		return "jdbc:mondrian:" + getJdbcString() + ";Catalog=" + getCatalogRealPath();
	}
	
	final OlapConnectionFactory _connection_factory = new OlapConnectionFactory() {
		public OlapConnection create() throws Exception {
			return ((OlapWrapper) DriverManager.getConnection(
					getMondrianConnectionString())).unwrap(OlapConnection.class);
		}
	};
	
	private Pivottable pivot;
	private OlapPivotModel model;
	private OlapPivotFieldControl pfc;
	
	private Button updateBtn;
	private Radio colOrient, rowOrient;
	
	public void onClick$updateBtn() {
		pfc.update();
	}
	
	public void onPivotFieldControlChange$pfc() {
		if (!pfc.isUpdated())
			updateBtn.setDisabled(false);
	}
	
	public void onCheck$dataOrient(CheckEvent event) {
		pivot.setDataFieldOrient(((Radio)event.getTarget()).getLabel());
	}
	
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		
		// create new pivot model
		model = new OlapPivotModel(_connection_factory, "Sales");
		model.setFieldType("[Store]", Type.COLUMN);
		model.setFieldType("[Product]", Type.ROW);
		model.setFieldType("[Measures].[Unit Sales]", Type.DATA);
		model.setFieldType("[Measures].[Store Cost]", Type.DATA);
		
		pivot.setGrandTotalForColumns(false);
		pivot.setGrandTotalForRows(false);
		
		pivot.setModel(model);
		pfc.setModel(model);
		pivot.setPivotRenderer(new OlapPivotRenderer());
		initControls();
	}
	
	private void initControls() {
		// data orientation
		("column".equals(pivot.getDataFieldOrient()) ? 
				colOrient : rowOrient).setChecked(true);
		pfc.syncModel(); // field control
	}
	
}
