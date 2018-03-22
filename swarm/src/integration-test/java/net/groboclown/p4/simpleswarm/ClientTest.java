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

package net.groboclown.p4.simpleswarm;

import net.groboclown.p4.simpleswarm.exceptions.InvalidSwarmServerException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;

public class ClientTest {
    public static void main(String[] argStrings)
            throws URISyntaxException, IOException, InvalidSwarmServerException {
        Iterator<String> args = Arrays.asList(argStrings).iterator();
        SwarmConfig config = new SwarmConfig();
        config
            .withUri(args.next())
            .withUsername(getVal(args, "P4USER"))
            .withPassword(getVal(args, "P4PASSWD"));

        SwarmClient client = SwarmClientFactory.createSwarmClient(config);
        int[] reviews = client.getReviewIdsForChangelist(Integer.parseInt(getVal(args, "change")));
        for (int review : reviews) {
            System.out.println("In review " + review);
        }
    }

    private static String getVal(Iterator<String> args, String key) {
        String ret = System.getProperty(key);
        if (ret == null) {
            ret = System.getenv(key);
        }
        if (ret == null && args.hasNext()) {
            ret = args.next();
        }
        if (ret == null) {
            throw new NullPointerException("Did not specify " + key);
        }
        return ret;
    }
}
