/*
 * 
 */
package org.zkoss.pivot.lab.olap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olap4j.Axis;
import org.olap4j.CellSet;
import org.olap4j.CellSetAxis;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.OlapStatement;
import org.olap4j.Position;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Measure;
import org.zkoss.pivot.Calculator;
import org.zkoss.pivot.PivotField;
import org.zkoss.pivot.PivotHeaderNode;
import org.zkoss.pivot.PivotField.Type;
import org.zkoss.pivot.PivotModelExt;
import org.zkoss.pivot.event.FieldDataEvent;
import org.zkoss.pivot.event.FieldDataListener;
import org.zkoss.pivot.impl.AbstractPivotModel;
import org.zkoss.pivot.lab.olap.OlapPivotHeaderTree.OlapNodeEventListener;
import org.zkoss.pivot.lab.olap.OlapUtil.Handler;

/**
 * 
 * @author simonpai
 */
public class OlapPivotModel extends AbstractPivotModel implements PivotModelExt {
	
	private static final long serialVersionUID = 1L;
	
	private final OlapConnectionFactory _factory;
	private Cube _cube;
	private final CubeCache _cache;
	private boolean _schemeDirty, _dataDirty;
	private OlapPivotHeaderTree _rowTree, _colTree;
	
	private boolean _debug; // debug mode
	
	// fields //
	private final List<OlapPivotField<?>> _fields = 
			new ArrayList<OlapPivotField<?>>();
	// for now, allow exactly one hierarchy on row/column
	private OlapHierarchyField _rowField, _colField;
	// only measure fields can be data fields
	private final List<OlapMeasureField> _dataFields = 
			new ArrayList<OlapMeasureField>();
	private final List<OlapPivotField<?>> _unusedFields = 
			new ArrayList<OlapPivotField<?>>();
	
	private final Map<OlapPivotField<?>, FieldDataListener> _fieldListeners = 
			new HashMap<OlapPivotField<?>, FieldDataListener>();
	
	/**
	 * 
	 * @param factory
	 * @param cubeName
	 */
	public OlapPivotModel(OlapConnectionFactory factory, String cubeName) {
		_factory = factory;
		_cache = new CubeCache();
		initFields(cubeName);
	}
	
	public void setDebug(boolean debug) {
		_debug = debug;
	}
	
	private void initFields(final String cubeName) {
		
		_rowField = _colField = null;
		_dataFields.clear();
		_unusedFields.clear();
		
		OlapUtil.runConnection(_factory, new Handler<OlapConnection>() {
			public void handle(OlapConnection conn) throws Exception {
				_cube = conn.getOlapSchema().getCubes().get(cubeName);
				
				OlapPivotField<?> f;
				// load available hierarchies, excluding [Measures]
				for (Hierarchy h : _cube.getHierarchies()) {
					if ("[Measures]".equals(h.getDimension().getUniqueName()))
						continue;
					_unusedFields.add(f = new OlapHierarchyField(h));
					_fields.add(f);
					f.setType(Type.UNUSED);
				}
				// load available measures
				for (Measure m : _cube.getMeasures()) {
					_unusedFields.add(f = new OlapMeasureField(m));
					_fields.add(f);
					f.setType(Type.UNUSED);
				}
				
				for (OlapPivotField<?> opf : _fields) {
					FieldDataListener listener = new FieldDataListener() {
						public void onChange(FieldDataEvent event) {
							OlapPivotModel.this._schemeDirty = true;
							OlapPivotModel.this.fireEvent();
						}
					};
					_fieldListeners.put(opf, listener);
					opf.addFieldDataListener(listener);
				}
			}
		});
		
		_schemeDirty = true;
	}
	
	public OlapPivotField<?> getField(String uniqueName, Type type) {
		for (OlapPivotField<?> f : type == null ? getFields() : getFields(type))
			if (uniqueName.equals(f.getElementUniqueName()))
				return f;
		return null;
	}
	
	@Override
	public OlapPivotField<?>[] getFields(Type type) {
		// need to return all level fields on row/column for rendering
		switch (type) {
		case DATA:
			return _dataFields.toArray(new OlapMeasureField[0]);
		case ROW:
			return getDisplayFields(_rowField);
		case COLUMN:
			return getDisplayFields(_colField);
		case UNUSED:
		default:
			return _unusedFields.toArray(new OlapPivotField<?>[0]);
		}
	}
	
	public OlapPivotField<?>[] getDisplayFields(OlapHierarchyField f) {
		return f.getLevelFields().toArray(new OlapPivotField<?>[0]);
	}
	
	@Override
	public OlapPivotField<?>[] getFields() {
		return _fields.toArray(new OlapPivotField<?>[0]);
	}
	
	public OlapHierarchyField getRowField() {
		return _rowField;
	}
	
	public OlapHierarchyField getColumnField() {
		return _colField;
	}
	
	public void setFieldType(String uniqueName, Type type) {
		setFieldType(getField(uniqueName, null), type);
	}
	
	public void setFieldType(String uniqueName, Type type, int index) {
		setFieldType(getField(uniqueName, null), type, index);
	}
	
