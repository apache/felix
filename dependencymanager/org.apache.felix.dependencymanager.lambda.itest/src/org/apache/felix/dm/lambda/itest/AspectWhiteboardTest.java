/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.lambda.itest;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.aspect;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public class AspectWhiteboardTest extends TestBase {

	public void testWhiteboardConsumer() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create service providers and consumer
        Component sp1 = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class.getName()).build();
        Component sp2 = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class.getName()).build();
        ServiceConsumer sci = new ServiceConsumer(e);
        Component sc = component(m).impl(sci).withSvc(ServiceInterface.class, srv->srv.required(false).add("add").remove("remove")).build();
        Component sa2 = aspect(m, ServiceInterface.class).rank(20).autoAdd(false).impl(new ServiceAspect(e, 3)).build();
        Component sa1 = aspect(m, ServiceInterface.class).rank(10).autoAdd(false).impl(new ServiceAspect(e, 4)).build();
        
        // start with a service consumer
        System.out.println("Adding consumer");
        m.add(sc);
        
        // then add two providers, so the consumer will see two services
        System.out.println("Adding 2 providers");
        m.add(sp1);
        m.add(sp2);
        
        // make sure consumer sees both services
        Assert.assertEquals(2, sci.services());
        
        // add an aspect with ranking 20
        System.out.println("Adding aspect with rank 20");
        m.add(sa2);
        
        // make sure the consumer sees the two new aspects and no longer sees the two original services
        Assert.assertEquals(2, sci.services());
        Assert.assertEquals(20, sci.highestRanking());
        Assert.assertEquals(20, sci.lowestRanking());
        
        // add an aspect with ranking 10
        System.out.println("Adding aspect with rank 10");
        m.add(sa1);
        
        // make sure the consumer still sees the two aspects with ranking 20
        Assert.assertEquals(2, sci.services());
        Assert.assertEquals(20, sci.highestRanking());
        Assert.assertEquals(20, sci.lowestRanking());
        
        // remove the aspect with ranking 20
        System.out.println("Removing aspect with rank 20");
        m.remove(sa2);
        
        // make sure the consumer now sees the aspects with ranking 10
        Assert.assertEquals(2, sci.services());
        Assert.assertEquals(10, sci.highestRanking());
        Assert.assertEquals(10, sci.lowestRanking());
        
        // remove one of the original services
        System.out.println("Removing 1 service");
        m.remove(sp1);
        
        // make sure the aspect of that service goes away
        Assert.assertEquals(1, sci.services());
        Assert.assertEquals(10, sci.highestRanking());
        Assert.assertEquals(10, sci.lowestRanking());
        
        // remove the aspect with ranking 10
        System.out.println("Removing aspect with rank 10");
        m.remove(sa1);
        
        // make sure only the original service remains
        Assert.assertEquals(1, sci.services());
        Assert.assertEquals(0, sci.highestRanking());
        Assert.assertEquals(0, sci.lowestRanking());

        System.out.println("Done with test");

        // end of test
        m.remove(sa2);
        m.remove(sp2);
        m.remove(sc);
    }
    
    public void testWhiteboardConsumerRef() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create service providers and consumer
        Component sp1 = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class.getName()).build();
        Component sp2 = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class.getName()).build();
        ServiceConsumer sci = new ServiceConsumer(e);
        Component sc = component(m).impl(sci).withSvc(ServiceInterface.class, srv->srv.optional().add(sci::addRef).remove(sci::removeRef)).build();
        Component sa2 = aspect(m, ServiceInterface.class).rank(20).impl(new ServiceAspect(e, 3)).build();
        Component sa1 = aspect(m, ServiceInterface.class).rank(10).impl(new ServiceAspect(e, 4)).build();
        
        // start with a service consumer
        System.out.println("Adding consumer");
        m.add(sc);
        
        // then add two providers, so the consumer will see two services
        System.out.println("Adding 2 providers");
        m.add(sp1);
        m.add(sp2);
        
        // make sure consumer sees both services
        Assert.assertEquals(2, sci.services());
        
        // add an aspect with ranking 20
        System.out.println("Adding aspect with rank 20");
        m.add(sa2);
        
        // make sure the consumer sees the two new aspects and no longer sees the two original services
        Assert.assertEquals(2, sci.services());
        Assert.assertEquals(20, sci.highestRanking());
        Assert.assertEquals(20, sci.lowestRanking());
        
        // add an aspect with ranking 10
        System.out.println("Adding aspect with rank 10");
        m.add(sa1);
        
        // make sure the consumer still sees the two aspects with ranking 20
        Assert.assertEquals(2, sci.services());
        Assert.assertEquals(20, sci.highestRanking());
        Assert.assertEquals(20, sci.lowestRanking());
        
        // remove the aspect with ranking 20
        System.out.println("Removing aspect with rank 20");
        m.remove(sa2);
        
        // make sure the consumer now sees the aspects with ranking 10
        Assert.assertEquals(2, sci.services());
        Assert.assertEquals(10, sci.highestRanking());
        Assert.assertEquals(10, sci.lowestRanking());
        
        // remove one of the original services
        System.out.println("Removing 1 service");
        m.remove(sp1);
        
        // make sure the aspect of that service goes away
        Assert.assertEquals(1, sci.services());
        Assert.assertEquals(10, sci.highestRanking());
        Assert.assertEquals(10, sci.lowestRanking());
        
        // remove the aspect with ranking 10
        System.out.println("Removing aspect with rank 10");
        m.remove(sa1);
        
        // make sure only the original service remains
        Assert.assertEquals(1, sci.services());
        Assert.assertEquals(0, sci.highestRanking());
        Assert.assertEquals(0, sci.lowestRanking());

        System.out.println("Done with test");

        // end of test
        m.remove(sa2);
        m.remove(sp2);
        m.remove(sc);
    }
    
    static interface ServiceInterface {
        public void invoke(Runnable run);
    }
    
    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke(Runnable run) {
            run.run();
        }
    }
    
    static class ServiceAspect implements ServiceInterface {
        private final Ensure m_ensure;
        private volatile ServiceInterface m_parentService;
        private final int m_step;
        
        public ServiceAspect(Ensure e, int step) {
            m_ensure = e;
            m_step = step;
        }
        public void start() {
        }
        
        public void invoke(Runnable run) {
            m_ensure.step(m_step);
            m_parentService.invoke(run);
        }
        
        public void stop() {
        }
    }

    static class ServiceConsumer implements Runnable {
        private List m_services = new ArrayList();
        private final Ensure m_ensure;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
        }
        
        public int services() {
            return m_services.size();
        }
        
        public int highestRanking() {
            int ranking = Integer.MIN_VALUE;
            for (int i = 0; i < m_services.size(); i++) {
                ServiceReference ref = (ServiceReference) m_services.get(i);
                Integer r = (Integer) ref.getProperty(Constants.SERVICE_RANKING);
                int rank = r == null ? 0 : r.intValue();
                ranking = Math.max(ranking, rank);
            }
            return ranking;
        }
        public int lowestRanking() {
            int ranking = Integer.MAX_VALUE;
            for (int i = 0; i < m_services.size(); i++) {
                ServiceReference ref = (ServiceReference) m_services.get(i);
                Integer r = (Integer) ref.getProperty(Constants.SERVICE_RANKING);
                int rank = r == null ? 0 : r.intValue();
                ranking = Math.min(ranking, rank);
            }
            return ranking;
        }
        
        // method ref callback
        public void addRef(ServiceInterface svc, ServiceReference ref) {
            add(ref, svc);
        }
        
        // refection callback
        public void add(ServiceReference ref, ServiceInterface svc) {
            System.out.println("Added: " + ServiceUtil.toString(ref));
            m_services.add(ref);
        }
        
        // method ref callback
        public void removeRef(ServiceInterface svc, ServiceReference ref) {
            remove(ref, svc);
        }

        // refection callback
        public void remove(ServiceReference ref, ServiceInterface svc) {
            System.out.println("Removed: " + ServiceUtil.toString(ref));
            m_services.remove(ref);
        }        
    }
}
