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
package org.talend.cwm.compare.factory.comparisonlevel;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeRightTarget;
import org.eclipse.emf.compare.diff.service.DiffService;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.database.DqRepositoryViewService;
import org.talend.cwm.compare.DQStructureComparer;
import org.talend.cwm.compare.exception.ReloadCompareException;
import org.talend.cwm.helper.CatalogHelper;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.PackageHelper;
import org.talend.cwm.helper.SchemaHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.helper.TableHelper;
import org.talend.cwm.helper.ViewHelper;
import org.talend.cwm.relational.TdTable;
import org.talend.cwm.relational.TdView;
import org.talend.dq.nodes.foldernode.AbstractDatabaseFolderNode;
import org.talend.dq.nodes.foldernode.IFolderNode;
import org.talend.dq.writer.EMFSharedResources;
import orgomg.cwm.objectmodel.core.Package;
import orgomg.cwm.resource.relational.Catalog;
import orgomg.cwm.resource.relational.ColumnSet;
import orgomg.cwm.resource.relational.Schema;

/**
 * DOC rli class global comment. Detailled comment
 */
public class CatalogSchemaComparisonLevel extends AbstractComparisonLevel {

    private static Logger log = Logger.getLogger(CatalogSchemaComparisonLevel.class);

    private boolean isCompareTabel;

    private boolean isCompareView;

    public CatalogSchemaComparisonLevel(AbstractDatabaseFolderNode dbFolderNode) {
        super(dbFolderNode);
        int folderNodeType = dbFolderNode.getFolderNodeType();
        if (folderNodeType == IFolderNode.TABLEFOLDER_NODE_TYPE) {
            isCompareTabel = true;
        }

        if (folderNodeType == IFolderNode.VIEWFOLDER_NODE_TYPE) {
            isCompareView = true;
        }
    }

    @Override
    protected Connection findDataProvider() {
        Connection provider = ConnectionHelper.getTdDataProvider((Package) selectedObj);
        return provider;
    }

    @Override
    protected boolean compareWithReloadObject() throws ReloadCompareException {

        // MOD scorreia 2009-01-16 option initialized in CTOR
        MatchModel match = null;
        try {
            match = MatchService.doContentMatch((Package) selectedObj, getSavedReloadObject(), options);
        } catch (InterruptedException e) {
            log.error(e, e);
            return false;
        }
        final DiffModel diff = DiffService.doDiff(match, false);
        EList<DiffElement> ownedElements = diff.getOwnedElements();
        for (DiffElement de : ownedElements) {
            handleSubDiffElement(de);
        }
        return true;
    }

    private void handleSubDiffElement(DiffElement de) {
        if (de.getSubDiffElements().size() > 0) {
            EList<DiffElement> subDiffElements = de.getSubDiffElements();
            for (DiffElement difElement : subDiffElements) {
                handleSubDiffElement(difElement);
            }

        } else {
            handleDiffPackageElement(de);
        }
    }

    @Override
    protected EObject getSavedReloadObject() throws ReloadCompareException {
        Package selectedPackage = (Package) selectedObj;
        // MOD mzhao 2009-01-20 Extract method findMatchedPackage to
        // DQStructureComparer class
        // for common use.
        Package findMatchPackage = DQStructureComparer.findMatchedPackage(selectedPackage, tempReloadProvider);
        reloadElementOfPackage(findMatchPackage);
        return findMatchPackage;
    }

    @Override
    protected Resource getLeftResource() throws ReloadCompareException {
        Package selectedPackage = (Package) selectedObj;
        // MOD mzhao 2009-01-20 Extract method findMatchedPackage to
        // DQStructureComparer class
        // for common use.
        Package findMatchPackage = DQStructureComparer.findMatchedPackage(selectedPackage, copyedDataProvider);
        List<ColumnSet> columnSets = new ArrayList<ColumnSet>();

        if (isCompareTabel) {
            columnSets.addAll(PackageHelper.getTables(findMatchPackage));
        }

        if (isCompareView) {
            columnSets.addAll(PackageHelper.getViews(findMatchPackage));
        }

        Resource leftResource = copyedDataProvider.eResource();

        // ComparatorsFactory.sort(columnSets,
        // ComparatorsFactory.MODELELEMENT_COMPARATOR_ID);
        leftResource.getContents().clear();
        for (ColumnSet columnSet : columnSets) {
            // MOD mzhao 2009-01-20 Extract method clearSubNode to
            // DQStructureComparer class
            // for common use.
            DQStructureComparer.clearSubNode(columnSet);
            leftResource.getContents().add(columnSet);
        }
        // }
        EMFSharedResources.getInstance().saveResource(leftResource);
        return upperCaseResource(leftResource);
    }

