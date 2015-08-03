/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.registry.security.vault.internal;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.registry.common.ResourceData;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.security.vault.observers.TenantDeploymentListenerImpl;
import org.wso2.carbon.registry.security.vault.service.RegistrySecurityService;
import org.wso2.carbon.registry.security.vault.util.SecureVaultUtil;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;

import java.util.Stack;

/**
 * @scr.component name="registry.security" immediate="true"
 * @scr.reference name="registry.service"
 *                interface=
 *                "org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="server.configuration"
 *                interface=
 *                "org.wso2.carbon.base.api.ServerConfigurationService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setServerConfigurationService"
 *                unbind="unsetServerConfigurationService"
 */
public class SynapseSecurityerviceComponent {

	private static Log log = LogFactory.getLog(SynapseSecurityerviceComponent.class);

    private static Stack<ServiceRegistration> registrations = new Stack<ServiceRegistration>();

	public SynapseSecurityerviceComponent() {
	}

	protected void activate(ComponentContext ctxt) {
        registrations.push(ctxt.getBundleContext().registerService(
                RegistrySecurityService.class.getName(), new RegistrySecurityServiceImpl(), null));

        TenantDeploymentListenerImpl listener = new TenantDeploymentListenerImpl();
        registrations.push(ctxt.getBundleContext().registerService(
                Axis2ConfigurationContextObserver.class.getName(), listener, null));

        try {
            SecureVaultUtil.createRegistryResource(-1234);
        } catch (RegistryException ignore) {
        }
        if (log.isDebugEnabled()) {
			log.debug("Registry security component activated");
		}
	}

	protected void deactivate(ComponentContext ctxt) {
        while (!registrations.empty()) {
            registrations.pop().unregister();
        }
        log.debug("Registry security component deactivated");
	}

	protected void setRegistryService(RegistryService regService) {
		if (log.isDebugEnabled()) {
			log.debug("RegistryService bound to the ESB initialization process");
		}
		SecurityServiceHolder.getInstance().setRegistryService(regService);
	}

	protected void unsetRegistryService(RegistryService regService) {
		if (log.isDebugEnabled()) {
			log.debug("RegistryService unbound from the ESB environment");
		}
		SecurityServiceHolder.getInstance().setRegistryService(null);
	}

	protected void setServerConfigurationService(ServerConfigurationService serverConfiguration) {
		SecurityServiceHolder.getInstance().setServerConfigurationService(serverConfiguration);
	}

	protected void unsetServerConfigurationService(ServerConfigurationService serverConfiguration) {
		SecurityServiceHolder.getInstance().setServerConfigurationService(null);
	}

    private static class RegistrySecurityServiceImpl implements RegistrySecurityService {

        @Override
        public String doEncrypt(String plainTextPass) throws RegistryException {
            try {
                return SecureVaultUtil.encryptValue(plainTextPass);
            } catch (AxisFault axisFault) {
                throw new RegistryException("Error while encrypting data");
            }
        }

    }

}