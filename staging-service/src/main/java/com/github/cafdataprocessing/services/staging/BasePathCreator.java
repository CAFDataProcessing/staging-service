/*
 * Copyright 2019-2021 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.services.staging;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BasePathCreator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BasePathCreator.class);

    @Autowired
    public BasePathCreator(final StagingProperties stagingProperties)
    {
        createFolder(stagingProperties.getBasePath());
    }

    private void createFolder(final String path)
    {
        final Path baseStagingPath = Paths.get(path);
        final File batchesFile = baseStagingPath.toFile();
        if (!batchesFile.exists()) {
            LOGGER.debug("Creating base staging folder: {}...", baseStagingPath);
            final boolean dirCreated = batchesFile.mkdirs();
            if (dirCreated) {
                LOGGER.debug("Created base staging folder: {}", baseStagingPath);
            } else {
                LOGGER.error("Error creating base staging folder: {}", baseStagingPath);
            }
        }
    }

}
