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

package com.qq.tars.server.core;

import com.qq.tars.common.util.concurrent.TaskQueue;
import com.qq.tars.common.util.concurrent.TaskThreadFactory;
import com.qq.tars.common.util.concurrent.TaskThreadPoolExecutor;
import com.qq.tars.net.core.Request;
import com.qq.tars.net.core.nio.WorkThread;
import com.qq.tars.rpc.protocol.tars.TarsServantRequest;
import com.qq.tars.server.apps.AppContextImpl;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

// TODO: 17/4/15 by zmyer
public class ServerThreadPoolDispatcher implements Executor {
    //线程执行对象映射表
    private final static HashMap<String, Executor> threadExecutors = new HashMap<String, Executor>();

    // TODO: 17/4/15 by zmyer
    public void execute(Runnable command) {
        getExecutor(command).execute(command);
    }

    // TODO: 17/4/15 by zmyer
    private static Executor getExecutor(Runnable command) {
        //从指令中读取请求对象
        TarsServantRequest request = getPortalServiceRequest(command);

        if (request == null)
            //创建默认的执行对象
            return getDefaultExecutor();

        //根据请求对象,读取执行器
        return getExecutor(request);
    }

    // TODO: 17/4/15 by zmyer
    private static Executor getDefaultExecutor() {
        //
        Executor executor = threadExecutors.get(null);

        if (executor == null) {
            executor = createExecutor(null);
        }

        return executor;
    }

    // TODO: 17/4/15 by zmyer
    private static Executor getExecutor(TarsServantRequest request) {
        String service = request.getServantName();

        Executor executor = threadExecutors.get(service);

        if (executor == null) {
            executor = createExecutor(request);
        }

        return executor;
    }

    // TODO: 17/4/15 by zmyer
    private static synchronized Executor createDefaultExecutor(String key) {
        //读取执行器对象
        Executor executor = threadExecutors.get(null);

        if (executor != null) {
            //将执行器插入到映射表中
            threadExecutors.put(key, executor);
            return executor;
        }
        //创建任务队列对象
        TaskQueue taskqueue = new TaskQueue(20000);
        //创建任务线程池执行对象
        TaskThreadPoolExecutor pool = new TaskThreadPoolExecutor(5, 512, 120, TimeUnit.SECONDS, taskqueue,
            new TaskThreadFactory("taserverThreadPool-exec-"));
        //设置任务队列所属的线程池对象
        taskqueue.setParent(pool);
        //向线程池执行器映射表中注册pool
        threadExecutors.put(null, pool);
        threadExecutors.put(key, pool);
        //返回线程池对象
        return pool;
    }

    // TODO: 17/4/15 by zmyer
    private static synchronized Executor createExecutor(TarsServantRequest request) {
        String key = null, contextName = null, serviceName = null;
        Executor executor = null;

        if (request == null)
            return createDefaultExecutor(null);

//        contextName = request.getWebappContext();
        serviceName = request.getServantName();
        key = contextName + '_' + serviceName;

        executor = threadExecutors.get(key);
        if (executor != null)
            return executor;

        int minPoolSize = -1, maxPoolSize = -1, queueSize = -1;
        AppContainer container = ContainerManager.getContainer(AppContainer.class);
        AppContextImpl context = container.getDefaultAppContext();

        // 客户端请求错误的app或service时，返回默认的executor.
        if (context == null) {
            return getDefaultExecutor();
        }
        ServantHomeSkeleton service = context.getCapHomeSkeleton(serviceName);
        if (service == null) {
            return getDefaultExecutor();
        }

        minPoolSize = service.getMinThreadPoolSize();
        maxPoolSize = service.getMaxThreadPoolSize();
        queueSize = service.getQueueSize();

        if (minPoolSize < 0 || maxPoolSize < 0 || queueSize < 0) {
            return createDefaultExecutor(key);
        }

        // ----------------------------------
        TaskQueue taskqueue = new TaskQueue(queueSize);
        TaskThreadPoolExecutor pool = new TaskThreadPoolExecutor(minPoolSize, maxPoolSize, 120, TimeUnit.SECONDS, taskqueue, new TaskThreadFactory("taserverThreadPool-exec-" + contextName + '.' + serviceName + '-'));
        taskqueue.setParent(pool);
        threadExecutors.put(key, pool);

        return pool;
    }

    // TODO: 17/4/15 by zmyer
    private static TarsServantRequest getPortalServiceRequest(Runnable command) {
        if (!(command instanceof WorkThread))
            return null;

        WorkThread workThread = (WorkThread) command;
        Request req = workThread.getRequest();

        if (!(req instanceof TarsServantRequest))
            return null;

        return (TarsServantRequest) req;
    }
}
