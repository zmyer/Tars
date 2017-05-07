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

import com.qq.tars.net.client.ticket.Ticket;
import com.qq.tars.net.core.IoBuffer;
import com.qq.tars.net.core.Request;
import com.qq.tars.net.core.Response;
import com.qq.tars.net.core.Session;
import com.qq.tars.net.core.SessionManager;
import com.qq.tars.net.protocol.ProtocolException;
import com.qq.tars.net.protocol.ProtocolFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: 17/4/15 by zmyer
public class TCPSession extends Session {
    //select键值
    private SelectionKey key = null;
    //通道对象
    private SelectableChannel channel = null;
    //selector管理器
    private SelectorManager selectorManager = null;
    //缓冲区大小
    private int bufferSize = 1024 * 4;
    //读取缓冲区对象
    private IoBuffer readBuffer = null;
    //非延时标记
    private boolean tcpNoDelay = false;
    //缓冲区队列
    private Queue<ByteBuffer> queue = new LinkedBlockingQueue<ByteBuffer>(1024 * 8);
    //hashCode生成器
    private static final AtomicInteger hashCodeGenerator = new AtomicInteger();
    //哈希code
    private int hashCode = 0;

    // TODO: 17/4/15 by zmyer
    public TCPSession(SelectorManager selectorManager) {
        this.selectorManager = selectorManager;
        this.hashCode = hashCodeGenerator.incrementAndGet();
    }

    // TODO: 17/4/15 by zmyer
    public void asyncClose() throws IOException {
        if (this.key == null)
            return; //Already closed if the key is null.

        //根据键值读取对应的selector对象
        Reactor reactor = selectorManager.getReactor(this.key);

        if (reactor != null) {
            //将该对象从reactor中注销
            reactor.unRegisterChannel(this);
        } else {
            throw new IOException("failed to find the selector for this session.");
        }
    }

    // TODO: 17/4/15 by zmyer
    public void close() throws IOException {
        if (this.status == SessionStatus.CLOSED)
            return;

        this.status = SessionStatus.CLOSED;
        if (channel != null)
            channel.close();
        if (key != null)
            key.cancel();

        this.key = null;
        this.channel = null;

        SessionManager.getSessionManager().unregisterSession(this);
    }

    // TODO: 17/4/13 by zmyer
    protected void read() throws IOException {
        //首先从channel中读取数据
        int ret = readChannel();

        if (this.status == SessionStatus.CLIENT_CONNECTED) {
            //如果是客户端连接,则开始处理应答消息
            readResponse();
        } else if (this.status == SessionStatus.SERVER_CONNECTED) {
            //如果是服务器连接,则开始处理请求消息
            readRequest();
        } else {
            throw new IllegalStateException("The current session status is invalid. [status:" + this.status + "]");
        }

        if (ret < 0) {
            //开始关闭会话
            close();
            return;
        }
    }

