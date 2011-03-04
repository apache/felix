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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.cometd.Bayeux;
import org.cometd.Channel;
import org.cometd.Client;

public class TimeServlet
    extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private enum SystemTopics {
        Time
    };

    private Bayeux bayeux;
    private Client client;
    private List<Channel> channels;
    private Timer updateTimer;
    private SimpleDateFormat logDateFormat;
    private String html =
        "<html>\n" +
        "  <head>\n" +
        "    <title>System Information</title>\n" +
        "    <script type='text/javascript' src='/dojo/dojo/dojo.js'></script>\n" +
        "    <script type='text/javascript'>\n" +
        "      dojo.require('dojox.cometd');\n" +
        "      dojo.addOnLoad(init);\n" +
        "      dojo.addOnUnload(destroy);\n" +
        "      function init() {\n" +
        "        dojox.cometd.init('/system/cometd');\n" +
        "        dojox.cometd.subscribe('/System/Time', updateTime);\n" +
        "      }\n" +
        "      function destroy() {\n" +
        "        dojox.cometd.unsubscribe('/System/Time');\n" +
        "        dojox.cometd.disconnect();\n" +
        "      }\n" +
        "      function updateTime(message) {\n" +
        "          document.getElementById('systemTime').innerHTML = message.data;\n" +
        "      }\n" +
        "    </script>\n" +
        "  </head>\n" +
        "  <body>\n" +
        "    <h2 id='systemTime'></h2>\n" +
        "  </body>\n" +
        "</html>\n";

    public TimeServlet(Bayeux bayeux)
    {
        this.bayeux = bayeux;
        this.client = this.bayeux.newClient(this.getClass().getName());
        this.channels = new ArrayList<Channel>();
        for (SystemTopics topic : SystemTopics.values()) {
            this.channels.add(topic.ordinal(), this.bayeux.getChannel("/System/" + topic, true));
        }
        this.updateTimer = new Timer("System.Time.ClientNotifier");
        this.updateTimer.scheduleAtFixedRate(new ClientNotifier(), 1000, 1000);
        this.logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
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
                channels.get(topic.ordinal()).publish(client, topicData, null);
            }
        }
    }
}
