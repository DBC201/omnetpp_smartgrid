/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.inifile.editor.text;


import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.hyperlink.MultipleHyperlinkPresenter;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.RGB;
import org.omnetpp.inifile.editor.editors.InifileEditorData;
import org.omnetpp.inifile.editor.text.assist.InifileCompletionProcessor;
import org.omnetpp.inifile.editor.text.highlight.InifileCodeColorizerScanner;
import org.omnetpp.inifile.editor.text.highlight.InifileCommentColorizerScanner;
import org.omnetpp.inifile.editor.text.highlight.InifileSyntaxHighlightPartitionScanner;
import org.omnetpp.inifile.editor.text.util.InifileHyperlinkDetector;
import org.omnetpp.inifile.editor.text.util.InifileTextHover;

/**
 * Configuration for a SourceViewer which shows an inifile.
 */
public class InifileSourceViewerConfiguration extends SourceViewerConfiguration {
    private InifileEditorData editorData;

    public InifileSourceViewerConfiguration(InifileEditorData editorData) {
        this.editorData = editorData;
    }

    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new DefaultAnnotationHover();
    }

    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        return null;
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

        ContentAssistant assistant = new ContentAssistant();
        //assistant.setDocumentPartitioning(...);
        assistant.setContentAssistProcessor(new InifileCompletionProcessor(editorData), IDocument.DEFAULT_CONTENT_TYPE);

        assistant.enableAutoActivation(true);
        assistant.setAutoActivationDelay(500);
        assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
        assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

        return assistant;
    }

    @Override
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
        return new DefaultTextDoubleClickStrategy();
    }

    @Override
    public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
        return new String[] { "\t", "    " };
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

        PresentationReconciler reconciler= new PresentationReconciler();
        // syntax highlighting is using a separate partitioner
        reconciler.setDocumentPartitioning(InifileSyntaxHighlightPartitionScanner.PARTITIONING_ID);

        // colorizers for inifile code
        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(new InifileCodeColorizerScanner()) {
            @Override
            public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent e, boolean documentPartitioningChanged) {
                // Note: the default getDamageRegion() returns the line of change, so syntax highlighting
                // begins from the start of the line. That is not enough for string constants broken
                // onto multiple lines using backslash line continuation, because the string colorizer
                // scanner doesn't know that the start of the line is already inside the string.
                // The solution is to start scanning at an earlier point (before the open quote),
                // i.e. at the start of the partition.
                return partition;
            }
        };
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        // colorizer for comments
        dr = new DefaultDamagerRepairer(new InifileCommentColorizerScanner());
        reconciler.setDamager(dr, InifileSyntaxHighlightPartitionScanner.INI_COMMENT);
        reconciler.setRepairer(dr, InifileSyntaxHighlightPartitionScanner.INI_COMMENT);

        return reconciler;
    }

    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return new InifileTextHover(editorData);
    }

    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        // Installs background NED parsing.
        // Based on: JavaSourceViewerConfiguration.getReconciler() in JDT which
        // creates and configures JavaReconciler; that in turn will eventually
        // result in calls to org.eclipse.jdt.internal.compiler.parser.Parser.
        MonoReconciler reconciler = new MonoReconciler(new InifileReconcileStrategy(editorData), true);
        reconciler.setIsIncrementalReconciler(false);
        reconciler.setIsAllowedToModifyDocument(false);
        reconciler.setProgressMonitor(new NullProgressMonitor());
        reconciler.setDelay(500);
        return reconciler;
    }

    @Override
    public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
        return new IHyperlinkDetector[] {
            new URLHyperlinkDetector(),
            new InifileHyperlinkDetector(editorData),
            //FIXME new NEDHyperlinkDetector()
        };
    }

    @Override
    public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
        return new MultipleHyperlinkPresenter(new RGB(0, 0, 255));
    }


}
