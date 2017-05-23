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

import com.qq.tars.common.support.Endpoint;
import com.qq.tars.common.util.Constants;
import com.qq.tars.net.core.Processor;
import com.qq.tars.net.core.Request;
import com.qq.tars.net.core.Response;
import com.qq.tars.net.core.Session;
import com.qq.tars.protocol.util.TarsHelper;
import com.qq.tars.rpc.exc.TarsException;
import com.qq.tars.rpc.protocol.tars.TarsServantRequest;
import com.qq.tars.rpc.protocol.tars.TarsServantResponse;
import com.qq.tars.server.apps.AppContextImpl;
import com.qq.tars.server.config.ConfigurationManager;
import com.qq.tars.server.config.ServantAdapterConfig;
import com.qq.tars.server.config.ServerConfig;
import com.qq.tars.support.log.Logger;
import com.qq.tars.support.log.Logger.LogType;
import com.qq.tars.support.om.OmServiceMngr;
import com.qq.tars.support.stat.InvokeStatHelper;
import java.util.Random;

// TODO: 17/4/15 by zmyer
public class TarsServantProcessor extends Processor {

    private Logger flowLogger = Logger.getLogger("tarsserver.log", LogType.LOCAL);

    private static final String FLOW_SEP_FLAG = "|";

    private static final Random rand = new Random(System.currentTimeMillis());

