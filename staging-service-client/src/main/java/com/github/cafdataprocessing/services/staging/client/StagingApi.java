/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
 */
package com.github.cafdataprocessing.services.staging.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
            mpBuilder.addFormDataPart("uploadData",
                                       fileToStage.getName(),
                                       new StreamingBody(MediaType.parse(fileToStage.getContentType()),
                                               fileToStage.openInputStream())
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
            response.body();
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
        private final InputStream inputStream;
        private final MediaType contentType;

        StreamingBody(final MediaType contentType, final InputStream inputStream) {
          this.inputStream = inputStream;
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
                source = Okio.source(inputStream);
                sink.writeAll(source);
            } finally {
                Util.closeQuietly(source);
            }
        }
      }

}
