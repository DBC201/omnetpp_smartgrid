/*--------------------------------------------------------------*
  Copyright (C) 2006-2020 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omnetpp.scave.model.Chart.ChartType;
import org.omnetpp.scave.model.Chart.DialogPage;

/**
 * Represents a chart template.
 */
public class ChartTemplate {
    private String id;
    private String name;
    private String description;
    private ChartType chartType;
    private String iconPath;
    private int supportedResultTypes; // a bitwise OR of the constants in ResultFileManager
    private String pythonScript;
    private List<DialogPage> dialogPages;
    private int score = 0;
    private String menuIconPath;
    private Map<String,String> properties;
    private String originFolder; // workspace path or plugin-relative path (latter starts with "plugin:")
    private boolean builtin;

    public ChartTemplate(String id, String name, String description, ChartType chartType, String iconPath, int supportedResultTypes,
            String pythonScript, List<DialogPage> dialogPages, int score, String menuIconPath, Map<String,String> properties,
            String originFolder, boolean builtin) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.chartType = chartType;
        this.iconPath = iconPath;
        this.supportedResultTypes = supportedResultTypes;
        this.pythonScript = pythonScript;
        this.dialogPages = dialogPages;
        this.score = score;
        this.menuIconPath = menuIconPath;
        this.properties = properties;
        this.originFolder = originFolder;
        this.builtin = builtin;
    }

    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Chart.ChartType getChartType() {
        return chartType;
    }

    public String getIconPath() {
        return iconPath;
    }

    public int getSupportedResultTypes() {
        return supportedResultTypes;
    }

    public String getPythonScript() {
        return pythonScript;
    }

    public List<DialogPage> getDialogPages() {
        return Collections.unmodifiableList(dialogPages);
    }

    public int getScore() {
        return score;
    }

    public String getMenuIconPath() {
        return menuIconPath;
    }

    public Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    public Map<String,String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public String getPropertyDefault(String name) {
        return properties.get(name);
    }

    public String getOriginFolder() {
        return originFolder;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    @Override
    public String toString() {
        return "'" + getName() + "' (id=" + getId() + ")";
    }

}
