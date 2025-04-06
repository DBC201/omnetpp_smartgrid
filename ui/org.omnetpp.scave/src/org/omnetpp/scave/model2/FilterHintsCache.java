/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.model2;

import static org.omnetpp.scave.model2.FilterField.FILE;
import static org.omnetpp.scave.model2.FilterField.ISFIELD;
import static org.omnetpp.scave.model2.FilterField.MODULE;
import static org.omnetpp.scave.model2.FilterField.NAME;
import static org.omnetpp.scave.model2.FilterField.RUN;
import static org.omnetpp.scave.model2.FilterField.TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.omnetpp.common.engine.PatternMatcher;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.ResultFileList;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engine.RunList;
import org.omnetpp.scave.engine.Scave;
import org.omnetpp.scave.model2.FilterField.Kind;

/**
 * Given an IDList, produces hints for different filter fields such as
 * module name, statistic name, run attributes etc.
 *
 * @author tomi, andras
 */
public class FilterHintsCache {
    //TODO cache hints for more than one IDList
    private ResultFileManager lastManager = null;
    private IDList lastIdList = null;
    private RunList cachedRunList = null;
    private Map<FilterField.Kind,String[]> cachedNameHints = new HashMap<>();
    private Map<FilterField,String[]> cachedValueHints = new HashMap<>();

    public FilterHintsCache() {
    }

    public String[] getValueHints(ResultFileManager manager, IDList idlist, FilterField field, String prefix) {
        return filter(getValueHints(manager, idlist, field), prefix);
    }

    public String[] getValueHints(ResultFileManager manager, IDList idlist, FilterField field) {
        setCachedIDList(manager, idlist);

        if (!cachedValueHints.containsKey(field))
            cachedValueHints.put(field,  computeValueHints(manager, idlist, field));

        return cachedValueHints.get(field);
    }

    public String[] getNameHints(ResultFileManager manager, IDList idlist, FilterField.Kind kind, String prefix) {
        return filter(getNameHints(manager, idlist, kind), prefix);
    }

    public String[] getNameHints(ResultFileManager manager, IDList idlist, FilterField.Kind kind) {
        setCachedIDList(manager, idlist);

        if (!cachedNameHints.containsKey(kind))
            cachedNameHints.put(kind,  computeNameHints(manager, idlist, kind));

        return cachedNameHints.get(kind);
    }

    protected void setCachedIDList(ResultFileManager manager, IDList idlist) {
        if (manager != lastManager || !idlist.equals(lastIdList)) {
            cachedNameHints.clear();
            cachedValueHints.clear();
            cachedRunList = null;
            lastManager = manager;
            lastIdList = idlist;
        }
    }

    public static String[] computeValueHints(ResultFileManager manager, IDList idlist, FilterField field) {
        if (field.equals(RUN)) {
            RunList runList = manager.getUniqueRuns(idlist);
            return manager.getRunNameFilterHints(runList).toArray();
        }
        else if (field.equals(FILE)) {
            ResultFileList fileList = manager.getUniqueFiles(idlist);
            return manager.getFilePathFilterHints(fileList).toArray();
        }
        else if (field.equals(MODULE)) {
            return manager.getModuleFilterHints(idlist).toArray();
        }
        else if (field.equals(TYPE)) {
            return new String [] {Scave.PARAMETER, Scave.SCALAR, Scave.VECTOR, Scave.STATISTICS, Scave.HISTOGRAM };
        }
        else if (field.equals(NAME)) {
            return manager.getResultNameFilterHints(idlist).toArray();
        }
        else if (field.equals(ISFIELD)) {
            return new String [] { Scave.TRUE, Scave.FALSE };
        }
        else if (field.getKind() == Kind.ItemField) {
            return manager.getResultItemAttributeFilterHints(idlist, field.getName()).toArray();
        }
        else if (field.getKind() == Kind.RunAttribute) {
            RunList runList = manager.getUniqueRuns(idlist);
            return manager.getRunAttributeFilterHints(runList, field.getName()).toArray();
        }
        else if (field.getKind() == Kind.IterationVariable) {
            RunList runList = manager.getUniqueRuns(idlist);
            return manager.getIterationVariableFilterHints(runList, field.getName()).toArray();
        }
        else if (field.getKind() == Kind.Config) {
            RunList runList = manager.getUniqueRuns(idlist);
            return manager.getConfigEntryFilterHints(runList, field.getName()).toArray();
        }
        else if (field.getKind() == Kind.ResultAttribute) {
            return manager.getResultItemAttributeFilterHints(idlist, field.getName()).toArray();
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public static String[] computeNameHints(ResultFileManager manager, IDList idlist, FilterField.Kind kind) {
        RunList runList = manager.getUniqueRuns(idlist);
        switch (kind) {
        case ItemField: return new String[] {Scave.TYPE, Scave.FILE, Scave.RUN, Scave.MODULE, Scave.NAME, Scave.ISFIELD};
        case RunAttribute: return manager.getUniqueRunAttributeNames(runList).keys().toArray();
        case IterationVariable: return manager.getUniqueIterationVariableNames(runList).keys().toArray();
        case ResultAttribute: return manager.getUniqueResultAttributeNames(idlist).keys().toArray();
        case Config: return manager.getUniqueConfigKeys(runList).keys().toArray();
        default: throw new IllegalArgumentException();
        }
    }

    public static String[] filter(String[] v, String prefix) {
        if (prefix == null || prefix.isEmpty())
            return v;

        // if prefix is a complete value: return all values, but make prefix the first
        int index = ArrayUtils.indexOf(v, prefix);
        if (index >= 0) {
            String[] result = Arrays.copyOf(v, v.length);
            for (int i = index; i > 0; --i)
                result[i] = result[i-1];
            result[0] = prefix;
            return result;
        }

        PatternMatcher prefixAsPattern = new PatternMatcher(prefix, true, false /*=substring*/, false /*=ignorecase*/);
        List<String> tmp = new ArrayList<>();
        for (String s : v)
            if (prefixAsPattern.matches(s))
                tmp.add(s);
        return tmp.toArray(new String[]{});
    }

    public RunList getRunList() {
        return cachedRunList;
    }
}
