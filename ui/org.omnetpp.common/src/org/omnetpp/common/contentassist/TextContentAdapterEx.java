/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.contentassist;

import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * Implementation of <code>IControlContentAdapterEx</code> for text widgets.
 */
public class TextContentAdapterEx extends TextContentAdapter implements IControlContentAdapterEx {

    public void replaceControlContents(Control control, int start, int end, String text, int cursorPosition) {
        Text textControl = (Text)control;
        textControl.setSelection(start, end);
        textControl.insert(text);
        textControl.setSelection(start + cursorPosition, start + cursorPosition);
    }
}
