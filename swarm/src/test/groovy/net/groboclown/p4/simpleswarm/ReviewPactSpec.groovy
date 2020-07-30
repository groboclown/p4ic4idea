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

package net.groboclown.p4.simpleswarm

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.groovy.PactBuilder
import au.com.dius.pact.consumer.groovy.Matchers
import groovy.json.JsonOutput
import net.groboclown.p4.simpleswarm.impl.ReviewActions
import spock.lang.Specification

class ReviewPactSpec extends Specification {
    private ReviewActions client
    private PactBuilder provider

    def setup() {
        //def username = System.getenv("username")
        //def ticket = System.getenv("ticket")
        def username = 'user1'
        def ticket = 'ticket1'
        SwarmConfig config = new SwarmConfig()
                .withLogger(new MockLogger())
                .withUri("http://localhost:65001")
                .withUsername(username)
                .withTicket(ticket)
                .withVersion(6.0)
        client = new ReviewActions(config)
        provider = new PactBuilder()
        provider.serviceConsumer 'net.groboclown.p4.swarm'
        provider.hasPactWith 'Swarm-v6'
        provider.port 65001
    }


    def 'Pact - Get Reviews For Changelist'() {
        given:
        def jsonFirst = [
                lastSeen: 3, // adding this in the response triggers a paging request.
                reviews: [
                        [ id: 3 ],
                ],
                totalCount: 1
        ]
        def jsonSecond = [
                reviews: [],
                totalCount: 0
        ]

        // The request path is to make requests across pages until no more pages are
        // available.  In this setup, there are 2 pages returned.

        provider.given('review exists for changelist 2')
            .uponReceiving('a request for reviews associated with changelist 2')
            .withAttributes(
                    method: 'GET',
                    path: '/api/v6/reviews',
                    query: ['change[]': '2', 'fields': 'id'],
                    // Including headers means matching all the headers...
                    headers: [
                            // This is the key header we care about.
                            Authorization: Matchers.regexp(
                                    '^Basic\\s+[A-Za-z0-9+/]+=*$',
                                    'Basic Ymdl89fds=',
                            ),
                    ])
            .willRespondWith(
                    status: 200,
                    body: JsonOutput.toJson(jsonFirst),
                    headers: ['Content-Type': 'application/json']
            )

        provider.given('no more reviews')
            .uponReceiving('a request for reviews associated with changelist 2, next page')
            .withAttributes(
                    method: 'GET',
                    path: '/api/v6/reviews',
                    query: ['change[]': '2', fields: 'id', after: '3'],
                    // Including headers means matching all the headers...
                    headers: [
                            // This is the key header we care about.
                            Authorization: Matchers.regexp(
                                    '^Basic\\s+[A-Za-z0-9+/]+=*$',
                                    'Basic Ymdl89fds=',
                            )
                    ])
            .willRespondWith(
                    status: 200,
                    body: JsonOutput.toJson(jsonSecond),
                    headers: ['Content-Type': 'application/json']
            )

        when:
        def result = null
        PactVerificationResult pactResult = provider.runTest {
            result = client.getReviewIdsForChangelist(2)
        }

        then:
        pactResult == PactVerificationResult.Ok.INSTANCE
        result == [3]
    }
}
