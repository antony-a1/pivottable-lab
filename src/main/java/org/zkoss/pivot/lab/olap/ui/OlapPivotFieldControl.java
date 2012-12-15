/*
 * 
 */
package org.zkoss.pivot.lab.olap.ui;

import java.util.HashMap;
import java.util.Map;

import org.zkoss.pivot.PivotField;
import org.zkoss.pivot.PivotModelExt;
import org.zkoss.pivot.PivotField.Type;
import org.zkoss.pivot.lab.olap.OlapMeasureField;
import org.zkoss.pivot.lab.olap.OlapPivotField;
import org.zkoss.pivot.lab.olap.OlapPivotModel;
import org.zkoss.pivot.ui.PivotFieldControlChangeEvent;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.IdSpace;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zk.ui.util.ConventionWires;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

/**
 * 
 * @author simonpai
 */
public class OlapPivotFieldControl extends Div implements IdSpace, AfterCompose {
	
	private static final long serialVersionUID = 1L;
	
	protected OlapPivotModel _model;
	protected boolean _dirty = false;
	protected Grid ulist, clist, rlist, dlist;
	
	/**
	 * Update changes to PivotModel.
	 */
	public void update() {
		if (_dirty) {
			// clear first due to single dimension limitation
			clearPivotFields(rlist);
			clearPivotFields(clist);
			clearPivotFields(ulist);
			updatePivotFields(rlist, Type.ROW);
			updatePivotFields(clist, Type.COLUMN);
			updatePivotFields(dlist, Type.DATA);
			updatePivotFields(ulist, Type.UNUSED);
			_dirty = false;
		}
	}
	
	protected void clearPivotFields(Grid grid) {
		ListModel<OlapPivotField<?>> m = grid.getModel();
		for (int i = 0; i < m.getSize(); i++)
			_model.setFieldType(m.getElementAt(i), Type.UNUSED);
	}
	
	protected void updatePivotFields(Grid grid, Type type) {
		ListModel<OlapPivotField<?>> m = grid.getModel();
		for (int i = 0; i < m.getSize(); i++)
			_model.setFieldType(m.getElementAt(i), type);
	}
	
	/**
	 * Return true if the PivotModel is up to date.
	 */
	public boolean isUpdated() {
		return !_dirty;
	}
	
	/**
	 * Return the pivot model associated with this control
	 */
	public PivotModelExt getModel() {
		return _model;
	}
	
	/**
	 * Set pivot model associated with this control.
	 */
	public void setModel(OlapPivotModel model) {
		_model = model; // always reload
		syncModel();
	}
	
	/**
	 * Reload underlying Grids from current model.
	 */
	public void syncModel() {
		// sync fields
		syncModel(rlist, PivotField.Type.ROW);
		syncModel(clist, PivotField.Type.COLUMN);
		syncModel(dlist, PivotField.Type.DATA);
		syncModel(ulist, PivotField.Type.UNUSED);
	}
	
	@Override
	public void afterCompose() {
		loadLayout();
	}
	
	protected void syncModel(Grid grid, Type type) {
		grid.setModel(new ListModelList<PivotField>(getDisplayFields(type)));
	}
	
	protected PivotField[] getDisplayFields(Type type) {
		if (_model == null)
			return new PivotField[0];
		switch (type) {
		case COLUMN:
			return new PivotField[] { _model.getColumnField() };
		case ROW:
			return new PivotField[] { _model.getRowField() };
		case DATA:
		case UNUSED:
		default:
			return _model.getFields(type);
		}
	}
	
	protected void loadLayout() {
		setZclass("z-pivot-field-control");
		
		// clean up
		ulist = rlist = clist = dlist = null;
		clearChildren(this);
		
		// prepare args
		Map<String, Object> args = getLabels();
		
		// create components
		Executions.createComponents(LAYOUT_SQUARE_URI, this, args);
		ConventionWires.wireFellows(this, this);
		ConventionWires.addForwards(this, this);
		
		// init grids
		initGrid(rlist, Type.ROW);
		initGrid(clist, Type.COLUMN);
		initGrid(dlist, Type.DATA);
		initGrid(ulist, Type.UNUSED);
		
		syncModel();
	}
	
	protected void initGrid(Grid grid, Type type) {
		grid.setDroppable(getDragGroup(type));
		grid.setRowRenderer(FIELD_RENDERER);
		grid.addEventListener("onDrop", ITEM_DROP_LISTENER);
	}
	
	
	
	// event handling //
	protected final EventListener<Event> ITEM_DROP_LISTENER = 
			new EventListener<Event>() {
		public void onEvent(Event event) throws Exception {
			itemDrop(event);
		}
	};
	
