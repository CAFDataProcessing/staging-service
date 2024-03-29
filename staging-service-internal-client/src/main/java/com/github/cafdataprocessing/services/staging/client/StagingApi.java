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
package com.github.cafdataprocessing.services.staging.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Extension of the generated client to support uploading file streams.
 *
 */
public class StagingApi extends com.github.cafdataprocessing.services.staging.client.internal.StagingApi
{
    private final String TENANT_HEADER_NAME = "X-TENANT-ID";
    private final String PUT_API_PATH = "/batches/";

    public void createOrReplaceBatch(final String tenantId, final String batchId, final Stream<MultiPart> uploadData)
        throws ApiException
    {
        final MultipartBody.Builder mpBuilder = new MultipartBody.Builder().setType(MultipartBody.MIXED);
        final Iterator<MultiPart> uploadDataIterator = uploadData.iterator();
        while (uploadDataIterator.hasNext()) {
            final MultiPart fileToStage = uploadDataIterator.next();
            mpBuilder.addFormDataPart(fileToStage.getName(),
                                      null,
                                      new StreamingBody(MediaType.parse(fileToStage.getContentType()),
                                                        fileToStage::openInputStream)
            );
        }
        final RequestBody requestBody = mpBuilder.build();
        final String apiPath = getApiClient().getBasePath() + PUT_API_PATH + batchId;
        final Map<String, String> stagingHeaders = new HashMap<>();
        stagingHeaders.put(TENANT_HEADER_NAME, tenantId);
        final Request.Builder reqBuilder = new Request.Builder();
        getApiClient().processHeaderParams(stagingHeaders, reqBuilder);
        final Request request = reqBuilder
            .url(apiPath)
            .put(requestBody)
            .build();
        try {
            final Response response = getApiClient().getHttpClient().newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new ApiException("Error uploading documents for tenant " + tenantId + " batch: " + batchId,
                                       response.code(),
                                       null,
                                       response.message());
            }
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }

    class StreamingBody extends RequestBody
    {
        private final InputStreamSupplier inputStreamSupplier;
        private final MediaType contentType;

        //Try to maker this a supplier
        StreamingBody(final MediaType contentType, final InputStreamSupplier inputStreamSupplier)
        {
            this.inputStreamSupplier = inputStreamSupplier;
            this.contentType = contentType;
        }

        @Override
        public MediaType contentType()
        {
            return contentType;
        }

        @Override
        public void writeTo(final BufferedSink sink) throws IOException
        {
            Source source = null;
            try {
                source = Okio.source(inputStreamSupplier.get());
                sink.writeAll(source);
            } finally {
                closeQuietly(source);
            }
        }
    }

    /**
     * Closes {@code closeable}, ignoring any checked exceptions. Does nothing if {@code closeable} is null.
     */
    private static void closeQuietly(final Closeable closeable)
    {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException ex) {
            }
        }
    }
}
