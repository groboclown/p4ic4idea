package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapAsString;
import static com.perforce.p4java.server.CmdSpec.PROPERTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.admin.IProperty;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.Property;
import com.perforce.p4java.option.server.GetPropertyOptions;
import com.perforce.p4java.option.server.PropertyOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IPropertyDelegator;

/**
 * Implementation to handle the Property command.
 */
public class PropertyDelegator extends BaseDelegator implements IPropertyDelegator {
    /**
     * Instantiate a new PropertyDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public PropertyDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String setProperty(
            final String name,
            final String value,
            final PropertyOptions opts) throws P4JavaException {

        if (isBlank(name)) {
            if (opts == null || isBlank(opts.getName())) {
                throw new IllegalArgumentException("Property/option name shouldn't null or empty");
            }
        }
        if (isBlank(value)) {
            if (opts == null || isBlank(opts.getValue())) {
                throw new IllegalArgumentException("Property/option value shouldn't null or empty");
            }
        }
        PropertyOptions propertyOptions = opts;
        if (isNull(opts)) {
            propertyOptions = new PropertyOptions();
        }
        if (isNotBlank(name)) {
            propertyOptions.setName(name);
        }
        if (isNotBlank(value)) {
            propertyOptions.setValue(value);
        }

        List<Map<String, Object>> resultMaps = execMapCmdList(
                PROPERTY,
                processParameters(propertyOptions, null, new String[]{"-a"}, server), null);
        return parseCommandResultMapAsString(resultMaps);
    }

    @Override
    public List<IProperty> getProperty(final GetPropertyOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(PROPERTY,
                processParameters(opts, null, new String[]{"-l"}, server), null);

        return buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                // p4ic4idea: explicit generics
                new Function<Map<String, Object>, IProperty>() {
                    @Override
                    // p4ic4idea: explicit generics
                    public IProperty apply(Map<String, Object> map) {
                        return new Property(map);
                    }
                }
        );
    }

    @Override
    public String deleteProperty(final String name, final PropertyOptions opts)
            throws P4JavaException {

        String error = "Property/option name shouldn't null or empty";
        if (isBlank(name)) {
            if (opts == null || isBlank(opts.getName())) {
                throw new IllegalArgumentException(error);
            }
        }

        PropertyOptions propertyOptions = opts;
        if (isNull(opts)) {
            propertyOptions = new PropertyOptions();
        }
        if (isNotBlank(name)) {
            propertyOptions.setName(name);
        }

        List<Map<String, Object>> resultMaps = execMapCmdList(
                PROPERTY,
                processParameters(propertyOptions, null, new String[]{"-d"}, server),
                null);
        return parseCommandResultMapAsString(resultMaps);
    }
}
