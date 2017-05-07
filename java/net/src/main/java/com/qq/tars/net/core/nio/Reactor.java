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

import com.qq.tars.net.core.Session;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

// TODO: 17/4/13 by zmyer
public final class Reactor extends Thread {
    //sector对象
    protected volatile Selector selector = null;
    //是否crash标记
    private volatile boolean crashed = false;
    //注册列表
    private final Queue<Object[]> register = new LinkedBlockingQueue<Object[]>();
    //注销列表
    private final Queue<Session> unregister = new LinkedBlockingQueue<Session>();
    //接收器对象
    private Acceptor acceptor = null;

    // TODO: 17/4/13 by zmyer
    public Reactor(SelectorManager selectorManager, String name) throws IOException {
        this(selectorManager, name, false);
    }

    // TODO: 17/4/13 by zmyer
    public Reactor(SelectorManager selectorManager, String name, boolean udpMode) throws IOException {
        super(name);

        if (udpMode) {
            //如果是udp,创建udp接收器
            this.acceptor = new UDPAcceptor(selectorManager);
        } else {
            //如果是tcp,创建tcp接收器
            this.acceptor = new TCPAcceptor(selectorManager);
        }
        //打开selector对象
        this.selector = Selector.open();
    }

    // TODO: 17/4/13 by zmyer
    public void unRegisterChannel(Session session) {
        if (this.unregister.contains(session))
            return;
        this.unregister.add(session);
        this.selector.wakeup();
    }

    // TODO: 17/4/13 by zmyer
    public void registerChannel(SelectableChannel channel, int ops, Object attachment) throws IOException {
        if (crashed) {
            throw new IOException("The reactor thread carsh.... ");
        }

        if (Thread.currentThread() == this) {
            //在selector对象中注册指定的事件类型
            SelectionKey key = channel.register(this.selector, ops, attachment);

            if (attachment instanceof TCPSession) {
                //如果是tcp会话对象,则将其注册到attachment对象中
                ((TCPSession) attachment).setKey(key);
            }
        } else {
            //如果当前的线程不是reactor线程,则直接将事件插入到注册列表中
            this.register.offer(new Object[] {channel, ops, attachment});
            //开始唤醒selector对象
            this.selector.wakeup();
        }
    }

    // TODO: 17/4/15 by zmyer
    public void run() {
        try {
            for (; ; ) {
                //开始io轮询
                selector.select();
                //首先将待注册的通道注册到selector对象中
                processRegister();
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid())
                        continue;

                    try {
                        //1. Update the last operation time
                        //如果当且attachement对象是会话对象
                        if (key.attachment() != null && key.attachment() instanceof Session) {
                            //更新会话对象的操作时间戳
                            ((Session) key.attachment()).updateLastOperationTime();
                        }

                        //2. Dispatch I/O event
                        //开始分发事件
                        dispatchEvent(key);

                    } catch (Throwable ex) {
                        disConnectWithException(key, ex);
                    }
                }
                //开始处理注销事件
                processUnRegister();
            }
        } catch (Throwable e) {
            crashed = true;
            e.printStackTrace();
        }
    }

    // TODO: 17/4/15 by zmyer
    private void disConnectWithException(final SelectionKey key, final Throwable ex) {
        try {
            //读取会话对象
            Session session = (Session) key.attachment();

            if (session == null) {
                //如果会话对象不存在,则直接关闭掉该通道对象
                if (key.channel() instanceof SocketChannel)
                    key.channel().close();
            } else {
                //关闭会话对象
                session.close();
            }
            ex.printStackTrace();
        } catch (Throwable e2) {
            ex.printStackTrace();
        }
    }

    // TODO: 17/4/15 by zmyer
    private void processUnRegister() {
        Session session = null;

        while ((session = this.unregister.poll()) != null) {
            try {
                //关闭掉待注销的会话对象
                ((TCPSession) session).close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // TODO: 17/4/15 by zmyer
    private void processRegister() {
        Object[] object;
        SelectableChannel channel;
        int ops;
        Object attachment;

        //从注册列表中读取待注册事件
        while ((object = this.register.poll()) != null) {
            try {
                //读取已经准备就绪的通道对象
                channel = (SelectableChannel) object[0];
                //读取操作类型
                ops = (Integer) object[1];
                attachment = object[2];

                //如果通道对象未打开,则返回
                if (!channel.isOpen())
                    continue;

                //开始将该通道注册到对应的io选择器中
                SelectionKey key = channel.register(this.selector, ops, attachment);

                if (attachment instanceof TCPSession) {
                    ((TCPSession) attachment).setKey(key);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // TODO: 17/4/15 by zmyer
    private void dispatchEvent(final SelectionKey key) throws IOException {
        if (key.isConnectable()) {
            //开始处理连接事件
            acceptor.handleConnectEvent(key);
        } else if (key.isAcceptable()) {
            //开始处理接受连接事件
            acceptor.handleAcceptEvent(key);
        } else if (key.isReadable()) {
            //开始处理读取事件
            acceptor.handleReadEvent(key);
        } else if (key.isValid() && key.isWritable()) {
            //开始处理写入事件
            acceptor.handleWriteEvent(key);
        }
    }
}
