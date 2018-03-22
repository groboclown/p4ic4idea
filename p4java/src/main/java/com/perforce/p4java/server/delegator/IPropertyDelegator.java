package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.admin.IProperty;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetPropertyOptions;
import com.perforce.p4java.option.server.PropertyOptions;

/**
 * Interface to handle the Property command.
 */
public interface IPropertyDelegator {
    /**
     * Updates a property value in the Perforce server, or adds the property
     * value to the Perforce server if it is not yet there.
     * <p>
     * <p>
     * This method require that the user have 'admin' access granted by 'p4
     * protect'.
     *
     * @param name  non-null property name.
     * @param value property value.
     * @param opts  PropertyOptions object describing optional parameters; if
     *              null, no options are set.
     * @return non-null result message string from the set (add/update)
     * operation.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    String setProperty(String name, String value, PropertyOptions opts)
            throws P4JavaException;

    /**
     * Gets a list of one or more property values from the Perforce server.
     * <p>
     * <p>
     * The -A flag require that the user have 'admin' access granted by 'p4
     * protect'.
     * <p>
     * <p>
     * Note that specifying the -n flag when using the -l flag substantially
     * improves the performance of this command.
     *
     * @param opts GetPropertyOptions object describing optional parameters; if
     *             null, no options are set.
     * @return a non-null (but possibly empty) list of property values.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    List<IProperty> getProperty(GetPropertyOptions opts) throws P4JavaException;

    /**
     * Deletes a property value from the Perforce server.
     * <p>
     * <p>
     * This method require that the user have 'admin' access granted by 'p4
     * protect'.
     *
     * @param name non-null property name.
     * @param opts PropertyOptions object describing optional parameters; if
     *             null, no options are set.
     * @return non-null result message string from the delete operation.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    String deleteProperty(String name, PropertyOptions opts)
            throws P4JavaException;
}
