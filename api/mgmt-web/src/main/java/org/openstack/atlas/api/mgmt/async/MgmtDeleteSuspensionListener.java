package org.openstack.atlas.api.mgmt.async;


import org.openstack.atlas.service.domain.entities.LoadBalancer;
import org.openstack.atlas.service.domain.entities.LoadBalancerStatus;
import org.openstack.atlas.service.domain.events.UsageEvent;
import org.openstack.atlas.service.domain.exceptions.EntityNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstack.atlas.service.domain.exceptions.UsageEventCollectionException;

import javax.jms.Message;

import java.util.Calendar;

import static org.openstack.atlas.service.domain.services.helpers.AlertType.DATABASE_FAILURE;
import static org.openstack.atlas.service.domain.services.helpers.AlertType.ZEUS_FAILURE;
import static org.openstack.atlas.service.domain.events.entities.CategoryType.UPDATE;
import static org.openstack.atlas.service.domain.events.entities.EventSeverity.CRITICAL;
import static org.openstack.atlas.service.domain.events.entities.EventSeverity.INFO;
import static org.openstack.atlas.service.domain.events.entities.EventType.UPDATE_LOADBALANCER;

public class MgmtDeleteSuspensionListener extends BaseListener {

    private final Log LOG = LogFactory.getLog(MgmtDeleteSuspensionListener.class);

    @Override
    public void doOnMessage(final Message message) throws Exception {
        LOG.debug("Entering " + getClass());
        LOG.debug(message);
        LoadBalancer requestLb = getEsbRequestFromMessage(message).getLoadBalancer();
        LoadBalancer dbLoadBalancer;

        try {
            dbLoadBalancer = loadBalancerService.get(requestLb.getId());
        } catch (EntityNotFoundException enfe) {
            String alertDescription = String.format("Load balancer '%d' not found in database.", requestLb.getId());
            LOG.error(alertDescription, enfe);
            notificationService.saveAlert(requestLb.getAccountId(), requestLb.getId(), enfe, DATABASE_FAILURE.name(), alertDescription);
            sendErrorToEventResource(requestLb);
            return;
        }

        try {
            if (isRestAdapter()) {
                LOG.debug(String.format("Removing suspension from load balancer '%d' in STM...", dbLoadBalancer.getId()));
                reverseProxyLoadBalancerStmService.removeSuspension(dbLoadBalancer);
                LOG.debug(String.format("Successfully removed suspension from load balancer '%d' in STM.", dbLoadBalancer.getId()));
            } else {
                LOG.debug(String.format("Removing suspension from load balancer '%d' in ZXTM...", dbLoadBalancer.getId()));
                reverseProxyLoadBalancerService.removeSuspension(dbLoadBalancer);
                LOG.debug(String.format("Successfully removed suspension from load balancer '%d' in ZXMT.", dbLoadBalancer.getId()));
            }
        } catch (Exception e) {
            loadBalancerService.setStatus(dbLoadBalancer, LoadBalancerStatus.ERROR);
            String alertDescription = String.format("Error removing suspension from load balancer '%d' in Zeus.", dbLoadBalancer.getId());
            LOG.error(alertDescription, e);
            notificationService.saveAlert(dbLoadBalancer.getAccountId(), dbLoadBalancer.getId(), e, ZEUS_FAILURE.name(), alertDescription);
            sendErrorToEventResource(requestLb);
            return;
        }

        Calendar eventTime = Calendar.getInstance();
        // Notify usage processor
        try {
            usageEventCollection.processZeroUsageEvent(dbLoadBalancer, UsageEvent.UNSUSPEND_LOADBALANCER, eventTime);
        } catch (UsageEventCollectionException uex) {
            LOG.error(String.format("Collection and processing of the usage event failed for load balancer: %s " +
                    ":: Exception: %s", dbLoadBalancer.getId(), uex));
        }

        // Update load balancer in DB
        LOG.debug("Deleting Suspension from database...");
        loadBalancerService.removeSuspension(dbLoadBalancer.getId());
        loadBalancerService.setStatus(dbLoadBalancer, LoadBalancerStatus.ACTIVE);

        // Add atom entry
        String atomTitle = "Load Balancer Un-Suspended";
        String atomSummary = "Load balancer un-suspended";
        notificationService.saveLoadBalancerEvent(requestLb.getUserName(), dbLoadBalancer.getAccountId(), dbLoadBalancer.getId(), atomTitle, atomSummary, UPDATE_LOADBALANCER, UPDATE, INFO);

        LOG.info(String.format("Remove suspension operation complete for load balancer '%d'.", dbLoadBalancer.getId()));
    }

    private void sendErrorToEventResource(LoadBalancer lb) {
        String title = "Error Un-Suspending Load Balancer";
        String desc = "Could not un-suspend the loadbalancer at this time.";
        notificationService.saveLoadBalancerEvent(lb.getUserName(), lb.getAccountId(), lb.getId(), title, desc, UPDATE_LOADBALANCER, UPDATE, CRITICAL);
    }
}
