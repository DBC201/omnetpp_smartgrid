/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.cdt.build;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.omnetpp.cdt.Activator;
import org.omnetpp.common.util.FileUtils;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.common.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Represents contents of the OMNeT++ build specification file
 * @author Andras
 */
public class BuildSpecification implements Cloneable {
    private static final String TYPE_ATTR = "type";
    public static final String BUILDSPEC_FILENAME = ".oppbuildspec";
    private static final String BUILDSPEC_ELEMENTNAME = "buildspec";
    private static final String DIR_ELEMENTNAME = "dir";
    private static final String PATH_ATTR = "path";
    private static final String MAKEMAKEOPTIONS_ATTR = "makemake-options";

    // Makefile types for a folder
    public static final int NONE = 0;
    public static final int CUSTOM = 1;
    public static final int MAKEMAKE = 2;

    private static class FolderSpec {
        int makefileType; // CUSTOM or MAKEMAKE (NONE's are not stored)
        MakemakeOptions makemakeOptions; // if type==MAKEMAKE, otherwise null
    }

    private IProject project;
    private Map<IContainer,FolderSpec> folderSpecs = new HashMap<IContainer,FolderSpec>();

    protected BuildSpecification(IProject project) {
        this.project = project;
    }

    @Override
    public BuildSpecification clone() {
        BuildSpecification result = new BuildSpecification(project);
        for (IContainer folder : folderSpecs.keySet()) {
            FolderSpec tmp = new FolderSpec();
            tmp.makefileType = folderSpecs.get(folder).makefileType;
            if (tmp.makefileType == MAKEMAKE)
                tmp.makemakeOptions = folderSpecs.get(folder).makemakeOptions.clone();
            result.folderSpecs.put(folder, tmp);
        }
        return result;
    }

    /**
     * Returns the list of folders where there's a makefile, either generated
     * or custom one. To add/remove folders, use setFolderMakeType().
     *
     */
    public List<IContainer> getMakeFolders() {
        return new ArrayList<IContainer>(folderSpecs.keySet());
    }

    /**
     * Returns the make type (MAKEMAKE, CUSTOM, or NONE) for the given folder.
     */
    public int getFolderMakeType(IContainer folder) {
        return !folderSpecs.containsKey(folder) ? NONE : folderSpecs.get(folder).makefileType;
    }

    /**
     * Sets the make type (MAKEMAKE, CUSTOM, or NONE) for the given folder.
     * If you specify NONE, that will remove the folder from the list of folders
     * returned by getMakeFolders().
     */
    public void setFolderMakeType(IContainer folder, int type) {
        if (type==NONE)
            folderSpecs.remove(folder);
        else {
            FolderSpec folderSpec = folderSpecs.get(folder);
            if (folderSpec == null)
                folderSpecs.put(folder, folderSpec = new FolderSpec());
            folderSpec.makefileType = type;
            if (type == MAKEMAKE && folderSpec.makemakeOptions == null)
                folderSpec.makemakeOptions = MakemakeOptions.createWithDefaultSettings();
        }
    }

    /**
     * Returns the set of folders where a makefile should be generated.
     */
    public List<IContainer> getMakemakeFolders() {
        List<IContainer> result = new ArrayList<IContainer>();
        for (IContainer folder : folderSpecs.keySet())
            if (folderSpecs.get(folder).makefileType == MAKEMAKE)
                result.add(folder);
        return result;
    }

    /**
     * Returns the options for the given folder, or null if the folder is not a makemake folder.
     */
    public MakemakeOptions getMakemakeOptions(IContainer folder) {
        FolderSpec folderSpec = folderSpecs.get(folder);
        if (folderSpec == null || folderSpec.makefileType != MAKEMAKE)
            return null;
        Assert.isNotNull(folderSpec.makemakeOptions);
        return folderSpec.makemakeOptions;
    }

    /**
     * Overwrites the makemake options for the given folder. The folder must already
     * be of type MAKEMAKE, else IllegalArgumentException is thrown.
     */
    public void setMakemakeOptions(IContainer folder, MakemakeOptions options) {
        FolderSpec folderSpec = folderSpecs.get(folder);
        if (options == null || folderSpec == null || folderSpec.makefileType != MAKEMAKE)
            throw new IllegalArgumentException(folder+" is not a makemake folder");
        folderSpec.makemakeOptions = options;
    }

    public static BuildSpecification createBlank(IProject project) {
        return new BuildSpecification(project);
    }

    public static BuildSpecification createInitial(IProject project) {
        // add project root immediately
        BuildSpecification buildSpec = new BuildSpecification(project);
        buildSpec.setFolderMakeType(project, MAKEMAKE);
        buildSpec.setMakemakeOptions(project, MakemakeOptions.createWithDefaultSettings());
        return buildSpec;
    }

