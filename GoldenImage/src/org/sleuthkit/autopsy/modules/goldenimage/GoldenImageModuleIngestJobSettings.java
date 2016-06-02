/*
 * Sample module ingest job settings in the public domain.  
 * Feel free to use this as a template for your module job settings.
 * 
 *  Contact: Brian Carrier [carrier <at> sleuthkit [dot] org]
 *
 *  This is free and unencumbered software released into the public domain.
 *  
 *  Anyone is free to copy, modify, publish, use, compile, sell, or
 *  distribute this software, either in source code form or as a compiled
 *  binary, for any purpose, commercial or non-commercial, and by any
 *  means.
 *  
 *  In jurisdictions that recognize copyright laws, the author or authors
 *  of this software dedicate any and all copyright interest in the
 *  software to the public domain. We make this dedication for the benefit
 *  of the public at large and to the detriment of our heirs and
 *  successors. We intend this dedication to be an overt act of
 *  relinquishment in perpetuity of all present and future rights to this
 *  software under copyright law.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 *  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE. 
 */
package org.sleuthkit.autopsy.modules.goldenimage;

import java.util.ArrayList;
import java.util.logging.Level;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettings;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.TskCoreException;

/**
 * Ingest job options for sample ingest module instances.
 */
public class GoldenImageModuleIngestJobSettings implements IngestModuleIngestJobSettings {

    private static final long serialVersionUID = 1L;
    
    private transient Content selectedDatasource;
    private long dataSourceID;
    
    

    GoldenImageModuleIngestJobSettings() {
	    
    }

    @Override
    public long getVersionNumber() {
        return serialVersionUID;
    }
    
    public long getDataSourceID(){
	    return dataSourceID;
    }
    
    public void setDataSourceID(long pDataSourceID){
	    dataSourceID = pDataSourceID;
	    
	    selectedDatasource = getDatasourceById(pDataSourceID);
    }
    
    public void setSelectedDatasource(Content pContent){
	    selectedDatasource = pContent;
	    setDataSourceID(pContent.getId());
    }
    
    public Content getSelectedDatasource(){
	    return selectedDatasource;
    }
    
    public Content getDatasourceById(long pDataSourceId){
	    Case currentCase = Case.getCurrentCase();
	    ArrayList<Content> listDS = new ArrayList<>();
	    try {
		    listDS.addAll(currentCase.getDataSources());
		    
	    } catch (TskCoreException ex) {
		    java.util.logging.Logger.getLogger(GoldenImageModuleIngestJobSettings.class.getName()).log(Level.SEVERE, null, ex);
	    }
	    
	    if(!listDS.isEmpty()){
		    for(Content c : listDS){
			    if(c.getId() == pDataSourceId){
				    return c;
			    }
		    }
	    }
	    return null;
    }
}
