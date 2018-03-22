package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.server.CmdSpec.LABELS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetLabelsOptions;

/**
 * @author Sean Shou
 * @since 16/09/2016
 */
@RunWith(NestedRunner.class)
public class LabelsDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String LABEL_SUMMARY_OWNER = "sshou";
    private static final String FILE_DEPOT_PATH = "//depot/dev/test.txt";
    private LabelsDelegator labelsDelegator;
    private List<Map<String, Object>> resultMaps;
    private List<IFileSpec> fileSpecs;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        labelsDelegator = new LabelsDelegator(server);

        Map<String, Object> resultMap = mock(Map.class);
        when(resultMap.get(MapKeys.OWNER_KEY)).thenReturn(LABEL_SUMMARY_OWNER);
        resultMaps = newArrayList(resultMap);

        fileSpecs = newArrayList(FileSpecBuilder.makeFileSpecList(FILE_DEPOT_PATH));
    }

    /**
     * test getLabels()
     */
    public class TestGetLabels {
        private String user;
        private int maxLabels;
        private String nameFilter;
        private int serverVersion;

        /**
         * Runs before every test.
         */
        @Before
        public void beforeEach() throws P4JavaException {
            user = "sean";
            maxLabels = 10;
            nameFilter = "Date_Modified>453470485";
            serverVersion = 20161;
        }

        /**
         * test getLabels(user, maxLabels, nameFilter, fileSpecs)
         */
        public class WhenUserMaxLabelsNameFilterAndFileSpecsGiven {
            /**
             * Expected throws <code>RequestException</code> when server version less than 20062 and user given.
             *
             * @throws Exception
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenUserRestrictionsIsNotSupportedByServerVersionLessThan20062()
                    throws Exception {
                //given
                serverVersion = 20051;
                when(server.getServerVersion()).thenReturn(serverVersion);

                //when
                labelsDelegator.getLabels(user, maxLabels, nameFilter, fileSpecs);
                //then
                verify(server, never()).execMapCmdList(eq(LABELS.toString()), any(String[].class), eq(null));
            }

            /**
             * Expected throws <code>RequestException</code> when server version less than 20061 and maxLimit given.
             *
             * @throws Exception
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenMaxLimitIsNotSupportedByServerVersionLessThan20061()
                    throws Exception {

                //given
                user = EMPTY;
                serverVersion = 20051;
                when(server.getServerVersion()).thenReturn(serverVersion);

                //when
                labelsDelegator.getLabels(user, maxLabels, nameFilter, fileSpecs);
                //then
                verify(server, never()).execMapCmdList(eq(LABELS.toString()), any(String[].class), eq(null));
            }

            /**
             * Expected throws <code>RequestException</code> when server version less than 20081 and nameFilter given.
             *
             * @throws Exception
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenNameFilterIsNotSupportedByServerVersionLessThan20081()
                    throws Exception {

                //given
                user = EMPTY;
                maxLabels = -1;
                serverVersion = 20071;
                when(server.getServerVersion()).thenReturn(serverVersion);

                //then
                labelsDelegator.getLabels(user, maxLabels, nameFilter, fileSpecs);
            }

            /**
             * Expected throws exception when any inner method throws that exception
             *
             * @throws Exception
             */
            @Test(expected = ConnectionException.class)
            public void shouldThrowConnectionExceptionWhenInnerMethodCallThrowsIt()
                    throws Exception {

                executeAndThrowsException(ConnectionException.class);
            }


            /**
             * Expected test wrapping and throws <code>RequestException</code>
             * when any inner method throws <code>P4JavaException</code>
             *
             * @throws Exception
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenInnerMethodCallThrowsP4JavaException()
                    throws Exception {

                executeAndThrowsException(P4JavaException.class);
            }

            private void executeAndThrowsException(Class<? extends Throwable> toBeThrown)
                    throws Exception {

                doThrow(toBeThrown).when(server).execMapCmdList(
                        eq(LABELS.toString()),
                        any(String[].class),
                        eq(null));
                when(server.getServerVersion()).thenReturn(serverVersion);
                labelsDelegator.getLabels(user, maxLabels, nameFilter, fileSpecs);
            }

            /**
             * Expected return non empty labels
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonEmptyList() throws Exception {
                //given
                String[] labelsCmdArguments = {
                        "-m" + maxLabels,
                        "-u" + user,
                        "-e" + nameFilter,
                        FILE_DEPOT_PATH};

                when(server.getServerVersion()).thenReturn(serverVersion);
                when(server.execMapCmdList(eq(LABELS.toString()), eq(labelsCmdArguments), eq(null)))
                        .thenReturn(resultMaps);

                //when
                List<ILabelSummary> labelSummaries = labelsDelegator.getLabels(
                        user,
                        maxLabels,
                        nameFilter,
                        fileSpecs);

                //then
                assertThat(labelSummaries.size(), is(1));
                assertThat(labelSummaries.get(0).getOwnerName(), is(LABEL_SUMMARY_OWNER));
            }

        }


        /**
         * test getLabels(fileSpecs, getLabelsOptions)
         */
        public class WhenFileSpecsAndGetLabelsOptionsGiven {
            private final String[] labelsCmdArguments = {
                    "-m" + maxLabels,
                    "-u" + user,
                    "-e" + nameFilter,
                    FILE_DEPOT_PATH};

            private final GetLabelsOptions opts = new GetLabelsOptions(
                    "-m" + maxLabels,
                    "-u" + user,
                    "-e" + nameFilter);

            /**
             * Expected return empty labels when command return null result map
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnEmptyListWhenResultMapIsNull() throws Exception {
                //given
                when(server.execMapCmdList(eq(LABELS.toString()), eq(labelsCmdArguments), eq(null)))
                        .thenReturn(null);

                //when
                List labels = labelsDelegator.getLabels(fileSpecs, opts);

                //then
                assertThat(labels.size(), is(0));
            }


            /**
             * Expected return non empty labels
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonEmptyLabelList() throws Exception {
                //given
                when(server.execMapCmdList(eq(LABELS.toString()), eq(labelsCmdArguments), eq(null)))
                        .thenReturn(resultMaps);

                //when
                List<ILabelSummary> labels = labelsDelegator.getLabels(fileSpecs, opts);

                //then
                assertThat(labels.size(), is(1));
                assertThat(labels.get(0).getOwnerName(), is(LABEL_SUMMARY_OWNER));
            }
        }
    }
}