    @Override
    protected Resource getRightResource() throws ReloadCompareException {
        Package selectedPackage = (Package) selectedObj;
        // MOD Extract method findMatchedPackage to DQStructureComparer class
        // for common use.
        Package toReloadObj = DQStructureComparer.findMatchedPackage(selectedPackage, tempReloadProvider);

        Resource rightResource = null;
        rightResource = tempReloadProvider.eResource();
        rightResource.getContents().clear();

        List<ColumnSet> columnSetList = reloadElementOfPackage(toReloadObj);
        if (isCompareTabel) {
            for (ColumnSet columnset : TableHelper.getTables(columnSetList)) {
                DQStructureComparer.clearSubNode(columnset);
                rightResource.getContents().add(columnset);
            }
        }

        if (isCompareView) {
            for (ColumnSet columnset : ViewHelper.getViews(columnSetList)) {
                DQStructureComparer.clearSubNode(columnset);
                rightResource.getContents().add(columnset);
            }
        }

        EMFSharedResources.getInstance().saveResource(rightResource);
        return upperCaseResource(rightResource);
    }

    /**
     * DOC rli Comment method "reloadElementOfPackage".
     * 
     * @param toReloadObj
     * @return
     * @throws ReloadCompareException
     */
    private List<ColumnSet> reloadElementOfPackage(Package toReloadObj) throws ReloadCompareException {
        List<ColumnSet> columnSetList = new ArrayList<ColumnSet>();
        try {
            Catalog catalogObj = SwitchHelpers.CATALOG_SWITCH.doSwitch(toReloadObj);
            Schema schemaObj = SwitchHelpers.SCHEMA_SWITCH.doSwitch(toReloadObj);
            if (catalogObj != null) {
                List<TdTable> tables = DqRepositoryViewService.getTables(tempReloadProvider, catalogObj, null, true);
                CatalogHelper.addTables(tables, catalogObj);
                columnSetList.addAll(tables);

                List<TdView> views = DqRepositoryViewService.getViews(tempReloadProvider, catalogObj, null, true);
                CatalogHelper.addViews(views, catalogObj);
                columnSetList.addAll(views);
            } else if (schemaObj != null) {
                List<TdTable> tables = DqRepositoryViewService.getTables(tempReloadProvider, schemaObj, null, true);
                SchemaHelper.addTables(tables, schemaObj);
                columnSetList.addAll(tables);

                List<TdView> views = DqRepositoryViewService.getViews(tempReloadProvider, schemaObj, null, true);
                SchemaHelper.addViews(views, schemaObj);
                columnSetList.addAll(views);

            } else {
                List<TdTable> tables = DqRepositoryViewService.getTables(tempReloadProvider, (Schema) toReloadObj, null, true);
                SchemaHelper.addTables(tables, (Schema) toReloadObj);
                columnSetList.addAll(tables);

                List<TdView> views = DqRepositoryViewService.getViews(tempReloadProvider, (Schema) toReloadObj, null, true);
                SchemaHelper.addViews(views, (Schema) toReloadObj);
                columnSetList.addAll(views);

            }
        } catch (Exception e1) {
            throw new ReloadCompareException(e1);
        }

        // EMFSharedResources.getInstance().saveResource(tempReloadProvider.eResource());
        return columnSetList;

    }

    @SuppressWarnings("deprecation")
    @Override
    protected void handleAddElement(ModelElementChangeRightTarget addElement) {
        EObject rightElement = addElement.getRightElement();
        ColumnSet columnSetSwitch = SwitchHelpers.COLUMN_SET_SWITCH.doSwitch(rightElement);
        if (columnSetSwitch != null) {

            if (isValidTableHandle(columnSetSwitch) || isValidViewHandle(columnSetSwitch)) {
                PackageHelper.addColumnSet(columnSetSwitch, (Package) selectedObj);
            }
        }
    }

    @Override
    protected void handleRemoveElement(ModelElementChangeLeftTarget removeElement) {
        ColumnSet removeColumnSet = SwitchHelpers.COLUMN_SET_SWITCH.doSwitch(removeElement.getLeftElement());
        if (removeColumnSet != null) {
            popRemoveElementConfirm();

            if (isValidTableHandle(removeColumnSet) || isValidViewHandle(removeColumnSet)) {
                PackageHelper.removeColumnSet(removeColumnSet, (Package) selectedObj);
            }
        }
    }

    /**
     * DOC bZhou Comment method "isValidViewHandle".
     * 
     * @param columnSetSwitch
     * @return
     */
    private boolean isValidViewHandle(ColumnSet columnSetSwitch) {
        TdView view = SwitchHelpers.VIEW_SWITCH.doSwitch(columnSetSwitch);
        return isCompareView && view != null;
    }

    /**
     * DOC bZhou Comment method "isValidTableHandle".
     * 
     * @param columnSetSwitch
     * @return
     */
    private boolean isValidTableHandle(ColumnSet columnSetSwitch) {
        TdTable table = SwitchHelpers.TABLE_SWITCH.doSwitch(columnSetSwitch);
        return isCompareTabel && table != null;
    }
}