	@Override
	public void setFieldType(PivotField field, Type type) {
		setFieldType0(field, type, -1);
		fireEvent();
	}
	
	@Override
	public void setFieldType(PivotField field, Type type, int index) {
		setFieldType0(field, type, index);
		fireEvent();
	}
	
	private void setFieldType0(PivotField field, PivotField.Type type, int index) {
		
		if (field == null)
			throw new IllegalArgumentException("Field cannot be null.");
		
		OlapPivotField<?> opf = cast(field);
		if (type == null)
			type = Type.UNUSED;
		
		switch (type) {
		case DATA:
			if (!(opf instanceof OlapMeasureField))
				throw new IllegalArgumentException("Data field should be a measure field: " + opf);
			break;
		case ROW:
		case COLUMN:
			if (!(opf instanceof OlapHierarchyField))
				throw new IllegalArgumentException("Column field should be a hierarchy field: " + opf);
			break;
		}
		
		switch (opf.getType()) {
		case DATA:
			_dataFields.remove(opf);
			break;
		case ROW:
			_rowField = null;
			break;
		case COLUMN:
			_colField = null;
			break;
		case UNUSED:
		default:
			_unusedFields.remove(opf);
		}
		
		opf.setType(type);
		
		switch (type) {
		case DATA:
			if (index < 0)
				_dataFields.add((OlapMeasureField) opf);
			else
				_dataFields.add(index, (OlapMeasureField) opf);
			break;
		case ROW:
			_rowField = (OlapHierarchyField) opf;
			break;
		case COLUMN:
			_colField = (OlapHierarchyField) opf;
			break;
		case UNUSED:
		default:
			if (index < 0)
				_unusedFields.add(opf);
			else
				_unusedFields.add(index, opf);
		}
		
		_schemeDirty = true;
	}
	
	public static final Calculator DEF = new Calculator() {
		public String getLabel() { return "[Default]"; }
		public String getLabelKey() { return null; }
	};
	
	/*package*/ static final Calculator[] _CAL_ARRAY = new Calculator[] { DEF };
	
	@Override
	public void setFieldSummary(PivotField field, Calculator summary) {
		// degenerated, do nothing
	}
	
	@Override
	public void setFieldSubtotals(PivotField field, Calculator[] subtotals) {
		// degenerated, do nothing
	}
	
	@Override
	public Calculator[] getSupportedCalculators() { return _CAL_ARRAY; }
	
	/* This is the member hierarchy query API of PivotModel.
	 */
	@Override
	public OlapPivotHeaderTree getRowHeaderTree() {
		updateSchema();
		return _rowTree;
	}
	
	/* This is the member hierarchy query API of PivotModel.
	 */
	@Override
	public OlapPivotHeaderTree getColumnHeaderTree() {
		updateSchema();
		return _colTree;
	}
	
	/* This is the major value query API of PivotModel.
	 */
	@Override
	public Number getValue(PivotHeaderNode rowNode, int rowCalIndex,
			PivotHeaderNode colNode, int colCalIndex, int dataIndex) {
		// ignore row/column calculator index
		OlapPivotHeaderNode oColNode = cast(colNode);
		OlapPivotHeaderNode oRowNode = cast(rowNode);
		if (oColNode.isRoot() || oRowNode.isRoot())
			return null; // ignore grand total
		Object v = _cache.get(oColNode, oRowNode)[dataIndex];
		return v instanceof Number ? (Number) v : null;
	}
	
	private void updateSchema() {
		/* Upon change of PivotModel schema, dirty flags are set. Row and column
		 * trees are recreated on demand.
		 */
		if (!_schemeDirty && !_dataDirty)
			return;
		
		_rowTree = new OlapPivotHeaderTree(this, _rowField, 
				new OlapNodeEventListener() {
			@Override
			public void onOpen(OlapPivotHeaderNode node, boolean open) {
				if (open && !node.isChildrenLoaded())
					_cache.openRowNode(node);
			}
		});
		_colTree = new OlapPivotHeaderTree(this, _colField, 
				new OlapNodeEventListener() {
			@Override
			public void onOpen(OlapPivotHeaderNode node, boolean open) {
				if (open && !node.isChildrenLoaded())
					_cache.openColumnNode(node);
			}
		});
		
		_cache.loadRoots();
		_schemeDirty = _dataDirty = false;
	}
	
	/* A cache storing values from OLAP service, which acquire new values using
	 * MDX query, upon node opening.
	 */
	protected class CubeCache {
		
		public void loadRoots() {
			final String mdx = buildMDX(null, null);
			if (_debug)
				System.out.println(mdx);
			OlapUtil.runStatement(_factory, new Handler<OlapStatement>() {
				public void handle(OlapStatement statement) throws Exception {
					loadCellSet(statement.executeOlapQuery(mdx));
				}
			});
		}
		
