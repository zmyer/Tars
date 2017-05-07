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

import com.qq.tars.client.Communicator;
import com.qq.tars.client.CommunicatorConfig;
import com.qq.tars.client.CommunicatorFactory;
import com.qq.tars.common.support.Endpoint;
import com.qq.tars.common.util.BeanAccessor;
import com.qq.tars.common.util.StringUtils;
import com.qq.tars.net.core.Processor;
import com.qq.tars.net.core.SessionManager;
import com.qq.tars.net.core.nio.SelectorManager;
import com.qq.tars.net.protocol.ProtocolFactory;
import com.qq.tars.net.util.Utils;
import com.qq.tars.rpc.ext.ExtendedProtocolFactory;
import com.qq.tars.rpc.protocol.tars.TarsCodec;
import com.qq.tars.rpc.protocol.tars.TarsServantProtocolFactory;
import com.qq.tars.server.common.ServerLogger;
import com.qq.tars.server.config.ConfigurationManager;
import com.qq.tars.server.config.ServantAdapterConfig;
import com.qq.tars.server.config.ServerConfig;
import com.qq.tars.server.ha.ConnectionSessionListener;
import com.qq.tars.support.log.LogConfCacheMngr;
import com.qq.tars.support.log.LoggerFactory;
import com.qq.tars.support.om.OmConstants;
import com.qq.tars.support.om.OmServiceMngr;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

// TODO: 17/4/13 by zmyer
public class Server {
    //主机地址
    private String host = null;
    //端口号
    private int port;
    //udp端口号
    private int udpPort;
    //tcp端口号
    private int tcpPort;
    //后台管理主机
    private String adminHost = null;
    //后台管理端口号
    private int adminPort;

    //协议工厂
    private ProtocolFactory mainProtocolFactory = null;

    //主selector管理者
    private SelectorManager mainSelectorManager = null;

    //udp selector管理者
    private SelectorManager udpSelectorManager = null;

    //tcp selector管理者
    private SelectorManager tcpSelectorManager = null;

    //事件处理对象
    private Processor processor = null;

    //容器对象
    private Container container = null;

    //执行线程池对象
    private Executor threadPool = null;

    //是否保持存活标记
    private boolean keepAlive = true;

    //会话超时时间
    private int sessionTimeOut;

    //会话检查时间间隔
    private int sessionCheckInterval;

    //udp协议工厂
    private ProtocolFactory udpProtocolFactory;

    //tcp协议工厂
    private ProtocolFactory tcpProtocolFactory;

    // TODO: 17/4/13 by zmyer
    public Server() {
        loadServerConfig();
    }

    // TODO: 17/4/13 by zmyer
    public void startUp(String args[]) {
        try {
            //初始化通信对象
            initCommunicator();

            //配置日志
            configLogger();

            //初始化服务器日志对象
            ServerLogger.init();

            //启动监控服务
            startOmService();

            //启动app容器对象
            startAppContainer();

            //启动NIO服务器
            startNIOServer();

            //启动会话管理器
            startSessionManager();

            //注册服务器hook
            registerServerHook();

            System.out.println("[SERVER] server is ready...");
        } catch (Throwable ex) {
            System.out.println("[SERVER] failed to start server...");
            System.out.close();
            System.err.close();
        }
    }

    // TODO: 17/4/15 by zmyer
    protected void startAppContainer() throws Exception {
        //读取main协议工厂对象
        this.mainProtocolFactory = new TarsServantProtocolFactory(new TarsCodec(ConfigurationManager.getInstance().getserverConfig().getCharsetName()));
        //创建事件处理器对象
        this.processor = new ServantProcessor();
        //创建app容器对象
        this.container = new AppContainer();
        //将该容器对象注册到管理器中
        ContainerManager.registerContainer(this.container);
        //创建线程池分发对象
        this.threadPool = new ServerThreadPoolDispatcher();
        //创建main选择器管理器
        this.mainSelectorManager = new SelectorManager(Utils.getSelectorPoolSize(), mainProtocolFactory,
            threadPool, processor, keepAlive, "server-tcp-reactor", false);
        //设置非延时标记
        this.mainSelectorManager.setTcpNoDelay(ConfigurationManager.getInstance().getserverConfig().isTcpNoDelay());
        //启动容器
        this.container.start();
    }

    // TODO: 17/4/13 by zmyer
    protected void startNIOServer() throws IOException {
        //启动主服务器
        startMainServer(host, port);
        //启动后台管理服务器
        startMainServer(adminHost, adminPort);

        //启动udp服务器
        startUDPServer();

        //启动tcp服务器
        startTCPServer();
    }

    // TODO: 17/4/15 by zmyer
    private void startOmService() {
        //启动om服务
        OmServiceMngr.getInstance().initAndStartOmService();
    }

