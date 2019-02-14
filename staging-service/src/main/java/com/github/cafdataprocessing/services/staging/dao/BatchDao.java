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
package com.github.cafdataprocessing.services.staging.dao;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.cafdataprocessing.services.staging.models.StagedFile;

public interface BatchDao {

    List<String> saveFiles(@Size(min = 1) String batchId, Stream<StagedFile> parts) throws JsonParseException, IOException;

    List<String> getFiles(@Size(min = 1, max = 256) @Valid String startsWith, @Size(min = 1, max = 256) @Valid String from,
            @Min(1) @Valid Integer limit) throws IOException;

    void deleteFiles(@Size(min = 1) String batchId) throws IOException;

}
