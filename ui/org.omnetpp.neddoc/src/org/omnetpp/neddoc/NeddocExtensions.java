package org.omnetpp.neddoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Gathers extension fragments that can be inserted into the generated documentation.
 * The extension file may contain several <docfragment> elements (in the root element).
 * Each <docfragment> must have an `anchor` attribute and one of the following
 * attributes: `nedtype`, `msgtype` or `filename` depending on what it wants to extend.
 *
 * nedtype: the fully qualified name of the NED type we want to extend.
 * Possible anchor names are: top, after-types, after-description, after-image,
 * after-diagrams, after-usage, after-inheritance, after-parameters, after-properties,
 * after-gates, after-signals, after-statistics, after-unassigned-parameters, bottom
 *
 * msgtype: the fully qualified name of the MSG type we want to extend.
 * Possible anchor names are: top, after-description, after-diagrams,
 * after-inheritance, after-fields, after-properties, bottom
 *
 * filename: The project relative file path and name of the file listing we want to extend.
 * Possible anchor values: top, after-types, bottom
 *
 * E.g.:
 *
 *  <docfragments>
 *     <docfragment nedtype="fully.qualified.NEDTypeName" anchor="after-signals">
 *        <![CDATA[
 *           after-signals
 *        ]]>
 *     </docfragment>
 *     <docfragment msgtype="fully.qualified.MSGType" anchor="after-diagrams">
 *        <![CDATA[
 *           after-diagrams
 *        ]]>
 *     </docfragment>
 *     <docfragment filename="src/inet/common/INETDefs.msg" anchor="after-types">
 *        <![CDATA[
 *           after-types
 *        ]]>
 *     </docfragment>
 *  </docfragments>
 *
 */
public class NeddocExtensions {
    private Map<String, Map<String, String>> nedExtensions = new HashMap<>();
    private Map<String, Map<String, String>> msgExtensions = new HashMap<>();
    private Map<String, Map<String, String>> fileExtensions = new HashMap<>();

    public enum ExtType { NED, MSG, FILE };

    public NeddocExtensions(IFile neddocExtensionsFile) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(neddocExtensionsFile.getContents());
            NodeList fragmentList = doc.getDocumentElement().getChildNodes();
            for(int i = 0; i < fragmentList.getLength(); i++) {
                Node element = fragmentList.item(i);
                if(element.getNodeType() == Node.ELEMENT_NODE) {
                    //TODO Rudolf: check element name is "docfragment"
                    var nedTypeAttribute = element.getAttributes().getNamedItem("nedtype");
                    String nedtype = nedTypeAttribute != null ? nedTypeAttribute.getNodeValue() : null;

                    var msgTypeAttribute = element.getAttributes().getNamedItem("msgtype");
                    String msgtype = msgTypeAttribute != null ? msgTypeAttribute.getNodeValue() : null;

                    var filenameAttribute = element.getAttributes().getNamedItem("filename");
                    String filename = filenameAttribute != null ? filenameAttribute.getNodeValue() : null;

                    var anchorAttribute = element.getAttributes().getNamedItem("anchor");
                    String anchor = anchorAttribute != null ? anchorAttribute.getNodeValue() : null;

                    String text = element.getTextContent();

                    Map<String, Map<String, String>> extensions = null;
                    String key = null;
                    if (nedtype != null) {
                        extensions = nedExtensions;
                        key = nedtype;
                    }
                    else if (msgtype != null) {
                        extensions = msgExtensions;
                        key = msgtype;
                    }
                    else if (filename != null) {
                        extensions = fileExtensions;
                        key = filename;
                    }
                    else {
                        // skip the element if neither of the nedtype, msgtype, filename attributes are present
                        //TODO Rudolf: warn user about useless element
                        continue;
                    }

                    Map<String, String> ext4location = extensions.getOrDefault(key, new HashMap<>());
                    extensions.put(key, ext4location);  // put extLocation back in case it was not yet present and was just created

                    ext4location.put(anchor, ext4location.getOrDefault(anchor, "") + text);
                }
            }
        }
        catch (SAXException | IOException | ParserConfigurationException | CoreException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFragment(ExtType type, String key, String anchor) {
        Map<String, String> exts4location =
                type == ExtType.NED ? nedExtensions.get(key) :
                type == ExtType.MSG ? msgExtensions.get(key) :
                type == ExtType.FILE ? fileExtensions.get(key) : null;
        return exts4location == null ? null : exts4location.get(anchor);
    }
}
