/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.status.StatusLogger;
import org.popcorn.log4j.PopcornProtos.PopcornPacket;
import org.popcorn.log4j.PopcornProtos.PopcornPacket.Builder;

/**
 * Network sending class for popcorn UDP packets.
 * @author mhald
 */
public class PopcornNetIO  {
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private static final int POPCORN_DEBUG = 128;
    private static final int POPCORN_INFO = 64;
    private static final int POPCORN_NOTICE = 32;
    private static final int POPCORN_WARNING = 16;
    private static final int POPCORN_ERROR = 8;
    private static final int POPCORN_CRITICAL = 4;
    private static final int POPCORN_ALERT = 2;
    private static final int POPCORN_EMERGENCY = 1;
    private static final int POPCORN_NONE = 0;
    
    private DatagramSocket ds;
    private final InetAddress address;
    private final int port;

    private String node;
	private String role;
	private String version;
	private static final String pid;
	
	// Attempt to get a PID value for the process; use an empty string if none is available
	static {
		String assign;
		try {
			assign = ManagementFactory.getRuntimeMXBean().getName();
		} catch (Exception e) {
			assign = "";
		}
		pid = assign;
	}

    /**
     * Popcorn network IO maanger.
     * @param host The host to connect to.
     * @param port The port on the host.
     * @param version 
     * @param role 
     * @param node 
     */
	public PopcornNetIO(final String host, final int port,
			String node, String role, String version) {
        this.port = port;
        this.node = node;
        this.role = role;
        this.version = version;
        
        try {
            address = InetAddress.getByName(host);
        } catch (final UnknownHostException ex) {
            final String msg = "Could not find host " + host;
            LOGGER.error(msg, ex);
            throw new AppenderRuntimeException(msg, ex);
        }

        try {
            ds = new DatagramSocket();
        } catch (final SocketException ex) {
            final String msg = "Could not instantiate DatagramSocket to " + host;
            LOGGER.error(msg, ex);
            throw new AppenderRuntimeException(msg, ex);
        }
    }

    /**
     * Write a log4j event out to the Popcorn server via UDP.
     * @param event
     * @throws IOException
     */
    public synchronized void write(LogEvent event) throws IOException {
    	Builder builder = PopcornPacket.newBuilder();
		builder.setPacketV(1)
			.setNode(node)
    		.setRole(role)
    		.setVersion(version)
    		.setLevel(converLogLevel(event.getLevel()));
		
		Throwable t;
		if ((t = event.getThrown()) != null) {
			// get the stack trace as a string
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			String stacktrace = sw.toString(); 
			builder.setMessage(stacktrace.replace("\n", " "));
		} else if (event.getMessage() != null) {
			builder.setMessage(event.getMessage().getFormattedMessage());
		}
		
		if (event.getSource() != null) {
			StackTraceElement ste = event.getSource();
			builder.setModule(ste.getFileName());
			builder.setFuncName(ste.getMethodName());
			builder.setLine(Integer.toString(ste.getLineNumber()));
		}
		
		builder.setPid(pid);
		PopcornPacket packet = builder.build();
    	
    	byte[] data = packet.toByteArray();
		final DatagramPacket udp = new DatagramPacket(data, data.length, address, port);
		ds.send(udp);
    }
    
	/**
	 * Convert the log4j level number its corresponding Popcorn level number.
	 * @param level
	 * @return
	 */
	private int converLogLevel(Level level) {
		if (level == Level.OFF) return POPCORN_NONE;
		if (level == Level.TRACE) return POPCORN_NOTICE;
		if (level == Level.DEBUG) return POPCORN_DEBUG;
		if (level == Level.INFO) return POPCORN_INFO;
		if (level == Level.ERROR) return POPCORN_ERROR;
		if (level == Level.WARN) return POPCORN_WARNING;
		if (level == Level.ERROR) return POPCORN_ERROR;
		if (level == Level.FATAL) return POPCORN_EMERGENCY;
		if (level == Level.ALL) return POPCORN_INFO;
		return POPCORN_DEBUG;
	}
}