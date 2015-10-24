/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the
 * NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.scr.integration.components.concurrency;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

public class CFactory implements Runnable {
    private ComponentFactory _cFactory;
    private Thread[] _threads = new Thread[2];
    
    public void bindCFactory(ComponentFactory cFactory) {
      _cFactory = cFactory;
    }
    
    void activate() {
      System.out.println("CFactory started");
      for (int i = 0; i < _threads.length; i++) {
        _threads[i] = new Thread(this);
        _threads[i].start();
      }
    }
    
    void deactivate() {
      System.out.println("CFactory stopped");
      for (int i = 0; i < _threads.length; i++) {
        _threads[i].interrupt();
        try {
          _threads[i].join();
        } catch (InterruptedException e) {
        }
      }
    }
    
    public void run() {
      while (true) {
        try {
          ComponentInstance ci = _cFactory.newInstance(null);
          ci.dispose();
          if (Thread.currentThread().isInterrupted()) {
            return;
          }
        } catch (Throwable t) {
          if (!(t instanceof InterruptedException)) {
            //System.out.println("CFactory thread exiting: got exception: " + t.toString());
          }
          return;
        }
      }
    }
  }
