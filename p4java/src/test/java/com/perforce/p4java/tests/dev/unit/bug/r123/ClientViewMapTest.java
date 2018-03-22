package com.perforce.p4java.tests.dev.unit.bug.r123;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.common.base.StringHelper.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.IMapEntry.EntryType;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test exotic client view mappings.
 */
@RunWith(JUnitPlatform.class)
@Jobs({ "job059253" })
@TestId("Dev123_ClientViewMapTest")
public class ClientViewMapTest extends P4JavaTestCase {
	/**
	 * Split a string by whitespace, except inside double quotes.
	 */
	private static final String TOKEN_REGEX_PATTERN = "([^\"]\\S*|\".+?\")\\s*";

	private IOptionsServer server = null;

	@BeforeEach
	public void setUp() throws Exception {
		server = getServer();
		assertThat(server, notNullValue());
		IClient client = getDefaultClient(server);
		assertThat(client, notNullValue());
		server.setCurrentClient(client);

	}

	@AfterEach
	public void tearDown() {
		if (nonNull(server)) {
			endServerSession(server);
		}
	}

	/**
	 * Test exotic client view mappings.
	 */
	@Test
	public void testClientViewPattern() throws Exception {
		String[] viewMappings = {
				"\"//depot/122Bugs/job0592 53/files/chang  es-2012.3.txt\" //job059253_client/files/abc123.txt",
				"\"//depot/122Bugs/job0592 53/files/\"chang  es-2012.3\".txt\" //job059253_client/files/abc123.txt",
				"\"//depot/122 Bugs/job059253/files/changes-201 2.3.txt\" \"//job059253_client/fi  les/ab  c123.txt\"",
				"+//depot/122Bugs/job059253/files/\"main~40%2523$%2525^&%252A()_+changes\"-2012.3.txt   \"//job059253_client/fi les/abc123.txt\"",
				"\"+//job059253_client/fi les/abc123.txt\"", "+//job059253_client/files/abc123.txt",
				"+//job059253_client/files/\"abc123.txt\"" };

		List<String> list = newArrayList();
		Pattern p = Pattern.compile(TOKEN_REGEX_PATTERN);
		for (String viewMapping : viewMappings) {
			Matcher m = p.matcher(viewMapping);
			while (m.find()) {
				if (m.groupCount() > 0) {
					if (isNotBlank(m.group(1))) {
						if (m.group(1).startsWith("\"")) {
							list.add(m.group(1).replaceAll("^\"|\"$", EMPTY));
						} else {
							list.add(m.group(1));
						}
					}
				}
			}
		}
		assertThat(list, notNullValue());
		list.forEach(System.out::println);
	}

	/**
	 * Test get client view map with exotic file paths.
	 */
	@Test
	public void testGetClient() throws Exception {
		String clientName = "job059253_client";

		try {
			IClient testClient = server.getClient(clientName);
			if (testClient == null) {
				IClient cl = server.getClientTemplate(clientName);
				server.setCurrentClient(cl);
				cl.setHostName("");
				ClientView cv = new ClientView();
				cv.addEntry(new ClientViewMapping(0, EntryType.INCLUDE,
						"//depot/spaces/21/files/\"main~!%40%23$%25^&%2A()_+changes\"-2012.3.txt",
						"//" + clientName + "/1345752263508"));
				cl.setClientView(cv);
				server.createClient(cl);
				testClient = server.getClient(clientName);
			}
			assertNotNull(testClient);

			ClientView clientView = testClient.getClientView();
			List<IClientViewMapping> viewMappings = clientView.getEntryList();
			assertNotNull(viewMappings);

			for (IClientViewMapping element : viewMappings) {
				String left = element.getLeft();
				String right = element.getRight();
				System.out.println(format("%s %s", left, right));
			}
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