    // TODO: 17/4/15 by zmyer
    private void initCommunicator() {
        //读取通信对象配置
        CommunicatorConfig config = ConfigurationManager.getInstance().getserverConfig()
            .getCommunicatorConfig();
        //创建通信对象
        Communicator communicator = CommunicatorFactory.getInstance().getCommunicator(config);
        //注册通信对象
        BeanAccessor.setBeanValue(CommunicatorFactory.getInstance(), "communicator", communicator);
    }

    // TODO: 17/4/15 by zmyer
    private void configLogger() {
        Communicator communicator = CommunicatorFactory.getInstance().getCommunicator();

        String objName = ConfigurationManager.getInstance().getserverConfig().getLog();
        String appName = ConfigurationManager.getInstance().getserverConfig().getApplication();
        String serviceName = ConfigurationManager.getInstance().getserverConfig().getServerName();

        String defaultLevel = ConfigurationManager.getInstance().getserverConfig().getLogLevel();
        String defaultRoot = ConfigurationManager.getInstance().getserverConfig().getLogPath();
        String dataPath = ConfigurationManager.getInstance().getserverConfig().getDataPath();

        LoggerFactory.config(communicator, objName, appName, serviceName, defaultLevel, defaultRoot);

        LogConfCacheMngr.getInstance().init(dataPath);
        if (StringUtils.isNotEmpty(LogConfCacheMngr.getInstance().getLevel())) {
            LoggerFactory.setDefaultLoggerLevel(LogConfCacheMngr.getInstance().getLevel());
        }
    }

    // TODO: 17/4/15 by zmyer
    private void startUDPServer() throws IOException {
        if (udpPort <= 0) {
            System.out.println("server.udpPort Not Exist or Invalid,  server Custom Protocol Service on UDP will be disabled.");
            return;
        }

        if (ExtendedProtocolFactory.getInstance() == null) {
            System.out.println("No Service use Custom Protocol Codec, Custom Protocol Service on UDP will be disabled.");
            return;
        }

        //udp协议工厂对象
        udpProtocolFactory = ExtendedProtocolFactory.getInstance();
        //创建selector管理器对象
        udpSelectorManager = new SelectorManager(1, udpProtocolFactory, threadPool, processor, false, "server-udp-reactor", true);
        //启动selector管理器
        udpSelectorManager.start();

        String[] ips = host.split(";");
        if (ips == null || ips.length == 0)
            throw new IllegalArgumentException("The host is illegal.");

        DatagramChannel serverChannel;
        for (String ip : ips) {
            if (ip == null || ip.trim().length() == 0)
                continue;
            ip = ip.trim();

            //创建服务器通道对象
            serverChannel = DatagramChannel.open();
            //创建数据包套接字对象
            DatagramSocket socket = serverChannel.socket();
            //套接字绑定地址和端口
            socket.bind(new InetSocketAddress(ip, udpPort));
            //设置非阻塞标记
            serverChannel.configureBlocking(false);
            //将对应的通道对象注册到指定的reactor对象中
            udpSelectorManager.getReactor(0).registerChannel(serverChannel,
                SelectionKey.OP_READ, null);

            System.out.println("[SERVER] server started at " + ip + " on ext UDP port " + String.valueOf(udpPort) + "...");
        }
    }

    // TODO: 17/4/15 by zmyer
    private void startTCPServer() throws IOException {
        if (tcpPort <= 0) {
            System.out.println("server.tcpPort Not Exist or Invalid,  server Custom Protocol Service on TCP will be disabled.");
            return;
        }

        if (ExtendedProtocolFactory.getInstance() == null) {
            System.out.println("No Service use Custom Protocol Codec, Custom Protocol Service on TCP will be disabled.");
            return;
        }

        //创建协议工厂对象
        this.tcpProtocolFactory = ExtendedProtocolFactory.getInstance();
        //创建selector管理器
        tcpSelectorManager = new SelectorManager(Utils.getSelectorPoolSize(), tcpProtocolFactory, threadPool, processor, keepAlive, "server-tcp-custom-reactor", false);
        //设置非延时标记
        tcpSelectorManager.setTcpNoDelay(ConfigurationManager.getInstance().getserverConfig().isTcpNoDelay());
        //启动selector管理器
        tcpSelectorManager.start();

        String[] ips = host.split(";");
        if (ips == null || ips.length == 0)
            throw new IllegalArgumentException("The host is illegal.");

        ServerSocketChannel serverChannel = null;
        for (String ip : ips) {
            if (ip == null || ip.trim().length() == 0)
                continue;
            ip = ip.trim();

            //创建服务器通道对象
            serverChannel = ServerSocketChannel.open();
            //给服务器通道对象绑定地址和端口号
            serverChannel.socket().bind(new InetSocketAddress(ip, tcpPort), 1024);
            //设置非阻塞标记
            serverChannel.configureBlocking(false);
            //开始将该通道注册到reactor对象中
            tcpSelectorManager.getReactor(0).registerChannel(serverChannel, SelectionKey.OP_ACCEPT, null);
            System.out.println("[SERVER] server started at " + ip + " on ext TCP port " + String.valueOf(tcpPort) + "...");
        }

    }