	protected void itemDrop(Event event) {
		DropEvent devt = (DropEvent) event;
		Component target = event.getTarget();
		Row row = (Row) devt.getDragged();
		Grid drag = row.getGrid();
		boolean insert = target instanceof Row;
		Grid drop = insert ? ((Row) target).getGrid() : (Grid) target;
		
		if (drag == dlist && dlist.getRows().getChildren().size() == 1) {
			// at least one value field is required
			Messagebox.show("At least 1 data field is required!");
			return;
		}
		ListModelList<OlapPivotField<?>> dragModel = cast(drag.getModel());
		ListModelList<OlapPivotField<?>> dropModel = cast(drop.getModel());
		
		// automatically remove current column/row
		boolean rowOrColumnDrop = drop == rlist || drop == clist;
		if (rowOrColumnDrop)
			cast(ulist.getModel()).add(dropModel.remove(0));
		
		OlapPivotField<?> field = (OlapPivotField<?>) row.getValue();
		PivotField.Type oldType = field.getType();
		PivotField.Type type = getFieldType(drop);
		
		if (!rowOrColumnDrop && insert) {
			int index = target.getParent().getChildren().indexOf(target);
			dragModel.remove(field);
			dropModel.add(index, field);
		} else {
			dragModel.remove(field);
			dropModel.add(field);
		}
		_dirty = true;
		
		Event evt = new PivotFieldControlChangeEvent(
				OlapPivotFieldControl.this, field, oldType, type);
		Events.postEvent(evt);
	}
	
	@SuppressWarnings("unchecked")
	protected <T> ListModelList<OlapPivotField<?>> cast(ListModel<T> model) {
		return (ListModelList<OlapPivotField<?>>) model;
	}
	
	protected final RowRenderer<OlapPivotField<?>> FIELD_RENDERER = new RowRenderer<OlapPivotField<?>>() {
		@SuppressWarnings("unused")
		public void render(Row row, OlapPivotField<?> data) throws Exception {
			renderField(row, data);
		}
		public void render(Row row, OlapPivotField<?> data, int index) throws Exception {
			renderField(row, data);
		}
	};
	
	protected void renderField(Row row, OlapPivotField<?> data) {
		String group = getDragGroup(data);
		row.setDraggable(group);
		row.setDroppable(group);
		row.setValue(data);
		
		Div div = new Div();
		div.setSclass(data instanceof OlapMeasureField ? 
				"olap-pivot-measure-field" : "olap-pivot-hierarchy-field");
		row.appendChild(div);
		div.appendChild(new Label(data.getTitle()));
		
		row.addEventListener("onDrop", ITEM_DROP_LISTENER);
	}
	
	
	
	// layout definition //
	public static final String LAYOUT_SQUARE_URI = "~./zul/pivot/pfc-layout-s.zul";
	
	// label definition //
	public static final String LABEL_RES_PREFIX = "pivot.control.";
	
	protected final Map<String, Object> getLabels() {
		Map<String, Object> m = new HashMap<String, Object>();
		for (Map.Entry<String, String> e : getDefaultLabels().entrySet()) {
			String key = e.getKey();
			Object value = getAttribute(key);
			if (value == null)
				value = Labels.getLabel(LABEL_RES_PREFIX + key);
			m.put(key, value != null ? value : e.getValue());
		}
		return m;
	}
	
	/**
	 * Override this method to provide the default label list.
	 */
	protected Map<String, String> getDefaultLabels() {
		return LABELS;
	}
	
	protected static final Map<String, String> LABELS = 
		new HashMap<String, String>();
	
	static {
		LABELS.put("rowListTitle", "Rows:");
		LABELS.put("columnListTitle", "Columns:");
		LABELS.put("dataListTitle", "Measures:");
		LABELS.put("unusedListTitle", "Unused:");
	}
	
	
	
	// helper //
	protected static final void clearChildren(Component c) {
		for (Component c0; (c0 = c.getFirstChild()) != null;)
			c0.detach();
	}
	
	protected final PivotField.Type getFieldType(Grid grid) {
		return grid == rlist ? PivotField.Type.ROW :
				grid == clist ? PivotField.Type.COLUMN :
				grid == dlist ? PivotField.Type.DATA : PivotField.Type.UNUSED;
	}
	
	protected final String getDragGroup(Type type) {
		switch (type) {
		case ROW:
		case COLUMN:
			return DRAG_GROUP_H;
		case DATA:
			return DRAG_GROUP_M;
		case UNUSED:
		default:
			return DRAG_GROUP_ALL;
		}
	}
	
	protected final String getDragGroup(OlapPivotField<?> field) {
		return field instanceof OlapMeasureField ? DRAG_GROUP_M : DRAG_GROUP_H;
	}
	
	protected static final String DRAG_GROUP_M = "_OLAP_PVT_FIELD_CONTROL_MEASURE_";
	protected static final String DRAG_GROUP_H = "_OLAP_PVT_FIELD_CONTROL_HIERARCHY_";
	protected static final String DRAG_GROUP_ALL = DRAG_GROUP_M + ", " + DRAG_GROUP_H;
	
}
