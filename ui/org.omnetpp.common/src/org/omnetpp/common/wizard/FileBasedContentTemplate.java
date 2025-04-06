package org.omnetpp.common.wizard;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.CommonPlugin;
import org.omnetpp.common.json.ExceptionErrorListener;
import org.omnetpp.common.json.JSONValidatingReader;
import org.omnetpp.common.util.FileUtils;
import org.omnetpp.common.util.ReflectionUtils;
import org.omnetpp.common.util.StringUtils;
import org.osgi.framework.Bundle;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.URLTemplateLoader;
import freemarker.core.Environment;
import freemarker.core.Environment.Namespace;
import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;

/**
 * Project template loaded from a workspace project or a resource bundle.
 * @author Andras
 */
public class FileBasedContentTemplate extends ContentTemplate {
    public static final String TEMPLATE_PROPERTIES_FILENAME = "template.properties";
    public static final String FILELIST_FILENAME = "filelist.txt"; // for URL loading
    public static final Image MISSING_IMAGE = ImageDescriptor.getMissingImageDescriptor().createImage();

    // property names:
    public static final String PROP_TEMPLATENAME = "templateName"; // template display name
    public static final String PROP_SUPPORTEDWIZARDTYPES = "supportedWizardTypes"; // list of items: "project", "simulation", "nedfile", "inifile", "network", etc
    public static final String PROP_TEMPLATEDESCRIPTION = "templateDescription"; // template description
    public static final String PROP_TEMPLATECATEGORY = "templateCategory"; // template category (parent tree node)
    public static final String PROP_TEMPLATEIMAGE = "templateImage"; // template icon name
    public static final String PROP_TEMPLATEISDEFAULT = "templateIsDefault"; // template is the default one or not
    public static final String PROP_IGNORERESOURCES = "ignoreResources"; // list of files NOT top copy into dest folder; basic glob patterns accepted
    public static final String PROP_VERBATIMFILES = "verbatimFiles"; // list of files to copy verbatim, even if they would be ignored otherwise; basic glob patterns accepted
    public static final String PROP_PRERUNTEMPLATE = "preRunTemplate"; // string: name of the FreeMarker template to run before other templates; this template WILL HAVE SIDE EFFECTS (can set variables, etc)

    // template is either given with an IFolder or with an URL
    protected IFolder templateFolder;
    protected URL templateUrl;
    protected Bundle bundleOfTemplate;
    protected boolean allowJarLoading = true;
    protected String[] fileList;
    protected Properties properties = new Properties();
    protected Set<String> supportedWizardTypes = new HashSet<String>();
    protected List<String> ignoreResourcePatterns = new ArrayList<String>();
    protected List<String> verbatimFilePatterns = new ArrayList<String>();
    private boolean imageAlreadyLoaded = false;

    /**
     * Currently only tests whether the given folder contains a template.properties file.
     */
    public static boolean looksLikeTemplateFolder(IFolder folder) {
        return folder.getFile(TEMPLATE_PROPERTIES_FILENAME).exists();
    }

    /**
     * Loads the template from the given subdirectory of the given plugin's folder.
     * The template loaded this way will be treated as a template loaded from an URL
     * (see constructor taking an URL).
     */
    public FileBasedContentTemplate(Plugin plugin, String templateFolder) throws CoreException {
        this(plugin.getBundle().getResource(templateFolder), plugin.getBundle());
    }

    /**
     * Loads the template from the given URL; equivalent to FileBasedContentTemplate(templateUrl, null).
     * Note that filelist.txt must be present in the folder that templateUrl points to.
     */
    public FileBasedContentTemplate(URL templateUrl) throws CoreException {
        this(templateUrl, null);
    }

