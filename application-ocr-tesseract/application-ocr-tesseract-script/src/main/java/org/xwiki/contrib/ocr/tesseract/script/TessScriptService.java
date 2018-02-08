/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.ocr.tesseract.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.ocr.script.OCRScriptService;
import org.xwiki.contrib.ocr.tesseract.api.TessException;
import org.xwiki.contrib.ocr.tesseract.data.TessDataFileStore;
import org.xwiki.contrib.ocr.tesseract.data.TessDataManager;
import org.xwiki.contrib.ocr.tesseract.data.file.TessLocalDataFile;
import org.xwiki.contrib.ocr.tesseract.data.file.TessRemoteDataFile;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Define script services that are specific to the Tesseract library implementation of the OCR application.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Named(OCRScriptService.ROLE_HINT + "." + TessScriptService.ROLE_HINT)
@Unstable
public class TessScriptService implements ScriptService
{
    /**
     * The role suffix of the script service.
     */
    static final String ROLE_HINT = "tesseract";

    private static final WikiReference MAIN_WIKI = new WikiReference("xwiki");

    private JobStatus lastDataStoreUpdateStatus;

    private JobStatus lastDataFileDownloadStatus;

    @Inject
    private TessDataFileStore tessDataFileStore;

    @Inject
    private TessDataManager tessDataManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * @return a list of locally installed files.
     * @throws TessException if an error occurred
     */
    public List<TessLocalDataFile> getLocalFiles() throws TessException
    {
        return tessDataFileStore.getLocalDataFiles();
    }

    /**
     * @return a list of remotely available files.
     * @throws TessException if an error occurred
     */
    public List<TessRemoteDataFile> getRemotelyAvailableFiles() throws TessException
    {
        return tessDataFileStore.getRemoteDataFiles();
    }

    /**
     * @return true if the {@link TessDataFileStore} should be updated.
     */
    public boolean checkForStoreUpdate()
    {
        return tessDataFileStore.needsUpdate();
    }

    /**
     * Update the {@link TessDataFileStore}.
     * Will check for admin rights before performing the update.
     *
     * @return the job status associated with the update job
     * @throws TessException if an error happens
     */
    public JobStatus updateStore() throws TessException
    {
        DocumentReference currentUser = documentAccessBridge.getCurrentUserReference();

        try {
            authorizationManager.checkAccess(Right.ADMIN, currentUser, MAIN_WIKI);
            lastDataStoreUpdateStatus = tessDataFileStore.updateStore();
            return lastDataStoreUpdateStatus;
        } catch (AccessDeniedException e) {
            throw new TessException("Failed to update the file store.", e);
        }
    }

    /**
     * @return the last registered data store update job status. If no job has been triggered yet, returns null.
     */
    public JobStatus getCurrentDataStoreUpdateStatus()
    {
        return lastDataStoreUpdateStatus;
    }

    /**
     * Uses the given {@link TessRemoteDataFile} to download a training file on the sever.
     * Will check for admin rights before performing the download.
     *
     * @param remoteDataFile the file to download
     * @return a job status associated with the download job
     * @throws TessException if an error happens
     */
    public JobStatus downloadDataFile(TessRemoteDataFile remoteDataFile) throws TessException
    {
        DocumentReference currentUser = documentAccessBridge.getCurrentUserReference();

        try {
            authorizationManager.checkAccess(Right.ADMIN, currentUser, MAIN_WIKI);
            lastDataFileDownloadStatus = tessDataManager.downloadDataFile(remoteDataFile);
            return lastDataFileDownloadStatus;
        } catch (AccessDeniedException e) {
            throw new TessException("Failed to download the data file.", e);
        }
    }

    /**
     * Does the same as {@link #downloadDataFile(TessRemoteDataFile)} but by taking the language of the file to
     * download as a parameter.
     *
     * @param lang the language of the file to download
     * @return a job status associated with the download job
     * @throws TessException if an error happens
     */
    public JobStatus downloadDataFile(String lang) throws TessException
    {
        return downloadDataFile(tessDataFileStore.getRemoteFile(lang));
    }

    /**
     * @return the last registered data file download job status. If no job has been triggered yet, returns null.
     */
    public JobStatus getCurrentDataFileDownloadStatus()
    {
        return this.lastDataFileDownloadStatus;
    }
}