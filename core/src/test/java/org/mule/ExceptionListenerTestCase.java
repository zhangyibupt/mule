/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.exception.AbstractExceptionListener;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExceptionListenerTestCase extends AbstractMuleTestCase
{

    @Test
    public void testAddGoodEndpoint() throws Exception
    {
        AbstractExceptionListener router = new DefaultMessagingExceptionStrategy(null);
        OutboundEndpoint endpoint = Mockito.mock(OutboundEndpoint.class);
        router.addEndpoint(endpoint);
        assertNotNull(router.getMessageProcessors());
        assertTrue(router.getMessageProcessors().contains(endpoint));
    }

    @Test
    public void testSetGoodEndpoints() throws Exception
    {
        List<MessageProcessor> list = new ArrayList<MessageProcessor>();
        list.add(Mockito.mock(OutboundEndpoint.class));
        list.add(Mockito.mock(OutboundEndpoint.class));

        AbstractExceptionListener router = new DefaultMessagingExceptionStrategy(null);
        assertNotNull(router.getMessageProcessors());
        assertEquals(0, router.getMessageProcessors().size());

        router.addEndpoint(Mockito.mock(OutboundEndpoint.class));
        assertEquals(1, router.getMessageProcessors().size());

        router.setMessageProcessors(list);
        assertNotNull(router.getMessageProcessors());
        assertEquals(2, router.getMessageProcessors().size());
    }
}
