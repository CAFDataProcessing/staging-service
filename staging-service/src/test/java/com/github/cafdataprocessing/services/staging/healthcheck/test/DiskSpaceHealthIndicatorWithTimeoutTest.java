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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.unit.DataSize;

/**
 *
 * @author TBroadbent
 */
public class DiskSpaceHealthIndicatorWithTimeoutTest
{
    private StagingController controller;
    
//    @Mock
    BatchDao fileSystemDao;
//    @Mock
    HttpServletRequest request;
//    @Mock
//    DiskSpaceHealthIndicatorProperties diskSpaceHealthIndicatorProperties;
//    @Mock
    StagingProperties stagingProperties;
//    @Mock
    File file;
//    @Mock
    DataSize dataSize;
    
    Health health;
    
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();
    
//    private File file;
    
    @Before
    public void setup() throws IOException
    {
        fileSystemDao = Mockito.mock(BatchDao.class);
        request = Mockito.mock(HttpServletRequest.class);
//        stagingProperties = Mockito.mock(StagingProperties.class);
    }
    
    @Test
    public void healthCheckTest() throws IOException{
        final DiskSpaceHealthIndicatorProperties diskSpaceHealthIndicatorProperties = 
            new DiskSpaceHealthIndicatorProperties();
        
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
    
    @Test(expected=IOException.class)
    public void healthCheckTestReadOnly() throws IOException
    {
        final DiskSpaceHealthIndicatorProperties diskSpaceHealthIndicatorProperties
            = new DiskSpaceHealthIndicatorProperties();
        
         stagingProperties = new StagingProperties();
        stagingProperties.setHealthcheckTimeoutSeconds(60);
        
        File file = new File(folder.getRoot().getAbsolutePath()+"/test");
        file.setReadOnly();
        file.setWritable(false);

        diskSpaceHealthIndicatorProperties.setPath(file);
        diskSpaceHealthIndicatorProperties.setThreshold(DataSize.ofMegabytes(1L));
        controller = new StagingController(fileSystemDao,
                                           request,
                                           diskSpaceHealthIndicatorProperties,
                                           stagingProperties);

        assertNotNull(controller.getStatus("test-tenant"));
    }
    
    
}
