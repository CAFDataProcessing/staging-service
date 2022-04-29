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
package com.github.cafdataprocessing.worker.ingestion.models;

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.TenantId;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchIdException;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidTenantIdException;
import lombok.Getter;

@Getter
public class Subbatch
{
    private final String fileName;
    private final TenantId tenantId;
    private final BatchId batchId;

    public Subbatch(final String fileName, final String tenantId, final String batchId)
        throws InvalidBatchIdException, InvalidTenantIdException
    {
        this.tenantId = new TenantId(tenantId);
        this.batchId = new BatchId(batchId);
        this.fileName = fileName;
    }

    @Override
    public String toString()
    {
        return "subbatch:" + tenantId.getValue() + "/" + batchId.getValue() + "/" + fileName;
    }
}
