/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.misc;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * A popup menu that blocks the execution until an item is selected or the menu is cancelled.
 *
 * @author rhornig
 */
public class BlockingMenu extends Menu {
    MenuItem selectedMenuItem = null;

    public BlockingMenu(Control parent) {
        super(parent);
    }

    public BlockingMenu(Decorations parent, int style) {
        super(parent, style);
    }

    public BlockingMenu(Menu parentMenu) {
        super(parentMenu);
    }

    public BlockingMenu(MenuItem parentItem) {
        super(parentItem);
    }

    /**
     * Opens a Menu and blocks the execution so it waits until selection is done.
     * Returns the selected MenuItem, or null if the menu was cancelled.
     */
    public MenuItem open() {
        setVisible(true);
        // block until a selection is done or the menu is cancelled
        while (!isDisposed () && isVisible ()) {
            if (!getDisplay().readAndDispatch()) getDisplay().sleep();
        }

//        Debug.println("returning menuitem: "+selectedMenuItem);
        return selectedMenuItem;
    }

    /**
     * Creates a new MenuItem with the given SWT style, and adds it to the menu.
     */
    public MenuItem addMenuItem(int style) {
        MenuItem mi = new MenuItem(this, style);
        mi.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                selectedMenuItem = (MenuItem)e.widget;
//              Debug.println("menuitem selected1: "+selectedMenuItem);
            }

            public void widgetSelected(SelectionEvent e) {
                selectedMenuItem = (MenuItem)e.widget;
//                Debug.println("menuitem selected2: "+selectedMenuItem);
            }

        });
        return mi;
    }

    // override with empty implementation to allow subclassing of SWT controls
    @Override
    protected void checkSubclass () {
    }
}
