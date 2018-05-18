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

package net.groboclown.p4plugin;

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;

import java.io.File;

public class PluginLoadMain {
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Must have plugin zip file as argument");
        }
        File file = new File(args[0]);
        //if (!file.exists() || !file.isFile() || !file.canRead()) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Must have plugin zip file as argument");
        }
        try {
            // Primary plugin loading classes:
            //   PluginManagerCore
            //   PluginDownloader
            //   InstalledPluginsManagerMain

            IdeaPluginDescriptorImpl pluginDescriptor = PluginDownloader.loadDescriptionFromJar(file);
            if (pluginDescriptor == null) {
                System.err.println("No error reported, but could not load plugin");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
