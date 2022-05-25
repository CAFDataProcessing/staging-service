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
package com.github.cafdataprocessing.services.staging.healthcheck.test;

import com.github.cafdataprocessing.services.staging.StagingController;
import com.github.cafdataprocessing.services.staging.StagingProperties;
import com.github.cafdataprocessing.services.staging.dao.BatchDao;
import com.github.cafdataprocessing.services.staging.models.StatusResponse;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;

/**
 *
 * @author TBroadbent
 */
public class DiskSpaceHealthIndicatorWithTimeoutTest
{
    private StagingController controller;

    BatchDao fileSystemDao;
    HttpServletRequest request;
    StagingProperties stagingProperties;
    File file;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setup() throws IOException
    {
        fileSystemDao = Mockito.mock(BatchDao.class);
        request = Mockito.mock(HttpServletRequest.class);
    }

    @Test
    public void healthCheckTest() throws IOException
    {
        final DiskSpaceHealthIndicatorProperties diskSpaceHealthIndicatorProperties
            = new DiskSpaceHealthIndicatorProperties();

        stagingProperties = new StagingProperties();
        stagingProperties.setHealthcheckTimeoutSeconds(60);

        diskSpaceHealthIndicatorProperties.setPath(folder.getRoot());
        diskSpaceHealthIndicatorProperties.setThreshold(DataSize.ofMegabytes(1L));
        controller = new StagingController(fileSystemDao,
                                           request,
                                           diskSpaceHealthIndicatorProperties,
                                           stagingProperties);

        assertNotNull(controller.getStatus("test-tenant"));
    }

    @Test
    public void healthCheckTestReadOnly() throws IOException
    {
        final DiskSpaceHealthIndicatorProperties diskSpaceHealthIndicatorProperties
            = new DiskSpaceHealthIndicatorProperties();

        stagingProperties = new StagingProperties();
        stagingProperties.setHealthcheckTimeoutSeconds(60);

        final File file = new File(folder.getRoot().getAbsolutePath() + "/test");
        file.setReadOnly();
        file.setWritable(false);

        diskSpaceHealthIndicatorProperties.setPath(file);
        diskSpaceHealthIndicatorProperties.setThreshold(DataSize.ofMegabytes(1L));
        controller = new StagingController(fileSystemDao,
                                           request,
                                           diskSpaceHealthIndicatorProperties,
                                           stagingProperties);

        final ResponseEntity<StatusResponse> response = controller.getStatus("test-tenant");
        assertNotNull(response);
        assertEquals(503, response.getStatusCodeValue());
    }

}
