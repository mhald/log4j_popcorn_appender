package org.popcorn.log4j;
/* Copyright 2013
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.OutputStream;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Protocol;

/**
 * An Appender that delivers events over a UDP messages to Popcorn.
 * @author mhald
 */
@Plugin(name="Popcorn", type="Core", elementType="appender", printObject = false)
public class PopcornAppender extends AbstractAppender {
	final private PopcornConnectionManager manager;

	/**
	 * 
	 * @param host The name of the host to connect to.
	 * @param portNum The port to connect to on the target host.
	 * @param protocol The Protocol to use.
	 * @param delay The interval in which failed writes should be retried.
	 * @param name The name of the Appender.
	 * @param immediateFlush "true" if data should be flushed on each write.
	 * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise. The default is "true".
	 * @param layout The layout to use (defaults to SerializedLayout).
	 * @param filter The Filter or null.
	 * @return A SocketAppender.
	 */
	@PluginFactory
	public static PopcornAppender createAppender(
			@PluginAttr("host") final String host,
			@PluginAttr("port") final String portNum,
			@PluginAttr("protocol") final String protocol,
			@PluginAttr("reconnectionDelay") final String delay,
			@PluginAttr("name") final String name,
			@PluginAttr("immediateFlush") final String immediateFlush,
			@PluginAttr("suppressExceptions") final String suppress,
			@PluginElement("layout") Layout layout,
			@PluginElement("filters") final Filter filter,
			@PluginAttr("node") final String node,
			@PluginAttr("role") final String role,
			@PluginAttr("version") final String version
			) {
		final boolean isFlush = immediateFlush == null ? true : Boolean.valueOf(immediateFlush);
		final boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
		final int port = portNum == null ? 0 : Integer.parseInt(portNum);
		if (layout == null) {
			layout = SerializedLayout.createLayout();
		}
		if (name == null) {
			LOGGER.error("No name provided for PopcornAppender");
			return null;
		}

		return new PopcornAppender(name, layout, filter, 
				new PopcornConnectionManager(host,port,node,role,version), 
				handleExceptions, isFlush);
	 }
	
	/**
	 * Instantiate a WriterAppender and set the output destination to a new
	 * {@link java.io.OutputStreamWriter} initialized with <code>os</code> as
	 * its {@link java.io.OutputStream}.
	 * 
	 * @param name The name of the Appender.
	 * @param layout The layout to format the message.
	 * @param popcornConnectionManager The OutputStreamManager.
	 */
	@SuppressWarnings("unchecked")
	protected PopcornAppender(final String name, final Layout layout,
			final Filter filter,
			final PopcornConnectionManager popcornConnectionManager,
			final boolean handleException, final boolean immediateFlush) {
		super(name, filter, layout, handleException);
		this.manager = popcornConnectionManager;
	}


	@Override
	public void append(LogEvent event) {
		try {
			manager.os.write(event);
		} catch (IOException e) {
			LOGGER.error("Cannot write to socket", e);

		}
	}
}