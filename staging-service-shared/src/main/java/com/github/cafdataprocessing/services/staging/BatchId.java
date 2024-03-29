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

import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchIdException;

public final class BatchId implements Comparable<BatchId>
{
    private final String value;

    public BatchId(final String value) throws InvalidBatchIdException
    {
        if (!value.matches("^[a-z0-9,.()\\-+_!]{0,127}[a-z0-9,()\\-+_!]$")) {
            throw new InvalidBatchIdException(value);
        }
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (!(obj instanceof BatchId)) {
            return false;
        }

        final BatchId other = (BatchId) obj;

        return value.equals(other.value);
    }

    @Override
    public int hashCode()
    {
        return value.hashCode();
    }

    @Override
    public int compareTo(final BatchId other)
    {
        return value.compareTo(other.value);
    }

    @Override
    public String toString()
    {
        return value;
    }
}
