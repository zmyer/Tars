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

package com.qq.tars.net.core;

import com.qq.tars.net.client.ticket.Ticket;
import com.qq.tars.net.protocol.ProtocolException;
import java.io.IOException;

// TODO: 17/4/15 by zmyer
public abstract class Response {

    private int ticketNum = Ticket.DEFAULT_TICKET_NUMBER;

    protected transient Session session = null;

    protected transient boolean asyncMode = false;

    private volatile boolean commited = false;

    public Response(Session session) {
        this.session = session;
    }

    public void init() throws ProtocolException {
    }

    public Session getSession() {
        return this.session;
    }

    public boolean isAsyncMode() {
        return asyncMode;
    }

    public void asyncCallStart() throws IOException {
        this.asyncMode = true;
    }

    // TODO: 17/5/22 by zmyer
    public void asyncCallEnd() throws IOException {
        //如果是非异步模式,则直接异常
        if (!this.asyncMode)
            throw new IllegalStateException("The response is not async mode.");
        ensureNotCommitted();
        //返回结果
        session.write(this);
    }

    public void setTicketNumber(int ticketNum) {
        this.ticketNum = ticketNum;
    }

    public int getTicketNumber() {
        return this.ticketNum;
    }

    // TODO: 17/5/22 by zmyer
    private synchronized void ensureNotCommitted() {
        if (commited)
            throw new IllegalStateException("Not allowed after response has committed.");
        this.commited = true;
    }
}
