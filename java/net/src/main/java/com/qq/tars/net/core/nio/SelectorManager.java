/**
 * Tencent is pleased to support the open source community by making Tars available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.qq.tars.net.core.nio;

import com.qq.tars.net.core.Processor;
import com.qq.tars.net.protocol.ProtocolFactory;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

// TODO: 17/4/13 by zmyer
public final class SelectorManager {
    //集合中reactor对象的数量
    private final AtomicLong sets = new AtomicLong(0);
    //reactor集合
    private final Reactor[] reactorSet;
    //协议工厂对象
    private ProtocolFactory protocolFactory = null;
    //线程池对象
    private Executor threadPool = null;
    //处理器对象
    private Processor processor = null;
    //selector线程池大小
    private final int selectorPoolSize;
    //是否启动管理器
    private volatile boolean started;
    //存活标记
    private boolean keepAlive;
    //是否延时标记
    private boolean isTcpNoDelay = false;

    // TODO: 17/4/13 by zmyer
    public SelectorManager(int selectorPoolSize, ProtocolFactory protocolFactory, Executor threadPool,
        Processor processor, boolean keepAlive, String reactorNamePrefix) throws IOException {
        this(selectorPoolSize, protocolFactory, threadPool, processor, keepAlive, reactorNamePrefix, false);
    }

    // TODO: 17/4/13 by zmyer
    public SelectorManager(int selectorPoolSize, ProtocolFactory protocolFactory, Executor threadPool,
        Processor processor, boolean keepAlive, String reactorNamePrefix, boolean udpMode) throws IOException {
        //如果是udp,则select池中的为1
        if (udpMode)
            selectorPoolSize = 1;

        this.selectorPoolSize = selectorPoolSize;
        this.protocolFactory = protocolFactory;
        this.threadPool = threadPool;
        this.processor = processor;
        this.keepAlive = keepAlive;

        //创建reactor池对象
        reactorSet = new Reactor[selectorPoolSize];

        for (int i = 0; i < reactorSet.length; i++) {
            //创建reactor对象
            reactorSet[i] = new Reactor(this, reactorNamePrefix + "-" + protocolFactory.getClass().getSimpleName().toLowerCase() + "-" + String.valueOf(i), udpMode);
        }
    }

    // TODO: 17/4/13 by zmyer
    public synchronized void start() {
        if (this.started) {
            return;
        }

        this.started = true;
        for (Reactor reactor : this.reactorSet) {
            reactor.start();
        }
    }

    // TODO: 17/4/13 by zmyer
    public synchronized void stop() {
        if (!this.started)
            return;

        this.started = false;
        for (Reactor reactor : this.reactorSet) {
            reactor.interrupt();
        }
    }

    // TODO: 17/4/13 by zmyer
    public Reactor getReactor(int index) {
        if (index < 0 || index > this.reactorSet.length - 1) {
            throw new IllegalArgumentException("failed to get one reactor thread...");
        }

        return this.reactorSet[index];
    }

    // TODO: 17/4/13 by zmyer
    public final Reactor nextReactor() {
        return this.reactorSet[(int) (this.sets.incrementAndGet() % this.selectorPoolSize)];
    }

    // TODO: 17/4/13 by zmyer
    public final Reactor getReactor(SelectionKey key) {
        Reactor reactor;
        Selector selector = key.selector();

        for (Reactor aReactorSet : this.reactorSet) {
            reactor = aReactorSet;
            if (reactor.selector == selector) {
                return reactor;
            }
        }

        return null;
    }

    // TODO: 17/4/13 by zmyer
    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    // TODO: 17/4/13 by zmyer
    public void setProtocolFactory(ProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    // TODO: 17/4/13 by zmyer
    public Processor getProcessor() {
        return processor;
    }

    // TODO: 17/4/13 by zmyer
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    // TODO: 17/4/13 by zmyer
    public Executor getThreadPool() {
        return threadPool;
    }

    // TODO: 17/4/13 by zmyer
    public void setThreadPool(Executor threadPool) {
        this.threadPool = threadPool;
    }

    // TODO: 17/4/13 by zmyer
    public boolean isKeepAlive() {
        return keepAlive;
    }

    // TODO: 17/4/13 by zmyer
    public boolean isTcpNoDelay() {
        return isTcpNoDelay;
    }

    // TODO: 17/4/13 by zmyer
    public void setTcpNoDelay(boolean on) {
        this.isTcpNoDelay = on;
    }
}
