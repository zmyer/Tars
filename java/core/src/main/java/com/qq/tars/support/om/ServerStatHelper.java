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

package com.qq.tars.support.om;

import com.qq.tars.client.Communicator;
import com.qq.tars.server.config.ConfigurationManager;
import com.qq.tars.server.config.ServantAdapterConfig;
import com.qq.tars.server.config.ServerConfig;
import com.qq.tars.support.stat.InvokeStatHelper;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: 17/5/22 by zmyer
public class ServerStatHelper {

    private static final ServerStatHelper instance = new ServerStatHelper();
    private final AtomicBoolean inited = new AtomicBoolean(false);

    private volatile Communicator communicator;

    private ServerStatHelper() {
    }

    // TODO: 17/5/22 by zmyer
    public static ServerStatHelper getInstance() {
        return instance;
    }

    // TODO: 17/5/22 by zmyer
    public void init(Communicator communicator) {
        if (inited.compareAndSet(false, true)) {
            this.communicator = communicator;
        }
    }

    // TODO: 17/5/22 by zmyer
    public void report() {
        try {
            //服务器配置
            ServerConfig serverConfig = ConfigurationManager.getInstance().getserverConfig();
            //服务适配配置
            LinkedHashMap<String, ServantAdapterConfig> adapterMap =
                serverConfig.getServantAdapterConfMap();
            if (adapterMap == null || adapterMap.isEmpty()) {
                return;
            }

            for (java.util.Map.Entry<String, ServantAdapterConfig> adapterEntry : adapterMap.entrySet()) {
                if (OmConstants.AdminServant.equals(adapterEntry.getKey())) {
                    continue;
                }

                //读取服务适配配置信息
                ServantAdapterConfig servantCfg = adapterEntry.getValue();
                //开始发送相关统计信息
                communicator.getStatHelper().report(InvokeStatHelper.getInstance().getProxyStat(servantCfg.getServant()));
            }
        } catch (Exception e) {
            OmLogger.record("ServerStatHelper|ReportThread error", e);
        }
    }
}
