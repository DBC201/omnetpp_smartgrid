/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common;

/**
 * View IDs, builder IDs and other UI constants.
 *
 * @author Andras
 */
public interface IConstants extends ICoreConstants {
    // whether this version is a COMMERCIAL build or not
    public static final boolean IS_COMMERCIAL = false;

    // The user facing product name
    public static final String PRODUCT_NAME = "OMNeT++";

    // fonts (use with JFaceResources.getFont(symbolicName); see JFaceResources for others)
    public static final String SMALL_FONT = "org.omnetpp.common.smallFont";

    // perspectives
    public static final String OMNETPP_PERSPECTIVE_ID = "org.omnetpp.main.OmnetppPerspective";
    public static final String SIMULATE_PERSPECTIVE_ID = "org.omnetpp.simulation.SimulationPerspective";

    // nature, builders
    public static final String OMNETPP_NATURE_ID = "org.omnetpp.main.omnetppnature";
    public static final String VECTORFILEINDEXER_BUILDER_ID = "org.omnetpp.scave.builder.vectorfileindexer";
    public static final String MAKEFILEBUILDER_BUILDER_ID = "org.omnetpp.cdt.MakefileBuilder";

    // editors
    public static final String SIMULATION_EDITOR_ID = "org.omnetpp.simulation.editors.SimulationEditor";

    // views
    public static final String NEW_VERSION_VIEW_ID = "org.omnetpp.main.NewVersionView";
    public static final String MODULEPARAMETERS_VIEW_ID = "org.omnetpp.inifile.ModuleParameters";
    public static final String MODULEHIERARCHY_VIEW_ID = "org.omnetpp.inifile.ModuleHierarchy";
    public static final String NEDINHERITANCE_VIEW_ID = "org.omnetpp.inifile.NedInheritance";
    public static final String VECTORBROWSER_VIEW_ID = "org.omnetpp.scave.VectorBrowserView";
    public static final String ANIMATION_VIEW_ID = "org.omnetpp.animation.editors.AnimationView";
    public static final String SEQUENCECHART_VIEW_ID = "org.omnetpp.sequencechart.editors.SequenceChartView";
    public static final String EVENTLOG_VIEW_ID = "org.omnetpp.eventlogtable.editors.EventLogTableView";
    public static final String SIMULATIONOBJECTTREE_VIEW_ID = "org.omnetpp.simulation.views.ObjectTreeView";
    public static final String MODULEOUTPUT_VIEW_ID = "org.omnetpp.simulation.views.ModuleOutputView";

    public static final String ID_CONSOLE_VIEW = "org.eclipse.ui.console.ConsoleView"; //from IConsoleConstants

    // wizards
    public static final String NEW_NEDFILE_WIZARD_ID = "org.omnetpp.ned.editor.wizards.NewNedFile";
    public static final String NEW_MSGFILE_WIZARD_ID = "org.omnetpp.msg.editor.wizards.NewMsgFile";
    public static final String NEW_INIFILE_WIZARD_ID = "org.omnetpp.inifile.editor.wizards.NewIniFile";
    public static final String NEW_SIMPLE_MODULE_WIZARD_ID = "org.omnetpp.ned.editor.wizards.NewSimpleModule";
    public static final String NEW_COMPOUND_MODULE_WIZARD_ID = "org.omnetpp.ned.editor.wizards.NewCompoundModule";
    public static final String NEW_NETWORK_WIZARD_ID = "org.omnetpp.ned.editor.wizards.NewNetwork";
    public static final String NEW_SIMULATION_WIZARD_ID = "org.omnetpp.ned.editor.wizards.NewSimulation";
    public static final String NEW_WIZARD_WIZARD_ID = "org.omnetpp.common.wizards.NewWizard";
    public static final String NEW_SCAVEFILE_WIZARD_ID = "org.omnetpp.scave.wizards.NewScaveFile";
    public static final String OMNETPP_EXPORT_WIZARD_ID = "org.omnetpp.common.wizards.ExportWizard";
    public static final String OMNETPP_IMPORT_WIZARD_ID = "org.omnetpp.common.wizards.ImportWizard";
    public static final String NEW_OMNETPP_PROJECT_WIZARD_ID = "org.omnetpp.main.wizards.NewOmnetppProject"; // not actually registered, because we could not solve that it appears only when CDT is not present
    public static final String NEW_OMNETPP_CC_PROJECT_WIZARD_ID = "org.omnetpp.cdt.wizards.NewOmnetppCCProject";
    public static final String NEW_OMNETPP_CLASS_WIZARD_ID = "org.omnetpp.cdt.wizards.NewOmnetppClass";

    // preference IDs
    public static final String PREF_OMNETPP_ROOT = "omnetppRoot";
    public static final String PREF_COPYRIGHT_LINE = "copyrightLine";
    public static final String PREF_DEFAULT_LICENSE = "defaultLicense";
    public static final String PREF_CUSTOM_LICENSE_HEADER = "customLicenseHeader";
    public static final String PREF_OMNETPP_IMAGE_PATH = "omnetppImagePath";
    public static final String PREF_DOXYGEN_EXECUTABLE = "doxygenExecutable";
    public static final String PREF_GRAPHVIZ_DOT_EXECUTABLE = "graphvizDotExecutable";

    // variable names
    public static final String VAR_NED_PATH = "opp_ned_path";
    public static final String VAR_NED_PACKAGE_EXCLUSIONS = "opp_ned_package_exclusions";
    public static final String VAR_SHARED_LIBS = "opp_shared_libs";
    public static final String VAR_LD_LIBRARY_PATH = "opp_ld_library_path";
    public static final String VAR_ADDITIONAL_PATH = "opp_additional_path"; // msys/bin, mingw/bin, etc
    public static final String VAR_IMAGE_PATH = "opp_image_path";
    public static final String VAR_OMNETPP_ROOT = "opp_root";
    public static final String VAR_OMNETPP_BIN_DIR = "opp_bin_dir";
    public static final String VAR_OMNETPP_INCL_DIR = "opp_incl_dir";
    public static final String VAR_OMNETPP_LIB_DIR = "opp_lib_dir";

}
