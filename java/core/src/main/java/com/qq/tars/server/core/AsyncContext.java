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

import com.qq.tars.protocol.util.TarsHelper;
import com.qq.tars.rpc.protocol.tars.TarsServantRequest;
import com.qq.tars.rpc.protocol.tars.TarsServantResponse;
import com.qq.tars.server.apps.AppContextImpl;
import com.qq.tars.support.log.Logger;
import com.qq.tars.support.log.Logger.LogType;
import java.io.IOException;

// TODO: 17/5/22 by zmyer
public final class AsyncContext {

    private static final AppContainer container = ContainerManager.getContainer(AppContainer.class);

    public static final String PORTAL_CAP_ASYNC_CONTEXT_ATTRIBUTE = "internal.asynccontext";

    private Context<TarsServantRequest, TarsServantResponse> context = null;
    private Logger flowLogger = Logger.getLogger("tarsserver.log", LogType.ALL);

    // TODO: 17/5/22 by zmyer
    public static AsyncContext startAsync() throws IOException {
        Context<TarsServantRequest, TarsServantResponse> context = ContextManager.getContext();
        AsyncContext aContext = new AsyncContext(context);
        context.response().asyncCallStart();
        context.setAttribute(PORTAL_CAP_ASYNC_CONTEXT_ATTRIBUTE, aContext);
        return aContext;
    }

    // TODO: 17/5/22 by zmyer
    private AsyncContext(Context<TarsServantRequest, TarsServantResponse> context) {
        this.context = context;
    }

    // TODO: 17/5/22 by zmyer
    private ServantHomeSkeleton getCapHomeSkeleton() {
        AppContextImpl appContext = container.getDefaultAppContext();
        return appContext.getCapHomeSkeleton(this.context.request().getServantName());
    }

    // TODO: 17/5/22 by zmyer
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return (T) this.context.getAttribute(name);
    }

    // TODO: 17/5/22 by zmyer
    public <T> T getAttribute(String name, T defaultValue) {
        return (T) this.context.getAttribute(name, defaultValue);
    }

    // TODO: 17/5/22 by zmyer
    public <T> void setAttribute(String name, T value) {
        this.context.setAttribute(name, value);
    }

    // TODO: 17/5/22 by zmyer
    public void writeException(Throwable ex) throws IOException {
        TarsServantResponse response = this.context.response();
        response.setRet(TarsHelper.SERVERUNKNOWNERR);
        response.setCause(ex);
        response.setResult(null);
        response.asyncCallEnd();
        //降低流控指标
        getCapHomeSkeleton().postInvokeCapHomeSkeleton(context);
        Long startTime = this.context.getAttribute(Context.INTERNAL_START_TIME);
        TarsServantProcessor.printServiceFlowLog(flowLogger, this.context.request(), response.getRet(), (System.currentTimeMillis() - startTime.longValue()), ex.toString());

    }

    // TODO: 17/5/22 by zmyer
    public void writeResult(Object result) throws IOException {
        TarsServantResponse response = this.context.response();
        response.setRet(TarsHelper.SERVERSUCCESS);
        response.setCause(null);
        response.setResult(result);
        response.asyncCallEnd();

        getCapHomeSkeleton().postInvokeCapHomeSkeleton(context);
        Long startTime = this.context.getAttribute(Context.INTERNAL_START_TIME);
        TarsServantProcessor.printServiceFlowLog(flowLogger, this.context.request(), response.getRet(), (System.currentTimeMillis() - startTime.longValue()), "");
    }
}
