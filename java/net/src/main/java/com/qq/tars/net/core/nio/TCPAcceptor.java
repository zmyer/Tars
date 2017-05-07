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

import com.qq.tars.net.core.Session.SessionStatus;
import com.qq.tars.net.core.SessionManager;
import com.qq.tars.net.util.Utils;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

// TODO: 17/4/13 by zmyer
public class TCPAcceptor extends Acceptor {

    // TODO: 17/4/13 by zmyer
    public TCPAcceptor(SelectorManager selectorManager) {
        super(selectorManager);
    }

    // TODO: 17/4/13 by zmyer
    public void handleConnectEvent(SelectionKey key) throws IOException {
        //1. Get the client channel
        //从选择键值中读取对应的通道对象
        SocketChannel client = (SocketChannel) key.channel();

        //2. Set the session status
        //读取tcp会话对象
        TCPSession session = (TCPSession) key.attachment();
        if (session == null)
            throw new RuntimeException("The session is null when connecting to ...");

        //3. Connect to server
        try {
            //连接完毕
            client.finishConnect();
            //设置选择键值的感兴趣的事件类型
            key.interestOps(SelectionKey.OP_READ);
            //设置会话状态为connected
            session.setStatus(SessionStatus.CLIENT_CONNECTED);
        } finally {
            //会话连接建立完成
            session.finishConnect();
        }
    }

    // TODO: 17/4/13 by zmyer
    public void handleAcceptEvent(SelectionKey key) throws IOException {
        //1. Accept TCP request
        //首先读取服务器套接字对象
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        //接收客户端连接请求
        SocketChannel channel = server.accept();
        //设置通道对象对应的套接字为非延时
        channel.socket().setTcpNoDelay(selectorManager.isTcpNoDelay());
        //设置通道非阻塞
        channel.configureBlocking(false);
        //设置qos标记
        Utils.setQosFlag(channel.socket());

        //2. Create NioSession for each TCP connection
        //首先创建tcp会话对象
        TCPSession session = new TCPSession(selectorManager);
        //设置会话通道对象
        session.setChannel(channel);
        //设置会话对象的状态为server_connected
        session.setStatus(SessionStatus.SERVER_CONNECTED);
        //设置会话对象的存活标记
        session.setKeepAlive(selectorManager.isKeepAlive());
        //设置会话对象的非延时标记
        session.setTcpNoDelay(selectorManager.isTcpNoDelay());

        //3. Register session
        //将创建的会话对象注册到会话管理器中
        SessionManager.getSessionManager().registerSession(session);

        //4. Register channel with the specified session
        //将该会话对象对应的通道注册到reactor对象中
        selectorManager.nextReactor().registerChannel(channel, SelectionKey.OP_READ, session);
    }

    // TODO: 17/4/13 by zmyer
    public void handleReadEvent(SelectionKey key) throws IOException {
        //读取tcp会话对象
        TCPSession session = (TCPSession) key.attachment();
        if (session == null) {
            throw new RuntimeException("The session is null when reading data...");
        }
        //开启会话对象读流程
        session.read();
    }

    // TODO: 17/4/13 by zmyer
    public void handleWriteEvent(SelectionKey key) throws IOException {
        //读取tcp会话对象
        TCPSession session = (TCPSession) key.attachment();
        if (session == null) {
            throw new RuntimeException("The session is null when writing data...");
        }
        //开启会话对象写流程
        session.doWrite();
    }
}
