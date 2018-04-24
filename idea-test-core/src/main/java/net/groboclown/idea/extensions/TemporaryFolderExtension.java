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

package net.groboclown.idea.extensions;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.engine.execution.AfterEachMethodAdapter;
import org.junit.jupiter.engine.extension.ExtensionRegistry;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.stream;

/**
 * Taken from
 * <a href="https://github.com/rherrmann/junit5-experiments/blob/master/src/main/java/com/codeaffine/junit5/TemporaryFolderExtension.java">github.com/rherrmann/junit5-experiments</a>
 */
public class TemporaryFolderExtension
        implements AfterEachMethodAdapter, TestInstancePostProcessor, ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(TemporaryFolder.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        try {
            return createFolder(extensionContext);
        } catch (IOException e) {
            throw new ParameterResolutionException("could not make tmp folder", e);
        }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, final ExtensionContext extensionContext) {
        stream(testInstance.getClass().getDeclaredFields())
            .filter(field -> TemporaryFolder.class.equals(field.getType()))
            .forEach(field -> inject(testInstance, field, extensionContext));
    }

    @Override
    public void invokeAfterEachMethod(ExtensionContext extensionContext, ExtensionRegistry extensionRegistry) {
        getFolders(extensionContext).forEach(
                TemporaryFolder::cleanUp
        );
    }

    private void inject(Object testInstance, Field field, ExtensionContext extensionContext) {
        field.setAccessible(true);
        try {
            field.set(testInstance, createFolder(extensionContext));
        } catch (IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    private TemporaryFolder createFolder(ExtensionContext extensionContext)
            throws IOException {
        TemporaryFolder folder = new TemporaryFolder();
        folder.prepare();
        getFolders(extensionContext).add(folder);
        return folder;
    }

    @SuppressWarnings("unchecked")
    private List<TemporaryFolder> getFolders(ExtensionContext extensionContext) {
        ExtensionContext.Store store = extensionContext.getStore(ExtensionContext.Namespace.create(getClass(), extensionContext.getRequiredTestMethod()));
        return (List<TemporaryFolder>)store.getOrComputeIfAbsent("folders", (key) -> new ArrayList<>());
    }
}
