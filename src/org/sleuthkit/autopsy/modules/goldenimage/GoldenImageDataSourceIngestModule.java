/*
 * GoldenImageDataSourceIngestModule
 * 
 */
package org.sleuthkit.autopsy.modules.goldenimage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.services.FileManager;
import org.sleuthkit.autopsy.casemodule.services.TagsManager;
import org.sleuthkit.autopsy.ingest.DataSourceIngestModule;
import org.sleuthkit.autopsy.ingest.DataSourceIngestModuleProgress;
import org.sleuthkit.autopsy.ingest.IngestJobContext;
import org.sleuthkit.autopsy.ingest.IngestModule;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.HashUtility;
import org.sleuthkit.datamodel.TagName;
import org.sleuthkit.datamodel.TskCoreException;

/**
 * Golden Image Ingest Module. This module iterates through every file of an
 * (dirty) image. It checks if the file is contained in another (golden) image.
 * In a next steps it creates an md5-hash of each file and compares them.
 * Depending on its result, it will tag the file either as Safe, Changed or
 * Deleted (Or leaves it untagged).
 */
class GoldenImageDataSourceIngestModule implements DataSourceIngestModule {

    // private final boolean skipKnownFiles;
    private IngestJobContext context = null;
    private final GoldenImageModuleIngestJobSettings settings;
    private final ArrayList<AbstractFile> comparisonFailFiles;
    private TagName giCustomDeletedTag = null;
    private ForkJoinPool executor;
    private Content dirtyImageDS = null;
    private Content goldenImageDS = null;
    private DataSourceIngestModuleProgress progressBar = null;
    private FileManager fileManager = null;
    private TagsManager tagsManager = null;

    GoldenImageDataSourceIngestModule(GoldenImageModuleIngestJobSettings pSettings) {
        settings = pSettings;
        comparisonFailFiles = new ArrayList<>();
        executor = new ForkJoinPool();
    }

    @Override
    public void startUp(IngestJobContext context) throws IngestModuleException {
        this.context = context;

    }

    @Override
    public ProcessResult process(Content dataSource, DataSourceIngestModuleProgress pProgressBar) {
        dirtyImageDS = dataSource;
        progressBar = pProgressBar;

        tagsManager = Case.getCurrentCase().getServices().getTagsManager();
        goldenImageDS = settings.getSelectedDatasource();

        if (goldenImageDS == null) {
            throw new IllegalStateException("Golden Image DS Ingest Module: The Golden Image Datasource is null.");
        }

        try {
            fileManager = Case.getCurrentCase().getServices().getFileManager();
            List<AbstractFile> allFiles = fileManager.findFiles(goldenImageDS, "%");
            if (!allFiles.isEmpty()) {
                ArrayList<FileWorkerThread> taskList = new ArrayList<>();
                progressBar.switchToDeterminate(allFiles.size());
                for (AbstractFile aFile : allFiles) {

                    //Check if the AbstractFile is a File. Continue if it's a directory or similar.
                    if (!aFile.isFile() || !aFile.canRead()) {
                        progressBar.progress("Jumping over Non-File.");
                        continue;
                    }

                    FileWorkerThread fileWorkerThread = new FileWorkerThread(aFile);
                    executor.submit(fileWorkerThread);

                }
            }

            //Stop processing if requested
            if (context.dataSourceIngestIsCancelled()) {
                return IngestModule.ProcessResult.OK;
            }

            int amountOfTasks = executor.getQueuedSubmissionCount();
            progressBar.switchToDeterminate(amountOfTasks);

            executor.shutdown();
            try {
                while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    progressBar.progress(amountOfTasks - executor.getQueuedSubmissionCount());
                    progressBar.progress("Comparing Files");
                }

            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }

            return IngestModule.ProcessResult.OK;

        } catch (TskCoreException ex) {
            Exceptions.printStackTrace(ex);
        }

