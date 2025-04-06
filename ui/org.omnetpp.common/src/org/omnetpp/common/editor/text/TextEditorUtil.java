/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.editor.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * TextEditor-related utility functions
 *
 * @author Andras
 */
public class TextEditorUtil {
    /**
     * Returns the document for the given text editor.
     */
    public static IDocument getDocument(ITextEditor editor) {
        return editor.getDocumentProvider().getDocument(editor.getEditorInput());
    }

    /**
     * Replaces the given region in the text editor's document, and optionally selects
     * the replaced region (if it's more than one line). Includes resetMarkerAnnotations().
     */
    public static void replaceRangeAndSelect(ITextEditor editor, int startOffset, int endOffset, String replacement, boolean selectUnlessOneLine) throws BadLocationException {
        // replace if differs
        IDocument doc = getDocument(editor);
        int selectionOffset = ((ITextSelection)editor.getSelectionProvider().getSelection()).getOffset();
        int selectionLength = ((ITextSelection)editor.getSelectionProvider().getSelection()).getLength();
        int oldLength =  endOffset-startOffset;
        String text = doc.get(startOffset, oldLength);
        if (!text.equals(replacement)) {
            doc.replace(startOffset, oldLength, replacement);
            endOffset = startOffset + replacement.length();
            resetMarkerAnnotations(editor);  // or markers will disappear from replaced region
        }

        // select it unless it's one line only
        if (selectUnlessOneLine && doc.getLineOfOffset(startOffset) != doc.getLineOfOffset(endOffset-1))
            editor.selectAndReveal(startOffset, replacement.length());
        else
            editor.selectAndReveal(selectionOffset +replacement.length()-oldLength, selectionLength);

    }

    /**
     * Needs to be called after modifying text editor contents via IDocument.replace(),
     * otherwise marker annotations will disappear from replaced regions.
     *
     * This function re-reads markers from the editor's underlying IFile, and synchronizes
     * them onto the marker annotations of the text editor.
     */
    public static void resetMarkerAnnotations(ITextEditor editor) {
        IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        if (model instanceof AbstractMarkerAnnotationModel) {
            AbstractMarkerAnnotationModel markerModel = (AbstractMarkerAnnotationModel) model;
            markerModel.resetMarkers();
        }
    }

    /**
     * Detects the boundary of a single word under the current position (defined by the wordDetector)
     */
    public static IRegion detectWordRegion(ITextViewer viewer, int documentOffset, IWordDetector wordDetector) throws BadLocationException {
        int offset = documentOffset;
        int length = 0;
        if (viewer.getDocument() == null)
            return new Region(documentOffset, 0);

        // find the first char that may not be the trailing part of a word.
        while (offset > 0 && wordDetector.isWordPart(viewer.getDocument().getChar(offset - 1)))
            offset--;

        // check if the first char of the word is also ok.
        if (offset > 0 && wordDetector.isWordStart(viewer.getDocument().getChar(offset - 1)))
            offset--;
        // now we should stand on the first char of the word
        if (offset + length < viewer.getDocument().getLength()
                && wordDetector.isWordStart(viewer.getDocument().getChar(offset + length)))
            length++;
        // now iterate through the rest of chars until a character cannot be recognized as an in/word char
        while(offset + length < viewer.getDocument().getLength()
                && wordDetector.isWordPart(viewer.getDocument().getChar(offset + length)))
            length++;

        return new Region(offset, length);
    }

    public static String get(ITextViewer viewer, IRegion region) throws BadLocationException {
        return viewer.getDocument().get(region.getOffset(), region.getLength());
    }

    public static String getWordRegion(ITextViewer viewer, int documentOffset, IWordDetector wordDetector) throws BadLocationException {
        return get(viewer, detectWordRegion(viewer, documentOffset, wordDetector));
    }
}
