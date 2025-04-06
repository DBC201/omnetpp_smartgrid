/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.builder;

import static org.omnetpp.scave.common.IndexFileUtils.getVectorFileFor;
import static org.omnetpp.scave.common.IndexFileUtils.isExistingVectorFile;
import static org.omnetpp.scave.common.IndexFileUtils.isIndexFile;
import static org.omnetpp.scave.common.IndexFileUtils.isIndexFileUpToDate;
import static org.omnetpp.scave.common.IndexFileUtils.performIndexing;

import java.io.File;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

public class VectorFileIndexBuilder extends IncrementalProjectBuilder {

    private Queue<IFile> filesToBeIndexed = new ConcurrentLinkedQueue<IFile>();

    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
            throws CoreException {
        if (kind == IncrementalProjectBuilder.FULL_BUILD) {
             fullBuild(monitor);
          }
          else {
             IResourceDelta delta = getDelta(getProject());
             if (delta == null) {
                fullBuild(monitor);
             }
             else {
                incrementalBuild(delta, monitor);
             }
          }
          return null;
    }

    protected void fullBuild(IProgressMonitor monitor) throws CoreException {
        // collect files to be indexed
        getProject().accept(new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                if (resource instanceof IFile) {
                    IFile file = (IFile)resource;
                    if (toBeIndexed(file))
                        filesToBeIndexed.offer(file);
                    return false;
                }
                else
                    return true;
            }
        });
        // generate index files
        doBuild(monitor);
    }

    protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor)
        throws CoreException {
        // add files to be indexed to the queue
        delta.accept(new IResourceDeltaVisitor() {
            public boolean visit(IResourceDelta delta) {
                if (delta.getResource() instanceof IFile) {
                    IFile file = (IFile)delta.getResource();
                    switch (delta.getKind()) {
                    case IResourceDelta.ADDED:
                    case IResourceDelta.CHANGED:
                        if (toBeIndexed(file))
                            filesToBeIndexed.offer(file);
                        break;
                    case IResourceDelta.REMOVED:
                        if (isIndexFile(file)) {
                            IFile vectorFile = getVectorFileFor(file);
                            if (vectorFile != null)
                                filesToBeIndexed.offer(vectorFile);
                        }
                    }
                }
                return true;
            }
        });
        // generate index files
        doBuild(monitor);
    }

    protected void doBuild(IProgressMonitor monitor) {
        try {
            monitor.beginTask("Indexing vector files", filesToBeIndexed.size());

            IFile file;
            while ((file = filesToBeIndexed.peek()) != null) {
                if (monitor.isCanceled())
                    throw new OperationCanceledException();
                if (isInterrupted())
                    break;

                file = filesToBeIndexed.poll();
                monitor.subTask("Indexing "+file.getName());
                try {
                    File path = file.getLocation().toFile();
                    if (path.exists() && isExistingVectorFile(file) && !isIndexFileUpToDate(file))
                    {
                        IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
                        performIndexing(file, subMonitor);
                        if (subMonitor.isCanceled())
                            throw new OperationCanceledException();
                    }
                }
                catch (Exception e) {
                    Activator.logError(e);
                    monitor.worked(1);
                }
            }
        }
        finally {
            monitor.done();
        }
    }

    protected boolean toBeIndexed(IFile file) {
        if (isExistingVectorFile(file) && !file.getParent().isDerived()) {
            return !isIndexFileUpToDate(file);
        }
        else
            return false;
    }
}
