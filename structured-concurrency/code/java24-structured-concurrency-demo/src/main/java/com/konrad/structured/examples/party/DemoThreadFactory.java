package com.konrad.structured.examples.party;

import java.util.concurrent.ThreadFactory;

public final class DemoThreadFactory {
    private DemoThreadFactory() {
    }

    public static final ThreadFactory
            VT_THREAD_FACTORY = Thread.ofVirtual().name("VT-", 0L).factory();

    public static final ThreadFactory
            PT_THREAD_FACTORY = Thread.ofPlatform().name("PT-", 0L).factory();
}
