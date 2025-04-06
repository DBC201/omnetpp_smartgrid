/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.eventlog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.omnetpp.common.util.BigDecimal;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.eventlog.IEvent;
import org.omnetpp.eventlog.IEventLog;
import org.omnetpp.eventlog.engine.Int64Vector;
import org.omnetpp.eventlog.engine.IntVector;
import org.omnetpp.eventlog.engine.LongVector;
import org.omnetpp.eventlog.engine.StringVector;

public class EventLogFilterParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient EventLogInput eventLogInput;

    public boolean enableCollectionLimits;

    public boolean enableRangeFilter;

    public boolean enableEventNumberRangeFilter;

    public boolean enableSimulationTimeRangeFilter;

    public boolean enableEventFilter;

    public boolean enableExcludedEventNumberFilter;

    public boolean enableModuleFilter;

    public boolean enableModuleExpressionFilter;

    public boolean enableModuleNEDTypeNameFilter;

    public boolean enableModuleNameFilter;

    public boolean enableModuleIdFilter;

    public boolean enableMessageFilter;

    public boolean enableMessageExpressionFilter;

    public boolean enableMessageClassNameFilter;

    public boolean enableMessageNameFilter;

    public boolean enableMessageIdFilter;

    public boolean enableMessageTreeIdFilter;

    public boolean enableMessageEncapsulationIdFilter;

    public boolean enableMessageEncapsulationTreeIdFilter;

    public boolean enableTraceFilter;

    public long lowerEventNumberLimit = -1;

    public long upperEventNumberLimit = -1;

    public EnabledLong[] excludedEventNumbers;

    public BigDecimal lowerSimulationTimeLimit;

    public BigDecimal upperSimulationTimeLimit;

    public long tracedEventNumber = -1;

    public boolean traceCauses = true;

    public boolean traceConsequences = true;

    public boolean traceMessageReuses = true;

    public boolean traceSelfMessages = true;

    public int causeEventNumberDelta = 1000;

    public int consequenceEventNumberDelta = 1000;

    public BigDecimal causeSimulationTimeDelta;

    public BigDecimal consequenceSimulationTimeDelta;

    public String moduleFilterExpression;

    public String[] moduleNEDTypeNames;

    public int[] moduleIds;

    public int[] moduleNameIds;

    public String[] messageClassNames;

    public String[] messageNames;

    public String messageFilterExpression;

    public EnabledLong[] messageIds;

    public EnabledLong[] messageTreeIds;

    public EnabledLong[] messageEncapsulationIds;

    public EnabledLong[] messageEncapsulationTreeIds;

    public boolean collectMessageReuses = true;

    public int maximumNumberOfMessageDependencies = 5;

    public int maximumDepthOfMessageDependencies = 15;

    public int maximumMessageDependencyCollectionTime = 100;

    public static class EnabledInt implements Serializable {
        private static final long serialVersionUID = 1L;

        public EnabledInt(boolean enabled, int value) {
            this.enabled = enabled;
            this.value = value;
        }

        public boolean enabled;

        public int value;
    }

    public static class EnabledLong implements Serializable {
        private static final long serialVersionUID = 1L;

        public EnabledLong(boolean enabled, long value) {
            this.enabled = enabled;
            this.value = value;
        }

        public boolean enabled;

        public long value;
    }

    public EventLogFilterParameters(EventLogInput eventLogInput) {
        this.eventLogInput = eventLogInput;
    }

    public void setEventLogInput(EventLogInput eventLogInput) {
        this.eventLogInput = eventLogInput;
    }

    public boolean isAnyEventFilterEnabled() {
        return enableEventNumberRangeFilter || enableSimulationTimeRangeFilter;
    }

    public boolean isAnyMessageFilterEnabled() {
        return enableMessageFilter &&
            (enableMessageExpressionFilter ||
             enableMessageClassNameFilter ||
             enableMessageNameFilter ||
             enableMessageIdFilter ||
             enableMessageTreeIdFilter ||
             enableMessageEncapsulationIdFilter ||
             enableMessageEncapsulationTreeIdFilter);
    }

    public boolean isAnyModuleFilterEnabled() {
        return enableModuleFilter &&
            (enableModuleExpressionFilter ||
             enableModuleNEDTypeNameFilter ||
             enableModuleNameFilter ||
             enableModuleIdFilter);
    }

    public long getFirstEventNumber() {
        IEventLog eventLog = eventLogInput.getEventLog();

        long lowerTraceEventNumberLimit1 = -1;
        if (enableTraceFilter && tracedEventNumber != -1 && causeEventNumberDelta != -1)
            lowerTraceEventNumberLimit1 = tracedEventNumber - causeEventNumberDelta;

        long lowerTraceEventNumberLimit2 = -1;
        if (enableTraceFilter && tracedEventNumber != -1 && causeSimulationTimeDelta != null) {
            IEvent event = eventLog.getEventForEventNumber(tracedEventNumber);
            event = eventLog.getLastEventNotAfterSimulationTime(new BigDecimal(event.getSimulationTime().doubleValue() - causeSimulationTimeDelta.doubleValue()));

            if (event != null)
                lowerTraceEventNumberLimit2 = event.getEventNumber();
        }

        long lowerEventNumberForSimulationTimeLimit = -1;
        if (enableSimulationTimeRangeFilter && lowerSimulationTimeLimit != null) {
            IEvent event = eventLog.getLastEventNotAfterSimulationTime(new BigDecimal(lowerSimulationTimeLimit.toPlainString()));

            if (event != null)
                lowerEventNumberForSimulationTimeLimit = event.getEventNumber();
        }

        long lowerEventNumberLimit = -1;
        if (enableEventNumberRangeFilter && this.lowerEventNumberLimit != -1)
            lowerEventNumberLimit = this.lowerEventNumberLimit;

        return Math.max(Math.max(lowerEventNumberLimit, lowerEventNumberForSimulationTimeLimit),
                        Math.max(lowerTraceEventNumberLimit1, lowerTraceEventNumberLimit2));
    }

    public long getLastEventNumber() {
        IEventLog eventLog = eventLogInput.getEventLog();

        long upperTraceEventNumberLimit1 = Long.MAX_VALUE;
        if (enableTraceFilter && tracedEventNumber != -1 && consequenceEventNumberDelta != -1)
            upperTraceEventNumberLimit1 = tracedEventNumber + consequenceEventNumberDelta;

        long upperTraceEventNumberLimit2 = Long.MAX_VALUE;
        if (enableTraceFilter && tracedEventNumber != -1 && consequenceSimulationTimeDelta != null) {
            IEvent event = eventLog.getEventForEventNumber(tracedEventNumber);
            event = eventLog.getFirstEventNotBeforeSimulationTime(new BigDecimal(event.getSimulationTime().doubleValue() + consequenceSimulationTimeDelta.doubleValue()));

            if (event != null)
                upperTraceEventNumberLimit2 = event.getEventNumber();
        }

        long upperEventNumberForSimulationTimeLimit = Long.MAX_VALUE;
        if (enableSimulationTimeRangeFilter && upperSimulationTimeLimit != null) {
            IEvent event = eventLog.getFirstEventNotBeforeSimulationTime(new BigDecimal(upperSimulationTimeLimit.toPlainString()));

            if (event != null)
                upperEventNumberForSimulationTimeLimit = event.getEventNumber();
        }

        long upperEventNumberLimit = Long.MAX_VALUE;
        if (enableEventNumberRangeFilter && this.upperEventNumberLimit != -1)
            upperEventNumberLimit = this.upperEventNumberLimit;

        long limit = Math.min(Math.min(upperEventNumberLimit, upperEventNumberForSimulationTimeLimit),
                             Math.min(upperTraceEventNumberLimit1, upperTraceEventNumberLimit2));

        if (limit == Long.MAX_VALUE)
            return -1;
        else
            return limit;
    }

    public List<Long> getExcludedEventNumbers() {
        if (enableExcludedEventNumberFilter)
            return createLongVector(excludedEventNumbers);
        else
            return new ArrayList<Long>();
    }

    public List<Integer> getModuleIds() {
        ArrayList<Integer> vector = new ArrayList<Integer>();

        if (enableModuleNameFilter)
            for (int id : moduleNameIds)
                vector.add(id);

        if (enableModuleIdFilter)
            for (int id : moduleIds)
                vector.add(id);

        return vector;
    }

    public List<String> getModuleNEDTypeNames() {
        return Arrays.asList(moduleNEDTypeNames);
    }

    public List<String> getMessageClassNames() {
        return Arrays.asList(messageClassNames);
    }

    public List<String> getMessageNames() {
        return Arrays.asList(messageNames);
    }

    public List<Long> getSelectedMessageIds() {
        if (enableMessageIdFilter)
            return createLongVector(messageIds);
        else
            return new ArrayList<Long>();
    }

    public List<Long> getSelectedMessageTreeIds() {
        if (enableMessageTreeIdFilter)
            return createLongVector(messageTreeIds);
        else
            return new ArrayList<Long>();
    }

    public List<Long> getSelectedMessageEncapsulationIds() {
        if (enableMessageEncapsulationIdFilter)
            return createLongVector(messageEncapsulationIds);
        else
            return new ArrayList<Long>();
    }

    public List<Long> getSelectedMessageEcapsulationTreeIds() {
        if (enableMessageEncapsulationTreeIdFilter)
            return createLongVector(messageEncapsulationTreeIds);
        else
            return new ArrayList<Long>();
    }

    public IntVector createIntVector(int[] ids) {
        IntVector vector = new IntVector();

        for (int id : ids)
            vector.add(id);

        return vector;
    }

    public List<Long> createLongVector(EnabledLong[] ids) {
        ArrayList<Long> vector = new ArrayList<Long>();

        for (EnabledLong id : ids)
            if (id.enabled)
                vector.add(id.value);

        return vector;
    }

    public Int64Vector createInt64Vector(EnabledLong[] ids) {
        Int64Vector vector = new Int64Vector();

        for (EnabledLong id : ids)
            if (id.enabled)
                vector.add(id.value);

        return vector;
    }

    @Override
    public String toString() {
        ArrayList<String> filters = new ArrayList<String>();

        if (enableRangeFilter) {
            ArrayList<String> rangeFilters = new ArrayList<String>();

            if (enableEventNumberRangeFilter) {
                if (lowerEventNumberLimit != -1 && upperEventNumberLimit != -1)
                    rangeFilters.add(lowerEventNumberLimit + " <= event number <= " + upperEventNumberLimit);
                else if (lowerEventNumberLimit != -1)
                    rangeFilters.add("event number >= " + lowerEventNumberLimit);
                else if (upperEventNumberLimit != -1)
                    rangeFilters.add("event number <= " + upperEventNumberLimit);
            }

            if (enableSimulationTimeRangeFilter) {
                if (lowerSimulationTimeLimit != null && upperSimulationTimeLimit != null)
                    rangeFilters.add(lowerSimulationTimeLimit + " <= simulation time of event <= " + upperSimulationTimeLimit);
                else if (lowerSimulationTimeLimit != null)
                    rangeFilters.add("simulation time of event >= " + lowerSimulationTimeLimit);
                else if (upperSimulationTimeLimit != null)
                    rangeFilters.add("simulation time of event <= " + upperSimulationTimeLimit);
            }

            filters.add(combineDesriptions("OR", rangeFilters));
        }

        if (enableEventFilter) {
            if (enableExcludedEventNumberFilter) {
                ArrayList<String> excludedEventNumberFilters = new ArrayList<String>();
                addMemberDescription(excludedEventNumberFilters, "event number", excludedEventNumbers);
                filters.add("NOT " + combineDesriptions("OR", excludedEventNumberFilters));
            }
        }

        if (enableModuleFilter) {
            ArrayList<String> moduleFilters = new ArrayList<String>();

            if (enableModuleExpressionFilter)
                moduleFilters.add("module matches the expression " + moduleFilterExpression);

            if (enableModuleNEDTypeNameFilter)
                addMemberDescription(moduleFilters, "module NED type", moduleNEDTypeNames);

            if (enableModuleNameFilter) {
                ModuleTreeItem root = eventLogInput.getModuleTreeRoot();
                String[] moduleNames = new String[moduleNameIds.length];

                for (int i = 0; i< moduleNames.length; i++) {
                    ModuleTreeItem item = root.findDescendantModule(moduleNameIds[i]);

                    if (item != null)
                        moduleNames[i] = item.getModuleFullPath();
                }
                // TODO: use names instead of IDs
                addMemberDescription(moduleFilters, "module name", moduleNames);
            }

            if (enableModuleIdFilter)
                addMemberDescription(moduleFilters, "module ID", moduleIds);

            filters.add(combineDesriptions("OR", moduleFilters));
        }

        if (enableMessageFilter) {
            ArrayList<String> messageFilters = new ArrayList<String>();

            if (enableMessageExpressionFilter)
                messageFilters.add("message matches the expression " + messageFilterExpression);

            if (enableMessageClassNameFilter)
                addMemberDescription(messageFilters, "message C++ class name", messageClassNames);

            if (enableMessageNameFilter)
                addMemberDescription(messageFilters, "message name", messageNames);

            if (enableMessageIdFilter)
                addMemberDescription(messageFilters, "message ID", messageIds);

            if (enableMessageTreeIdFilter)
                addMemberDescription(messageFilters, "message tree ID", messageTreeIds);

            if (enableMessageEncapsulationIdFilter)
                addMemberDescription(messageFilters, "message encapsulation ID", messageEncapsulationIds);

            if (enableMessageEncapsulationTreeIdFilter)
                addMemberDescription(messageFilters, "message encapsulation tree ID", messageEncapsulationTreeIds);

            filters.add(combineDesriptions("OR", messageFilters));
        }

        if (enableTraceFilter) {
            ArrayList<String> traceFilters = new ArrayList<String>();

            if (traceCauses)
                traceFilters.add("cause");

            if (traceConsequences)
                traceFilters.add("consequence");

            filters.add("it is the " + StringUtils.join(traceFilters, " or ") + " of the event with event number " + tracedEventNumber);

            // TODO: should these included in the filter description?
            // if (traceMessageReuses) {
            // }
            // if (traceSelfMessages) {
            // }
            // causeEventNumberDelta;
            // consequenceEventNumberDelta;
            // causeSimulationTimeDelta;
            // consequenceSimulationTimeDelta;
        }

        return combineDesriptions("AND", filters, "Filter for all events", "Filter for events where ");
    }

    private String combineDesriptions(String operator, ArrayList<String> descriptions) {
        return combineDesriptions(operator, descriptions, "", "");
    }

    private String combineDesriptions(String operator, ArrayList<String> descriptions, String zeroDescriptionCountValue, String nonZeroDescriptionCountPrefix) {
        String result = null;

        if (descriptions.size() == 0)
            result = zeroDescriptionCountValue;
        else {
            result = nonZeroDescriptionCountPrefix;

            if (descriptions.size() == 1)
                result += descriptions.get(0);
            else
                result += "(" + StringUtils.join(descriptions, ") " + operator + " (") + ")";
        }

        return result;
    }

    private void addMemberDescription(ArrayList<String> descriptions, String prefix, int[] elements) {
        if (elements != null)
            addMemberDescription(descriptions, prefix, Arrays.asList(ArrayUtils.toObject(elements)));
    }

    private void addMemberDescription(ArrayList<String> descriptions, String prefix, EnabledInt[] elements) {
        if (elements != null) {
            ArrayList<Integer> enabledElements = new ArrayList<Integer>();

            for (EnabledInt element : elements)
                if (element.enabled)
                    enabledElements.add(element.value);

            addMemberDescription(descriptions, prefix, enabledElements);
        }
    }

    private void addMemberDescription(ArrayList<String> descriptions, String prefix, EnabledLong[] elements) {
        if (elements != null) {
            ArrayList<Long> enabledElements = new ArrayList<Long>();

            for (EnabledLong element : elements)
                if (element.enabled)
                    enabledElements.add(element.value);

            addMemberDescription(descriptions, prefix, enabledElements);
        }
    }

    private void addMemberDescription(ArrayList<String> descriptions, String prefix, String[] elements) {
        if (elements != null)
            addMemberDescription(descriptions, prefix, Arrays.asList(elements));
    }

    private void addMemberDescription(ArrayList<String> descriptions, String prefix, List<?> elements) {
        if (elements == null || elements.size() == 0)
            return;
        else if (elements.size() == 1)
            descriptions.add(prefix + " = " + elements.get(0));
        else if (elements.size() <= 3)
            descriptions.add(prefix + " is in (" + StringUtils.join(elements, ", ") + ")");
        else
            descriptions.add(prefix + " is in (" + StringUtils.join(elements.subList(0, 3), ", ") + ", ...)");
    }
}
