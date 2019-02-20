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
package com.github.cafdataprocessing.services.staging.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.stream.Stream;

import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.Util;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Extension of the generated client to support uploading file streams.
 *
 */
public class StagingApi extends com.github.cafdataprocessing.services.staging.client.internal.StagingApi {
    private final String PUT_API_PATH = "/batches/";

    public StagingBatchResponse addDocumentsToBatch(final String batchId, final Stream<MultiPart> uploadData)
            throws ApiException, FileNotFoundException, IOException {
        final MultipartBuilder mpBuilder = new MultipartBuilder().type(MultipartBuilder.MIXED);
        final Iterator<MultiPart> uploadDataIterator = uploadData.iterator();
        while(uploadDataIterator.hasNext())
        {
            final MultiPart fileToStage = uploadDataIterator.next();
            mpBuilder.addFormDataPart(fileToStage.getName(),
                                       null,
                                       new StreamingBody(MediaType.parse(fileToStage.getContentType()),
                                               fileToStage::openInputStream)
                                     );
        }
        final RequestBody requestBody = mpBuilder.build();
        final String apiPath = getApiClient().getBasePath() + PUT_API_PATH + batchId;
        final Request request = new Request.Builder()
                .url(apiPath)
                .put(requestBody)
                .build();
        final Response response = getApiClient().getHttpClient().newCall(request).execute();
        if (!response.isSuccessful())
        {
            throw new ApiException("Error adding documents to batch: " + batchId,
                                   response.code(),
                                   null,
                                   response.body().string());
        }
        else
        {
            final Type returnType = new TypeToken<StagingBatchResponse>(){}.getType();
            return (StagingBatchResponse)getApiClient().handleResponse(response, returnType);
        }
    }

    class StreamingBody extends RequestBody {
        private final InputStreamSupplier inputStreamSupplier;
        private final MediaType contentType;

        //Try to maker this a supplier
        StreamingBody(final MediaType contentType, final InputStreamSupplier inputStreamSupplier) {
          this.inputStreamSupplier = inputStreamSupplier;
          this.contentType = contentType;
        }

        @Override
        public MediaType contentType() {
          return contentType;
        }

        @Override
        public void writeTo(final BufferedSink sink) throws IOException {
            Source source = null;
            try {
                source = Okio.source(inputStreamSupplier.get());
                sink.writeAll(source);
            } finally {
                Util.closeQuietly(source);
            }
        }
      }

}
