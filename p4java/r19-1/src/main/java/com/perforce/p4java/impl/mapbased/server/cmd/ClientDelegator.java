package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.common.function.BiPredicate;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.common.function.FunctionWithException;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.DeleteClientOptions;
import com.perforce.p4java.option.server.GetClientTemplateOptions;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.option.server.UpdateClientOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IClientDelegator;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.rethrowFunction;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleErrorStr;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.isExistClientOrLabelOrUser;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.isInfoMessage;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.isNonExistClientOrLabelOrUser;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapAsString;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString;
import static com.perforce.p4java.server.CmdSpec.CLIENT;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.replacePattern;

/**
 * @author Sean Shou
 * @since 15/09/2016
 */
public class ClientDelegator extends BaseDelegator implements IClientDelegator {
	public ClientDelegator(IOptionsServer server) {
		super(server);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IClient getClient(final String clientName)
			throws ConnectionException, RequestException, AccessException {
		Validate.notBlank(clientName, "Null or empty client name passed in updateClient()");

		List<Map<String, Object>> resultMaps = execMapCmdList(
				CLIENT,
				new String[]{"-o", clientName},
				null);
		return getClientOrNullFromHelixResultMap(
				resultMaps,
				null,
				server,
				new BiPredicate<Map<String, Object>, GetClientTemplateOptions>() {
					@Override
					public boolean test(Map<String, Object> map, GetClientTemplateOptions opts) {
						return isExistClientOrLabelOrUser(map);
					}
				},
				rethrowFunction(
						new FunctionWithException<Map<String, Object>, Boolean>() {
							@Override
							public Boolean apply(Map<String, Object> map) throws P4JavaException {
								return !handleErrorStr(map) && !isInfoMessage(map);
							}
						}
				)
		);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IClient getClient(@Nonnull IClientSummary clientSummary)
			throws ConnectionException, RequestException, AccessException {
		Validate.notNull(clientSummary);
		return getClient(clientSummary.getName());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IClient getClientTemplate(String clientName)
			throws ConnectionException, RequestException, AccessException {
		return getClientTemplate(clientName, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IClient getClientTemplate(final String clientName, final boolean allowExistent)
			throws ConnectionException, RequestException, AccessException {

		try {
			return getClientTemplate(clientName, new GetClientTemplateOptions(allowExistent));
		} catch (final ConnectionException | AccessException | RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IClient getClientTemplate(
			@Nonnull String clientName,
			GetClientTemplateOptions getClientTemplateOptions) throws P4JavaException {

		Validate.notBlank(clientName, "Null or empty client name passed in updateClient()");
		List<Map<String, Object>> resultMaps = execMapCmdList(
				CLIENT,
				Parameters.processParameters(
						getClientTemplateOptions,
						null,
						new String[]{"-o", clientName},
						server),
				null);

		return getClientOrNullFromHelixResultMap(
				resultMaps,
				getClientTemplateOptions,
				server,
				new BiPredicate<Map<String, Object>, GetClientTemplateOptions>() {
					@Override
					public boolean test(Map<String, Object> map, GetClientTemplateOptions opts) {
						return isNonExistClientOrLabelOrUser(map)
								|| (nonNull(opts) && opts.isAllowExistent());
					}
				},
				rethrowFunction(new FunctionWithException<Map<String, Object>, Boolean>() {
					@Override
					public Boolean apply(Map<String, Object> map) throws P4JavaException {
						return !handleErrorStr(map) && !isInfoMessage(map);
					}
				})
		);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String createClient(@Nonnull final IClient newClient)
			throws ConnectionException, RequestException, AccessException {
		Validate.notNull(newClient);

		replaceWithUnderscoreIfClientNameContainsWhitespacesOrTabs(newClient);

		List<Map<String, Object>> resultMaps = execMapCmdList(
				CLIENT,
				new String[]{"-i"},
				InputMapper.map(newClient));

		return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createTempClient(@Nonnull final IClient newClient) throws ConnectionException, AccessException, RequestException {
		Validate.notNull(newClient);

		replaceWithUnderscoreIfClientNameContainsWhitespacesOrTabs(newClient);
		server.setCurrentClient(newClient);

		List<Map<String, Object>> resultMaps = execMapCmdList(
				CLIENT,
				new String[]{"-x","-i"},
				InputMapper.map(newClient));

		for (Map<String, Object> resultMap : resultMaps) {
			ResultMapParser.handleErrorStr(resultMap);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String updateClient(@Nonnull final IClient client)
			throws ConnectionException, RequestException, AccessException {
		Validate.notNull(client);

		List<Map<String, Object>> resultMaps = execMapCmdList(
				CLIENT,
				new String[]{"-i"},
				InputMapper.map(client));

		return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String updateClient(@Nonnull final IClient client, final boolean force)
			throws ConnectionException, RequestException, AccessException {

		try {
			return updateClient(client, new UpdateClientOptions(force));
		} catch (final ConnectionException | AccessException | RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String updateClient(@Nonnull final IClient client, final UpdateClientOptions opts)
			throws P4JavaException {

		Validate.notNull(client);
		Validate.notBlank(client.getName(), "Null or empty client name passed in updateClient()");

		List<Map<String, Object>> resultMaps = execMapCmdList(
				CLIENT,
				Parameters.processParameters(
						opts,
						null,
						"-i",
						server),
				InputMapper.map(client));

		return parseCommandResultMapAsString(resultMaps);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String deleteClient(final String clientName, final boolean force)
			throws ConnectionException, RequestException, AccessException {

		try {
			return deleteClient(clientName, new DeleteClientOptions(force));
		} catch (final ConnectionException | AccessException | RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String deleteClient(final String clientName, final DeleteClientOptions opts)
			throws P4JavaException {

		Validate.notBlank(clientName, "Null or empty client name passed in updateClient()");
		List<Map<String, Object>> resultMaps = execMapCmdList(
				CLIENT,
				Parameters.processParameters(
						opts,
						null,
						new String[]{"-d", clientName},
						server),
				null);

		return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
	}

	/**
	 * {@inheritDoc}
	 */
	public String switchClientView(final String templateClientName,
								   final String targetClientName,
								   final SwitchClientViewOptions opts) throws P4JavaException {

		Validate.notBlank(templateClientName, "Template client name shouldn't blank");

		List<String> args = new ArrayList<>(Arrays.asList("-s", "-t", templateClientName));
		if (isNotBlank(targetClientName)) {
			args.add(targetClientName);
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(
				CLIENT,
				processParameters(
						opts,
						null,
						args.toArray(new String[args.size()]),
						server),
				null);

		return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String switchStreamView(final String streamPath,
								   final String targetClientName,
								   final SwitchClientViewOptions opts) throws P4JavaException {

		Validate.notBlank(streamPath, "Stream path shouldn't blank");

		List<String> args = new ArrayList<>(Arrays.asList("-s", "-S", streamPath));
		if (isNotBlank(targetClientName)) {
			args.add(targetClientName);
		}

		List<Map<String, Object>> resultMaps = execMapCmdList(
				CLIENT,
				processParameters(
						opts,
						null,
						args.toArray(new String[args.size()]),
						server),
				null);

		return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
	}

	private static void replaceWithUnderscoreIfClientNameContainsWhitespacesOrTabs(IClient newClient) {
		final String TABS = "\t";
		String name = newClient.getName();
		if (containsAny(name, SPACE, TABS)) {
			String newClientName = replacePattern(name, "\\s", "_");
			newClient.setName(newClientName);
		}
	}

	private static IClient getClientOrNullFromHelixResultMap(
			@Nullable final List<Map<String, Object>> resultMaps,
			@Nullable final GetClientTemplateOptions opts,
			@Nonnull final IOptionsServer server,
			@Nonnull final BiPredicate<Map<String, Object>, GetClientTemplateOptions> conditions,
			@Nonnull final Function<Map<String, Object>, Boolean> handle)
			throws AccessException, RequestException {

		IClient client = null;
		if (nonNull(resultMaps)) {
			for (Map<String, Object> map : resultMaps) {
				if (nonNull(map)) {
					if (handle.apply(map)) {
						if (conditions.test(map, opts)) {
							client = new Client(server, map);
						}
					}
				}
			}
		}

		return client;
	}
}
