package com.github.dumas.shardingsphere.jdbc.spi;

import org.apache.shardingsphere.authority.config.AuthorityRuleConfiguration;
import org.apache.shardingsphere.authority.model.ShardingSpherePrivileges;
import org.apache.shardingsphere.authority.spi.PrivilegeProvider;
import org.apache.shardingsphere.infra.metadata.user.Grantee;

public final class AllPermittedPrivilegeProvider implements PrivilegeProvider {

    @Override
    public ShardingSpherePrivileges build(AuthorityRuleConfiguration ruleConfig, Grantee grantee) {
        return sql -> true;
    }

    @Override
    public Object getType() {
        return "ALL_PERMITTED";
    }
}
