// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.recycle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.resource.Resource;
import org.talend.commons.emf.FactoriesUtil;
import org.talend.commons.utils.StringUtils;
import org.talend.core.model.properties.ItemState;
import org.talend.core.model.properties.Property;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dq.helper.PropertyHelper;
import org.talend.dq.writer.EMFSharedResources;
import org.talend.resource.ResourceManager;
import org.talend.top.repository.ProxyRepositoryManager;
import org.talend.utils.sugars.ReturnCode;

/**
 * @author qiongli handle logical delete and restore .MOD qiongli 2010-10-8,bug 15674
 */
public class LogicalDeleteFileHandle {


    private static Logger log = Logger.getLogger(LogicalDeleteFileHandle.class);

    private static String slashStr = "\\";

    private static HashSet<Property> delPropertys = null;

    /**
     * 
     * Get all Children from the static var by 'folderPath',contain file or subFoldern.
     * 
     * @param folderPath
     * @return
     */
    public static List<Object> getLogicalDelNodes(String folderPath) {
        List<Object> ls = new ArrayList<Object>();
        try {
            IFile file = null;
            IFolder folder = null;
            HashSet<String> set = new HashSet<String>();
            DQRecycleBinNode rbn = null;
            
            for (Property property : delPropertys) {
                file = PropertyHelper.getItemFile(property);
                if (file.getParent().getFullPath().toOSString().equals(folderPath)) {
                    rbn = new DQRecycleBinNode();
                    rbn.setObject(property);
                    ls.add(rbn);
                } else {
                    // add the subFoler
                    addToSet(file.getFullPath().toOSString(), folderPath, set);
                }
            }

            Iterator<String> iterator = set.iterator();
            while (iterator.hasNext()) {
                rbn = new DQRecycleBinNode();
                folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(iterator.next()));
                rbn.setObject(folder);
                ls.add(rbn);
            }
        } catch (Exception exc) {
            log.error(exc, exc);
        }
        return ls;
    }

    /**
     * 
     * Make sure the same subFoleder only appear once.
     * 
     * @param fullPath
     * @param folderPath
     * @param hashSet
     */
    public static void addToSet(String fullPath, String folderPath, HashSet<String> hashSet) {
        if (!fullPath.startsWith(folderPath))
            return;
        String subFolderName = fullPath.replace(folderPath, PluginConstant.EMPTY_STRING);
        String[] temp = StringUtils.split(subFolderName, '\\');
        if (temp != null && temp.length > 0) {
            subFolderName = temp[0];
            hashSet.add(folderPath + slashStr + subFolderName);
        }

    }

    /**
     * 
     * Logical delete file.set the property of isDelete to 'true'.refresh the varible 'delPropertys'.
     * 
     * @param ifile
     * @return
     * @throws Exception
     */
    public static ReturnCode deleteLogical(IFile ifile) throws Exception {
        ReturnCode rc = new ReturnCode();

        IFile propFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
                ifile.getFullPath().removeFileExtension().addFileExtension(FactoriesUtil.PROPERTIES_EXTENSION));
        if (propFile.exists()) {
            Property property = PropertyHelper.getProperty(propFile);
            ItemState itemState = property.getItem().getState();
            if (!itemState.isDeleted()) {
                itemState.setDeleted(true);
                Resource propertyResource = property.eResource();
                rc.setOk(EMFSharedResources.getInstance().saveResource(propertyResource));
            }
            refreshDelPropertys(1, property);
        }

        // svn commit
        ProxyRepositoryManager.getInstance().save();
        // finish
        ifile.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
        rc.setMessage(DefaultMessagesImpl.getString("LogicalDeleteFileHandle.logicalDelSuccess"));

        return rc;
    }

    /**
     * 
     * DOC qiongli Comment method "hasChildDeleted".if has one deleted child at least.
     * 
     * @param folder
     * @return
     */
    public static boolean hasChildDeleted(IFolder folder) {
        String strPath = folder.getFullPath().toOSString();
        IFile file = null;
        for (Property property : delPropertys) {
            file = PropertyHelper.getItemFile(property);
            if (file.getFullPath().toOSString().startsWith(strPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * DOC qiongli if it's all children are logical deleted. bug 14697,bug 15674
     * 
     * @param folder
     * @return
     */
    public static boolean isAllChildrenDeleted(IFolder folder) {

        IResource[] members = null;
        try {
            if (!folder.exists())
                return true;
            members = folder.members();
        } catch (CoreException e) {
            e.printStackTrace();
        }
        String fileType = null;
        for (IResource res : members) {
            if (res.getType() == IResource.FILE) {
                IFile file = (IFile) res;
                fileType = file.getFileExtension();
                if (fileType != null && fileType.equals(FactoriesUtil.PROPERTIES_EXTENSION)) {
                    Property prop = PropertyHelper.getProperty(file);
                    if (!prop.getItem().getState().isDeleted())
                        return false;
                }
            } else if (res.getType() == IResource.FOLDER) {// add the empty folder
                if (!res.getName().equals(PluginConstant.SVN_SUFFIX))
                    return isAllChildrenDeleted((IFolder) res);
            }
        }
        return true;
    }

    /**
     * 
     * DOC qiongli Comment method "getLogicalDelElemFromFolder".read from file system.
     * 
     * @param folder
     * @param fileList
     * @return
     * @throws Exception
     */
    private static List<Property> getLogicalDelElemFromFolder(IFolder folder, List<Property> fileList) throws Exception {
        IResource[] members = null;

        if (!folder.exists()) {
            return fileList;
        }
        members = folder.members();

        String type = null;
        for (IResource res : members) {
            if (res.getType() == IResource.FILE) {
                IFile file = (IFile) res;
                type = file.getFileExtension();
                if (type != null && type.equals(FactoriesUtil.PROPERTIES_EXTENSION)) {
                    Property property = PropertyHelper.getProperty(file);
                    if (property.getItem().getState().isDeleted()) {
                        fileList.add(property);
                    }
                }
            } else if (res.getType() == IResource.FOLDER) {// add the empty folder
                if (!res.getName().equals(PluginConstant.SVN_SUFFIX))
                    getLogicalDelElemFromFolder((IFolder) res, fileList);
            }
        }
        return fileList;

    }

    /**
     * 
     * DOC qiongli Comment method "getDelPropertyLs".Get all logical deleted Property.
     * 
     * @return
     */
    public static HashSet<Property> getDelPropertyLs() {

        if (delPropertys != null)
            return delPropertys;
        delPropertys = new HashSet<Property>();
        IFolder dataProfileFolder = ResourceManager.getDataProfilingFolder();
        IFolder libFolder = ResourceManager.getLibrariesFolder();
        IFolder metadataFolder = ResourceManager.getMetadataFolder();
        try {
            delPropertys.addAll(getLogicalDelElemFromFolder(dataProfileFolder, new ArrayList<Property>()));
            delPropertys.addAll(getLogicalDelElemFromFolder(libFolder, new ArrayList<Property>()));
            delPropertys.addAll(getLogicalDelElemFromFolder(metadataFolder, new ArrayList<Property>()));
        } catch (Exception e) {
            log.error(e, e);
        }
        return delPropertys;
    }

    /**
     * 
     * DOC qiongli Comment method "refreshDelProperty".Add or remove property.
     * 
     * @param type
     * @param prop
     */
    public static void refreshDelPropertys(int type, Property prop) {
        if (type == 0) {
            Iterator<Property> it = delPropertys.iterator();
            while (it.hasNext()) {
                Property property = it.next();
                if (property.getId().equals(prop.getId())) {
                    if (!prop.eIsProxy())
                        EMFSharedResources.getInstance().unloadResource(prop.eResource().getURI().toString());
                    it.remove();
                    break;
                }
            }
        } else if (type == 1) {
            delPropertys.add(prop);
        }
    }

}