    /**
     * Loads the template from the given URL. The URL should point to the folder
     * which contains the template.properties file.
     *
     * BundleOfTemplate may be null. If it is null, the folder should also contain
     * a filelist.txt, containing the list of the files in the template folder,
     * one per line. This is needed to overcome the Java limitation that contents of
     * a resource bundle (or URL) cannot be enumerated. If bundleOfTemplate is not null,
     * filelist.txt is not needed because we can use Bundle's methods to enumerate the files.
     */
    public FileBasedContentTemplate(URL templateUrl, Bundle bundleOfTemplate) throws CoreException {
        super();
        this.templateUrl = templateUrl;
        this.bundleOfTemplate = bundleOfTemplate;

        if (bundleOfTemplate != null)
            if (!templateUrl.getProtocol().contains("bundle") || !templateUrl.getHost().equals(bundleOfTemplate.getResource("/").getHost()))
                throw new IllegalArgumentException("Template URL " + templateUrl + " is not from the given OSGi bundle");

        properties = loadPropertiesFrom(openFile(TEMPLATE_PROPERTIES_FILENAME));

        // note: image will be loaded lazily, in getImage()
        setName(StringUtils.defaultIfEmpty(properties.getProperty(PROP_TEMPLATENAME), StringUtils.substringAfterLast(templateUrl.getPath(), "/")));
        String templateSource = (bundleOfTemplate != null) ? bundleOfTemplate.getSymbolicName() : templateUrl.toString();
        setDescription(StringUtils.defaultIfEmpty(properties.getProperty(PROP_TEMPLATEDESCRIPTION), "Template loaded from " + templateSource));
        setCategory(StringUtils.defaultIfEmpty(properties.getProperty(PROP_TEMPLATECATEGORY), null));
        setIsDefault("true".equals(properties.getProperty(PROP_TEMPLATEISDEFAULT)));

        // other initializations
        init();
    }

    /**
     * Loads the template from the given folder. This is a relatively cheap operation
     * (only the template.properties file is read), so it is OK for the wizard to
     * instantiate WorkspaceBasedContentTemplate for each candidate folder just to determine
     * whether it should be offered to the user.
     */
    public FileBasedContentTemplate(IFolder folder) throws CoreException {
        super();
        this.templateFolder = folder;

        properties = loadPropertiesFrom(openFile(TEMPLATE_PROPERTIES_FILENAME));

        // note: image will be loaded lazily, in getImage()
        setName(StringUtils.defaultIfEmpty(properties.getProperty(PROP_TEMPLATENAME), folder.getName()));
        String description = properties.getProperty(PROP_TEMPLATEDESCRIPTION);
        String extraInfo = "Template contributed by project \"" + folder.getProject().getName() + "\"";
        setDescription((StringUtils.isEmpty(description) ? "" : description+"<br><br>") + extraInfo);
        String defaultCategory = "Wizards from project \"" + folder.getProject().getName() + "\"";
        setCategory(StringUtils.defaultIfEmpty(properties.getProperty(PROP_TEMPLATECATEGORY), defaultCategory));

        init();
    }

    protected void init() {
        ignoreResourcePatterns.add("**/*.xswt");
        ignoreResourcePatterns.add("**/*.fti");  // note: "*.ftl" is NOT to be added! (or they'd be skipped altogether)
        ignoreResourcePatterns.add("**/*.jar");
        ignoreResourcePatterns.add("**/*.bak");
        ignoreResourcePatterns.add("**/*~");
        ignoreResourcePatterns.add("**/*~?");
        ignoreResourcePatterns.add("**/backups/");
        ignoreResourcePatterns.add(TEMPLATE_PROPERTIES_FILENAME);
        ignoreResourcePatterns.add(FILELIST_FILENAME);

        // the following options may not be modified via the wizard, so they are initialized here
        String[] labels = XSWTDataBinding.toStringArray(StringUtils.defaultString(properties.getProperty(PROP_SUPPORTEDWIZARDTYPES))," *, *");
        supportedWizardTypes.addAll(Arrays.asList(labels));

        for (String item : XSWTDataBinding.toStringArray(StringUtils.defaultString(properties.getProperty(PROP_IGNORERESOURCES))," *, *"))
            ignoreResourcePatterns.add(item);

        for (String item : XSWTDataBinding.toStringArray(StringUtils.defaultString(properties.getProperty(PROP_VERBATIMFILES))," *, *"))
            verbatimFilePatterns.add(item);
    }

