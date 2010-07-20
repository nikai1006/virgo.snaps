/*******************************************************************************
 * Copyright (c) 2008, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.virgo.snaps.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import org.eclipse.virgo.kernel.deployer.core.ApplicationDeployer;
import org.eclipse.virgo.kernel.deployer.core.DeploymentException;
import org.eclipse.virgo.kernel.deployer.core.DeploymentIdentity;
import org.eclipse.virgo.test.framework.dmkernel.DmKernelTestRunner;

@RunWith(DmKernelTestRunner.class)
public abstract class AbstractDeployerTests {

    private static final CountDownLatch latch = new CountDownLatch(1);
    
    private BundleContext context;

    private ApplicationDeployer deployer;

    protected BundleContext getContext() {
        return context;
    }

    protected ApplicationDeployer getDeployer() {
        return deployer;
    }

    protected DeploymentIdentity deploy(String path) throws DeploymentException {
        File f = new File(path);
        assertTrue(f.getAbsolutePath() + " does not exist.", f.exists());
        return getDeployer().deploy(f.toURI());
    }        
    
    @BeforeClass
    public static void registerEventHandler() {
        
        EventHandler eventHandler = new InitialArtifactDeploymentAwaitingEventHandler();
        
        Properties properties = new Properties();
        properties.setProperty("event.topics", "org/eclipse/virgo/kernel/*");
        
        FrameworkUtil.getBundle(AbstractDeployerTests.class).getBundleContext().registerService(EventHandler.class.getName(), eventHandler, properties);       
    }
    
    private static final class InitialArtifactDeploymentAwaitingEventHandler implements EventHandler {
        
        public void handleEvent(Event event) {
            if ("org/eclipse/virgo/kernel/userregion/systemartifacts/DEPLOYED".equals(event.getTopic())) {
                latch.countDown();  
            }
        }
    }

    @Before
    public void setUp() throws Exception {   
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                fail("System artifacts were not deployed within 30 seconds");
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            fail("System artifacts were not deployed within 30 seconds");
        }
        
        this.context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        
        ServiceReference appDeployerServiceReference = this.context.getServiceReference(ApplicationDeployer.class.getName());
        assertNotNull("ApplicationDeployer service reference not found", appDeployerServiceReference);
        this.deployer = (ApplicationDeployer) this.context.getService(appDeployerServiceReference);
        assertNotNull("ApplicationDeployer service not found", this.deployer);
    }
}
