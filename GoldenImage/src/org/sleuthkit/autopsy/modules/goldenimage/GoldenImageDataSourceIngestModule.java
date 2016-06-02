/*
 * GoldenImageDataSourceIngestModule
 * 
 */
package org.sleuthkit.autopsy.modules.goldenimage;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.services.FileManager;
import org.sleuthkit.autopsy.casemodule.services.TagsManager;
import org.sleuthkit.autopsy.ingest.DataSourceIngestModuleProgress;
import org.sleuthkit.autopsy.ingest.IngestModule;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.autopsy.ingest.DataSourceIngestModule;
import org.sleuthkit.autopsy.ingest.IngestJobContext;
import org.sleuthkit.autopsy.ingest.IngestMessage;
import org.sleuthkit.autopsy.ingest.IngestServices;
import org.sleuthkit.datamodel.HashUtility;
import org.sleuthkit.datamodel.TagName;

/**
 * Golden Image Ingest Module.
 * This module iterates through every file of an (dirty) image. It checks if the file
 * is contained in another (golden) image. In a  next steps it creates an md5-hash
 * of each file and compares them. Depending on its result, it will tag the file
 * either as Safe, Changed or Deleted (Or leaves it untagged).
 */
class GoldenImageDataSourceIngestModule implements DataSourceIngestModule {

   // private final boolean skipKnownFiles;
    private IngestJobContext context = null;
    private final GoldenImageModuleIngestJobSettings settings;
    private final ArrayList<AbstractFile> comparisonFailFiles;
    private TagName giCustomDeletedTag = null;
    
    

    GoldenImageDataSourceIngestModule(GoldenImageModuleIngestJobSettings pSettings) {
	    settings = pSettings;
	    comparisonFailFiles = new ArrayList<>();
    }

    @Override
    public void startUp(IngestJobContext context) throws IngestModuleException {
        this.context = context;
	
    }

    @Override
    public ProcessResult process(Content dataSource, DataSourceIngestModuleProgress progressBar) {
	
	    /** LOGGING START **/
	    //Post Message: GI Started
	    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	Date startDate = new Date();
	System.out.println(dateFormat.format(startDate));
		String msgStartTime = "Golden Image Ingest Module started: "+startDate;
		IngestMessage messageStartTime = IngestMessage.createMessage(
			IngestMessage.MessageType.DATA,
			GoldenImageIngestModuleFactory.getModuleName(),
			msgStartTime);
		IngestServices.getInstance().postMessage(messageStartTime);
	    /** LOGGING END **/
		
	TagsManager tagsManager = Case.getCurrentCase().getServices().getTagsManager();
	Content goldenImageDS = settings.getSelectedDatasource();
	
	
	if(goldenImageDS == null){
		throw new IllegalStateException("Golden Image DS Ingest Module: The Golden Image Datasource is null.");
	}
	
	try {
		FileManager fileManager = Case.getCurrentCase().getServices().getFileManager();
		List<AbstractFile> allFiles = fileManager.findFiles(goldenImageDS, "%");
		
		int giFileCount = 0;
		int hashedGIFilesCount = 0;
		int hashedDIFilesCount = 0;
		
		if(!allFiles.isEmpty()){
			progressBar.switchToDeterminate(allFiles.size());
			for(AbstractFile aFile : allFiles){
				//Stop processing if requested
				if (context.dataSourceIngestIsCancelled()) {
					return IngestModule.ProcessResult.OK;
				}
				
				giFileCount++;
				
				
				//Check if the AbstractFile is a File. Continue if it's a directory or similar.
				if(!aFile.isFile() || !aFile.canRead()){
					progressBar.progress("Jumping over Non-File.",giFileCount);
					continue;
				}
				
				
				AbstractFile dirtyImageFile = findFile(dataSource, aFile);
				String diFileName = "";
				String giFileName = (aFile.getName() == null?"":aFile.getName());
				
				//Check if dirtyImageFile exists & is readable
				if(dirtyImageFile != null && dirtyImageFile.isFile() && dirtyImageFile.canRead()){
					diFileName = (dirtyImageFile.getName()==null?"":dirtyImageFile.getName());
					//Check if the md5-Sum of the 2 Files are calculated; If not, calculate.
					if(calculateHash(dirtyImageFile)){
						hashedDIFilesCount++;
					}
					if(calculateHash(aFile)){
						hashedGIFilesCount++;
					}
					
					
					//Compare Files
					if(dirtyImageFile.getMd5Hash() == null || aFile.getMd5Hash() == null){
						//Can't compare - One of the hashes is missing
						comparisonFailFiles.add(aFile);
					}else if(dirtyImageFile.getMd5Hash().equals(aFile.getMd5Hash())){
						tagsManager.addContentTag(dirtyImageFile, GoldenImageIngestModuleFactory.giTagSafe, "");
						
					}else if(!dirtyImageFile.getMd5Hash().equals(aFile.getMd5Hash())){
						tagsManager.addContentTag(dirtyImageFile, GoldenImageIngestModuleFactory.giTagChanged, "The Content of this file is different from it's equivalent on the golden image.");
						
					}else{
						//Untagged File
						
					}
				}else{
					//File wasn't found in the dirty image
					tagsManager.addContentTag(aFile, getCustomDeletedTag(dataSource.getName()),"The file exists on the Golden Image, but not on the Dirty Image.");
				}
				
				progressBar.progress("GI: Comparing "+diFileName+" ("+dataSource.getName()+") with "+giFileName+" ("+goldenImageDS.getName()+")",giFileCount);
			}
		}
		
		//Stop processing if requested
		if (context.dataSourceIngestIsCancelled()) {
			return IngestModule.ProcessResult.OK;
		}
		
		/** LOGGING START **/
		// Post Stats: Hashing
		String msgStatsHashing = "Golden Image Hashing Stats: \n Hashed GI Files: "+hashedGIFilesCount+"\n Hashed DI Files: "+hashedDIFilesCount;
		IngestMessage messageStatsHashing = IngestMessage.createMessage(
			IngestMessage.MessageType.DATA,
			GoldenImageIngestModuleFactory.getModuleName(),
			msgStatsHashing);
		IngestServices.getInstance().postMessage(messageStatsHashing);
		
		//Post Stats: General
		String msgStatsGeneral = "Golden Image General Stats:\n Total Files: "+allFiles.size()+"\n Processed Files: "+giFileCount+"\n Comparison Failures: "+comparisonFailFiles.size();
		IngestMessage messageStatsGeneral = IngestMessage.createMessage(
			IngestMessage.MessageType.DATA,
			GoldenImageIngestModuleFactory.getModuleName(),
			msgStatsGeneral);
		IngestServices.getInstance().postMessage(messageStatsGeneral);
		

		//Post Message: GI Ended
		Date endDate = new Date();
		String msgEndTime = "Golden Image Ingest Module started: "+endDate;
		IngestMessage messageEndTime = IngestMessage.createMessage(
			IngestMessage.MessageType.DATA,
			GoldenImageIngestModuleFactory.getModuleName(),
			msgEndTime);
		IngestServices.getInstance().postMessage(messageEndTime);
		/** LOGGING END **/
		
		return IngestModule.ProcessResult.OK;
		
		
	} catch (TskCoreException ex) {
		Exceptions.printStackTrace(ex);
	}
	  
	return IngestModule.ProcessResult.ERROR;
    }
    
