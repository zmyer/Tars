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

package com.qq.tars.server.config;

import com.qq.tars.common.support.Endpoint;
import com.qq.tars.common.util.Config;

// TODO: 17/4/15 by zmyer
public class ServantAdapterConfig {

    private Endpoint endpoint = null;
    private int maxConns = 128;
    private int queueCap = 1024;
    private int queueTimeout = 10000;
    private String servant = null;
    private String protocol = "tars";
    private int threads = 1;

    // TODO: 17/5/22 by zmyer
    public ServantAdapterConfig load(Config conf, String adapterName) {
        String path = "/tars/application/server/" + adapterName;
        endpoint = Endpoint.parseString(conf.get(path + "<endpoint>"));
        protocol = conf.get(path + "<protocol>", "tars");
        maxConns = conf.getInt(path + "<maxconns>", 128);
        queueCap = conf.getInt(path + "<queuecap>", 1024);
        queueTimeout = conf.getInt(path + "<queuetimeout>", 10000);
        servant = conf.get(path + "<servant>");
        threads = conf.getInt(path + "<threads>", 1);
        return this;
    }

    // TODO: 17/5/22 by zmyer
    public Endpoint getEndpoint() {
        return endpoint;
    }

    // TODO: 17/5/22 by zmyer
    public ServantAdapterConfig setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    // TODO: 17/5/22 by zmyer
    public int getMaxConns() {
        return maxConns;
    }

    // TODO: 17/5/22 by zmyer
    public ServantAdapterConfig setMaxConns(int maxConns) {
        this.maxConns = maxConns;
        return this;
    }

    // TODO: 17/5/22 by zmyer
    public int getQueueCap() {
        return queueCap;
    }

    // TODO: 17/5/22 by zmyer
    public ServantAdapterConfig setQueueCap(int queueCap) {
        this.queueCap = queueCap;
        return this;
    }

    // TODO: 17/5/22 by zmyer
    public int getQueueTimeout() {
        return queueTimeout;
    }

    // TODO: 17/5/22 by zmyer
    public ServantAdapterConfig setQueueTimeout(int queueTimeout) {
        this.queueTimeout = queueTimeout;
        return this;
    }

    // TODO: 17/5/22 by zmyer
    public String getServant() {
        return servant;
    }

    // TODO: 17/5/22 by zmyer
    public ServantAdapterConfig setServant(String servant) {
        this.servant = servant;
        return this;
    }

    // TODO: 17/5/22 by zmyer
    public int getThreads() {
        return threads;
    }

    // TODO: 17/5/22 by zmyer
    public ServantAdapterConfig setThreads(int threads) {
        this.threads = threads;
        return this;
    }

    // TODO: 17/5/22 by zmyer
    public String getProtocol() {
        return protocol;
    }

    // TODO: 17/5/22 by zmyer
    public ServantAdapterConfig setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }
}
