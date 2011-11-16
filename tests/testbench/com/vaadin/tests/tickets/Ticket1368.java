package com.vaadin.tests.tickets;

import com.vaadin.Application;
import com.vaadin.tests.TestForTablesInitialColumnWidthLogicRendering;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Root;
import com.vaadin.ui.Table;

/**
 */
public class Ticket1368 extends Application.LegacyApplication {

    private Table t;

    @Override
    public void init() {

        final Root mainWin = new Root("Test app to #1368");
        setMainWindow(mainWin);

        t = TestForTablesInitialColumnWidthLogicRendering.getTestTable(3, 5);

        mainWin.addComponent(t);

        ComboBox addColumn = new ComboBox();
        addColumn.setImmediate(true);
        addColumn.setNewItemsAllowed(true);
        addColumn.setNewItemHandler(new ComboBox.NewItemHandler() {
            public void addNewItem(String newItemCaption) {
                t.addContainerProperty(newItemCaption, String.class, "-");
            }
        });
        mainWin.addComponent(addColumn);

    }
}