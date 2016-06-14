/*
 * Factory for the Golden Image module.
 */
package org.sleuthkit.autopsy.modules.goldenimage;

import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.NbBundle;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.services.TagsManager;

import org.sleuthkit.autopsy.ingest.IngestModuleFactory;
import org.sleuthkit.autopsy.ingest.DataSourceIngestModule;
import org.sleuthkit.autopsy.ingest.FileIngestModule;
import org.sleuthkit.autopsy.ingest.IngestModuleFactoryAdapter;
import org.sleuthkit.autopsy.ingest.IngestModuleGlobalSettingsPanel;
import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettings;
import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettingsPanel;
import org.sleuthkit.datamodel.TagName;
import org.sleuthkit.datamodel.TskCoreException;


@ServiceProvider(service = IngestModuleFactory.class) // Sample is discarded at runtime 
public class GoldenImageIngestModuleFactory  extends IngestModuleFactoryAdapter{

    private static final String VERSION_NUMBER = "1.0.0";
    public static String giTagChangedName = "DI_Changed";
    public static String giTagGoodName = "DI_Good";
    public static TagName giTagChanged;
    public static TagName giTagGood;
    
    public GoldenImageIngestModuleFactory(){
	   super();
	   
	   //Register Tags
	    TagsManager tagsManager = Case.getCurrentCase().getServices().getTagsManager();
	    try {
		    giTagChanged = tagsManager.addTagName(giTagChangedName, "The file exists on the golden image, but the content was changed.", TagName.HTML_COLOR.LIME);
		    giTagGood = tagsManager.addTagName(giTagGoodName, "The file exists on the golden image and wasn't changed.", TagName.HTML_COLOR.LIME);
	    } catch (TagsManager.TagNameAlreadyExistsException ex) {
		    try {
			for (TagName tagName : tagsManager.getAllTagNames()) {
				if(giTagChanged != null && giTagGood != null)
					break;
				
				if (tagName.getDisplayName().equals(giTagChangedName)) {
				    giTagChanged = tagName;
				}
				
				if (tagName.getDisplayName().equals(giTagGoodName)) {
				    giTagGood = tagName;
				}
			}
		    } catch (TskCoreException ex1) {
			Exceptions.printStackTrace(ex1);
		    }
	    } catch (TskCoreException ex) {
		    Exceptions.printStackTrace(ex);
	    }
    }

    // This class method allows the ingest module instances created by this 
    // factory to use the same display name that is provided to the Autopsy
    // ingest framework by the factory.
    static String getModuleName() {
        return NbBundle.getMessage(GoldenImageIngestModuleFactory.class, "GoldenImageIngestModuleFactory.moduleName");
    }

    /**
     * Gets the display name that identifies the family of ingest modules the
     * factory creates. Autopsy uses this string to identify the module in user
     * interface components and log messages. The module name must be unique. so
     * a brief but distinctive name is recommended.
     *
     * @return The module family display name.
     */
    @Override
    public String getModuleDisplayName() {
        return getModuleName();
    }

    /**
     * Gets a brief, user-friendly description of the family of ingest modules
     * the factory creates. Autopsy uses this string to describe the module in
     * user interface components.
     *
     * @return The module family description.
     */
    @Override
    public String getModuleDescription() {
        return NbBundle.getMessage(GoldenImageIngestModuleFactory.class, "GoldenImageIngestModuleFactory.moduleDescription");
    }

    
    /**
     * Gets the version number of the family of ingest modules the factory
     * creates.
     *
     * @return The module family version number.
     */
    @Override
    public String getModuleVersionNumber() {
        return VERSION_NUMBER;
    }

    /**
     * Queries the factory to determine if it provides a user interface panel to
     * allow a user to change settings that are used by all instances of the
     * family of ingest modules the factory creates. For example, the Autopsy
     * core hash lookup ingest module factory provides a global settings panel
     * to import and create hash databases. The hash databases are then enabled
     * or disabled per ingest job using an ingest job settings panel. If the
     * module family does not have global settings, the factory may extend
     * IngestModuleFactoryAdapter to get an implementation of this method that
     * returns false.
     *
     * @return True if the factory provides a global settings panel.
     */
    @Override
    public boolean hasGlobalSettingsPanel() {
        return false;
    }

    /**
     * Gets a user interface panel that allows a user to change settings that
     * are used by all instances of the family of ingest modules the factory
     * creates. For example, the Autopsy core hash lookup ingest module factory
     * provides a global settings panel to import and create hash databases. The
     * imported hash databases are then enabled or disabled per ingest job using
     * ingest an ingest job settings panel. If the module family does not have a
     * global settings, the factory may extend IngestModuleFactoryAdapter to get
     * an implementation of this method that throws an
     * UnsupportedOperationException.
     *
     * @return A global settings panel.
     */
    @Override
    public IngestModuleGlobalSettingsPanel getGlobalSettingsPanel() {
	    /*
        GoldenImageGlobalSettingsPanel globalSettingsPanel = new GoldenImageGlobalSettingsPanel();
        globalSettingsPanel.load();
        return globalSettingsPanel;*/
	    return null;
    }

    /**
     * Gets the default per ingest job settings for instances of the family of
     * ingest modules the factory creates. For example, the Autopsy core hash
     * lookup ingest modules family uses hash databases imported or created
     * using its global settings panel. All of the hash databases are enabled by
     * default for an ingest job. If the module family does not have per ingest
     * job settings, the factory may extend IngestModuleFactoryAdapter to get an
     * implementation of this method that returns an instance of the
     * NoIngestModuleJobSettings class.
     *
     * @return The default ingest job settings.
     */
    @Override
    public IngestModuleIngestJobSettings getDefaultIngestJobSettings() {
        return new GoldenImageModuleIngestJobSettings();
    }

