/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.eventlog;

public interface IEventLogChangeListener {
    /**
     * Called when new content has been appended to the event log file by the simulation runtime environment.
     */
    public void eventLogAppended();

    /**
     * Called when the event log file content has been overwritten by the simulation runtime environment.
     */
    public void eventLogOverwritten();

    /**
     * Called when the event log has been filtered by the user.
     */
    public void eventLogFiltered();

    /**
     * Called when the event log filter has been removed by the user.
     */
    public void eventLogFilterRemoved();

    /**
     * Called when a long running event log operation starts just before the progress monitor is shown.
     */
    public void eventLogLongOperationStarted();

    /**
     * Called when a long running event log operation either normally or abnormally ends (by pressing cancel).
     */
    public void eventLogLongOperationEnded();

    /**
     * Called when a long running event log operation has done some progress.
     */
    public void eventLogProgress();

    /**
     * Called when synchronization fails due to file changes caused by the simulation runtime environment.
     */
    public void eventLogSynchronizationFailed();
}
