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
package com.github.cafdataprocessing.services.staging.utils;

import com.microfocus.caf.worker.document.schema.validator.InvalidDocumentException;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JsonMinifierTest
{

    final String inprogressContentFolderPath = "/etc/store/batches/acme-com/in_progress/test_batch/files";
    final int fieldValueSizeThreshold = 8192; // 8KB

    @SuppressWarnings("serial")
    @Test
    public void minifyJsonTest() throws Exception
    {
        System.out.println("minifyJsonTest...");
        final InputStream inputStream = JsonMinifierTest.class.getResource("/batch1.json").openStream();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final Map<String, String> map = new HashMap<String, String>()
        {
            {
                put("A_Christmas_Carol1.txt", UUID.randomUUID().toString());
                put("A_Christmas_Carol2.txt", UUID.randomUUID().toString());
            }
        };
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, map);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "minifyJsonTest");
    }

    @Test
    public void minifyNoRefJsonTest() throws Exception
    {
        System.out.println("minifyNoRefJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': {'data': 'Mark Roberts'},"
            + "        'TO': {'data': 'Gene Simmons'},"
            + "        'SUBJECT': {'data': 'Favourite book'},"
            + "        'CONTENT': {'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyNoRefJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "minifyNoRefJsonTest");
    }

    @Test
    public void minifyUTFEncodingOnlyJsonTest() throws Exception
    {
        System.out.println("minifyUTFEncodingOnlyJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': {'encoding': 'utf-8', 'data': 'Mark Roberts'},"
            + "        'TO': {'data': 'Gene Simmons'},"
            + "        'SUBJECT': {'data': 'Favourite book'},"
            + "        'CONTENT': {'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, new HashMap<>());
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyUTFEncodingOnlyJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "minifyUTFEncodingOnlyJsonTest");
    }

    @Test
    public void minifyStorageRefJsonTest() throws Exception
    {
        System.out.println("minifyStorageRefJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': {'data': 'Mark Roberts', 'encoding': 'utf-8'},"
            + "        'TO': {'data': 'Gene Simmons'},"
            + "        'SUBJECT': {'data': 'Favourite book'},"
            + "        'CONTENT': {'data': 'This is the book that popularised the use of the phrase Merry Christmas.'},"
            + "        'BINARY_FILE': {"
            + "            'data': 'http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc',"
            + "            'encoding': 'storage_ref'"
            + "           }"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyStorageRefJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "minifyStorageRefJsonTest");
    }

    @SuppressWarnings("serial")
    @Test
    public void minifyLocalRefJsonTest() throws Exception
    {
        System.out.println("minifyLocalRefJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': {'data': 'Mark Roberts', 'encoding': 'utf-8'},"
            + "        'TO': {'data': 'Gene Simmons'},"
            + "        'SUBJECT': {'data': 'Favourite book'},"
            + "        'CONTENT': {'data': 'This is the book that popularised the use of the phrase Merry Christmas.'},"
            + "        'BINARY_FILE': {"
            + "            'data': 'http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc',"
            + "            'encoding': 'storage_ref'"
            + "           },"
            + "        'COVER_PIC': {"
            + "            'encoding': 'local_ref',"
            + "            'data': 'Front_Cover.jpg'"
            + "          }"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final Map<String, String> map = new HashMap<String, String>()
        {
            {
                put("Front_Cover.jpg", UUID.randomUUID().toString());
            }
        };
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, map);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyLocalRefJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "minifyLocalRefJsonTest");
    }

    @SuppressWarnings("serial")
    @Test
    public void storageAndLocalRefFirstTest() throws Exception
    {
        System.out.println("minifyLocalRefJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'BINARY_FILE': {"
            + "            'encoding': 'storage_ref',"
            + "            'data': 'http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc'"
            + "           },"
            + "        'COVER_PIC': {"
            + "            'encoding': 'local_ref',"
            + "            'data': 'Front_Cover.jpg'"
            + "          }"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final Map<String, String> map = new HashMap<String, String>()
        {
            {
                put("Front_Cover.jpg", UUID.randomUUID().toString());
            }
        };
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, map);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyLocalRefJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "minifyLocalRefJsonTest");
    }

    @Test
    public void minifySimpleDocJsonTest() throws Exception
    {
        System.out.println("minifySimpleDocJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'STATUS': 'DELETED'"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifySimpleDocJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "minifySimpleDocJsonTest");
    }

    @Test
    public void minifyBase64EncodingJsonTest() throws Exception
    {
        System.out.println("minifyBase64EncodingJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': {'data': 'Mark Roberts', 'encoding': 'utf-8'},"
            + "        'TO': {'data': 'Gene Simmons'},"
            + "        'SUBJECT': {'data': 'Favourite book'},"
            + "        'CONTENT': {'data': 'This is the book that popularised the use of the phrase Merry Christmas.'},"
            + "        'BINARY_FILE': {"
            + "            'data': 'http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc',"
            + "            'encoding': 'storage_ref'"
            + "           },"
            + "        'COVER_PIC': {"
            + "            'encoding': 'base64',"
            + "            'data': 'QSBDaHJpc3RtYXMgQ2Fyb2wgQm9vayBDb3Zlcg=='"
            + "          }"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, new HashMap<>());
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyBase64EncodingJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("base64"), "minifyBase64EncodingJsonTest");
    }

    @Test
    public void minifyArrayFieldJsonTest() throws Exception
    {
        System.out.println("minifyArrayFieldJsonTest...");
        final InputStream inputStream = JsonMinifierTest.class.getResource("/batch2.json").openStream();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyArrayFieldJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "minifyArrayFieldJsonTest");
    }

    @Test
    public void minifyNestedDocJsonTest() throws Exception
    {
        System.out.println("minifyNestedDocJsonTest...");
        final InputStream inputStream = JsonMinifierTest.class.getResource("/batch3.json").openStream();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyNestedDocJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("subdocuments"), "minifyNestedDocJsonTest");
    }

    @Test
    public void validateAndMinifyInvalidJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyInvalidJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': {'data': 'Mark Roberts'},"
            + "        'TO': {'data': 'Gene Simmons'},"
            + "        'SUBJECT': {'data': 'Favourite book'},"
            + "        'CONTENT': {'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            JsonMinifier.validateAndMinifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                               inprogressContentFolderPath, fieldValueSizeThreshold, null);
            fail("validateAndMinifyInvalidJsonTest failed: Validation done incorectly");
        } catch (final InvalidDocumentException e) {
            assertNotNull(e.getMessage(), "validateAndMinifyInvalidJsonTest");
        }
    }

    @SuppressWarnings("serial")
    @Test
    public void validateAndMinifyJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyJsonTest...");
        final InputStream inputStream = JsonMinifierTest.class.getResource("/batch1.json").openStream();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final Map<String, String> map = new HashMap<String, String>()
        {
            {
                put("A_Christmas_Carol1.txt", UUID.randomUUID().toString());
                put("A_Christmas_Carol2.txt", UUID.randomUUID().toString());
            }
        };
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, map);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "validateAndMinifyJsonTest");
    }

    @Test
    public void validateAndMinifyNoRefJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyNoRefJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': [{'data': 'Mark Roberts'}],"
            + "        'TO': [{'data': 'Gene Simmons'}],"
            + "        'SUBJECT': [{'data': 'Favourite book'}],"
            + "        'CONTENT': [{'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}]"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyNoRefJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "validateAndMinifyNoRefJsonTest");
    }

    @Test
    public void validateAndMinifyUTFEncodingOnlyJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyUTFEncodingOnlyJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': [{'encoding': 'utf-8', 'data': 'Mark Roberts'}],"
            + "        'TO': [{'data': 'Gene Simmons'}],"
            + "        'SUBJECT': [{'data': 'Favourite book'}],"
            + "        'CONTENT':[ {'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}]"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyUTFEncodingOnlyJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "validateAndMinifyUTFEncodingOnlyJsonTest");
    }

    @Test
    public void validateAndMinifyStorageRefJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyStorageRefJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': [{'data': 'Mark Roberts', 'encoding': 'utf-8'}],"
            + "        'TO': [{'data': 'Gene Simmons'}],"
            + "        'SUBJECT': [{'data': 'Favourite book'}],"
            + "        'CONTENT': [{'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}],"
            + "        'BINARY_FILE': [{"
            + "            'data': 'http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc',"
            + "            'encoding': 'storage_ref'"
            + "           }]"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyStorageRefJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "validateAndMinifyStorageRefJsonTest");
    }

    @SuppressWarnings("serial")
    @Test
    public void validateAndMinifyLocalRefJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyLocalRefJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': [{'data': 'Mark Roberts', 'encoding': 'utf-8'}],"
            + "        'TO': [{'data': 'Gene Simmons'}],"
            + "        'SUBJECT': [{'data': 'Favourite book'}],"
            + "        'CONTENT': [{'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}],"
            + "        'BINARY_FILE': [{"
            + "            'data': 'http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc',"
            + "            'encoding': 'storage_ref'"
            + "           }],"
            + "        'COVER_PIC': [{"
            + "            'encoding': 'local_ref',"
            + "            'data': 'Front_Cover.jpg'"
            + "          }]"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final Map<String, String> map = new HashMap<String, String>()
        {
            {
                put("Front_Cover.jpg", UUID.randomUUID().toString());
            }
        };
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, map);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyLocalRefJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "validateAndMinifyLocalRefJsonTest");
    }

    @Test
    public void validateAndMinifySimpleDocJsonTest() throws Exception
    {
        System.out.println("validateAndMinifySimpleDocJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'STATUS': [{'data': 'DELETED'}]"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifySimpleDocJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "validateAndMinifySimpleDocJsonTest");
    }

    @Test
    public void validateAndMinifyBase64EncodingJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyBase64EncodingJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': [{'data': 'Mark Roberts', 'encoding': 'utf-8'}],"
            + "        'TO': [{'data': 'Gene Simmons'}],"
            + "        'SUBJECT': [{'data': 'Favourite book'}],"
            + "        'CONTENT': [{'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}],"
            + "        'BINARY_FILE': [{"
            + "            'data': 'http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc',"
            + "            'encoding': 'storage_ref'"
            + "           }],"
            + "        'COVER_PIC': [{"
            + "            'encoding': 'base64',"
            + "            'data': 'QSBDaHJpc3RtYXMgQ2Fyb2wgQm9vayBDb3Zlcg=='"
            + "          }]"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyBase64EncodingJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("base64"), "validateAndMinifyBase64EncodingJsonTest");
    }

    @Test
    public void validateAndMinifyArrayFieldJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyArrayFieldJsonTest...");
        final InputStream inputStream = JsonMinifierTest.class.getResource("/batch2.json").openStream();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyArrayFieldJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "validateAndMinifyArrayFieldJsonTest");
    }

    @Test
    public void validateAndMinifyNestedDocJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyNestedDocJsonTest...");
        final InputStream inputStream = JsonMinifierTest.class.getResource("/batch3.json").openStream();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyNestedDocJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("subdocuments"), "validateAndMinifyNestedDocJsonTest");
    }

    @Test
    public void validateAndMinifyNoRefMultiValueJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyNoRefMultiValueJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': [{'data': 'Mark Roberts'}],"
            + "        'TO': [{'data': 'Gene Simmons'}],"
            + "        'SUBJECT': [{'data': 'Favourite book'}],"
            + "        'CONTENT': [{'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}],"
            + "        'PUBLISHER': [{'data': 'Chapman and Hall'}, {'data': 'Elliot Stock'}]"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, null);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyNoRefMultiValueJsonTest : Minified Json : " + minifiedJson);
        assertFalse(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "validateAndMinifyNoRefMultiValueJsonTest");
    }

    @SuppressWarnings("serial")
    @Test
    public void validateAndMinifyLocalRefMultiValueJsonTest() throws Exception
    {
        System.out.println("validateAndMinifyLocalRefMultiValueJsonTest...");
        String testJson
            = "{"
            + "    'document': {"
            + "      'reference': 'batch2.msg',"
            + "      'fields': {"
            + "        'FROM': [{'data': 'Mark Roberts'}],"
            + "        'TO': [{'data': 'Gene Simmons'}],"
            + "        'SUBJECT': [{'data': 'Favourite book'}],"
            + "        'CONTENT': [{'data': 'This is the book that popularised the use of the phrase Merry Christmas.'}],"
            + "        'SUMMARY': [{'data': 'A_Christmas_Carol2.txt', 'encoding': 'local_ref'}],"
            + "        'PUBLISHER': [{'data': 'Chapman and Hall'}, {'data': 'Elliot Stock'}]"
            + "      }"
            + "    }"
            + "  }";
        testJson = testJson.replaceAll("'", "\"");
        final InputStream inputStream = new ByteArrayInputStream(testJson.getBytes("UTF-8"));
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final Map<String, String> map = new HashMap<String, String>()
        {
            {
                put("A_Christmas_Carol2.txt", UUID.randomUUID().toString());
            }
        };
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, map);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("validateAndMinifyLocalRefMultiValueJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("/etc/store/batches/acme-com/completed/test_batch/files"), "validateAndMinifyLocalRefMultiValueJsonTest");
        assertTrue(minifiedJson.contains("\"PUBLISHER\":[{\"data\":\"Chapman and Hall\"}"), "validateAndMinifyLocalRefMultiValueJsonTest");
    }

    @SuppressWarnings("serial")
    @Test
    public void minifyLargeDataJsonTest() throws Exception
    {
        System.out.println("minifyLargeDataJsonTest...");
        final InputStream inputStream = JsonMinifierTest.class.getResource("/largeDataBatch.json").openStream();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final Map<String, String> map = new HashMap<String, String>()
        {
            {
                put("A_Christmas_Carol1.txt", UUID.randomUUID().toString());
                put("A_Christmas_Carol2.txt", UUID.randomUUID().toString());
            }
        };
        JsonMinifier.minifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                inprogressContentFolderPath, fieldValueSizeThreshold, map);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyLargeDataJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("\"SUBJECT\":[{\"data\":\"/etc/store/batches/acme-com/completed/test_batch/files/"),
                "minifyLargeDataJsonTest");
    }

    @SuppressWarnings("serial")
    @Test
    public void minifyLargeBase64DataJsonTest() throws Exception
    {
        System.out.println("minifyLargeBase64DataJsonTest...");
        final InputStream inputStream = JsonMinifierTest.class.getResource("/largeBase64DataBatch.json").openStream();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final Map<String, String> map = new HashMap<String, String>()
        {
            {
                put("A_Christmas_Carol1.txt", UUID.randomUUID().toString());
                put("A_Christmas_Carol2.txt", UUID.randomUUID().toString());
            }
        };
        JsonMinifier.validateAndMinifyJson(inputStream, outStream, "/etc/store/batches/acme-com/completed/test_batch/files",
                                           inprogressContentFolderPath, fieldValueSizeThreshold, map);
        final String minifiedJson = outStream.toString("UTF-8");
        System.out.println("minifyLargeBase64DataJsonTest : Minified Json : " + minifiedJson);
        assertTrue(minifiedJson.contains("\"COVER_PIC\":[{\"data\":\"/etc/store/batches/acme-com/completed/test_batch/files"),
                "minifyLargeBase64DataJsonTest");
        assertTrue(minifiedJson.contains(".bin"), "minifyLargeBase64DataJsonTest");
    }
}