    /**
     * Queries the factory to determine if it provides user a interface panel to
     * allow a user to make per ingest job settings for instances of the family
     * of ingest modules the factory creates. For example, the Autopsy core hash
     * lookup ingest module factory provides an ingest job settings panels to
     * enable or disable hash databases per ingest job. If the module family
     * does not have per ingest job settings, the factory may extend
     * IngestModuleFactoryAdapter to get an implementation of this method that
     * returns false.
     *
     * @return True if the factory provides ingest job settings panels.
     */
    @Override
    public boolean hasIngestJobSettingsPanel() {
        return true;
    }

    /**
     * Gets a user interface panel that can be used to set per ingest job
     * settings for instances of the family of ingest modules the factory
     * creates. For example, the core hash lookup ingest module factory provides
     * an ingest job settings panel to enable or disable hash databases per
     * ingest job. If the module family does not have per ingest job settings,
     * the factory may extend IngestModuleFactoryAdapter to get an
     * implementation of this method that throws an
     * UnsupportedOperationException.
     *
     * @param setting Per ingest job settings to initialize the panel.
     *
     * @return An ingest job settings panel.
     */
    @Override
    public IngestModuleIngestJobSettingsPanel getIngestJobSettingsPanel(IngestModuleIngestJobSettings settings) {
        
        return new GoldenImageIngestModuleIngestJobSettingsPanel((GoldenImageModuleIngestJobSettings) settings);
    }

    /**
     * Queries the factory to determine if it is capable of creating data source
     * ingest modules. If the module family does not include data source ingest
     * modules, the factory may extend IngestModuleFactoryAdapter to get an
     * implementation of this method that returns false.
     *
     * @return True if the factory can create data source ingest modules.
     */
    @Override
    public boolean isDataSourceIngestModuleFactory() {
        return true;
    }

    /**
     * Creates a data source ingest module instance.
     * <p>
     * Autopsy will generally use the factory to several instances of each type
     * of module for each ingest job it performs. Completing an ingest job
     * entails processing a single data source (e.g., a disk image) and all of
     * the files from the data source, including files extracted from archives
     * and any unallocated space (made to look like a series of files). The data
     * source is passed through one or more pipelines of data source ingest
     * modules. The files are passed through one or more pipelines of file
     * ingest modules.
     * <p>
     * The ingest framework may use multiple threads to complete an ingest job,
     * but it is guaranteed that there will be no more than one module instance
     * per thread. However, if the module instances must share resources, the
     * modules are responsible for synchronizing access to the shared resources
     * and doing reference counting as required to release those resources
     * correctly. Also, more than one ingest job may be in progress at any given
     * time. This must also be taken into consideration when sharing resources
     * between module instances. modules.
     * <p>
     * If the module family does not include data source ingest modules, the
     * factory may extend IngestModuleFactoryAdapter to get an implementation of
     * this method that throws an UnsupportedOperationException.
     *
     * @param settings The settings for the ingest job.
     *
     * @return A data source ingest module instance.
     */
    @Override
    public DataSourceIngestModule createDataSourceIngestModule(IngestModuleIngestJobSettings settings) {
        
	    if (!(settings instanceof GoldenImageModuleIngestJobSettings)) {
            throw new IllegalArgumentException("Expected settings argument to be instanceof GoldenImageModuleIngestJobSettings");
        }
	
        return new GoldenImageDataSourceIngestModule((GoldenImageModuleIngestJobSettings) settings);
    }

    /**
     * Queries the factory to determine if it is capable of creating file ingest
     * modules. If the module family does not include file ingest modules, the
     * factory may extend IngestModuleFactoryAdapter to get an implementation of
     * this method that returns false.
     *
     * @return True if the factory can create file ingest modules.
     */
    @Override
    public boolean isFileIngestModuleFactory() {
        return false;
    }

    /**
     * Creates a file ingest module instance.
     * <p>
     * Autopsy will generally use the factory to several instances of each type
     * of module for each ingest job it performs. Completing an ingest job
     * entails processing a single data source (e.g., a disk image) and all of
     * the files from the data source, including files extracted from archives
     * and any unallocated space (made to look like a series of files). The data
     * source is passed through one or more pipelines of data source ingest
     * modules. The files are passed through one or more pipelines of file
     * ingest modules.
     * <p>
     * The ingest framework may use multiple threads to complete an ingest job,
     * but it is guaranteed that there will be no more than one module instance
     * per thread. However, if the module instances must share resources, the
     * modules are responsible for synchronizing access to the shared resources
     * and doing reference counting as required to release those resources
     * correctly. Also, more than one ingest job may be in progress at any given
     * time. This must also be taken into consideration when sharing resources
     * between module instances. modules.
     * <p>
     * If the module family does not include file ingest modules, the factory
     * may extend IngestModuleFactoryAdapter to get an implementation of this
     * method that throws an UnsupportedOperationException.
     *
     * @param settings The settings for the ingest job.
     *
     * @return A file ingest module instance.
     */
    @Override
    public FileIngestModule createFileIngestModule(IngestModuleIngestJobSettings settings) {
	    /*
        if (!(settings instanceof GoldenImageModuleIngestJobSettings)) {
            throw new IllegalArgumentException("Expected settings argument to be instanceof SampleModuleIngestJobSettings");
        }
        return new GoldenImageFileIngestModule((GoldenImageModuleIngestJobSettings) settings);*/
	    return null;
    }
}
