/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations und
 */

package org.wso2.carbon.identity.handler.event.account.lock.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.handler.event.account.lock.constants.AccountConstants;
import org.wso2.carbon.identity.handler.event.account.lock.exception.AccountLockException;
import org.wso2.carbon.identity.handler.event.account.lock.exception.AccountLockRuntimeException;
import org.wso2.carbon.identity.handler.event.account.lock.internal.AccountServiceDataHolder;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.service.RealmService;

public class AccountUtil {

    private static final Log log = LogFactory.getLog(AccountUtil.class);

    public static String getUserStoreDomainName(UserStoreManager userStoreManager) {
        String domainNameProperty = null;
        if(userStoreManager instanceof org.wso2.carbon.user.core.UserStoreManager) {
            domainNameProperty = ((org.wso2.carbon.user.core.UserStoreManager)
                                          userStoreManager).getRealmConfiguration()
                    .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
            if(StringUtils.isBlank(domainNameProperty)) {
                domainNameProperty = IdentityUtil.getPrimaryDomainName();
            }
        }
        return domainNameProperty;
    }

    public static String getTenantDomain(UserStoreManager userStoreManager) {
        try {
            return IdentityTenantUtil.getTenantDomain(userStoreManager.getTenantId());
        } catch (UserStoreException e) {
            throw AccountLockRuntimeException.error(e.getMessage(), e);
        }
    }

    public static String getConnectorConfig(String key, String tenantDomain) throws IdentityEventException {
        try {
            Property[] connectorConfigs;
            IdentityGovernanceService identityGovernanceService = AccountServiceDataHolder.getInstance()
                    .getIdentityGovernanceService();
            if (identityGovernanceService != null) {
                connectorConfigs = identityGovernanceService.getConfiguration(new String[]{key}, tenantDomain);
                if (connectorConfigs != null && connectorConfigs.length > 0) {
                    return connectorConfigs[0].getValue();
                }
            }
            return null;
        } catch (IdentityGovernanceException e) {
            throw new IdentityEventException("Error while getting connector configurations for property :" + key, e);
        }
    }

    public static boolean isAccountStateClaimExisting(String tenantDomain) throws AccountLockException {

        UserRealm userRealm = null;
        ClaimManager claimManager = null;
        boolean isExist = false;

        RealmService realmService = AccountServiceDataHolder.getInstance().getRealmService();
        if (realmService != null) {
            try {
                int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
                // Get tenant's user realm.
                userRealm = realmService.getTenantUserRealm(tenantId);
                if (userRealm != null) {
                    // Get claim manager for manipulating attributes.
                    claimManager = (ClaimManager) userRealm.getClaimManager();
                    if (claimManager != null) {
                        Claim claim = claimManager.getClaim(AccountConstants.ACCOUNT_STATE_CLAIM_URI);
                        if (claim != null) {
                            isExist = true;
                        }
                    }
                }
            } catch (UserStoreException e) {
                throw new AccountLockException("Error while retrieving accountState claim from ClaimManager.", e);
            }
        }
        return isExist;
    }
}
