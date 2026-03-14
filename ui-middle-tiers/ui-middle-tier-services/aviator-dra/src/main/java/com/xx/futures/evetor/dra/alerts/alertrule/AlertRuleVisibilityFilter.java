package com.xx.futures.evetor.dra.alerts.alertrule;

import java.util.List;
import java.util.Set;

public class AlertRuleVisibilityFilter implements VisibilityFilter {

    private final PermitEngine permitEngine;
    private final AviatorContext aviatorContext;

    @Inject
    public AlertRuleVisibilityFilter(PermitEngine permitEngine, AviatorContext aviatorContext) {
        this.permitEngine = permitEngine;
        this.aviatorContext = aviatorContext;
    }

    @Override
    public DraExpression build(User user) {
        String kerberos = getKerberos(user);
        Set<Role> roles = getUserEntitlements(kerberos);

        if (roles.contains(Role.Tech) || roles.contains(Role.MissionControl)) {
            return TRUE.build(user);
        } else {
            return DraExpression.EQUAL(AviatorBaseDraServer.KERBEROS, wrapInQuotes(kerberos));
        }
    }

    private String getKerberos(User user) { return user.credentials().toString(); }

    private Set<Role> getUserEntitlements(String kerberos) {
        List<String> roles = permitEngine.getRoles(kerberos);
        return BaseEntitlementUtils.getUserRolesForEnvironment(aviatorContext.getEnvironment(), roles);
    }

    private String wrapInQuotes(String value) { return "\"" + value + "\""; }
}
