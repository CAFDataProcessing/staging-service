/*
 * Copyright 2019-2023 Open Text.
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
package com.github.cafdataprocessing.services.staging.exceptions;

public class BatchNotFoundException extends Exception
{

    public BatchNotFoundException()
    {
        super();
    }

    public BatchNotFoundException(final String message)
    {
        super(message);
    }

    public BatchNotFoundException(final Throwable cause)
    {
        super(cause);
    }

    public BatchNotFoundException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

}
