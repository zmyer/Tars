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
import com.qq.tars.client.CommunicatorFactory;
import com.qq.tars.server.ServerVersion;
import com.qq.tars.server.config.ConfigurationManager;
import com.qq.tars.support.config.ConfigHelper;
import com.qq.tars.support.node.NodeHelper;
import com.qq.tars.support.notify.NotifyHelper;
import com.qq.tars.support.property.CommonPropertyPolicy;
import com.qq.tars.support.property.JvmPropertyPolicy.GCNumCount;
import com.qq.tars.support.property.JvmPropertyPolicy.GCTimeSum;
import com.qq.tars.support.property.JvmPropertyPolicy.MemoryHeapCommittedAvg;
import com.qq.tars.support.property.JvmPropertyPolicy.MemoryHeapMaxAvg;
import com.qq.tars.support.property.JvmPropertyPolicy.MemoryHeapUsedAvg;
import com.qq.tars.support.property.JvmPropertyPolicy.ThreadNumAvg;
import com.qq.tars.support.property.PropertyReportHelper;
import com.qq.tars.support.property.PropertyReportHelper.Policy;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

// TODO: 17/4/15 by zmyer
public class OmServiceMngr {
    //单例对象
    private static final OmServiceMngr Instance = new OmServiceMngr();

    // TODO: 17/4/15 by zmyer
    public static OmServiceMngr getInstance() {
        return Instance;
    }

    // TODO: 17/4/15 by zmyer
    public void initAndStartOmService() {
        //获取通信对象
        Communicator communicator = CommunicatorFactory.getInstance().getCommunicator();
        //获取应用名称
        String app = ConfigurationManager.getInstance().getserverConfig().getApplication();
        //获取服务器名称
        String serverName = ConfigurationManager.getInstance().getserverConfig().getServerName();
        //获取服务器根目录
        String basePath = ConfigurationManager.getInstance().getserverConfig().getBasePath();
        //获取模块名称
        String modualName = ConfigurationManager.getInstance().getserverConfig()
            .getCommunicatorConfig().getModuleName();
        //设置配置信息
        ConfigHelper.getInstance().setConfigInfo(communicator, app, serverName, basePath);
        //设置节点信息
        NodeHelper.getInstance().setNodeInfo(communicator, app, serverName);
        //设置公告信息
        NotifyHelper.getInstance().setNotifyInfo(communicator, app, serverName);
        //设置通信对象模块名称
        PropertyReportHelper.getInstance().setPropertyInfo(communicator, modualName);
        //通告版本
        NodeHelper.getInstance().reportVersion(ServerVersion.getVersion());

        Policy avgPolicy = new CommonPropertyPolicy.Avg();
        Policy maxPolicy = new CommonPropertyPolicy.Max();
        PropertyReportHelper.getInstance().createPropertyReporter(OmConstants.PropWaitTime, avgPolicy, maxPolicy);

        PropertyReportHelper.getInstance().createPropertyReporter(OmConstants.PropHeapUsed, new MemoryHeapUsedAvg());
        PropertyReportHelper.getInstance().createPropertyReporter(OmConstants.PropHeapCommitted, new MemoryHeapCommittedAvg());
        PropertyReportHelper.getInstance().createPropertyReporter(OmConstants.PropHeapMax, new MemoryHeapMaxAvg());
        PropertyReportHelper.getInstance().createPropertyReporter(OmConstants.PropThreadCount, new ThreadNumAvg());
        for (GarbageCollectorMXBean gcMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            PropertyReportHelper.getInstance().createPropertyReporter(OmConstants.PropGcCount + gcMXBean.getName(), new GCNumCount(gcMXBean.getName()));
            PropertyReportHelper.getInstance().createPropertyReporter(OmConstants.PropGcTime + gcMXBean.getName(), new GCTimeSum(gcMXBean.getName()));
        }
        //初始化统计对象
        ServerStatHelper.getInstance().init(communicator);
        //启动调度服务
        ScheduledServiceMngr.getInstance().start();
    }

    // TODO: 17/ 4/15 by zmyer
    public void reportWaitingTimeProperty(int value) {
        PropertyReportHelper.getInstance().reportPropertyValue(OmConstants.PropWaitTime, value);
    }
}
