/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
