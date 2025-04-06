/*--------------------------------------------------------------*
  Copyright (C) 2006-2020 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.omnetpp.common.ui.TimeTriggeredProgressMonitorDialog2;
import org.omnetpp.scave.model.Chart;

/**
 * Provides the Python script of a Chart object as a Document for text editors.
 * Is used to open Charts in ChartScriptEditor, which is based on PyEdit.
 *
 * Also has a basic AnnotationModel so PyDev can mark syntax errors, and we can
 * put error markers on lines of the script when catching runtime exceptions.
 */
class ChartScriptDocumentProvider extends AbstractDocumentProvider {

    /** The document containing the edited version of the chart script */
    private IDocument doc = null;

    /** Stores the error markers put on the script */
    public ProjectionAnnotationModel annotationModel = new ProjectionAnnotationModel();

    @Override
    protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
        return new TimeTriggeredProgressMonitorDialog2(Display.getCurrent().getActiveShell(), 1000);
    }

    @Override
    public boolean isModifiable(Object element) {
        return true;
    }

    @Override
    public boolean isReadOnly(Object element) {
        return false;
    }

    @Override
    public boolean canSaveDocument(Object element) {
        return true;
    }

    @Override
    protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite)
            throws CoreException {
        // this is not used, should not be called
    }

    @Override
    protected IDocument createDocument(Object element) throws CoreException {
        return getDocument(element);
    }

    @Override
    public IDocument getDocument(Object element) {
        Chart chart = ((ChartScriptEditorInput) element).getChart();

        if (doc == null)
            doc = new Document(chart.getScript());

        return doc;
    }

    @Override
    protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
        return annotationModel;
    }
}