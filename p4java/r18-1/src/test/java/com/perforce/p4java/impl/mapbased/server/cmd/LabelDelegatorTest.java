package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.exception.MessageSeverityCode.E_FAILED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.LABEL;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
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

import com.google.common.collect.Lists;
import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.DeleteLabelOptions;

/**
 * @author Sean Shou
 * @since 16/09/2016
 */
@RunWith(NestedRunner.class)
public class LabelDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_NOT_IN_INFO_RANGE = "168435456";
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";

    private LabelDelegator labelDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private static final String LABEL_NAME = "testLabel";
    private ILabel label;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        labelDelegator = new LabelDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = Lists.newArrayList(resultMap);

        label = mock(ILabel.class);
        when(label.getName()).thenReturn(LABEL_NAME);
        ViewMap<ILabelMapping> mockViewMap = mock(ViewMap.class);
        when(mockViewMap.getEntryList()).thenReturn(null);
        when(label.getViewMapping()).thenReturn(mockViewMap);
    }

    /**
     * Test getLabel()
     */
    public class TestGetLabel {
        private final String[] getCmdArguments = {"-o", LABEL_NAME};

        /**
         * Expected throw <code>IllegalArgumentException</code>
         * when input 'labelName' is a blank string.
         *
         * @throws Exception
         */
        @Test(expected = IllegalArgumentException.class)
        public void shouldThrowIllegalArgumentExceptionWhenLabelNameIsBlank() throws Exception {
            labelDelegator.getLabel(EMPTY);
            verify(server, never()).execMapCmdList(eq(LABEL.toString()), any(String[].class), eq(null));
        }

        /**
         * Expected throw exception that was thrown in any inner code.
         *
         * @throws Exception
         */
        @Test
        public void shouldThrownExceptionWhenInnerMethodCallThrownIt() throws Exception {
            //given
            doThrow(ConnectionException.class)
                    .when(server).execMapCmdList(eq(LABEL), eq(getCmdArguments), eq(null));

            //then
            labelDelegator.getLabel(LABEL_NAME);
        }

        /**
         * Expected return null label name when command result maps is null.
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnNullLabelWhenResultMapIsNull() throws Exception {
            //given
            when(server.execMapCmdList(eq(LABEL), eq(getCmdArguments), eq(null))).thenReturn(null);

            //when
            ILabel testLabel = labelDelegator.getLabel(LABEL_NAME);

            //then
            assertThat(testLabel, nullValue());
        }

        /**
         * Expected return non-blank label name
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnNonBlankLabel() throws Exception {
            //given
            givenNonInfoMessageResultMap();
            when(server.execMapCmdList(eq(LABEL.toString()), eq(getCmdArguments), eq(null)))
                    .thenReturn(resultMaps);

            //when
            ILabel testLabel = labelDelegator.getLabel(LABEL_NAME);

            //then
            assertThat(testLabel, notNullValue());
        }
    }

    /**
     * Test createLabel()
     */
    public class TestCreateLabel {
        private final String[] createCmdArguments = {"-i"};

        /**
         * Expected throw <code>NullPointerException</code>
         * when to be created 'labelName' is a blank string.
         *
         * @throws Exception
         */
        @Test(expected = NullPointerException.class)
        public void shouldThrowNullPointerExceptionWhenLabelNameIsNull() throws Exception {
            labelDelegator.createLabel(null);
            verify(server, never()).execMapCmdList(
                    eq(LABEL.toString()),
                    any(String[].class),
                    any(Map.class));
        }


        /**
         * Expected return non-blank created label name
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnNonBlankCreatedLabelName() throws Exception {
            //given
            givenInfoMessageResultMap();
            when(server.execMapCmdList(eq(LABEL.toString()), eq(createCmdArguments), any(Map.class)))
                    .thenReturn(resultMaps);
            //when
            String actualLabelName = labelDelegator.createLabel(label);

            //then
            assertThat(actualLabelName, is(LABEL_NAME));
        }
    }

    /**
     * Test updateLabel()
     */
    public class TestUpdateLabel {
        private final String[] updateCmdArguments = {"-i"};

        /**
         * Expected throw <code>NullPointerException</code>
         * when to be updated 'labelName' is a blank string.
         *
         * @throws Exception
         */
        @Test(expected = NullPointerException.class)
        public void shouldThrowNullPointerExceptionWhenLabelNameIsNull() throws Exception {
            ILabel label = null;
            labelDelegator.updateLabel(label);
            verify(server, never()).execMapCmdList(eq(LABEL.toString()), any(String[].class), any(Map.class));
        }

        /**
         * Expected return non-blank updated label name
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnNonBlankUpdatedLabelName() throws Exception {
            //given
            givenInfoMessageResultMap();
            when(server.execMapCmdList(eq(LABEL.toString()), eq(updateCmdArguments), any(Map.class)))
                    .thenReturn(resultMaps);

            //when
            String actualLabelName = labelDelegator.updateLabel(label);

            //then
            assertThat(actualLabelName, is(LABEL_NAME));
        }
    }

    /**
     * Test deleteLabel()
     */
    public class TestDeleteLabel {
        private final String[] deletedCmdArguments = {"-d", LABEL_NAME};

        /**
         * Test deleteLabel(labelName, force)
         */
        public class WhenLabelNameAndForceGiven {
            /**
             * Expected throw <code>ConnectionException</code>
             * when any inner method call throws it
             *
             * @throws Exception
             */
            @Test(expected = ConnectionException.class)
            public void shouldThrownConnectionExceptionWhenInnerMethodCallThrowsIt()
                    throws Exception {

                executeAndThrowsExpectedException(ConnectionException.class);
            }

            /**
             * Expected throw <code>RequestException</code>
             * when any inner method call throws <code>P4JavaException</code>
             *
             * @throws Exception
             */
            @Test(expected = RequestException.class)
            public void shouldThrownRequestExceptionWhenInnerMethodCallThrowsP4JavaException()
                    throws Exception {

                executeAndThrowsExpectedException(P4JavaException.class);
            }

            private void executeAndThrowsExpectedException(Class<? extends Throwable> toBeThrown)
                    throws Exception {

                doThrow(toBeThrown).when(server).execMapCmdList(
                        eq(LABEL.toString()), eq(deletedCmdArguments), eq(null));

                labelDelegator.deleteLabel(LABEL_NAME, false);
            }


            /**
             * Expected return non-blank deleted label name
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonBlankDeletedLabelName() throws P4JavaException {
                //given
                givenInfoMessageResultMap();
                when(server.execMapCmdList(eq(LABEL.toString()), eq(deletedCmdArguments), eq(null)))
                        .thenReturn(resultMaps);

                //when
                String deleteLabelName = labelDelegator.deleteLabel(LABEL_NAME, false);
                //then
                assertThat(deleteLabelName, is(LABEL_NAME));
            }
        }

        /**
         * Test deleteLabel(labelName, deleteLabelOptions)
         */
        public class WhenLabelNameAndDeleteLabelOptionsGiven {
            private DeleteLabelOptions opts;

            /**
             * Runs before every test.
             */
            @Before
            public void beforeEach() {
                opts = new DeleteLabelOptions();
            }

            /**
             * Expected throw <code>IllegalArgumentException</code>
             * when to be updated 'labelName' is a blank string.
             *
             * @throws Exception
             */
            @Test(expected = IllegalArgumentException.class)
            public void shouldThrownIllegalArgumentException() throws Exception {
                labelDelegator.deleteLabel(EMPTY, opts);
                verify(server, never()).execMapCmdList(
                        eq(LABEL.toString()),
                        any(String[].class),
                        eq(null));
            }

            /**
             * Expected return non-blank deleted label name
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonBlankDeletedLabelName() throws Exception {
                //given
                givenInfoMessageResultMap();
                when(server.execMapCmdList(eq(LABEL.toString()), eq(deletedCmdArguments), eq(null)))
                        .thenReturn(resultMaps);
                //when
                String deleteLabelName = labelDelegator.deleteLabel(LABEL_NAME, opts);

                //then
                assertThat(deleteLabelName, is(LABEL_NAME));
            }
        }
    }

    private void givenInfoMessageResultMap() {
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(resultMap.get(FMT0)).thenReturn(LABEL_NAME);
    }

    private void givenNonInfoMessageResultMap() {
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_NOT_IN_INFO_RANGE);
        when(resultMap.containsKey(MapKeys.UPDATE_KEY)).thenReturn(true);
    }
}