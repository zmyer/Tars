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

package com.qq.tars.client.rpc;

import com.qq.tars.rpc.common.InvokeContext;
import com.qq.tars.rpc.common.Invoker;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

// TODO: 17/5/22 by zmyer
@SuppressWarnings("serial")
public class ServantInvokeContext implements InvokeContext, Serializable {
    //调度器
    private Invoker<?> invoker;
    //函数名称
    private String methodName;
    //函数参数
    private Object[] arguments;
    //函数参数类型
    private Class<?>[] parameterTypes;
    //附加属性
    private Map<String, String> attachments;

    // TODO: 17/5/22 by zmyer
    public ServantInvokeContext(Method method, Object[] arguments,
        Map<String, String> attachments) {
        this(method, arguments, attachments, null);
    }

    // TODO: 17/5/22 by zmyer
    public ServantInvokeContext(Method method, Object[] arguments,
        Map<String, String> attachments, Invoker<?> invoker) {
        this.setInvoker(invoker);
        this.setMethodName(method.getName());
        this.setParameterTypes(method.getParameterTypes());
        this.setArguments(arguments);
        this.setAttachments(attachments);

        if (invoker != null) {
            this.addAttachmentsIfAbsent(invoker.getUrl().getParameters());
        }
    }

    // TODO: 17/5/22 by zmyer
    public Invoker<?> getInvoker() {
        return invoker;
    }

    // TODO: 17/5/22 by zmyer
    public void setInvoker(Invoker<?> invoker) {
        this.invoker = invoker;
    }

    // TODO: 17/5/22 by zmyer
    public String getMethodName() {
        return methodName;
    }

    // TODO: 17/5/22 by zmyer
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    // TODO: 17/5/22 by zmyer
    public Object[] getArguments() {
        return arguments;
    }

    // TODO: 17/5/22 by zmyer
    public Map<String, String> getAttachments() {
        return attachments;
    }

    // TODO: 17/5/22 by zmyer
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    // TODO: 17/5/22 by zmyer
    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
    }

    // TODO: 17/5/22 by zmyer
    public void setArguments(Object[] arguments) {
        this.arguments = arguments == null ? new Object[0] : arguments;
    }

    // TODO: 17/5/22 by zmyer
    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments == null ? new HashMap<String, String>() : attachments;
    }

    // TODO: 17/5/22 by zmyer
    public void setAttachment(String key, String value) {
        attachments.put(key, value);
    }

    // TODO: 17/5/22 by zmyer
    public void setAttachmentIfAbsent(String key, String value) {
        if (!attachments.containsKey(key)) {
            attachments.put(key, value);
        }
    }

    // TODO: 17/5/22 by zmyer
    public void addAttachments(Map<String, String> attachments) {
        this.attachments.putAll(attachments);
    }

    // TODO: 17/5/22 by zmyer
    public void addAttachmentsIfAbsent(Map<String, String> attachments) {
        if (attachments == null) {
            return;
        }
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            setAttachmentIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    // TODO: 17/5/22 by zmyer
    public String getAttachment(String key) {
        return attachments.get(key);
    }

    // TODO: 17/5/22 by zmyer
    public String getAttachment(String key, String defaultValue) {
        String value = attachments.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