    // TODO: 17/4/15 by zmyer
    public void readRequest() throws IOException {
        Request request;
        IoBuffer tempBuffer;

        try {
            //临死缓冲区对象
            tempBuffer = readBuffer.duplicate().flip();

            while (true) {
                tempBuffer.mark();

                if (tempBuffer.remaining() > 0) {
                    //开始解码缓冲区
                    request = selectorManager.getProtocolFactory().getDecoder().decodeRequest(tempBuffer, this);
                } else {
                    request = null;
                }

                if (request != null) {
                    try {
                        //重置请求消息生成时间戳
                        request.resetBornTime();
                        //开始在另外的线程对象中处理请求对象
                        selectorManager.getThreadPool().execute(new WorkThread(request, selectorManager));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    //重置临时缓冲区
                    tempBuffer.reset();
                    //重置读取缓冲区
                    readBuffer = resetIoBuffer(tempBuffer);
                    break;
                }
            }
        } catch (ProtocolException ex) {
            close();
            ex.printStackTrace();
        }
    }

    // TODO: 17/4/15 by zmyer
    public void readResponse() throws IOException {
        Response response;
        IoBuffer tempBuffer;

        try {
            tempBuffer = readBuffer.duplicate().flip();

            while (true) {
                tempBuffer.mark();

                if (tempBuffer.remaining() > 0) {
                    //解码应答消息
                    response = selectorManager.getProtocolFactory().getDecoder().decodeResponse(tempBuffer, this);
                } else {
                    response = null;
                }

                if (response != null) {
                    try {
                        if (response.getTicketNumber() == Ticket.DEFAULT_TICKET_NUMBER)
                            response.setTicketNumber(response.getSession().hashCode());
                        //开始在另外的线程对象中处理应答消息
                        selectorManager.getThreadPool().execute(new WorkThread(response, selectorManager));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    //重置临时缓冲区
                    tempBuffer.reset();
                    //重置读取缓冲区
                    readBuffer = resetIoBuffer(tempBuffer);
                    break;
                }
            }

        } catch (ProtocolException ex) {
            close();
            ex.printStackTrace();
        }
    }

    // TODO: 17/4/15 by zmyer
    public void write(Request request) throws IOException {
        try {
            //编码请求对象
            IoBuffer buffer = selectorManager.getProtocolFactory().getEncoder().encodeRequest(request, this);
            //写入请求对象
            write(buffer);
        } catch (ProtocolException ex) {
            throw new IOException("protocol error:", ex);
        }
    }

    // TODO: 17/4/15 by zmyer
    public void write(Response response) throws IOException {
        try {
            //编码应答消息
            IoBuffer buffer = selectorManager.getProtocolFactory().getEncoder().encodeResponse(response, this);
            //写入应答消息
            write(buffer);
        } catch (ProtocolException ex) {
            throw new IOException("protocol error:", ex);
        }
    }

    // TODO: 17/4/15 by zmyer
    public String getRemoteIp() {
        if (this.status != SessionStatus.CLOSED) {
            //读取主机地址
            return ((SocketChannel) this.channel).socket().getInetAddress().getHostAddress();
        }

        return null;
    }

    // TODO: 17/4/15 by zmyer
    public int getRemotePort() {
        if (this.status != SessionStatus.CLOSED) {
            //读取主机端口号
            return ((SocketChannel) this.channel).socket().getPort();
        }

        return -1;
    }

    // TODO: 17/4/15 by zmyer
    public void accept() throws IOException {
    }

    // TODO: 17/4/15 by zmyer
    protected void write(IoBuffer buffer) throws IOException {
        if (buffer == null)
            return;

        if (channel == null || key == null)
            throw new IOException("Connection is closed");

        //将该缓冲区插入到队列中
        if (!this.queue.offer(buffer.buf())) {
            throw new IOException("The session queue is full. [ queue size:" + queue.size() + " ]");
        }

        if (key != null) {
            //设置该会话对应的selector键值,并修改键值对应的写事件
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            //立即唤醒selector对象
            key.selector().wakeup();
        }
    }

    // TODO: 17/4/15 by zmyer
    protected synchronized int doWrite() throws IOException {
        int writeBytes = 0;

        while (true) {
            //从缓冲区队列中读取待写出的缓冲区对象
            ByteBuffer wBuf = queue.peek();

            if (wBuf == null) {
                //如果缓冲区中没有待写出的缓冲区,则将selector键值对应的事件改为READ
                key.interestOps(SelectionKey.OP_READ);

                if (queue.peek() != null) {
                    //如果队列中存在缓冲区,则将selector键值改为READ | WRITE
                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                }

                //开始唤醒selector对象
                key.selector().wakeup();
                break;
            }

            //从通道对象中写出缓冲区
            int bytesWritten = ((SocketChannel) channel).write(wBuf);

            if (bytesWritten == 0 && wBuf.remaining() > 0) // Socket buffer is full.
            {
                //如果队列中存在缓冲区,则将selector键值改为READ | WRITE
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                //开始唤醒selector对象
                key.selector().wakeup();
                break;
            }

            if (wBuf.remaining() == 0) {
                //登记写入缓冲区数量
                writeBytes++;
                //从队列中删除缓冲区对象
                queue.remove();
                continue;
            } else {
                return -1;
            }
        }

        //如果会话对象不是存活状态,则关闭会话
        if (!isKeepAlive())
            close();

        //返回结果
        return writeBytes;
    }

    // TODO: 17/4/15 by zmyer
    protected IoBuffer resetIoBuffer(IoBuffer buffer) {
        IoBuffer newBuffer = null;

        if (buffer != null && buffer.remaining() > 0) {
            int len = buffer.remaining();
            byte[] bb = new byte[len];
            buffer.get(bb);
            newBuffer = IoBuffer.wrap(bb);
            newBuffer.position(len);
        }

        return newBuffer;
    }

    // TODO: 17/4/15 by zmyer
    public SelectionKey getKey() {
        return key;
    }

    // TODO: 17/4/15 by zmyer
    public void setKey(SelectionKey key) {
        this.key = key;
    }

    // TODO: 17/4/15 by zmyer
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    // TODO: 17/4/15 by zmyer
    public void setTcpNoDelay(boolean on) {
        this.tcpNoDelay = on;
    }

    // TODO: 17/4/15 by zmyer
    public SelectableChannel getChannel() {
        return channel;
    }

    // TODO: 17/4/15 by zmyer
    public void setChannel(SelectableChannel channel) {
        this.channel = channel;
    }

    // TODO: 17/4/15 by zmyer
    public ProtocolFactory getProtocolFactory() {
        return this.selectorManager.getProtocolFactory();
    }

    // TODO: 17/4/13 by zmyer
    private int readChannel() throws IOException {
        int readBytes = 0, ret = 0;

        //分配缓冲区对象
        ByteBuffer data = ByteBuffer.allocate(1024 * 2);

        if (readBuffer == null) {
            //分配读取缓冲区
            readBuffer = IoBuffer.allocate(bufferSize);
        }

        //开始从通道中读取缓冲区
        while ((ret = ((SocketChannel) channel).read(data)) > 0) {
            data.flip();
            readBytes += data.remaining();
            readBuffer.put(data.array(), data.position(), data.remaining());
            data.clear();
        }

        return ret < 0 ? ret : readBytes;
    }

    // TODO: 17/4/15 by zmyer
    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
