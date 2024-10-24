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
package com.github.cafdataprocessing.services.staging;

import com.github.cafdataprocessing.services.staging.client.ApiClient;
import com.github.cafdataprocessing.services.staging.client.ApiException;
import com.github.cafdataprocessing.services.staging.client.MultiPart;
import com.github.cafdataprocessing.services.staging.client.MultiPartContent;
import com.github.cafdataprocessing.services.staging.client.MultiPartDocument;
import com.github.cafdataprocessing.services.staging.client.StagingApi;
import com.github.cafdataprocessing.services.staging.client.StagingBatchList;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StagingServiceWindowsIT
{
    private static final String STAGING_SERVICE_URI = System.getenv("staging-service");
    //private static final String STAGING_SERVICE_URI = "http://192.168.56.10:8080";
    private final StagingApi stagingApi;

    public StagingServiceWindowsIT()
    {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(STAGING_SERVICE_URI);
        stagingApi = new StagingApi();
        stagingApi.setApiClient(apiClient);
    }

    @BeforeEach
    public void windowsOnly()
    {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        System.out.println("Windows Tests");
    }

    @Test
    public void uploadWindowsPathTest() throws Exception
    {
        final String tenantId = "tenant-windows1";
        final String file1AbsolutePath = Paths.get("src", "test", "resources", "A_Christmas_Carol1.txt").toAbsolutePath().toString();
        final String file2AbsolutePath = Paths.get("src", "test", "resources", "A_Christmas_Carol2.txt").toAbsolutePath().toString();
        final String[] contentFiles = new String[]{file1AbsolutePath, file2AbsolutePath};
        final StringBuilder document = new StringBuilder("{\n"
            + "  \"document\": {\n"
            + "    \"reference\": \"fav-book.msg\",\n"
            + "    \"fields\": {\n"
            + "      \"FROM\": [{\"data\": \"Mark Roberts\"}],\n"
            + "      \"TO\": [{\"data\": \"Gene Simmons\"}],\n"
            + "      \"SUBJECT\": [{\"data\": \"Favourite book\"}],\n"
            + "      \"CONTENT\": [{\"data\": \"This is the book that popularised the use of the phrase \\\"Merry Christmas\\\".\"}]\n"
            + "    },\n"
            + "    \"subdocuments\": [{\n"
            + "      \"reference\": \"xmas-carol.doc\",\n"
            + "      \"fields\": {\n"
            + "        \"BINARY_FILE\": [{\n"
            + "           \"data\": \"http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc\",\n"
            + "           \"encoding\": \"storage_ref\"\n"
            + "          }],\n"
            + "        \"TITLE\": [{\"data\": \"A Christmas Carol\"}],\n"
            + "        \"AUTHOR\": [{\"data\": \"Charles Dickens\"}],\n"
            + "        \"PUB_DATE\": [{\"data\": \"December 19, 1843\"}],\n"
            + "        \"CONTENT\": [{\n"
            + "           \"data\": \"");
        document.append(file1AbsolutePath.replaceAll("\\\\", "\\\\\\\\"));
        document.append("\",\n"
            + "           \"encoding\": \"local_ref\"\n"
            + "          }],\n"
            + "        \"SUMMARY\": [{\n"
            + "            \"encoding\": \"local_ref\",\n"
            + "            \"data\": \"");
        document.append(file2AbsolutePath.replaceAll("\\\\", "\\\\\\\\"));
        document.append("\"\n"
            + "          }],\n"
            + "        \"PUBLISHER\": [\n"
            + "          {\"data\": \"Chapman and Hall\"},\n"
            + "          {\"data\": \"Elliot Stock\"}\n"
            + "        ]\n"
            + "      }\n"
            + "    }]\n"
            + "  }\n"
            + "}");

        System.out.println("Document prepared:\n" + document.toString());
        final String[] documentFiles = new String[]{document.toString()};
        final String batchId = "test-batch1";
        stageMultiParts(tenantId, batchId, contentFiles, documentFiles);
        final StagingBatchList response = stagingApi.getBatches(tenantId, batchId, batchId, 10);
        assertEquals(1, response.getEntries().size(), "uploadDocumentsToBatchTest, 1 batch uploaded");
    }

    private void stageMultiParts(final String tenantId, final String batchId, final String[] contentFiles, final String[] documentFiles)
        throws IOException, ApiException
    {
        final List<MultiPart> uploadData = new ArrayList<>();
        for (final String file : contentFiles) {
            uploadData.add(new MultiPartContent(file, () -> new FileInputStream(file)));
        }
        for (final String file : documentFiles) {
            File documentFile = Files.createTempFile(Files.createTempDirectory("docBase"), "doc-", ".json").toFile();
            documentFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(new ByteArrayInputStream(file.getBytes()), documentFile);
            uploadData.add(new MultiPartDocument(documentFile));
        }
        stagingApi.createOrReplaceBatch(tenantId, batchId, uploadData.stream());
    }
}
