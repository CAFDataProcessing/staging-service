/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
 */
package com.github.cafdataprocessing.services.staging.models;


import com.github.cafdataprocessing.services.staging.dao.InputStreamSupplier;

public class StagedFile {

    private final String name;
    private final String contentType;
    private final InputStreamSupplier inputStreamSupplier;
    
    public StagedFile(String name, String contentType, final InputStreamSupplier inputStreamSupplier) {
        super();
        this.name = name;
        this.contentType = contentType;
        this.inputStreamSupplier = inputStreamSupplier;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }
    /**
     * @return the inputStreamSupplier
     */
    public InputStreamSupplier getInputStreamSupplier() {
        return inputStreamSupplier;
    }
    
}
