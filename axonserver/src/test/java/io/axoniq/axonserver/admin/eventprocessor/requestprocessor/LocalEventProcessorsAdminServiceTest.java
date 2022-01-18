/*
 * Copyright (c) 2017-2022 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.admin.eventprocessor.requestprocessor;

import io.axoniq.axonserver.admin.eventprocessor.api.EventProcessorInstance;
import io.axoniq.axonserver.component.processor.EventProcessorIdentifier;
import io.axoniq.axonserver.component.processor.ProcessorEventPublisher;
import io.axoniq.axonserver.component.processor.listener.ClientProcessor;
import io.axoniq.axonserver.component.processor.listener.ClientProcessors;
import io.axoniq.axonserver.component.processor.listener.FakeClientProcessor;
import io.axoniq.axonserver.grpc.control.EventProcessorInfo;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.junit.*;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

/**
 * @author Sara Pellegrini
 */
@RunWith(MockitoJUnitRunner.class)
public class LocalEventProcessorsAdminServiceTest {

    @Mock
    ProcessorEventPublisher publisher;

    @Test
    public void pauseTest() {
        String processorName = "processorName";
        String tokenStore = "tokenStore";
        ClientProcessor clientA = new FakeClientProcessor("Client-A", processorName, tokenStore);
        ClientProcessor clientB = new FakeClientProcessor("Client-B", processorName, tokenStore);
        ClientProcessor clientC = new FakeClientProcessor("Client-C", "anotherProcessor", tokenStore);
        ClientProcessor clientD = new FakeClientProcessor("Client-D", processorName, "anotherTokenStore");
        ClientProcessors processors = () -> asList(clientA, clientB, clientC, clientD).iterator();
        LocalEventProcessorsAdminService testSubject = new LocalEventProcessorsAdminService(publisher, processors);
        testSubject.pause(new EventProcessorIdentifier(processorName, tokenStore), () -> "authenticated-user")
                   .block();
        verify(publisher).pauseProcessorRequest("default", "Client-A", processorName);
        verify(publisher).pauseProcessorRequest("default", "Client-B", processorName);
        verifyNoMoreInteractions(publisher);
    }

    @Test
    public void startTest() {
        String processorName = "processorName";
        String tokenStore = "tokenStore";
        ClientProcessor clientA = new FakeClientProcessor("Client-A", processorName, tokenStore);
        ClientProcessor clientB = new FakeClientProcessor("Client-B", processorName, tokenStore);
        ClientProcessor clientC = new FakeClientProcessor("Client-C", "anotherProcessor", tokenStore);
        ClientProcessor clientD = new FakeClientProcessor("Client-D", processorName, "anotherTokenStore");
        ClientProcessors processors = () -> asList(clientA, clientB, clientC, clientD).iterator();
        LocalEventProcessorsAdminService testSubject = new LocalEventProcessorsAdminService(publisher, processors);
        testSubject.start(new EventProcessorIdentifier(processorName, tokenStore), () -> "authenticated-user")
                   .block();
        verify(publisher).startProcessorRequest("default", "Client-A", processorName);
        verify(publisher).startProcessorRequest("default", "Client-B", processorName);
        verifyNoMoreInteractions(publisher);
    }


    @Test
    public void splitTest() {
        String processorName = "processorName";
        String tokenStore = "tokenStore";
        ClientProcessor clientA = new FakeClientProcessor("Client-A", processorName, tokenStore);
        ClientProcessor clientB = new FakeClientProcessor("Client-B", processorName, tokenStore);
        ClientProcessor clientC = new FakeClientProcessor("Client-C", "anotherProcessor", tokenStore);
        ClientProcessor clientD = new FakeClientProcessor("Client-D", processorName, "anotherTokenStore");
        ClientProcessors processors = () -> asList(clientA, clientB, clientC, clientD).iterator();
        LocalEventProcessorsAdminService testSubject = new LocalEventProcessorsAdminService(publisher, processors);
        testSubject.split(new EventProcessorIdentifier(processorName, tokenStore), () -> "authenticated-user")
                   .block();
        List<String> clients = asList("Client-A", "Client-B");
        verify(publisher).splitSegment("default", clients, processorName);
        verifyNoMoreInteractions(publisher);
    }

