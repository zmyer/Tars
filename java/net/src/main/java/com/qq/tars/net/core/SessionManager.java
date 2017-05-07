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

import com.qq.tars.net.core.nio.SessionManagerImpl;

// TODO: 17/4/15 by zmyer
public abstract class SessionManager {

    // TODO: 17/4/15 by zmyer
    private static SessionManager manager = new SessionManagerImpl();

    // TODO: 17/4/15 by zmyer
    public abstract void registerSession(Session session);

    // TODO: 17/4/15 by zmyer
    public abstract void unregisterSession(Session session);

    // TODO: 17/4/15 by zmyer
    public abstract void addSessionListener(SessionListener listener);

    // TODO: 17/4/15 by zmyer
    public abstract void setTimeout(long timeout);

    // TODO: 17/4/15 by zmyer
    public abstract void setCheckInterval(long interval);

    // TODO: 17/4/15 by zmyer
    public abstract void start();

    // TODO: 17/4/15 by zmyer
    public static SessionManager getSessionManager() {
        return manager;
    }
}
