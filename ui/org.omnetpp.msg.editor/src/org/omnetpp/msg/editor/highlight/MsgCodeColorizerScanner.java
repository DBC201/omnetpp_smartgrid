/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.msg.editor.highlight;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.NumberRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.WordRule;
import org.omnetpp.common.editor.text.SyntaxHighlightHelper;

/**
 * An MSG file code scanner.
 *
 * @author rhornig
 */
public class MsgCodeColorizerScanner extends RuleBasedScanner {

    /**
     * Creates a NED code scanner
     */
    public MsgCodeColorizerScanner() {

        setDefaultReturnToken(SyntaxHighlightHelper.codeDefaultToken);

        List<IRule> rules= new ArrayList<IRule>();

        // Add rule for strings
        rules.add(new SingleLineRule("\"", "\"", SyntaxHighlightHelper.codeStringToken, '\\'));

        // Add rule for detecting numeric constants
        rules.add(new NumberRule(SyntaxHighlightHelper.codeNumberToken));

        // detect properties (in the form "@propname")
        rules.add(new WordRule(new SyntaxHighlightHelper.NedAtWordDetector(), SyntaxHighlightHelper.codePropertyToken));

        // Add word rule for keywords, types, and constants. If not recognized as either of these
        // then this is a regular identifier, so return an identifierToken by default
        WordRule wordRule= new WordRule(new SyntaxHighlightHelper.NedWordDetector(), SyntaxHighlightHelper.codeIdentifierToken);
        for (int i= 0; i < SyntaxHighlightHelper.highlightMsgKeywords.length; i++)
            wordRule.addWord(SyntaxHighlightHelper.highlightMsgKeywords[i], SyntaxHighlightHelper.codeKeywordToken);
        for (int i= 0; i < SyntaxHighlightHelper.highlightMsgTypes.length; i++)
            wordRule.addWord(SyntaxHighlightHelper.highlightMsgTypes[i], SyntaxHighlightHelper.codeTypeToken);
        for (int i= 0; i < SyntaxHighlightHelper.highlightConstants.length; i++)
            wordRule.addWord(SyntaxHighlightHelper.highlightConstants[i], SyntaxHighlightHelper.codeStringToken);
        rules.add(wordRule);

        IRule[] result= new IRule[rules.size()];
        rules.toArray(result);
        setRules(result);
    }
}
