//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.ui;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.gwt.ui.SmartTable;

/**
 * Extends smart table with the ability to name rows.
 */
public class NamedRowSmartTable extends SmartTable
{
    /**
     * Creates a new table with no style and default cell padding and spacing.
     */
    public NamedRowSmartTable ()
    {
        super();
    }

    /**
     * Creates a new table with no style and the given cell padding and spacing.
     */
    public NamedRowSmartTable (int cellPadding, int cellSpacing)
    {
        super(cellPadding, cellSpacing);
    }

    /**
     * Creates a new table with the given style, cell padding and spacing.
     */
    public NamedRowSmartTable (String styleName, int cellPadding, int cellSpacing)
    {
        super(styleName, cellPadding, cellSpacing);
    }

    /**
     * Associates a name with a numerical row. If rows before the given row are inserted or
     * deleted, the associated is updated.
     */
    public void nameRow (String name, int row)
    {
        _namedRows.put(name, row);
    }

    /**
     * Retrieves the associated numerical row value for a previous call to
     * {@link #nameRow(String, int)}, or -1 if there is no association.
     */
    public int getNamedRow (String name)
    {
        Integer row = _namedRows.get(name);
        if (row == null) {
            return -1;
        }
        return row;
    }

    /**
     * Retrieves a cell mutator object for the given row name and column, or null if there is no
     * association of a row for the given name.
     */
    public CellMutator cell (String rowName, int col)
    {
        int row = getNamedRow(rowName);
        if (row == -1) {
            throw new RuntimeException("Named row " + rowName + " not found");
        }
        return cell(row, col);
    }

    @Override // from FlexTable
    public void removeAllRows ()
    {
        super.removeAllRows();
        _namedRows.clear();
    }

    @Override // from HTMLTable
    public int insertRow (int beforeRow)
    {
        int added = super.insertRow(beforeRow);
        for (Map.Entry<String, Integer> entry : _namedRows.entrySet()) {
            int row = entry.getValue();
            if (row >= beforeRow) {
                entry.setValue(row + 1);
            }
        }
        return added;
    }

    @Override // from HTMLTable
    public void removeRow (int delete)
    {
        super.removeRow(delete);
        for (Iterator<Map.Entry<String, Integer>> iter = _namedRows.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, Integer> entry = iter.next();
            int row = entry.getValue();
            if (row == delete) {
                iter.remove();
            } else if (row > delete) {
                entry.setValue(row - 1);
            }
        }
    }

    protected Map<String, Integer> _namedRows = Maps.newHashMap();
}
