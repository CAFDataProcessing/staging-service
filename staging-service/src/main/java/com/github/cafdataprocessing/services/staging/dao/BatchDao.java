/*
 * Copyright 2019-2022 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.services.staging.dao;

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.TenantId;
import com.github.cafdataprocessing.services.staging.exceptions.BatchNotFoundException;
import com.github.cafdataprocessing.services.staging.exceptions.IncompleteBatchException;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchException;
import com.github.cafdataprocessing.services.staging.exceptions.StagingException;
import java.util.List;
import org.apache.commons.fileupload.FileItemIterator;
import com.github.cafdataprocessing.services.staging.models.BatchStatusResponse;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public interface BatchDao
{

    List<String> saveFiles(TenantId tenantId, @Size(min = 1) BatchId batchId, FileItemIterator fileItemIterator)
        throws IncompleteBatchException, InvalidBatchException, StagingException;

    List<String> getBatches(TenantId tenantId, @Size(min = 1, max = 256) @Valid String startsWith, @Size(min = 1, max = 256) @Valid BatchId from,
                            @Min(1) @Valid Integer limit) throws StagingException;

    void deleteBatch(TenantId tenantId, @Size(min = 1) BatchId BatchId) throws BatchNotFoundException, StagingException;

    void cleanUpStaleInprogressBatches();

    BatchStatusResponse getBatchStatus(TenantId tenantId, @Size(max = 1) BatchId batchId) throws BatchNotFoundException, StagingException;

}
