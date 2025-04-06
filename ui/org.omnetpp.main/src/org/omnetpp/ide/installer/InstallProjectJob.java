package org.omnetpp.ide.installer;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.omnetpp.ide.OmnetppMainPlugin;

/**
 * This class wraps the project installation task into a job.
 *
 * @author levy
 */
public class InstallProjectJob extends Job {
    protected URL projectDescriptionURL;
    protected ProjectInstallationOptions projectInstallationOptions;

    public InstallProjectJob(URL projectDescriptionURL, ProjectInstallationOptions projectInstallationOptions) {
        super("Installing Project");
        this.projectDescriptionURL = projectDescriptionURL;
        this.projectInstallationOptions = projectInstallationOptions;
    }

    @Override
    protected IStatus run(IProgressMonitor progressMonitor) {
        try {
            InstallProjectTask installProjectTask = new InstallProjectTask(projectDescriptionURL, projectInstallationOptions);
            installProjectTask.run(progressMonitor);
            return Status.OK_STATUS;
        }
        catch (OperationCanceledException e) {
            return Status.CANCEL_STATUS;
        }
        catch (CoreException e) {
            return e.getStatus();
        }
        catch (Exception e) {
            return new Status(IStatus.ERROR, OmnetppMainPlugin.PLUGIN_ID, e.getMessage(), e.getCause());
        }
    }
}