/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor.chain;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.StringAppendTransformer;
import org.mule.util.ObjectUtils;

import org.junit.Test;

public class DefaultMessageProcessorChainTestCase extends AbstractMuleTestCase
{

    @Test
    public void testMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingMP("1"), new AppendingMP("2"), new AppendingMP("3"));
        assertEquals("0123", builder.build().process(getTestEvent("0")).getMessageAsString());
    }

    public void testMPChainWithBuilder() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingMP("1"));
        builder.chain(new MessageProcessorBuilder()
        {
            public MessageProcessor build()
            {
                return new AppendingMP("2");
            }
        });
        builder.chain(new AppendingMP("3"));
        assertEquals("0123", builder.build().process(getTestEvent("0")).getMessageAsString());
    }

    @Test
    public void testInterceptingMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"),
            new AppendingInterceptingMP("3"));
        assertEquals("0before1before2before3after3after2after1", builder.build()
            .process(getTestEvent("0"))
            .getMessageAsString());
    }

    @Test
    public void testMixedMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new AppendingMP("2"), new AppendingMP("3"),
            new AppendingInterceptingMP("4"), new AppendingMP("5"));
        assertEquals("0before123before45after4after1", builder.build()
            .process(getTestEvent("0"))
            .getMessageAsString());
    }

    @Test
    public void testNestedMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingMP("1"), new DefaultMessageProcessorChainBuilder().chain(
            new AppendingMP("a"), new AppendingMP("b")).build(), new AppendingMP("2"));
        assertEquals("01ab2", builder.build().process(getTestEvent("0")).getMessageAsString());
    }

    @Test
    public void testNestedInterceptingMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new DefaultMessageProcessorChainBuilder().chain(
            new AppendingInterceptingMP("a"), new AppendingInterceptingMP("b")).build(),
            new AppendingInterceptingMP("2"));
        assertEquals("0before1beforeabeforebafterbafterabefore2after2after1", builder.build().process(
            getTestEvent("0")).getMessageAsString());
    }

    @Test
    public void testNestedMixedMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingMP("1"), new DefaultMessageProcessorChainBuilder().chain(
            new AppendingInterceptingMP("a"), new AppendingMP("b")).build(), new AppendingInterceptingMP("2"));
        assertEquals("01beforeabafterabefore2after2", builder.build()
            .process(getTestEvent("0"))
            .getMessageAsString());
    }

    @Test
    public void testInterceptingMPChainStopFlow() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2", true),
            new AppendingInterceptingMP("3"));
        assertEquals("0before1after1", builder.build().process(getTestEvent("0")).getMessageAsString());
    }

    /**
     * Note: Stopping the flow of a nested chain causes the nested chain to return early, but does not stop
     * the flow of the parent chain.
     */
    @Test
    public void testNestedInterceptingMPChainStopFlow() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new DefaultMessageProcessorChainBuilder().chain(
            new AppendingInterceptingMP("a", true), new AppendingInterceptingMP("b")).build(),
            new AppendingInterceptingMP("3"));
        assertEquals("0before1before3after3after1", builder.build()
            .process(getTestEvent("0"))
            .getMessageAsString());
    }

    @Test
    public void testMPChainLifecycle() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        AppendingMP mp1 = new AppendingInterceptingMP("1");
        AppendingMP mp2 = new AppendingInterceptingMP("2");
        MessageProcessor chain = builder.chain(mp1, mp2).build();
        ((Lifecycle) chain).initialise();
        ((Lifecycle) chain).start();
        ((Lifecycle) chain).stop();
        ((Lifecycle) chain).dispose();
        assertLifecycle(mp1);
        assertLifecycle(mp2);
    }

    @Test
    public void testNestedMPChainLifecycle() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        DefaultMessageProcessorChainBuilder nestedBuilder = new DefaultMessageProcessorChainBuilder();
        AppendingMP mp1 = new AppendingInterceptingMP("1");
        AppendingMP mp2 = new AppendingInterceptingMP("2");
        AppendingMP mpa = new AppendingInterceptingMP("a");
        AppendingMP mpb = new AppendingInterceptingMP("b");
        MessageProcessor chain = builder.chain(mp1, nestedBuilder.chain(mpa, mpb).build(), mp2).build();
        ((Lifecycle) chain).initialise();
        ((Lifecycle) chain).start();
        ((Lifecycle) chain).stop();
        ((Lifecycle) chain).dispose();
        assertLifecycle(mp1);
        assertLifecycle(mp2);
        assertLifecycle(mpa);
        assertLifecycle(mpb);
    }

    public void testNoneIntercepting() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestNonIntercepting(), new TestNonIntercepting(), new TestNonIntercepting());
        MuleEvent restul = builder.build().process(getTestEvent(""));
        assertEquals("MessageProcessorMessageProcessorMessageProcessor", restul.getMessageAsString());
    }

    public void testAllIntercepting() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestIntercepting(), new TestIntercepting(), new TestIntercepting());
        MuleEvent restul = builder.build().process(getTestEvent(""));
        assertEquals("InterceptingMessageProcessorInterceptingMessageProcessorInterceptingMessageProcessor",
            restul.getMessageAsString());
    }

    public void testMix() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(),
            new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting());
        MuleEvent restul = builder.build().process(getTestEvent(""));
        assertEquals(
            "InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
            restul.getMessageAsString());
    }

    public void testMixStaticFactoryt() throws Exception
    {
        MessageProcessorChain chain = DefaultMessageProcessorChain.from(new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(),
            new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting());
        MuleEvent restul = chain.process(getTestEvent(""));
        assertEquals(
            "InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
            restul.getMessageAsString());
    }
    
    public void testMix2() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestNonIntercepting(), new TestIntercepting(), new TestNonIntercepting(),
            new TestNonIntercepting(), new TestNonIntercepting(), new TestIntercepting());
        MuleEvent restul = builder.build().process(getTestEvent(""));
        assertEquals(
            "MessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
            restul.getMessageAsString());
    }

    public void testMix2StaticFactory() throws Exception
    {
        MessageProcessorChain chain = DefaultMessageProcessorChain.from(new TestNonIntercepting(), new TestIntercepting(), new TestNonIntercepting(),
            new TestNonIntercepting(), new TestNonIntercepting(), new TestIntercepting());
        MuleEvent restul = chain.process(getTestEvent(""));
        assertEquals(
            "MessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
            restul.getMessageAsString());
    }
    
    static class TestNonIntercepting implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return new StringAppendTransformer("MessageProcessor").process(event);
        }
    }

    static class TestIntercepting extends AbstractInterceptingMessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return processNext(new StringAppendTransformer("InterceptingMessageProcessor").process(event));
        }
    }

    private void assertLifecycle(AppendingMP mp)
    {
        assertTrue(mp.initialised);
        assertTrue(mp.started);
        assertTrue(mp.stopped);
        assertTrue(mp.disposed);
    }

    private static class AppendingMP implements MessageProcessor, Lifecycle
    {
        String appendString;
        boolean initialised;
        boolean started;
        boolean stopped;
        boolean disposed;

        public AppendingMP(String append)
        {
            this.appendString = append;
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return new DefaultMuleEvent(new DefaultMuleMessage(event.getMessageAsString() + appendString,
                muleContext), event);
        }

        public void initialise() throws InitialisationException
        {
            initialised = true;
        }

        public void start() throws MuleException
        {
            started = true;
        }

        public void stop() throws MuleException
        {
            stopped = true;
        }

        public void dispose()
        {
            disposed = true;
        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }
    }

    private static class AppendingInterceptingMP extends AppendingMP implements InterceptingMessageProcessor
    {
        private boolean stopProcessing;
        private MessageProcessor next;

        public AppendingInterceptingMP(String append)
        {
            this(append, false);
        }

        public AppendingInterceptingMP(String append, boolean stopProcessing)
        {
            super(append);
            this.stopProcessing = stopProcessing;
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (stopProcessing)
            {
                return event;
            }

            MuleEvent intermediateEvent = new DefaultMuleEvent(new DefaultMuleMessage(
                event.getMessageAsString() + "before" + appendString, muleContext), event);
            if (next != null)
            {
                intermediateEvent = next.process(intermediateEvent);
            }
            return new DefaultMuleEvent(new DefaultMuleMessage(intermediateEvent.getMessageAsString()
                                                               + "after" + appendString, muleContext),
                intermediateEvent);
        }

        public void setListener(MessageProcessor mp)
        {
            next = mp;
        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }
    }
}