    private TagName getCustomDeletedTag(String pDirtyImageName){
	    if(giCustomDeletedTag != null){
		    return giCustomDeletedTag;
	    }
	    
	    TagsManager tagsManager = Case.getCurrentCase().getServices().getTagsManager();
	    try {
		    giCustomDeletedTag = tagsManager.addTagName("DI_DELETED_"+pDirtyImageName, "The file exists on the Golden Image, but not on the Dirty Image.", TagName.HTML_COLOR.LIME);
	    } catch (TagsManager.TagNameAlreadyExistsException ex) {
		    try {
			for (TagName tagName : tagsManager.getAllTagNames()) {
				if(giCustomDeletedTag != null)
					break;
				
				if (tagName.getDisplayName().equals("DI_DELETED_"+pDirtyImageName)) {
				    giCustomDeletedTag = tagName;
				}
			}
		    } catch (TskCoreException ex1) {
			Exceptions.printStackTrace(ex1);
		    }
	    } catch (TskCoreException ex) {
		    Exceptions.printStackTrace(ex);
	    }
	    
	    return giCustomDeletedTag;
    }
    
    /**
     * This method searches for a file by filename and filepath in the given Datasource.
     * 
     * @param pDataSource The Datasource in which the file should be searched in
     * @param pFile The File which should be found in the Datasource
     * 
     * @return Returns an AbstractFile if the file was found in the datasource. Returns Null if it wasn't found.
     */
    private AbstractFile findFile(Content pDataSource, AbstractFile pFile){
	    if(pFile.getName() == null || pFile.getName().equals("") || pFile.getParentPath() == null || pFile.getParentPath().equals("")){
		    return null;
	    }
	    AbstractFile foundFile = null;
	    
	    
	    FileManager fileManager = Case.getCurrentCase().getServices().getFileManager();
	    try {
		    ArrayList<AbstractFile> foundFiles = new ArrayList<>(fileManager.findFiles(pDataSource, (pFile.getName() != null ? pFile.getName() : ""), (pFile.getParentPath()!=null?pFile.getParentPath():"")));
		    if(foundFiles.isEmpty()){
			    foundFile = null;
		    }else{
			    foundFile = foundFiles.get(0);
		    }
	    } catch (TskCoreException ex) {
		    Exceptions.printStackTrace(ex);
	    }
	    
	    return foundFile;
    }
    
    /** This method takes an Abstract File, checks if its hash is already calculated, if not it tries to calculate it. 
     * @param AbstractFile The Abstract File of which the hash should be checked & calculated.
     * @return boolean true if the hash was calculated, false if it already exists or an error occured.
     *
     */
    private boolean calculateHash(AbstractFile pFile){
	if(pFile.isFile() && pFile.canRead() && (pFile.getMd5Hash() == null || pFile.getMd5Hash().isEmpty())){
		try {
			HashUtility.calculateMd5(pFile);
			return true;
		} catch (IOException ex) {
			String uniquePath = "";
			try {
				uniquePath = pFile.getUniquePath();
			} catch (TskCoreException ex1) {
				Exceptions.printStackTrace(ex1);
			}
			
			Exceptions.printStackTrace(ex);
			return false;
		}
	}
	return false;
    }
}
