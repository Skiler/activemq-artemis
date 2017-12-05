/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.integration.mqtt.imported;

import java.io.File;
import java.io.IOException;
import java.security.ProtectionDomain;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.junit.Test;

public class MQTTSecurityCRLTest extends ActiveMQTestBase {

   protected String noprivUser = "noprivs";
   protected String noprivPass = "noprivs";

   protected String browseUser = "browser";
   protected String browsePass = "browser";

   protected String guestUser = "guest";
   protected String guestPass = "guest";

   protected String fullUser = "user";
   protected String fullPass = "pass";


   public File basedir() throws IOException {
      ProtectionDomain protectionDomain = getClass().getProtectionDomain();
      return new File(new File(protectionDomain.getCodeSource().getLocation().getPath()), "../..").getCanonicalFile();
   }

   @Test
   public void crlTest() throws Exception {

      ActiveMQServer server1 = initServer("mqttCrl/broker.xml", "broker");
      BlockingConnection connection1 = null;
      try {
         server1.start();

         while (!server1.isStarted()) {
            Thread.sleep(50);
         }

         String basedir = basedir().getPath();
         System.setProperty("javax.net.ssl.trustStore", basedir + "/src/test/resources/mqttCrl/client.ts");
         System.setProperty("javax.net.ssl.trustStorePassword", "password");
         System.setProperty("javax.net.ssl.trustStoreType", "jks");
         System.setProperty("javax.net.ssl.keyStore", basedir + "/src/test/resources/mqttCrl/activemq-revoke.jks");
         System.setProperty("javax.net.ssl.keyStorePassword", "password");
         System.setProperty("javax.net.ssl.keyStoreType", "jks");

         connection1 = retrieveMQTTConnection("ssl://localhost:1883");

         // Subscribe to topics
         Topic[] topics = {new Topic("test/+/some/#", QoS.AT_MOST_ONCE)};
         connection1.subscribe(topics);

         // Publish Messages
         String payload1 = "This is message 1";

         connection1.publish("test/1/some/la", payload1.getBytes(), QoS.AT_LEAST_ONCE, false);

         Message message1 = connection1.receive(5, TimeUnit.SECONDS);

         assertEquals(payload1, new String(message1.getPayload()));

      } finally {
         if (connection1 != null) {
            connection1.disconnect();
         }
         if (server1.isStarted()) {
            server1.stop();
         }
      }

   }

   protected void configureBrokerSecurity(ActiveMQServer server) {
      ActiveMQJAASSecurityManager securityManager = (ActiveMQJAASSecurityManager) server.getSecurityManager();

      // User additions
      securityManager.getConfiguration().addUser(noprivUser, noprivPass);
      securityManager.getConfiguration().addRole(noprivUser, "nothing");
      securityManager.getConfiguration().addUser(browseUser, browsePass);
      securityManager.getConfiguration().addRole(browseUser, "browser");
      securityManager.getConfiguration().addUser(guestUser, guestPass);
      securityManager.getConfiguration().addRole(guestUser, "guest");
      securityManager.getConfiguration().addUser(fullUser, fullPass);
      securityManager.getConfiguration().addRole(fullUser, "full");
   }

   private BlockingConnection retrieveMQTTConnection(String host) throws Exception {

      MQTT mqtt = new MQTT();
      mqtt.setConnectAttemptsMax(1);
      mqtt.setReconnectAttemptsMax(0);
      mqtt.setHost(host);
      mqtt.setTracer(new MQTTTestSupport().createTracer());
      mqtt.setUserName(fullUser);
      mqtt.setPassword(fullPass);

//         mqtt.setTracer(createTracer());
//         if (clientId != null) {
//            mqtt.setClientId(clientId);
//         }
      mqtt.setCleanSession(true);

      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(new KeyManager[0], new TrustManager[]{new MQTTTestSupport.DefaultTrustManager()}, new SecureRandom());
      //mqtt.setSslContext(ctx);

      BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();
      return connection;
   }

   private ActiveMQServer initServer(String configFile, String name) throws Exception {
      Configuration configuration = createConfiguration(configFile, name);


      ActiveMQServer server = createServer(true, configuration);
      configureBrokerSecurity(server);
      return server;
   }

   protected Configuration createConfiguration(String fileName, String name) throws Exception {
      FileConfiguration fc = new FileConfiguration();
      FileDeploymentManager deploymentManager = new FileDeploymentManager(fileName);
      deploymentManager.addDeployable(fc);

      deploymentManager.readConfiguration();

      // we need this otherwise the data folder will be located under activemq-server and not on the temporary directory
      fc.setPagingDirectory(getTestDir() + "/" + name + "/" + fc.getPagingDirectory());
      fc.setLargeMessagesDirectory(getTestDir() + "/" + name + "/" + fc.getLargeMessagesDirectory());
      fc.setJournalDirectory(getTestDir() + "/" + name + "/" + fc.getJournalDirectory());
      fc.setBindingsDirectory(getTestDir() + "/" + name + "/" + fc.getBindingsDirectory());

      return fc;
   }


}
