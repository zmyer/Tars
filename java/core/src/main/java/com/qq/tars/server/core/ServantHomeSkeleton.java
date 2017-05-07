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

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import com.qq.tars.rpc.protocol.tars.TarsServantRequest;
import com.qq.tars.rpc.protocol.tars.TarsServantResponse;
import com.qq.tars.server.apps.AppContextImpl;

// TODO: 17/4/15 by zmyer
public class ServantHomeSkeleton extends AppService {
    //动用的次数
    private AtomicInteger invokeNumbers = new AtomicInteger(0);
    //名称
    private String name = null;
    //服务对象
    private Object service = null;
    //api类对象
    private Class<?> apiClass = null;
    //最大负载限制
    private int maxLoadLimit = -1;
    //线程池中最小的线程数量
    private int minThreadPoolSize = -1;
    //最大的线程数量
    private int maxThreadPoolSize = -1;
    //队列大小
    private int queueSize = -1;

    // TODO: 17/4/15 by zmyer
    public ServantHomeSkeleton(String name, Object service, Class<?> apiClass, int loadLimit) {
        this.name = name;
        this.service = service;
        this.apiClass = apiClass;
        this.maxLoadLimit = loadLimit;
    }

    // TODO: 17/4/15 by zmyer
    public Object getService() {
        return service;
    }

    // TODO: 17/4/15 by zmyer
    public Object invoke(Method method, Object... args) throws Exception {
        //值对象
        Object value = null;
        //执行上下文对象
        Context<TarsServantRequest, TarsServantResponse> context = null;
        try {
            //读取当前线程的执行上下文对象
            context = ContextManager.getContext();
            //服务调用前准备
            preInvokeCapHomeSkeleton(context);

//            if ("isAlive".equals(methodName) && method == null && (args == null || args.length == 0)) {
//                return Boolean.TRUE;
//            }

//            if (method == null) throw new RuntimeException("The serivce has no method named:" + methodName);

//            if (!enableOverload || methodID == 0) { // 服务器或客户端不支持重载
//                if (!badMethodMap.isEmpty() && badMethodMap.containsKey(method.getName())) {
//                    String x = enableOverload ? "client side's taserver-proxy too old, update it to fix this problem."//
//                    : "only TAS protocol support Method Overload, " + "remove @Service annotation to diable NONE TAS protocols and then Method Overload will be enabled automatically.";
//                    throw new UnsupportedOperationException("Method Overload Not Supported( " + x + "), method=" + methodName);
//                }
//            }
            //开始进行rpc调用
            value = method.invoke(this.service, fixParamValueType(method, args));
        } finally {
            if (!ContextManager.getContext().response().isAsyncMode()) {
                //如果不是异步调用,则调用后处理
                postInvokeCapHomeSkeleton(context);
            }
        }
        return value;
    }

    // TODO: 17/4/15 by zmyer
    private Object[] fixParamValueType(Method method, Object args[]) {
        if (args == null || args.length == 0) return args;
        //读取参数类型列表
        Class<?> parameterTypes[] = method.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length == 0) return args;

        if (args.length != parameterTypes.length) return args;

        for (int i = 0; i < parameterTypes.length; i++) {
            //填充参数集合
            args[i] = fixValueDataType(parameterTypes[i], args[i]);
        }
        //返回参数集合
        return args;
    }

    // TODO: 17/4/15 by zmyer
    private final Object fixValueDataType(Class<?> dataType, Object value) {
        Object dataValue = value;

        if (dataType != null && dataValue != null) {
            if ("short" == dataType.getName()) {
                dataValue = Short.valueOf(dataValue.toString());
            } else if ("byte" == dataType.getName()) {
                dataValue = Byte.valueOf(dataValue.toString());
            } else if (char.class == dataType) {
                dataValue = ((String) value).charAt(0);
            } else if ("float" == dataType.getName()) {
                dataValue = Float.valueOf(dataValue.toString());
            }
        }

        return dataValue;
    }

    // TODO: 17/4/15 by zmyer
    public void preInvokeCapHomeSkeleton(Context<TarsServantRequest, TarsServantResponse> context) {
        if (this.maxLoadLimit == -1) {
            return;
        }
        this.invokeNumbers.incrementAndGet();
        if (this.invokeNumbers.intValue() > this.maxLoadLimit) {
            throw new RuntimeException(this.name + " is overload. limit=" + this.maxLoadLimit);
        }
    }

    // TODO: 17/4/15 by zmyer
    public void postInvokeCapHomeSkeleton(Context<TarsServantRequest, ?> context) {
        if (this.maxLoadLimit == -1) {
            return;
        }
        this.invokeNumbers.decrementAndGet();
    }

    // TODO: 17/4/15 by zmyer
    public Class<?> getApiClass() {
        return this.apiClass;
    }

    // TODO: 17/4/15 by zmyer
    public int getMinThreadPoolSize() {
        return minThreadPoolSize;
    }

    // TODO: 17/4/15 by zmyer
    public int getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    // TODO: 17/4/15 by zmyer
    public int getQueueSize() {
        return queueSize;
    }

    // TODO: 17/4/15 by zmyer
    public void setMinThreadPoolSize(int minThreadPoolSize) {
        this.minThreadPoolSize = minThreadPoolSize;
    }

    // TODO: 17/4/15 by zmyer
    public void setMaxThreadPoolSize(int maxThreadPoolSize) {
        this.maxThreadPoolSize = maxThreadPoolSize;
    }

    // TODO: 17/4/15 by zmyer
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    // TODO: 17/4/15 by zmyer
    public String name() {
        return this.name;
    }

    // TODO: 17/4/15 by zmyer
    private AppContextImpl appContext;

    // TODO: 17/4/15 by zmyer
    public void setAppContext(AppContextImpl context) {
        appContext = context;
    }

    // TODO: 17/4/15 by zmyer
    public AppContextImpl getAppContext() {
        return appContext;
    }
}
