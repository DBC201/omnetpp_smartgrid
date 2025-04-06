/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ide.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

/*
 * A page used as a filler for nodes in the property page dialog
 * for which no page is supplied.
 */
public class EmptyPropertyPage extends PropertyPage {
    protected Control createContents(Composite parent) {
        return new Composite(parent, SWT.NONE);
    }
}