    /**
     * Returns the workspace folder from which the template was loaded. Returns null
     * if the template was loaded from an URL.
     */
    public IFolder getTemplateFolder() {
        return templateFolder;
    }

    /**
     * Returns the URL folder from which the template was loaded. Returns null
     * if the template was loaded from a workspace folder.
     */
    public URL getTemplateUrl() {
        return templateUrl;
    }

    /**
     * Returns the values in the supportedWizardTypes property file entry.
     */
    public Set<String> getSupportedWizardTypes() {
        return supportedWizardTypes;
    }

    public boolean getAllowJarLoading() {
        return allowJarLoading;
    }

    public String getTemplateProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Sets whether to allow adding jars in the plugin directory into the CLASSPATH.
     * Must be invoked "early enough" to take effect.
     */
    public void setAllowJarLoading(boolean allowJarLoading) {
        this.allowJarLoading = allowJarLoading;
    }

    public String getIdentifierString() {
        return templateFolder==null ? templateUrl.toString() : templateFolder.getFullPath().toString();
    }

    /**
     * Overridden to provide lazy loading.
     */
    @Override
    public Image getImage() {
        if (!imageAlreadyLoaded) {
            imageAlreadyLoaded = true;
            String imageFileName = properties.getProperty(PROP_TEMPLATEIMAGE);
            if (imageFileName != null) {
                ignoreResourcePatterns.add(imageFileName);
                try {
                    ImageRegistry imageRegistry = CommonPlugin.getDefault().getImageRegistry();
                    String key = asURL(imageFileName).toString();
                    Image image = imageRegistry.get(key);
                    if (image == null)
                        imageRegistry.put(key, image = new Image(Display.getDefault(), openFile(imageFileName)));
                    setImage(image);
                }
                catch (Exception e) {
                    CommonPlugin.logError("Error loading image for content template " + getName(), e);
                    setImage(MISSING_IMAGE);
                }
            }
        }
        return super.getImage();
    }

    private static Properties loadPropertiesFrom(InputStream is) throws CoreException {
        try {
            Properties result = new Properties();
            result.load(is);
            is.close();
            return result;
        }
        catch (IOException e) {
            try { is.close(); } catch (IOException e2) { }
            throw CommonPlugin.wrapIntoCoreException(e);
        }
    }

    /**
     * Overridden to add new variables into the context.
     */
    @Override
    protected CreationContext createContext(IContainer folder, IWizard wizard, String wizardType) {
        CreationContext context = super.createContext(folder, wizard, wizardType);

        // default values for recognized options (will be overwritten from property file)
        context.getVariables().put(PROP_IGNORERESOURCES, "");

        // add property file entries as template variables
        for (Object key : properties.keySet()) {
            Object value = properties.get(key);
            Assert.isTrue(key instanceof String && value instanceof String);
                context.getVariables().put((String)key, parseJSON((String)value));
        }

        // add more predefined variables (these ones cannot be overwritten from the property file, would make no sense)
        if (templateUrl != null) {
            context.getVariables().put("templateURL", templateUrl.toString());
        }
        if (templateFolder != null) {
            context.getVariables().put("templateFolderName", templateFolder.getName());
            context.getVariables().put("templateFolderPath", templateFolder.getFullPath().toString());
            context.getVariables().put("templateProject", templateFolder.getProject().getName());
        }
        return context;
    }

    /**
     * Overridden so that we can load JAR files from the template folder
     */
    @Override
    protected ClassLoader createClassLoader() {
        if (!allowJarLoading)
            return super.createClassLoader();
        try {
            List<URL> urls = new ArrayList<URL>();
            for (String fileName : getFileList())
                if (fileName.endsWith(".jar"))
                    urls.add(asURL(fileName));
            return new URLClassLoader(urls.toArray(new URL[]{}), super.createClassLoader());
        }
        catch (Exception e) {
            CommonPlugin.logError("Error assembling classpath for loading jars from the workspace", e);
            return getClass().getClassLoader();
        }
    }