    @Test
    public void mergeTest() {
        String processorName = "processorName";
        String tokenStore = "tokenStore";
        ClientProcessor clientA = new FakeClientProcessor("Client-A", processorName, tokenStore);
        ClientProcessor clientB = new FakeClientProcessor("Client-B", processorName, tokenStore);
        ClientProcessor clientC = new FakeClientProcessor("Client-C", "anotherProcessor", tokenStore);
        ClientProcessor clientD = new FakeClientProcessor("Client-D", processorName, "anotherTokenStore");
        ClientProcessors processors = () -> asList(clientA, clientB, clientC, clientD).iterator();
        LocalEventProcessorsAdminService testSubject = new LocalEventProcessorsAdminService(publisher, processors);
        testSubject.merge(new EventProcessorIdentifier(processorName, tokenStore), () -> "authenticated-user")
                   .block();
        List<String> clients = asList("Client-A", "Client-B");
        verify(publisher).mergeSegment("default", clients, processorName);
        verifyNoMoreInteractions(publisher);
    }

    @Test
    public void testMoveSegment() {
        String processorName = "processorName";
        String tokenStore = "tokenStore";
        ClientProcessor clientA = new FakeClientProcessor("Client-A", processorName, tokenStore);
        ClientProcessor clientB = new FakeClientProcessor("Client-B", processorName, tokenStore);
        ClientProcessor clientC = new FakeClientProcessor("Client-C", "anotherProcessor", tokenStore);
        ClientProcessor clientD = new FakeClientProcessor("Client-D", processorName, "anotherTokenStore");
        ClientProcessor clientE = new FakeClientProcessor("Client-E", processorName, tokenStore);
        ClientProcessors processors = () -> asList(clientA, clientB, clientC, clientD, clientE).iterator();
        LocalEventProcessorsAdminService testSubject = new LocalEventProcessorsAdminService(publisher, processors);
        testSubject.move(new EventProcessorIdentifier(processorName, tokenStore), 2, "Client-B",
                         () -> "authenticated-user").block();
        verify(publisher).releaseSegment("default", "Client-A", processorName, 2);
        verify(publisher).releaseSegment("default", "Client-E", processorName, 2);
        verifyNoMoreInteractions(publisher);
    }

    @Test
    public void testGetByComponent() {
        EventProcessorInfo eventProcessorA = buildEventProcessorInfo("blue", "X");
        EventProcessorInfo eventProcessorB = buildEventProcessorInfo("blue", "X");
        EventProcessorInfo eventProcessorC = buildEventProcessorInfo("blue", "Y");
        EventProcessorInfo eventProcessorD = buildEventProcessorInfo("green", "X");
        EventProcessorInfo eventProcessorE = buildEventProcessorInfo("green", "Y");
        ClientProcessor clientA = new FakeClientProcessor("Client-A", true, eventProcessorA);
        ClientProcessor clientB = new FakeClientProcessor("Client-B", false, eventProcessorB);
        ClientProcessor clientC = new FakeClientProcessor("Client-C", false, eventProcessorC);
        ClientProcessor clientD = new FakeClientProcessor("Client-D", false, eventProcessorD);
        ClientProcessor clientE = new FakeClientProcessor("Client-E", true, eventProcessorE);

        ClientProcessors processors = () -> asList(clientA, clientB, clientC, clientD, clientE).iterator();
        LocalEventProcessorsAdminService testSubject = new LocalEventProcessorsAdminService(publisher, processors);
        Flux<String> clients = testSubject.eventProcessorsByApplication("component",
                                                                        () -> "authenticated-user")
                                          .flatMap(eventProcessor -> Flux.fromIterable(eventProcessor.instances()))
                                          .map(EventProcessorInstance::clientId);

        StepVerifier.create(clients)
                    .expectNext("Client-A", "Client-B", "Client-E")
                    .verifyComplete();
    }

    @Nonnull
    private EventProcessorInfo buildEventProcessorInfo(String processorName, String tokenStoreId) {
        return EventProcessorInfo.newBuilder()
                                 .setProcessorName(processorName)
                                 .setTokenStoreIdentifier(tokenStoreId)
                                 .build();
    }
}