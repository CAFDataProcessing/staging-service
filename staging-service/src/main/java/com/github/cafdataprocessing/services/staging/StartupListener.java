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
package com.github.cafdataprocessing.services.staging;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StartupListener implements ApplicationListener<ApplicationPreparedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupListener.class);

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        final String basePath = event.getApplicationContext().getEnvironment().getProperty("staging.basePath");
        createFolder(basePath);
    }

    private void createFolder(final String path) {
        final Path baseStagingPath = Paths.get(path.toString());
        final File batchesFile = baseStagingPath.toFile();
        if (!batchesFile.exists()) {
            LOGGER.debug("Creating base staging folder: {}...", baseStagingPath);
            final boolean dirCreated = batchesFile.mkdirs();
            if (dirCreated) {
                LOGGER.debug("Created base staging folder: {}", baseStagingPath);
            }
            else
            {
                LOGGER.error("Error creating base staging folder: {}", baseStagingPath);
            }
        }
    }

}
