package com.konrad.structured.examples.party;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ThreadFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoThreadFactory {
    public static final ThreadFactory
            THREAD_FACTORY = Thread.ofVirtual().name("VT-", 0L).factory();
}
