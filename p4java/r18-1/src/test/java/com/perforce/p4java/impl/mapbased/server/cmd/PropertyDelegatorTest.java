package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.server.CmdSpec.PROPERTY;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.UnitTestWhen;
import com.perforce.p4java.admin.IProperty;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetPropertyOptions;
import com.perforce.p4java.option.server.PropertyOptions;

/**
 * @author Sean Shou
 * @since 5/10/2016
 */
public class PropertyDelegatorTest extends AbstractP4JavaUnitTest {
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private PropertyDelegator propertyDelegator;
    private List<Map<String, Object>> resultMaps;

    private GetPropertyOptions getPropertyOptions;
    private PropertyOptions propertyOptions;
    private String name;
    private String value;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        propertyDelegator = new PropertyDelegator(server);
        Map<String, Object> resultMap = mock(Map.class);
        resultMaps = newArrayList(resultMap);

        getPropertyOptions = mock(GetPropertyOptions.class);
        propertyOptions = mock(PropertyOptions.class);

        name = EMPTY;
        value = "myValue";
    }

    /**
     * Expected return property list
     *
     * @throws Exception
     */
    @Test
    public void testGetProperty() throws Exception {
        // given
        when(server.execMapCmdList(eq(PROPERTY.toString()), any(String[].class), eq(null)))
                .thenReturn(resultMaps);
        // when
        List<IProperty> property = propertyDelegator.getProperty(getPropertyOptions);
        // then
        assertThat(property.size(), is(1));
    }

    /**
     * Expected throws <code>IllegalArgumentException</code> when both name and option's name is blank
     *
     * @throws Exception
     */
    @Test
    public void testSetPropertyShouldThrownIllegalArgumentExceptionWhenNameAndGetNameIsBlank()
            throws Exception {

        when(propertyOptions.getName()).thenReturn(EMPTY);
        setPropertyExpectedThrowsIllegalArgumentException();
    }

    /**
     * Expected throws <code>IllegalArgumentException</code> when name is blank and option is null
     *
     * @throws Exception
     */
    @Test
    public void testSetPropertyShouldThrownIllegalArgumentExceptionWhenNameIsBlankAndOptionIsNull()
            throws Exception {

        propertyOptions = null;
        setPropertyExpectedThrowsIllegalArgumentException();
    }

    /**
     * Expected throws <code>IllegalArgumentException</code> when value and option's value is blank
     *
     * @throws Exception
     */
    @Test
    public void testSetPropertyShouldThrownIllegalArgumentExceptionWhenValueAndGetValueIsBlank()
            throws Exception {
        name = "not blank name";
        when(propertyOptions.getValue()).thenReturn(EMPTY);
        value = EMPTY;
        setPropertyExpectedThrowsIllegalArgumentException();
    }

    /**
     * Expected throws <code>IllegalArgumentException</code> when value is blank and option is null
     *
     * @throws Exception
     */
    @Test
    public void testSetPropertyShouldThrownIllegalArgumentExceptionWhenValueIsBlankAndOptionIsNull()
            throws Exception {
        value = EMPTY;
        propertyOptions = null;

        setPropertyExpectedThrowsIllegalArgumentException();
    }

    /**
     * Expected return non blank property name when name is not blank and propertyOptions is null
     *
     * @throws Exception
     */
    @Test
    public void testSetPropertyShouldReturnNonBlankPropertyNameWhenPropertyOptionsIsNull()
            throws Exception {
        // given
        name = "not blank name";
        when(server.execMapCmdList(eq(PROPERTY.toString()), any(String[].class), eq(null)))
                .thenReturn(resultMaps);
        propertyOptions = null;
        // when
        String setProperty = propertyDelegator.setProperty(name, value, propertyOptions);
        // then
        assertThat(isNoneBlank(setProperty), is(true));
    }

    /**
     * Expected return non blank property name when propertyOptions is not null
     *
     * @throws Exception
     */
    @Test
    public void testSetPropertyShouldReturnNonBlankPropertyNameWhenPropertyOptionsIsNonNull()
            throws Exception {
        // given
        name = "not blank name";
        when(server.execMapCmdList(eq(PROPERTY.toString()), any(String[].class), eq(null)))
                .thenReturn(resultMaps);
        // when
        when(propertyOptions.getName()).thenReturn(name);
        when(propertyOptions.getValue()).thenReturn(value);
        String setProperty = propertyDelegator.setProperty(name, value, propertyOptions);

        // then
        assertThat(isNoneBlank(setProperty), is(true));
        verify(propertyOptions).setName(name);
        verify(propertyOptions).setValue(value);
    }

    private void setPropertyExpectedThrowsIllegalArgumentException()
            throws Exception {

        thrown.expect(IllegalArgumentException.class);
        // given
        propertyDelegator.setProperty(name, value, propertyOptions);
    }

    /**
     * Expected throws <code>IllegalArgumentException</code> when both name and option's name is blank
     *
     * @throws Exception
     */
    @Test
    public void testDeletePropertyShouldThrownIllegalArgumentExceptionWhenNameAndGetNameIsBlank()
            throws Exception {
        when(propertyOptions.getName()).thenReturn(EMPTY);
        deletePropertyExpectedThrowsIllegalArgumentException();
    }

    /**
     * Expected throws <code>IllegalArgumentException</code> when name is blank and option is null
     *
     * @throws Exception
     */
    @Test
    public void testDeletePropertyShouldThrownIllegalArgumentExceptionWhenNameIsBlankAndOptionIsNull()
            throws Exception {

        propertyOptions = null;
        deletePropertyExpectedThrowsIllegalArgumentException();
    }

    private void deletePropertyExpectedThrowsIllegalArgumentException()
            throws Exception {

        thrown.expect(IllegalArgumentException.class);
        // then
        propertyDelegator.deleteProperty(EMPTY, propertyOptions);
    }

    /**
     * Expected return non blank deleted property name when propertyOptions is null
     *
     * @throws Exception
     */
    @Test
    public void testDeletePropertyShouldReturnNonBlankDeletePropertyNameWhenPropertyOptionsIsNull()
            throws Exception {
        name = "not blank name";
        propertyOptions = null;
        deletePropertyShouldReturnNonBlankDeletePropertyName(
                new UnitTestWhen<String>() {
                    @Override
                    public String when() throws P4JavaException,
                            InvocationTargetException,
                            IllegalAccessException {
                        return propertyDelegator.deleteProperty(name, propertyOptions);
                    }
                });
    }

    /**
     * Expected return non blank deleted property name when propertyOptions is not null
     *
     * @throws Exception
     */
    @Test
    public void testDeletePropertyShouldReturnNonBlankDeletePropertyNameWhenPropertyOptionsIsNonNull()
            throws Exception {
        when(propertyOptions.getName()).thenReturn("name");
        when(propertyOptions.getValue()).thenReturn("value");
        deletePropertyShouldReturnNonBlankDeletePropertyName(
                new UnitTestWhen<String>() {
                    @Override
                    public String when() throws P4JavaException,
                            InvocationTargetException,
                            IllegalAccessException {
                        return propertyDelegator.deleteProperty("name", propertyOptions);
                    }
                });
    }

    private void deletePropertyShouldReturnNonBlankDeletePropertyName(
            UnitTestWhen<String> unitTestWhen) throws Exception {
        // given
        when(server.execMapCmdList(eq(PROPERTY.toString()), any(String[].class), eq(null)))
                .thenReturn(resultMaps);
        // when
        String deleteProperty = unitTestWhen.when();
        // then
        assertThat(isNoneBlank(deleteProperty), is(true));
    }
}