		public void openRowNode(final OlapPivotHeaderNode rowNode) {
			final String mdx = buildMDX(null, rowNode);
			if (_debug)
				System.out.println(mdx);
			OlapUtil.runStatement(_factory, new Handler<OlapStatement>() {
				public void handle(OlapStatement statement) throws Exception {
					final CellSet cells = statement.executeOlapQuery(mdx);
					// append new nodes to rowNode
					final CellSetAxis rowAxis = cells.getAxes().get(Axis.ROWS.axisOrdinal());
					for (Position p : rowAxis.getPositions())
						rowNode.append(p.getMembers().get(0)); // as we only allow 1 dimension on axis
					loadCellSet(cells);
				}
			});
			rowNode.setChildrenLoaded(true);
		}
		
		public void openColumnNode(final OlapPivotHeaderNode colNode) {
			final String mdx = buildMDX(colNode, null);
			if (_debug)
				System.out.println(mdx);
			OlapUtil.runStatement(_factory, new Handler<OlapStatement>() {
				public void handle(OlapStatement statement) throws Exception {
					final CellSet cells = statement.executeOlapQuery(mdx);
					// append new nodes to colNode
					final CellSetAxis colAxis = cells.getAxes().get(Axis.COLUMNS.axisOrdinal());
					for (Position p : colAxis.getPositions())
						colNode.append(p.getMembers().get(0)); // as we only allow 1 dimension on axis
					loadCellSet(cells);
				}
			});
			colNode.setChildrenLoaded(true);
		}
		
		protected void loadCellSet(CellSet cells) throws OlapException {
			CellSetAxis colAxis = cells.getAxes().get(Axis.COLUMNS.axisOrdinal());
			CellSetAxis rowAxis = cells.getAxes().get(Axis.ROWS.axisOrdinal());
			CellSetAxis dataAxis = cells.getAxes().get(Axis.PAGES.axisOrdinal());
			final int dataSize = dataAxis.getPositionCount();
			for (Position colPos : colAxis.getPositions()) {
				final OlapPivotHeaderNode colNode = _colTree.getNode(colPos);
				for (Position rowPos : rowAxis.getPositions()) {
					final Object[] values = new Object[dataSize];
					int i = 0;
					for (Position dataPos : dataAxis.getPositions())
						values[i++] = cells.getCell(colPos, rowPos, dataPos).getValue();
					put(colNode, _rowTree.getNode(rowPos), values);
				}
			}
		}
		
		protected final Map<String, Object[]> _data = new HashMap<String, Object[]>();
		protected void put(OlapPivotHeaderNode colNode, OlapPivotHeaderNode rowNode, Object[] values) {
			_data.put(hash(colNode, rowNode), values);
		}
		protected Object[] get(OlapPivotHeaderNode colNode, OlapPivotHeaderNode rowNode) {
			return _data.get(hash(colNode, rowNode));
		}
		protected String hash(OlapPivotHeaderNode colNode, OlapPivotHeaderNode rowNode) {
			return colNode.getHash() + "#" + rowNode.getHash();
		}
		public void clear() {
			_data.clear();
		}
		
		// MDX building //
		protected String buildMDX(OlapPivotHeaderNode colNode, OlapPivotHeaderNode rowNode) {
			final String colMDX = colNode == null ? 
					buildTreeMDX(_colTree) : buildNodeChildrenMDX(colNode);
			final String rowMDX = rowNode == null ? 
					buildTreeMDX(_rowTree) : buildNodeChildrenMDX(rowNode);
			return "SELECT " + colMDX + " ON COLUMNS, " + rowMDX + 
					" ON ROWS, {" + buildDataMDX() + "} ON PAGES" + 
					"\nFROM " + _cube.getName();
		}
		
		protected String buildTreeMDX(OlapPivotHeaderTree tree) {
			StringBuilder sb = new StringBuilder();
			addNodeMDX(sb, tree.getRoot());
			return "{" + sb.append('}').toString();
		}
		
		protected String buildNodeChildrenMDX(OlapPivotHeaderNode node) {
			return "{" + node.getMember().getUniqueName() + ".Children}";
		}
		
		protected void addNodeMDX(StringBuilder sb, OlapPivotHeaderNode node) {
			if (!node.isRoot()) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(node.getMember().getUniqueName());
			}
			if (node.isOpen())
				for (OlapPivotHeaderNode c : node.getChildren())
					addNodeMDX(sb, c);
		}
		
		protected String buildDataMDX() {
			StringBuilder sb = new StringBuilder();
			for (OlapMeasureField f : _dataFields) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(f.getMeasure().getUniqueName());
			}
			return sb.toString();
		}
		
	}
	
	// helper //
	protected OlapPivotField<?> cast(PivotField field) {
		if (!(field instanceof OlapPivotField) || !_fields.contains(field))
			throw new IllegalArgumentException(
					"The field does not belong to this PivotModel: " + 
					field.getFieldName());
		return (OlapPivotField<?>) field;
	}
	
	protected OlapPivotHeaderNode cast(PivotHeaderNode node) {
		if (!(node instanceof OlapPivotHeaderNode))
			throw new IllegalArgumentException(
					"The node does not belong to this model: " + node);
		return (OlapPivotHeaderNode) node;
	}
	
}
