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

package org.talend.dataprofiler.core.ui.wizard.analysis.provider;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.dataprofiler.core.ui.utils.ComparatorsFactory;
import org.talend.dataprofiler.core.ui.views.provider.MNComposedAdapterFactory;
import orgomg.cwm.resource.relational.Catalog;

/**
 * DOC mzhao class global comment. Catalog content provider.
 */
public class CatalogContentProvider extends AdapterFactoryContentProvider {

    private static Logger log = Logger.getLogger(CatalogContentProvider.class);

    public CatalogContentProvider() {
        super(MNComposedAdapterFactory.getAdapterFactory());
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider#getChildren(java.lang.Object)
     */
    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IContainer) {
            try {
                return ((IContainer) parentElement).members();
            } catch (CoreException e) {
                log.error("Can't get the children of container:" + ((IContainer) parentElement).getLocation()); //$NON-NLS-1$
            }
        } else if (parentElement instanceof IRepositoryViewObject) {
            IRepositoryViewObject repoistoryViewObj = (IRepositoryViewObject) parentElement;
            Item item = repoistoryViewObj.getProperty().getItem();
            if (item instanceof ConnectionItem) {
                ((ConnectionItem) item).getConnection().getDataPackage();
                return ComparatorsFactory.sort(((ConnectionItem) item).getConnection().getDataPackage().toArray(),
                        ComparatorsFactory.MODELELEMENT_COMPARATOR_ID);
            }
        }

        // else if (parentElement instanceof IFile) {
        // IFile prvFile = (IFile) parentElement;
        // if (FactoriesUtil.isProvFile(prvFile.getFileExtension())) {
        // parentElement = PrvResourceFileHelper.getInstance().getFileResource((IFile) parentElement);
        // return ComparatorsFactory.sort(super.getChildren(parentElement),
        // ComparatorsFactory.MODELELEMENT_COMPARATOR_ID);
        // }
        // }
        return super.getChildren(parentElement);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(Object object) {
        // TODO Auto-generated method stub
        return this.getChildren(object);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider#getParent(java.lang.Object)
     */
    @Override
    public Object getParent(Object element) {
        if (element instanceof IContainer) {
            return ((IContainer) element).getParent();
        }
        return super.getParent(element);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider#hasChildren(java.lang.Object)
     */
    @Override
    public boolean hasChildren(Object element) {
        return !(element instanceof Catalog);
    }
}