    /**
     * Reads the build spec file from the given OMNeT++ project. Returns null
     * if there is no build spec file in the project.
     *
     * @throws CoreException on I/O errors and build spec file errors (syntax, invalid option etc)
     */
    public static BuildSpecification readBuildSpecFile(IProject project) throws CoreException {
        IFile buildSpecFile = project.getFile(BUILDSPEC_FILENAME);
        if (!buildSpecFile.exists()) {
            return null;
        }
        else {
            try {
                if (!project.getWorkspace().isTreeLocked())
                    buildSpecFile.refreshLocal(IResource.DEPTH_ZERO, null);
                if (buildSpecFile.getContents().read()!='<')
                    return readBuildSpecFileOld(project, FileUtils.readTextFile(buildSpecFile.getContents(),null));
                BuildSpecification buildSpec = new BuildSpecification(project);
                DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = docBuilder.parse(buildSpecFile.getContents());
                Element root = doc.getDocumentElement();
                NodeList dirs = root.getElementsByTagName(DIR_ELEMENTNAME);
                for (int i=0; i<dirs.getLength(); i++) {
                    Element dir = (Element)dirs.item(i);
                    String path = dir.getAttribute(PATH_ATTR);
                    IContainer folder = path.equals(".") ? project : project.getFolder(new Path(path));

                    FolderSpec folderSpec = new FolderSpec();
                    folderSpec.makefileType = dir.getAttribute(TYPE_ATTR).equals("makemake") ? MAKEMAKE : CUSTOM;
                    String args = dir.getAttribute(MAKEMAKEOPTIONS_ATTR);
                    if (!StringUtils.isEmpty(args)) {
                        folderSpec.makemakeOptions = MakemakeOptions.parse(args);
                        if (!folderSpec.makemakeOptions.getParseErrors().isEmpty())
                            throw Activator.wrapIntoCoreException(new MakemakeException(buildSpecFile.getFullPath() + ": " + folderSpec.makemakeOptions.getParseErrors().get(0)));
                    }
                    if (folderSpec.makefileType == MAKEMAKE && folderSpec.makemakeOptions == null)
                        folderSpec.makemakeOptions = MakemakeOptions.createWithDefaultSettings();
                    buildSpec.folderSpecs.put(folder, folderSpec);
                }
                return buildSpec;
            }
            catch (Exception e) { // catches ParserConfigurationException, SAXException, IOException
                throw Activator.wrapIntoCoreException("Cannot parse "+buildSpecFile.getFullPath(), e);
            }
        }
    }

    //TODO XXX remove this method in 4.b9
    public static BuildSpecification readBuildSpecFileOld(IProject project, String contents) throws CoreException {
        BuildSpecification buildSpec = new BuildSpecification(project);
        for (String line : StringUtils.splitToLines(contents)) {
            line = line.trim();
            if (line.equals("") || line.startsWith("#"))
                continue;

            if (line.startsWith("version ")) {
                // ignore
            }
            else {
                Matcher matcher = Pattern.compile("(.*?):(.*)").matcher(line);
                if (matcher.matches()) {
                    String folderPath = matcher.group(1).trim();
                    String args = matcher.group(2).trim();

                    IContainer folder = folderPath.equals(".") ? project : project.getFolder(new Path(folderPath));
                    if (!StringUtils.isEmpty(args)) {
                        MakemakeOptions makemakeOptions = MakemakeOptions.parse(args);
                        if (!makemakeOptions.getParseErrors().isEmpty())
                            throw Activator.wrapIntoCoreException(new MakemakeException(BUILDSPEC_FILENAME + ": " + makemakeOptions.getParseErrors().get(0)));
                        FolderSpec folderSpec = new FolderSpec();
                        folderSpec.makefileType = MAKEMAKE;
                        folderSpec.makemakeOptions = makemakeOptions;
                        buildSpec.folderSpecs.put(folder, folderSpec);
                    }
                }
                else {
                    String message = BUILDSPEC_FILENAME + ": invalid line syntax: " + line;
                    throw Activator.wrapIntoCoreException(new RuntimeException(message));
                }
            }
        }
        return buildSpec;
    }

    /**
     * Saves the build spec to file.
     */
    public void save() throws CoreException {
        try {
            // build DOM tree
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element root = doc.createElement(BUILDSPEC_ELEMENTNAME);
            root.setAttribute("version", "4.0");
            doc.appendChild(root);

            for (IContainer folder : folderSpecs.keySet()) {
                FolderSpec folderSpec = folderSpecs.get(folder);
                Element dirElement = doc.createElement(DIR_ELEMENTNAME);
                String projectRelativePath = folder.equals(project) ? "." : folder.getProjectRelativePath().toString();
                dirElement.setAttribute(PATH_ATTR, projectRelativePath);
                dirElement.setAttribute(TYPE_ATTR, folderSpec.makefileType == MAKEMAKE ? "makemake" : "custom");
                if (folderSpec.makefileType == MAKEMAKE && folderSpec.makemakeOptions != null)
                    dirElement.setAttribute(MAKEMAKEOPTIONS_ATTR, folderSpec.makemakeOptions.toString());
                root.appendChild(dirElement);
            }

            // save it
            IFile buildSpecFile = project.getFile(BUILDSPEC_FILENAME);
            String content = XmlUtils.serialize(doc);
            if (!buildSpecFile.exists())
                buildSpecFile.create(new ByteArrayInputStream(content.getBytes()), IFile.FORCE, null);
            else
                buildSpecFile.setContents(new ByteArrayInputStream(content.getBytes()), IFile.FORCE, null);
        }
        catch (Exception e) {  // catches: ParserConfigurationException,ClassCastException,ClassNotFoundException,InstantiationException,IllegalAccessException
            throw Activator.wrapIntoCoreException("Cannot save build specification: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        String result = project.getName() + " { ";
        for (IContainer folder: folderSpecs.keySet()) {
            FolderSpec folderSpec = folderSpecs.get(folder);
            int type = folderSpec.makefileType;
            result += folder.getFullPath().toString() + ":" + (type==NONE ? "NONE" : type==MAKEMAKE ? "MAKEMAKE" : "CUSTOM");
            result += " ";
        }
        result += "}";
        return result;
    }
}