    static class URLTemplateLoader2 extends URLTemplateLoader {
        private URL baseUrl;

        public URLTemplateLoader2(URL baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Object findTemplateSource(String name) throws IOException {
            // WORKAROUND: When Freemarker tries the file with local suffix
            // ("en_US" etc), the file won't exist, and this method throws a
            // FileNotFoundException from the URLTemplateSource constructor.
            // That's exactly what the method documentation says NOT to do,
            // as it aborts template processing. Solution: We probe whether
            // the file exists, and only proceed to the original implementation
            // if it does.
            try {
                URL url = getURL(name);
                InputStream stream = url.openStream();
                try { stream.close(); } catch (IOException e) { }
            } catch (IOException e) {
                return null;
            }
            return super.findTemplateSource(name);
        }

        @Override
        protected URL getURL(String name) {
            try {
                return new URL(baseUrl.toString() + "/" + name);
            }
            catch (MalformedURLException e) {
                throw new IllegalArgumentException("Illegal template name: " + name, e);
            }
        }
    }

    @Override
    protected Configuration createFreemarkerConfiguration() {
        // add workspace template loader
        Configuration cfg = super.createFreemarkerConfiguration();
        cfg.setTemplateLoader(new MultiTemplateLoader( new TemplateLoader[] {
                cfg.getTemplateLoader(),
                templateUrl!=null ? new URLTemplateLoader2(templateUrl) : new WorkspaceTemplateLoader(templateFolder)
        }));
        return cfg;
    }

    public ICustomWizardPage[] createCustomPages() throws CoreException {
        // collect page IDs from property file ("page.1", "page.2" etc keys)
        int[] pageIDs = new int[0];
        for (Object key : properties.keySet())
            if (((String)key).matches("page\\.[0-9]+\\.(file|class)"))
                pageIDs = ArrayUtils.add(pageIDs, Integer.parseInt(((String)key).replaceFirst("^page\\.([0-9]+)\\.(file|class)$", "$1")));
        Arrays.sort(pageIDs);

        // create pages
        ICustomWizardPage[] result = new ICustomWizardPage[pageIDs.length];
        for (int i = 0; i < pageIDs.length; i++) {
            // create page
            int pageID = pageIDs[i];
            String xswtFileName = properties.getProperty("page."+pageID+".file");
            String pageClass = properties.getProperty("page."+pageID+".class");
            String condition = properties.getProperty("page."+pageID+".condition");
            if (xswtFileName != null) {
                try {
                    result[i] = new XSWTWizardPage(getName()+"#"+pageID, this, condition, openFile(xswtFileName), xswtFileName);
                }
                catch (IOException e) {
                    throw new CoreException(new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, "Error loading template file "+xswtFileName, e));
                }
            }
            else if (pageClass != null) {
                try {
                    Class<?> clazz = getClassLoader().loadClass(pageClass);
                    result[i] = (ICustomWizardPage) ReflectionUtils.invokeConstructor(clazz, getName()+"#"+pageID, this, condition==null?"":condition); // if condition is null, ReflectionUtils will throw NPE
                }
                catch (Exception e) {
                    throw new CoreException(new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, "Error instantiating wizard page class "+pageClass, e));
                }
            }
            else {
                Assert.isTrue(false);
            }

            // set title and description
            String title = properties.getProperty("page."+pageID+".title");
            if (!StringUtils.isEmpty(title))
                result[i].setTitle(title);
            else if (StringUtils.isEmpty(result[i].getTitle()))
                result[i].setTitle(getName());

            String description = properties.getProperty("page."+pageID+".description");
            if (!StringUtils.isEmpty(description))
                result[i].setDescription(description);
            else if (StringUtils.isEmpty(result[i].getDescription()))
                result[i].setDescription("Select options below"); // note: "1 of 5" is not good, because of conditional pages
        }

