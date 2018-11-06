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

package net.groboclown.p4.simpleswarm.impl;

import net.groboclown.p4.simpleswarm.exceptions.UnauthorizedAccessException;
import net.groboclown.p4.simpleswarm.impl.http.HttpClientRequester;
import net.groboclown.p4.simpleswarm.impl.http.RequestFunction;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MockHttpRequester implements HttpClientRequester {
    public interface Verifier<T> {
        void verify(T t);
    }

    public static class ExecuteBuilder<T> {
        private Verifier<HttpUriRequest> requestVerifier;
        private Verifier<HttpClientContext> contextVerifier;
        private Verifier<CredentialsProvider> credentialVerifier;
        private Verifier<T> funcReturnVerifier;
        private Verifier<Exception> exceptionVerifier;
        private HttpResponse response;
        private Exception exception;

        ExecuteBuilder<T> expectsRequest(Verifier<HttpUriRequest> v) {
            requestVerifier = v;
            return this;
        }

        ExecuteBuilder<T> expectsContext(Verifier<HttpClientContext> v) {
            contextVerifier = v;
            return this;
        }

        ExecuteBuilder<T> expectsCredential(Verifier<CredentialsProvider> v) {
            credentialVerifier = v;
            return this;
        }

        ExecuteBuilder<T> expectsReturn(Verifier<T> v) {
            funcReturnVerifier = v;
            return this;
        }

        ExecuteBuilder<T> expectsException(Verifier<Exception> v) {
            exceptionVerifier = v;
            return this;
        }

        ExecuteBuilder<T> withResponse(HttpResponse r) {
            response = r;
            return this;
        }

        ExecuteBuilder<T> withResponse(int code, String status, String mimeType, String body) {
            BasicHttpResponse r = new BasicHttpResponse(
                    new BasicStatusLine(HttpVersion.HTTP_1_1, code, status)
            );
            StringEntity e = new StringEntity(body, ContentType.create(mimeType, Charset.forName("UTF-8")));
            r.setEntity(e);
            return withResponse(r);
        }

        ExecuteBuilder<T> withException(Exception e) {
            exception = e;
            return this;
        }

        private T handle(HttpUriRequest request, HttpClientContext context, CredentialsProvider provider,
                RequestFunction<T> func)
                throws Exception {
            if (requestVerifier != null) {
                requestVerifier.verify(request);
            }
            if (contextVerifier != null) {
                contextVerifier.verify(context);
            }
            if (credentialVerifier != null) {
                credentialVerifier.verify(provider);
            }
            if (exception != null) {
                throw exception;
            }
            try {
                T ret = func.run(response);
                if (funcReturnVerifier != null) {
                    funcReturnVerifier.verify(ret);
                }
                return ret;
            } catch (IOException | UnauthorizedAccessException e) {
                if (exceptionVerifier != null) {
                    exceptionVerifier.verify(e);
                }
                throw e;
            }
        }
    }

    private List<ExecuteBuilder<?>> executeVerifiers = new ArrayList<>();
    private int executeIndex = -1;

    <T> ExecuteBuilder<T> withCall() {
        ExecuteBuilder<T> ret = new ExecuteBuilder<>();
        executeVerifiers.add(ret);
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T execute(HttpUriRequest request, HttpClientContext context, RequestFunction<T> func)
            throws IOException, UnauthorizedAccessException {
        executeIndex++;
        assertTrue(
                executeIndex < executeVerifiers.size(),
                "Expected " + executeVerifiers.size() + " invocations, but " + "encountered more");
        ExecuteBuilder builder = executeVerifiers.get(executeIndex);
        return handle((ExecuteBuilder<T>) builder, request, context, null, func);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T execute(HttpUriRequest request, HttpClientContext context, CredentialsProvider provider,
            RequestFunction<T> func)
            throws IOException, UnauthorizedAccessException {
        executeIndex++;
        assertTrue(
                executeIndex < executeVerifiers.size(),
                "Expected " + executeVerifiers.size() + " invocations, but encountered more");
        ExecuteBuilder builder = executeVerifiers.get(executeIndex);
        return handle((ExecuteBuilder<T>) builder, request, context, provider, func);
    }

    private static <T> T handle(ExecuteBuilder<T> exec, HttpUriRequest request, HttpClientContext context,
            CredentialsProvider provider, RequestFunction<T> func)
            throws IOException, UnauthorizedAccessException {
        try {
            return exec.handle(request, context, provider, func);
        } catch (IOException | UnauthorizedAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
