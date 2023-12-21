/*
 * Copyright 2019-2024 Open Text.
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
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
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

public final class DiskSpaceHealthIndicatorWithTimeoutTest
{
    private StagingController controller;

    private BatchDao fileSystemDao;
    private HttpServletRequest request;
    private StagingProperties stagingProperties;
    private DiskSpaceHealthIndicatorProperties diskSpaceHealthIndicatorProperties;

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setup()
    {
        fileSystemDao = Mockito.mock(BatchDao.class);
        request = Mockito.mock(HttpServletRequest.class);

        diskSpaceHealthIndicatorProperties = new DiskSpaceHealthIndicatorProperties();
        diskSpaceHealthIndicatorProperties.setPath(folder.getRoot());
        diskSpaceHealthIndicatorProperties.setThreshold(DataSize.ofMegabytes(1L));

        stagingProperties = new StagingProperties();
        stagingProperties.setHealthcheckTimeoutSeconds(60);
    }

    @Test
    public void healthCheckTest()
    {
        diskSpaceHealthIndicatorProperties.setPath(folder.getRoot());
        diskSpaceHealthIndicatorProperties.setThreshold(DataSize.ofMegabytes(1L));
        controller = new StagingController(fileSystemDao,
                                           request,
                                           diskSpaceHealthIndicatorProperties,
                                           stagingProperties);

        final ResponseEntity<StatusResponse> response = controller.getStatus("test-tenant");
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void healthCheckTestReadOnly()
    {
        final Path file = folder.getRoot().toPath().resolve("/test");

        diskSpaceHealthIndicatorProperties.setPath(file.toFile());
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