        return result;
    }

    /**
     * Parse text as JSON. Returns Boolean, String, Number, List or Map. If the
     * text does not look like JSON, treats it as an unquoted literal string, and
     * returns it as String.
     *
     * @throws IllegalArgumentException on JSON parse error.
     */
    public static Object parseJSON(String text) {
        String numberRegex = "\\s*[+-]?[0-9.]+([eE][+-]?[0-9]+)?\\s*"; // sort of
        text = text.trim();
        if (text.equals("true") || text.equals("false") || text.matches(numberRegex) ||
                text.startsWith("[") || text.startsWith("{") || text.startsWith("\"")) {
            // looks like JSON -- parse as such
            JSONValidatingReader reader = new JSONValidatingReader(new ExceptionErrorListener());
            return reader.read(text); // throws IllegalArgumentException on parse errors
        }
        else {
            // apparently not JSON -- take it as a literal string with missing quotes
            return text;
        }
    }

    public void performFinish(CreationContext context) throws CoreException {
        // execute a template that can modify the variables in the context. We do this before
        // substituteNestedVariables(), so that the template has a chance to modify
        // variables before they are substituted into template.properties
        executePreRunTemplate(context);

        // substitute variables
        substituteNestedVariables(context);

        // copy over files and folders, with template substitution
        copyFiles(context);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void executePreRunTemplate(CreationContext context) throws CoreException {
        if (context.getVariable(PROP_PRERUNTEMPLATE) != null) {
            String preRunTemplateName = XSWTDataBinding.toString(context.getVariable(PROP_PRERUNTEMPLATE));
            if (StringUtils.isNotEmpty(preRunTemplateName)) {
                Environment env = processTemplateForSideEffects(getFreemarkerConfiguration(), preRunTemplateName, context);
                try {
                    // The Environment's main namespace contains the variables, macros, functions etc
                    // set by the template. We'd like the assignments in this template to take effect,
                    // so we take over the contents of the main namespace into the context.
                    // Note that the context also includes macros like "iif", but this likely presents
                    // no problem.
                    Namespace mainNamespace = env.getMainNamespace();
                    Map map = mainNamespace.toMap();
                    context.getVariables().putAll(map);
                }
                catch (TemplateModelException e) {
                    CommonPlugin.logError("Error extracting variables set by the pre-run template " + preRunTemplateName, e);
                }
            }
        }
    }

    /**
     * Instantiate the template given with an URL: copy files and folders given
     * in file list to the destination folder specified in the context.
     */
    protected void copyFiles(CreationContext context) throws CoreException {
        String[] fileList = getFileList();
        for (String fileName : fileList) {
            if (fileName.endsWith("/")) {
                if (!matchesAny(fileName, ignoreResourcePatterns))
                    createFolder(fileName, context);
            }
            else if (context.getFolder()==null) {
                // export wizard: only process ftl files but do not save them (they may create files directly, via FileUtils)
                boolean isFtlFile = StringUtils.defaultString(new Path(fileName).getFileExtension()).equals("ftl");
                if (isFtlFile && !matchesAny(fileName, ignoreResourcePatterns) && !matchesAny(fileName, verbatimFilePatterns))
                    processTemplateForSideEffects(getFreemarkerConfiguration(), fileName, context);
            }
            else {
                boolean isFtlFile = StringUtils.defaultString(new Path(fileName).getFileExtension()).equals("ftl");
                InputStream inputStream = openFile(fileName);
                if (matchesAny(fileName, verbatimFilePatterns) || (!isFtlFile && !matchesAny(fileName, ignoreResourcePatterns)))
                    createVerbatimFile(fileName, inputStream, context);
                else if (isFtlFile && !matchesAny(fileName, ignoreResourcePatterns))
                    createTemplatedFile(fileName.replaceFirst("\\.ftl$", ""), getFreemarkerConfiguration(), fileName, context);
            }
        }
    }

    /**
     * Exists to abstract out the difference between workspace loading (IFile)
     * and URL (or resource bundle) based loading.
     */
    protected URL asURL(String fileName) throws CoreException {
        try {
            if (templateUrl != null)
                return new URL(templateUrl.toString() + "/" + fileName);
            else
                return new URL("file", "", templateFolder.getFile(new Path(fileName)).getLocation().toPortableString());
        }
        catch (MalformedURLException e) {
            throw CommonPlugin.wrapIntoCoreException("Could not make URL for file " + fileName, e);
        }
    }

    /**
     * Exists to abstract out the difference between workspace loading (IFile)
     * and URL (or resource bundle) based loading.
     */
    protected InputStream openFile(String fileName) throws CoreException {
        if (templateUrl != null) {
            try {
                return new URL(templateUrl.toString() + "/" + fileName).openStream();
            }
            catch (IOException e) {
                throw CommonPlugin.wrapIntoCoreException("Could not read file " + fileName, e);
            }
        }
        else {
            return templateFolder.getFile(new Path(fileName)).getContents();
        }
    }

    /**
     * Exists to abstract out the difference between workspace loading (IFile)
     * and URL (or resource bundle) based loading.
     */
    @SuppressWarnings("rawtypes")
    protected String[] getFileList() throws CoreException {
        if (fileList == null) {
            if (bundleOfTemplate != null) {
                // URL points into an OSGi bundle, so we can use its findEntries() method to produce the file list
                String folderName = StringUtils.removeStart(templateUrl.getPath(),  bundleOfTemplate.getResource("/").getPath());
                Enumeration e = bundleOfTemplate.findEntries(folderName, "*", true);
                List<String> list = new ArrayList<String>();
                while (e.hasMoreElements()) {
                    URL fileUrl = (URL) e.nextElement();
                    String filePath = StringUtils.removeStart(fileUrl.getPath(), templateUrl.getPath());
                    list.add(filePath);
                }
                fileList = list.toArray(new String[]{});
            }
            else if (templateUrl != null) {
                try {
                    // generic URL, so we expect to find a filelist.txt
                    String filelistTxt = FileUtils.readTextFile(openFile(FILELIST_FILENAME), null);
                    fileList = filelistTxt.trim().split("\\s*\n\\s*");
                } catch (IOException e) {
                    throw CommonPlugin.wrapIntoCoreException("Could not read filelist.txt", e);
                }
            }
            else {
                // workspace
                List<String> list = new ArrayList<String>();
                collectFiles(templateFolder, templateFolder, list);
                fileList = list.toArray(new String[]{});
            }
        }
        return fileList;
    }

    private void collectFiles(IContainer folder, IContainer baseFolder, List<String> result) throws CoreException {
        int segmentCount = baseFolder.getFullPath().segmentCount();
        for (IResource resource : folder.members()) {
            String relativePath = resource.getFullPath().removeFirstSegments(segmentCount).toString();
            if (resource instanceof IFile)
                result.add(relativePath);
            else {
                result.add(relativePath + "/");
                collectFiles((IContainer)resource, baseFolder, result);
            }
        }
    }

    /**
     * Returns whether the given file name matches any of the given glob patterns.
     * Only a limited subset of glob patterns is recognized: '*' and '?' only,
     * no curly braces or square brackets. Extra: "**" is recognized.
     * Note: "*.txt" means text files in the ROOT folder. To mean "*.txt in any
     * folder", specify "** / *.txt" (without the spaces).
     */
    protected static boolean matchesAny(String path, List<String> basicGlobPatterns) {
        path = path.replace('\\', '/'); // simplify matching
        for (String pattern : basicGlobPatterns) {
            String regex = "^" + pattern.replace(".", "\\.").replace("?", ".").replace("**/", "(.&/)?").replace("**", ".&").replace("*", "[^/]*").replace(".&", ".*") + "$";
            if (path.matches(regex))
                return true;
        }
        return false;
    }

    public void cloneTo(CreationContext context) throws CoreException {
        assertContextFolder(context);
        for (String fileName : getFileList()) {
            if (!fileName.endsWith("/")) {
                IFile destFile = context.getFolder().getFile(new Path(fileName));
                createVerbatimFile(destFile, openFile(fileName), context);
            }
        }
    }

}