    // TODO: 17/4/15 by zmyer
    @Override
    public Response process(Request req, Session session) {
        //容器对象
        AppContainer container;
        //请求对象
        TarsServantRequest request = null;
        //应答对象
        TarsServantResponse response = null;
        //rpc对象
        ServantHomeSkeleton skeleton;
        //值对象
        Object value;
        //app上下文对象
        AppContextImpl appContext;
        //类加载器对象
        ClassLoader oldClassLoader = null;
        //等待时间
        int waitingTime = -1;
        //开始时间
        long startTime = req.getProcessTime();
        String remark = "";

        try {
            //类加载器
            oldClassLoader = Thread.currentThread().getContextClassLoader();
            request = (TarsServantRequest) req;
            //创建应答的消息
            response = createResponse(request, session);
            //设置应答消息的ticket编号
            response.setTicketNumber(req.getTicketNumber());

            if (response.getRet() != TarsHelper.SERVERSUCCESS
                || TarsHelper.isPing(request.getFunctionName())) {
                return response;
            }
            //最大的等待时间
            int maxWaitingTimeInQueue = ConfigurationManager.getInstance()
                .getserverConfig().getServantAdapterConfMap()
                .get(request.getServantName()).getQueueTimeout();
            //计算请求在队列里面等待的时间
            waitingTime = (int) (startTime - req.getBornTime());
            //如果超时了,则直接抛出异常
            if (waitingTime > maxWaitingTimeInQueue) {
                throw new TarsException("Wait too long, server busy.");
            }

            //创建容器对象
            container = ContainerManager.getContainer(AppContainer.class);
            //创建执行上下文对象
            Context<?, ?> context = ContextManager.registerContext(request, response);
            context.setAttribute(Context.INTERNAL_START_TIME, startTime);
            context.setAttribute(Context.INTERNAL_CLIENT_IP, session.getRemoteIp());
            context.setAttribute(Context.INTERNAL_APP_NAME, container.getDefaultAppContext().name());
            context.setAttribute(Context.INTERNAL_SERVICE_NAME, request.getServantName());
            context.setAttribute(Context.INTERNAL_METHOD_NAME, request.getFunctionName());
            context.setAttribute(Context.INTERNAL_SESSION_DATA, session);

            //读取容器执行上下文对象
            appContext = container.getDefaultAppContext();
            if (appContext == null)
                throw new RuntimeException("failed to find the application named:[ROOT]");

            //设置类加载器对象
            Thread.currentThread().setContextClassLoader(appContext.getAppContextClassLoader());

            //从上下文中读取rpc对象
            skeleton = appContext.getCapHomeSkeleton(request.getServantName());
            if (skeleton == null)
                throw new RuntimeException("failed to find the service named[" + request.getServantName() + "]");

            //开始执行具体的请求对象
            value = skeleton.invoke(request.getMethodInfo().getMethod(), request.getMethodParameters());
            //设置应答结果
            response.setResult(value);
        } catch (Throwable cause) {
            System.out.println("ERROR: " + cause.getMessage());

            //异步模式
            if (response.isAsyncMode()) {
                try {
                    //读取上下文对象
                    Context<TarsServantRequest, TarsServantResponse> context =
                        ContextManager.getContext();
                    //读取异步上下文对象
                    AsyncContext aContext =
                        context.getAttribute(AsyncContext.PORTAL_CAP_ASYNC_CONTEXT_ATTRIBUTE);
                    if (aContext != null)
                        //开始写入异常对象
                        aContext.writeException(cause);
                } catch (Exception ex) {
                    System.out.println("ERROR: " + ex.getMessage());
                }
            } else {
                //直接填充应答消息
                response.setResult(null);
                response.setCause(cause);
                response.setRet(TarsHelper.SERVERUNKNOWNERR);
                remark = cause.toString();
            }
        } finally {
            //还原之前的类加载器
            if (oldClassLoader != null) {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            //及时释放掉本次的调用上下文对象
            ContextManager.releaseContext();
            if (!response.isAsyncMode()) {
                printServiceFlowLog(flowLogger, request, response.getRet(), (System.currentTimeMillis() - startTime), remark);
            }
            //报告本次调用的耗时情况
            OmServiceMngr.getInstance().reportWaitingTimeProperty(waitingTime);
            //报告本次调用基本信息
            reportServerStat(request, response, startTime);
        }
        return response;
    }

    // TODO: 17/4/15 by zmyer
    private void reportServerStat(TarsServantRequest request, TarsServantResponse response,
        long startTime) {
        if (request.getVersion() == TarsHelper.VERSION2 || request.getVersion() == TarsHelper.VERSION3) {
            //报告服务器统计信息
            reportServerStat(Constants.TARS_TUP_CLIENT, request, response, startTime);
        } else if (request.getMessageType() == TarsHelper.ONEWAY) {
            //报告单向调用服务器统计信息
            reportServerStat(Constants.TARS_ONE_WAY_CLIENT, request, response, startTime);
        }
    }

    // TODO: 17/4/15 by zmyer
    private void reportServerStat(String moduleName, TarsServantRequest request,
        TarsServantResponse response, long startTime) {
        ServerConfig serverConfig = ConfigurationManager.getInstance().getserverConfig();
        ServantAdapterConfig servantAdapterConfig = serverConfig.getServantAdapterConfMap().get(request.getServantName());
        if (servantAdapterConfig == null) {
            return;
        }
        Endpoint serverEndpoint = servantAdapterConfig.getEndpoint();
        String masterIp = request.getIoSession().getRemoteIp();
        int result = response.getRet() == TarsHelper.SERVERSUCCESS ?
            Constants.INVOKE_STATUS_SUCC : Constants.INVOKE_STATUS_EXEC;
        //进行数据统计
        InvokeStatHelper.getInstance().addProxyStat(request.getServantName()).addInvokeTime(moduleName,
            request.getServantName(), serverConfig.getCommunicatorConfig().getSetDivision(),
            request.getFunctionName(), (masterIp == null ? "0.0.0.0" : masterIp),
            serverEndpoint.host(), serverEndpoint.port(), result, (System.currentTimeMillis() - startTime));
    }

    // TODO: 17/4/15 by zmyer
    public static void printServiceFlowLog(Logger logger, TarsServantRequest request, int status,
        long cost,
        String remark) {
        if (status == TarsHelper.SERVERSUCCESS && !isFlowLogEnable())
            return;

        StringBuilder sb = new StringBuilder();
        Object args[] = request.getMethodParameters();
        int len = 25;

        sb.append(FLOW_SEP_FLAG);
        sb.append(request.getIoSession().getRemoteIp()).append(FLOW_SEP_FLAG);
        sb.append(request.getFunctionName()).append(FLOW_SEP_FLAG);

        if (null != args) {
            StringBuilder sbArgs = new StringBuilder();
            for (Object arg : args) {
                if (arg == null) {
                    sbArgs.append("NULL").append(",");
                } else if (arg instanceof Number || arg instanceof Boolean) {
                    sbArgs.append(arg).append(",");
                } else {
                    sbArgs.append(encodeStringParam(arg.toString(), len)).append(",");
                }
            }
            sbArgs = sbArgs.length() >= 1 ? sbArgs.deleteCharAt(sbArgs.length() - 1) : sbArgs;
            sb.append(sbArgs);
        }

        sb.append(FLOW_SEP_FLAG);
        sb.append(status).append(FLOW_SEP_FLAG).append(cost);
        sb.append(FLOW_SEP_FLAG).append(remark);

        logger.info(sb.toString());
    }

    // TODO: 17/4/15 by zmyer
    private static boolean isFlowLogEnable() {
        return ConfigurationManager.getInstance().getserverConfig().getLogRate() - rand.nextInt(100) > 0;
    }

    // TODO: 17/4/15 by zmyer
    private static String encodeStringParam(String longParam, int len) {
        if (longParam == null || longParam.length() == 0)
            return "";
        String shortParam = longParam;

        if (len > 0) {
            shortParam = longParam.length() > len ? longParam.substring(0, len) + "..(" + longParam.length() + ")" : longParam;
        }

        return shortParam.replaceAll(" ", "_").replaceAll(" ", "_").replaceAll("\n", "+").replace(',', '，').replace('(', '（').replace(')', '）');
    }

    // TODO: 17/4/15 by zmyer
    private TarsServantResponse createResponse(TarsServantRequest request, Session session) {
        TarsServantResponse response = new TarsServantResponse(session);
        response.setRet(request.getRet());
        response.setVersion(request.getVersion());
        response.setPacketType(request.getPacketType());
        response.setMessageType(request.getMessageType());
        response.setStatus(request.getStatus());
        response.setRequest(request);
        response.setCharsetName(request.getCharsetName());
        response.setTimeout(request.getTimeout());
        response.setContext(request.getContext());
        return response;
    }
}
