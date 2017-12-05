/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.jms.example;

import java.io.File;
import java.io.IOException;
import java.security.ProtectionDomain;
import java.util.concurrent.TimeUnit;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

/**
 * A simple JMS Queue example that uses dual broker authentication mechanisms for SSL and non-SSL connections.
 */
public class MqttCrlEnabledExample {

   public static File basedir() throws IOException {
      ProtectionDomain protectionDomain = MqttCrlEnabledExample.class.getProtectionDomain();
      return new File(new File(protectionDomain.getCodeSource().getLocation().getPath()), "../..").getCanonicalFile();
   }


   public static void main(final String[] args) throws Exception {
      BlockingConnection connection1 = null;
      try {

         String basedir = basedir().getPath();
         System.setProperty("javax.net.ssl.trustStore", basedir + "/src/main/resources/activemq/server0/client.ts");
         System.setProperty("javax.net.ssl.trustStorePassword", "password");
         System.setProperty("javax.net.ssl.trustStoreType", "jks");
         System.setProperty("javax.net.ssl.keyStore", basedir + "/src/main/resources/activemq/server0/activemq-revoke.jks");
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
         System.out.println("Message received: " + new String(message1.getPayload()));

      } finally {
         if (connection1 != null) {
            connection1.disconnect();
         }
      }

   }


   private static BlockingConnection retrieveMQTTConnection(String host) throws Exception {

      MQTT mqtt = new MQTT();
      mqtt.setConnectAttemptsMax(1);
      mqtt.setReconnectAttemptsMax(0);
      mqtt.setHost(host);
      mqtt.setUserName("consumer");
      mqtt.setPassword("activemq");

//         mqtt.setTracer(createTracer());
//         if (clientId != null) {
//            mqtt.setClientId(clientId);
//         }
      mqtt.setCleanSession(true);


      BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();
      connection.disconnect();
      connection = mqtt.blockingConnection();
      connection.connect();
      return connection;
   }

}