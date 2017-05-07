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
import com.qq.tars.net.client.ticket.TicketManager;
import com.qq.tars.net.core.Request;
import com.qq.tars.net.core.Response;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

// TODO: 17/4/15 by zmyer
public final class WorkThread implements Runnable {
    //请求对象
    private Request req;
    //应答对象
    private Response resp;
    //selector管理器
    private SelectorManager selectorManager = null;

    // TODO: 17/4/15 by zmyer
    public Request getRequest() {
        return req;
    }

    // TODO: 17/4/15 by zmyer
    WorkThread(Response resp, SelectorManager selectorManager) {
        this.resp = resp;
        udpSession = null;
    }

    // TODO: 17/4/15 by zmyer
    WorkThread(Request req, SelectorManager selectorManager) {
        this.req = req;
        udpSession = null;
        this.selectorManager = selectorManager;
    }

    //UDP会话对象
    private final UDPSession udpSession;
    //udp缓冲区
    private ByteBuffer udpBuffer;

    // TODO: 17/4/15 by zmyer
    WorkThread(UDPSession session, ByteBuffer buffer, SelectorManager selectorManager) {
        this.udpSession = session;
        this.udpBuffer = buffer;
        this.selectorManager = selectorManager;
    }

    // TODO: 17/4/15 by zmyer
    private void parseDatagramPacket() throws IOException {
        //解析udp缓冲区
        Object obj = this.udpSession.parseDatagramPacket(udpBuffer);
        if (obj instanceof Request) {
            this.req = (Request) obj;
        } else if (obj instanceof Response) {
            this.resp = (Response) obj;
        }
    }

    // TODO: 17/4/15 by zmyer
    public void run() {
        try {
            if (udpSession != null) {
                //如果udp会话不为空,则开始解析数据包
                parseDatagramPacket();
                //重置udp缓冲区
                udpBuffer = null;
            }

            if (req != null) {
                //设置处理请求开始时间戳
                req.setProcessTime(System.currentTimeMillis());
                //初始化请求对象
                req.init();
                //开始在proessor处理
                Response res = selectorManager.getProcessor().process(req, req.getIoSession());
                if (!res.isAsyncMode())
                    //如果非异步模式,则需要将结果及时返回
                    req.getIoSession().write(res);
            } else if (resp != null) {
                //初始化应答消息
                resp.init();
                //创建Ticket对象
                Ticket<Response> ticket = TicketManager.getTicket(resp.getTicketNumber());
                if (ticket == null) {
                    String s = "failed to fetch request for this response. [from:" + resp.getSession().getRemoteIp() + ":" + resp.getSession().getRemotePort() + "]";
                    System.out.println(s);
                    return;
                }
                //开始填充执行上下文对象
                fillDitributedContext(ticket.request().getDistributedContext());
                //设置ticket对象的英大消息
                ticket.notifyResponse(resp);
                ticket.countDown();
                //从ticket管理器中删除该ticket对象
                TicketManager.removeTicket(ticket.getTicketNumber());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            clearDistributedContext();
        }
    }

    // TODO: 17/4/15 by zmyer
    private void fillDitributedContext(Map<String, String> data) {
    }

    // TODO: 17/4/15 by zmyer
    private void clearDistributedContext() {
    }
}
