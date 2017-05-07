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
import com.qq.tars.net.protocol.ProtocolEncoder;
import com.qq.tars.net.protocol.ProtocolException;
import com.qq.tars.net.protocol.ProtocolFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;

// TODO: 17/4/18 by zmyer
public class UDPSession extends Session {
    //缓冲区大小
    private int bufferSize = 1024 * 4;
    //selector管理器
    private SelectorManager selectorManager = null;
    //通道对象
    private SelectableChannel channel = null;
    //套接字地址
    private SocketAddress target = null;

    // TODO: 17/4/18 by zmyer
    public UDPSession(SelectorManager selectorManager) {
        this.selectorManager = selectorManager;
    }

    // TODO: 17/4/18 by zmyer
    public SelectableChannel getChannel() {
        return channel;
    }

    // TODO: 17/4/18 by zmyer
    public void setBufferSize(int size) {
        if (size <= 0 || size > 1024 * 64)
            return;
        this.bufferSize = size;
    }

    // TODO: 17/4/18 by zmyer
    public void setChannel(SelectableChannel channel) {
        this.channel = channel;
    }

    // TODO: 17/4/18 by zmyer
    @Override
    protected void accept() throws IOException {
        throw new IllegalStateException("Can't handle accept method.");
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public void asyncClose() throws IOException {
        close();
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public void close() throws IOException {
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public String getRemoteIp() {
        if (this.target != null) {
            return ((InetSocketAddress) target).getAddress().getHostAddress();
        }

        return null;
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public int getRemotePort() {
        if (this.channel != null) {
            return ((InetSocketAddress) target).getPort();
        }
        return 0;
    }

    // TODO: 17/4/18 by zmyer
    public ProtocolFactory getProtocolFactory() {
        return this.selectorManager.getProtocolFactory();
    }

    // TODO: 17/4/18 by zmyer
    public final void setTarget(SocketAddress address) {
        if (address != null)
            this.target = address;
    }

    // TODO: 17/4/18 by zmyer
    @Override
    protected void read() throws IOException {
        ByteBuffer buffer = doRead();
        selectorManager.getThreadPool().execute(new WorkThread(this, buffer, selectorManager));
    }

    // TODO: 17/4/18 by zmyer
    public void write(Request request) throws IOException {
        try {
            ProtocolFactory factory = selectorManager.getProtocolFactory();
            ProtocolEncoder encoder = factory.getEncoder();
            IoBuffer buffer = encoder.encodeRequest(request, this);
            int size = ((DatagramChannel) this.channel).send(buffer.buf(), target);
            if (size <= 0)
                throw new IOException("failed to send data. {target=" + target + "}");

        } catch (ProtocolException ex) {
            throw new IOException("protocol error:", ex);
        }
    }

    // TODO: 17/4/18 by zmyer
    @Override
    public void write(Response response) throws IOException {
        try {
            IoBuffer buffer = selectorManager.getProtocolFactory().getEncoder().encodeResponse(response, this);
            int size = ((DatagramChannel) this.channel).send(buffer.buf(), target);
            if (size <= 0)
                throw new IOException("failed to send data. {target=" + target + "}");
        } catch (ProtocolException ex) {
            throw new IOException("protocol error:", ex);
        }
    }

    // TODO: 17/4/18 by zmyer
    public ByteBuffer doRead() throws IOException {
        ByteBuffer data = ByteBuffer.allocate(bufferSize);
        SocketAddress address = ((DatagramChannel) this.channel).receive(data);
        data.flip();
        if (data.remaining() >= bufferSize) {
            throw new IOException("package size CAN NOT >= " + bufferSize);
        }
        if (this.target == null)
            setTarget(address);
        return data;
    }

    // TODO: 17/4/18 by zmyer
    Object parseDatagramPacket(ByteBuffer data) throws IOException {
        try {
            if (this.status == SessionStatus.CLIENT_CONNECTED) {
                return readResponse(data);
            } else if (this.status == SessionStatus.SERVER_CONNECTED) {
                return readRequest(data);
            } else {
                throw new IllegalStateException("The current session status is invalid. [status:" + this.status + "]");
            }
        } catch (ProtocolException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // TODO: 17/4/18 by zmyer
    private Response readResponse(ByteBuffer data) throws IOException, ProtocolException {
        Response response = selectorManager.getProtocolFactory().getDecoder().decodeResponse(IoBuffer.wrap(data), this);

        if (response != null) {
            if (response.getTicketNumber() == Ticket.DEFAULT_TICKET_NUMBER)
                response.setTicketNumber(response.getSession().hashCode());
        }

        return response;
    }

    // TODO: 17/4/18 by zmyer
    private Request readRequest(ByteBuffer data) throws IOException, ProtocolException {
        Request request = selectorManager.getProtocolFactory().getDecoder().decodeRequest(IoBuffer.wrap(data), this);
        if (request == null)
            throw new ProtocolException("failed to decode udp packet.");
        request.resetBornTime();
        return request;
    }
}
