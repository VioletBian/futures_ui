package com.xx.futures.evetor.limitusageloaderserver.rest;

import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("@v0_/limitusage")
@Api("/limitusage")
public class LimitUsageApi extends RestBase {

    private final static OctaneLogger log = LogUtility.getLogger();
    private final LimitUsageLoader limitUsageLoader;

    @Inject
    public LimitUsageApi(
        NamedProperties properties,
        PermitEngine permitEngine,
        BaseEntitlementUtils baseEntitlementUtils,
        Messaging.MessagingExchange.EnvironmentNamespace env,
        LimitUsageLoader limitUsageLoader
    ) {
        super(properties, permitEngine, env, baseEntitlementUtils);
        this.limitUsageLoader = limitUsageLoader;
    }

    @GET
    @Path("@v0_/all")
    @ApiOperation(value = "data", notes = "Returns the latest limit usage for all active accounts")
    @Produces(MediaType.APPLICATION_JSON)
    @Authorize(getResourceFunction = "getResourcePermit", action = "view")
    public Response getAllLimitUsage() {
        log.info("Retrieving the latest limit usage data for all active accounts...");

        try {
            Map<String, ClearingData.LimitUsage> limitUsageCache = limitUsageLoader.getLimitUsageCache();
            log.info("Limit Usage cache map size is [{}]", limitUsageCache.size());

            return limitUsageCache.isEmpty()
                ? Response.status(Response.Status.NO_CONTENT).build()
                : Response.ok(limitUsageCache, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Path("@v0_/account")
    @ApiOperation(value = "account", notes = "Returns limit usage for a given account id or GMI id")
    @Produces(MediaType.APPLICATION_JSON)
    @Authorize(getResourceFunction = "getResourcePermit", action = "view")
    public Response getLimitUsageById(@QueryParam("account") String account) {
        log.info("Retrieving limit usage for account [{}]", account);

        try {
            Map<String, Object> limitUsage = limitUsageLoader.findLimitUsage(account);
            log.info("Limit Usage: [{}]", limitUsage);

            return Response.ok(limitUsage, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Path("@v0_/accounts")
    @ApiOperation(
        value = "List of accounts",
        notes = "Returns limit usage for each given account id or GMI id of a list"
    )
    @Produces(MediaType.APPLICATION_JSON)
    @Authorize(getResourceFunction = "getResourcePermit", action = "view")
    public Response getLimitUsageById(@QueryParam("accountlist") String accountList) {
        log.info("Retrieving limit usages for accounts [{}]", accountList);

        try {
            Map<String, Object> limitUsages = limitUsageLoader.findLimitUsages(accountList.split(","));
            log.info("Limit Usages: [{}]", limitUsages);

            return Response.ok(limitUsages, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
    }
}