    // TODO: 17/4/13 by zmyer
    private void startMainServer(String ip, int port) throws IOException {
        if (port <= 0) {
            System.out.println("server.port Not Exist or Invalid,  server Protocol Service on TCP will be disabled.");
            return;
        }

        //启动主selector管理器
        mainSelectorManager.start();

        //解析host
        String[] ips = host.split(";");
        if (ips == null || ips.length == 0) {
            throw new IllegalArgumentException("The host is illegal.");
        }

        ServerSocketChannel serverChannel;
        if (ip == null)
            return;
        ip = ip.trim();

        System.out.println("[SERVER] server starting at " + ip + " on TCP port " + String.valueOf(port) + "...");
        //创建服务器通道对象
        serverChannel = ServerSocketChannel.open();
        //为通道对象绑定指定的ip和端口
        serverChannel.socket().bind(new InetSocketAddress(ip, port), 1024);
        //设置通道对象为非阻塞模式
        serverChannel.configureBlocking(false);
        //将该通道对象注册到reactor对象中
        mainSelectorManager.getReactor(0).registerChannel(serverChannel, SelectionKey.OP_ACCEPT, null);

        System.out.println("[SERVER] server started at " + ip + " on TCP port " + String.valueOf(port) + "...");
    }

    // TODO: 17/4/15 by zmyer
    protected void startSessionManager() throws IOException {
        //创建会话管理器
        SessionManager sessionManager = SessionManager.getSessionManager();
        //设置会话超时时间
        sessionManager.setTimeout(sessionTimeOut);
        //设置会话检查时间间隔
        sessionManager.setCheckInterval(sessionCheckInterval);

        //统计连接数
        int connCount = 0;
        for (Entry<String, ServantAdapterConfig> adapterConfigEntry : ConfigurationManager.getInstance().getserverConfig().getServantAdapterConfMap().entrySet()) {
            if (OmConstants.AdminServant.equals(adapterConfigEntry.getKey())) {
                continue;
            }
            connCount += adapterConfigEntry.getValue().getMaxConns();
        }

        //创建连接会话监听器
        ConnectionSessionListener sessionListener = new ConnectionSessionListener(connCount);
        //将该会话监听器注册到会话管理器中
        sessionManager.addSessionListener(sessionListener);
        //启动会话管理器
        sessionManager.start();
    }

    // TODO: 17/4/15 by zmyer
    protected void loadServerConfig() {
        try {
            ConfigurationManager.getInstance().init();
            //读取服务器配置
            ServerConfig cfg = ConfigurationManager.getInstance().getserverConfig();
            ServerLogger.initNamiCoreLog(cfg.getLogPath(), cfg.getLogLevel());
            System.setProperty("com.qq.nami.server.udp.bufferSize", String.valueOf(cfg.getUdpBufferSize()));
            System.setProperty("server.root", cfg.getBasePath());

            Endpoint adminEndpoint = cfg.getLocal();

            adminHost = adminEndpoint.host();
            adminPort = adminEndpoint.port();

            host = cfg.getLocalIP();
            LinkedHashMap<String, ServantAdapterConfig> map = cfg.getServantAdapterConfMap();
            for (ServantAdapterConfig sc : map.values()) {
                if (sc.getProtocol().equals("tars")) {
                    if (port == 0) {
                        port = sc.getEndpoint().port();
                    }
                } else {
                    if (sc.getEndpoint().type().equals("tcp") && tcpPort == 0) {
                        tcpPort = sc.getEndpoint().port();
                    }
                    if (sc.getEndpoint().type().equals("udp") && udpPort == 0) {
                        udpPort = sc.getEndpoint().port();
                    }
                }
            }
            cfg.setLocalPort(port);
            sessionTimeOut = cfg.getSessionTimeOut();
            sessionCheckInterval = cfg.getSessionCheckInterval();
        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
            System.err.println("The exception occured at load server config");
        }
    }

    // TODO: 17/4/15 by zmyer
    private void registerServerHook() {
        //注册关闭hook
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    if (mainSelectorManager != null) {
                        //关闭main选择器
                        mainSelectorManager.stop();
                    }
                    if (udpSelectorManager != null) {
                        //关闭udp选择器
                        udpSelectorManager.stop();
                    }

                    System.out.println("[SERVER] server stopped successfully.");

                    // 2. Stop Container
                    if (container != null) {
                        //关闭容器
                        container.stop();
                    }
                } catch (Exception ex) {
                    System.err.println("The exception occured at stopping server...");
                }
            }
        });
    }
}
