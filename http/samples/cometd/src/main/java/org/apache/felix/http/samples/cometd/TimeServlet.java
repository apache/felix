/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.samples.cometd;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cometd.bayeux.MarkedReference;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;

public class TimeServlet
    extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private enum SystemTopics {
        Time
    };

    private BayeuxServer bayeuxServer;
    private LocalSession session;
    private List<ServerChannel> channels;
    private Timer updateTimer;
    private SimpleDateFormat logDateFormat;
    private String html =
        "<html>\n" +
        "  <head>\n" +
        "    <title>System Information</title>\n" +
        "    <script type='text/javascript' src='/js/dojo/dojo.js'></script>\n" +
        "    <script type='text/javascript'>\n" +
        "      dojo.require('dojox.cometd');\n" +
        "dojo.addOnLoad(function()\n" +
        "{\n" +
        "    var cometd = dojox.cometd;\n" +
        "\n" +
        "    function _connectionEstablished()\n" +
        "    {\n" +
        "        dojo.byId('body').innerHTML += '<div>CometD Connection Established</div>';\n" +
        "    }\n" +
        "\n" +
        "    function _connectionBroken()\n" +
        "    {\n" +
        "        dojo.byId('body').innerHTML += '<div>CometD Connection Broken</div>';\n" +
        "    }\n" +
        "\n" +
        "    function _connectionClosed()\n" +
        "    {\n" +
        "        dojo.byId('body').innerHTML += '<div>CometD Connection Closed</div>';\n" +
        "    }\n" +
        "\n" +
        "    // Function that manages the connection status with the Bayeux server\n" +
        "    var _connected = false;\n" +
        "    function _metaConnect(message)\n" +
        "    {\n" +
        "        if (cometd.isDisconnected())\n" +
        "        {\n" +
        "            _connected = false;\n" +
        "            _connectionClosed();\n" +
        "            return;\n" +
        "        }\n" +
        "\n" +
        "        var wasConnected = _connected;\n" +
        "        _connected = message.successful === true;\n" +
        "        if (!wasConnected && _connected)\n" +
        "        {\n" +
        "            _connectionEstablished();\n" +
        "        }\n" +
        "        else if (wasConnected && !_connected)\n" +
        "        {\n" +
        "            _connectionBroken();\n" +
        "        }\n" +
        "    }\n" +
        "\n" +
        "    // Function invoked when first contacting the server and\n" +
        "    // when the server has lost the state of this client\n" +
        "    function _metaHandshake(handshake)\n" +
        "    {\n" +
        "        if (handshake.successful === true)\n" +
        "        {\n" +
        "            cometd.batch(function()\n" +
        "            {\n" +
        "                cometd.subscribe('/System/Time', function(message)\n" +
        "                {\n" +
        "                    dojo.byId('systemTime').innerHTML = '<div>' + message.data + '</div>';\n" +
        "                });\n" +
        "            });\n" +
        "        }\n" +
        "    }\n" +
        "\n" +
        "    // Disconnect when the page unloads\n" +
        "    dojo.addOnUnload(function()\n" +
        "    {\n" +
        "        cometd.disconnect(true);\n" +
        "    });\n" +
        "\n" +
        "    var cometURL = \"http://localhost:8080/system/cometd\";\n" +
        "    cometd.configure({\n" +
        "        url: cometURL,\n" +
        "        logLevel: 'debug'\n" +
        "    });\n" +
        "\n" +
        "    cometd.addListener('/meta/handshake', _metaHandshake);\n" +
        "    cometd.addListener('/meta/connect', _metaConnect);\n" +
        "\n" +
        "    cometd.handshake();\n" +
        "});\n" +
        "    </script>\n" +
        "  </head>\n" +
        "  <body>\n" +
        "    <h2 id='systemTime'></h2>\n" +
        "  </body>\n" +
        "</html>\n";

    public TimeServlet(BayeuxServer bayeuxServer)
    {
        this.bayeuxServer = bayeuxServer;
        this.channels = new ArrayList<ServerChannel>();
        for (SystemTopics topic : SystemTopics.values()) {
        	MarkedReference<ServerChannel> created = bayeuxServer.createChannelIfAbsent("/System/" + topic, new ConfigurableServerChannel.Initializer() {
				@Override
                public void configureChannel(ConfigurableServerChannel channel) {
					channel.setPersistent(true);
				}
        	});
    		this.channels.add(topic.ordinal(), created.getReference());
        }
        this.logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		this.updateTimer = new Timer("System.Time.ClientNotifier");
		this.updateTimer.scheduleAtFixedRate(new ClientNotifier(), 1000, 1000);
		this.session = bayeuxServer.newLocalSession(TimeServlet.class.getName());
		this.session.handshake();
    }

    @Override
    public void init(ServletConfig config)
        throws ServletException
    {
        doLog("Init with config [" + config + "]");
        super.init(config);
    }

    @Override
    public void destroy()
    {
        doLog("Destroyed servlet");
        this.updateTimer.cancel();
        this.session.disconnect();
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println(this.html);
    }

    private void doLog(String message)
    {
        System.out.println("## TimeServlet: " + message);
    }

    private class ClientNotifier
        extends TimerTask
    {

        @Override
        public void run() {
            for (SystemTopics topic : SystemTopics.values()) {
                String topicData;
                switch (topic) {
                    case Time:
                        topicData = logDateFormat.format(new Date());
                        break;
                    default:
                        topicData = "unknown topic: " + topic.toString();
                }
                ServerMessage.Mutable message = bayeuxServer.newMessage();
                message.setChannel(channels.get(topic.ordinal()).getId());
                message.setData(topicData);
                channels.get(topic.ordinal()).publish(session, message);
            }
        }
    }

}
