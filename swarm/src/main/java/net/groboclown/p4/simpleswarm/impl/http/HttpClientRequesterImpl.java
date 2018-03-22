/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.groboclown.p4.simpleswarm.impl.http;

import net.groboclown.p4.simpleswarm.exceptions.UnauthorizedAccessException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

public class HttpClientRequesterImpl
        implements HttpClientRequester {
    @Override
    public <T> T execute(HttpUriRequest request, HttpClientContext context, RequestFunction<T> func)
            throws IOException, UnauthorizedAccessException {
        HttpClient client = HttpClientBuilder.create().build();
        try {
            return func.run(client.execute(request, context));
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }

    @Override
    public <T> T execute(HttpUriRequest request, HttpClientContext context, CredentialsProvider provider,
            RequestFunction<T> func)
            throws IOException, UnauthorizedAccessException {
        HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
        try {
            return func.run(client.execute(request, context));
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }
}
