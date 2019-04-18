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

package net.groboclown.p4.server.impl.config.win;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;

import javax.annotation.Nullable;

public class JnaWinRegistry {
    public static WinReg.HKEY HKEY_LOCAL_MACHINE = WinReg.HKEY_LOCAL_MACHINE;
    public static WinReg.HKEY HKEY_CURRENT_USER = WinReg.HKEY_CURRENT_USER;

    public static boolean isAvailable() {
        try {
            return Platform.isWindows();
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static String readString(WinReg.HKEY hkey, String key, String valueName) {
        if (! isAvailable()) {
            return null;
        }
        try {
            return Advapi32Util.registryGetStringValue(hkey, key, valueName);
        } catch (Win32Exception e) {
            return null;
        }
    }
}
