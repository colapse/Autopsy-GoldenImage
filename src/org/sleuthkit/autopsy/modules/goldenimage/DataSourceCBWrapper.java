/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sleuthkit.autopsy.modules.goldenimage;

import org.sleuthkit.datamodel.Content;

/**
 *
 * @author root
 */
public class DataSourceCBWrapper {
	private Content content;
	
	public DataSourceCBWrapper(Content pContent){
		content = pContent;
	}
	
	public Content getContent(){
		return content;
	}
	
	@Override
	public String toString(){
		return content.getName();
	}
}
