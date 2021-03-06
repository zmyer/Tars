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

package com.qq.tars.server.ha;

import com.qq.tars.net.core.Session.SessionStatus;
import com.qq.tars.net.core.SessionEvent;
import com.qq.tars.net.core.SessionListener;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: 17/4/15 by zmyer
public class ConnectionSessionListener implements SessionListener {
    //连接统计对象
    private final AtomicInteger connStat = new AtomicInteger(0);
    //最大连接数目
    private final int MaxConnCount;

    // TODO: 17/4/15 by zmyer
    public ConnectionSessionListener(int connCount) {
        MaxConnCount = connCount;
        System.out.println("MaxConnCount=" + MaxConnCount);
    }

    // TODO: 17/4/15 by zmyer
    @Override
    public synchronized void onSessionCreated(SessionEvent se) {
        System.out.println("onSessionCreated: " + connStat.get());

        //如果会话连接数超过了最大连接限制,则直接断开该会话
        if (connStat.get() >= MaxConnCount) {
            try {
                System.out.println("reached the max connection threshold, close the session.");
                se.getSession().close();
            } catch (IOException e) {
            }
            return;
        }

        //递增链接数目
        connStat.incrementAndGet();
    }

    // TODO: 17/4/15 by zmyer
    @Override
    public synchronized void onSessionDestoryed(SessionEvent se) {
        System.out.println("onSessionDestoryed: " + connStat.get());
        if (se.getSession() != null && se.getSession().getStatus() == SessionStatus.SERVER_CONNECTED
            && connStat.get() > 0) {
            //递减链接数目
            connStat.decrementAndGet();
        }
    }
}