        return IngestModule.ProcessResult.ERROR;
    }

    private TagName getCustomDeletedTag(String pDirtyImageName) {
        if (giCustomDeletedTag != null) {
            return giCustomDeletedTag;
        }

        TagsManager tagsManager = Case.getCurrentCase().getServices().getTagsManager();
        try {
            giCustomDeletedTag = tagsManager.addTagName("DI_DELETED_" + pDirtyImageName, "The file exists on the Golden Image, but not on the Dirty Image.", TagName.HTML_COLOR.LIME);
        } catch (TagsManager.TagNameAlreadyExistsException ex) {
            try {
                for (TagName tagName : tagsManager.getAllTagNames()) {
                    if (giCustomDeletedTag != null) {
                        break;
                    }

                    if (tagName.getDisplayName().equals("DI_DELETED_" + pDirtyImageName)) {
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
     * This method searches for a file by filename and filepath in the given
     * Datasource.
     *
     * @param pDataSource The Datasource in which the file should be searched in
     * @param pFile The File which should be found in the Datasource
     *
     * @return Returns an AbstractFile if the file was found in the datasource.
     * Returns Null if it wasn't found.
     */
    private AbstractFile findFile(Content pDataSource, AbstractFile pFile) {
        if (pFile.getName() == null || pFile.getName().equals("") || pFile.getParentPath() == null || pFile.getParentPath().equals("")) {
            return null;
        }

        try {
            ArrayList<AbstractFile> foundFiles = new ArrayList<>(fileManager.findFiles(pDataSource, (pFile.getName() != null ? pFile.getName() : ""), (pFile.getParentPath() != null ? pFile.getParentPath() : "")));
            if (foundFiles.size() > 0) {
                return foundFiles.get(0);
            }

        } catch (TskCoreException ex) {
            Exceptions.printStackTrace(ex);
        }

        return null;
    }

    /**
     * This method takes an Abstract File, checks if its hash is already
     * calculated, if not it tries to calculate it.
     *
     * @param AbstractFile The Abstract File of which the hash should be checked
     * & calculated.
     * @return boolean true if the hash was calculated, false if it already
     * exists or an error occured.
     *
     */
    private boolean calculateHash(AbstractFile pFile) {
        if (pFile.isFile() && pFile.canRead() && (pFile.getMd5Hash() == null || pFile.getMd5Hash().isEmpty())) {
            try {
                HashUtility.calculateMd5(pFile);
                return true;
            } catch (Exception ex) {
                String uniquePath = "";
                try {
                    uniquePath = pFile.getUniquePath();
                } catch (TskCoreException ex1) {
                    return false;
                }
                return false;
            }
        }
        return false;
    }

    private class FileWorkerThread implements Runnable {

        private final AbstractFile goldenImageFile;

        public FileWorkerThread(AbstractFile pGoldenImageFile) {
            goldenImageFile = pGoldenImageFile;
        }

        @Override
        public void run() {
            AbstractFile dirtyImageFile = findFile(dirtyImageDS, goldenImageFile);


            //Check if dirtyImageFile exists & is readable
            if (dirtyImageFile != null && dirtyImageFile.isFile() && dirtyImageFile.canRead()) {
                calculateHash(dirtyImageFile);
                calculateHash(goldenImageFile);

                if (dirtyImageFile.getMd5Hash() == null || goldenImageFile.getMd5Hash() == null) {
                    //Can't compare - One of the hashes is missing
                    comparisonFailFiles.add(goldenImageFile);
                } else if (dirtyImageFile.getMd5Hash().equals(goldenImageFile.getMd5Hash())) {
                    try {
                        tagsManager.addContentTag(dirtyImageFile, GoldenImageIngestModuleFactory.giTagGood, "");
                    } catch (TskCoreException ex) {
                        return;
                    }

                } else {
                    try {
                        tagsManager.addContentTag(dirtyImageFile, GoldenImageIngestModuleFactory.giTagChanged, "The Content of this file is different from it's equivalent on the golden image.");
                    } catch (TskCoreException ex) {
                        return;
                    }

                }
            } else {
                try {
                    tagsManager.addContentTag(goldenImageFile, getCustomDeletedTag(dirtyImageDS.getName()), "The file exists on the Golden Image, but not on the Dirty Image.");
                } catch (TskCoreException ex) {
                    return;
                }
            }
        }
    }
}
