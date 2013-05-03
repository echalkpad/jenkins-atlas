package org.openstack.atlas.usagerefactor.helpers;

import org.apache.camel.processor.loadbalancer.LoadBalancer;
import org.apache.commons.logging.LogFactory;
import org.openstack.atlas.service.domain.events.UsageEvent;
import org.openstack.atlas.service.domain.usage.entities.LoadBalancerHostUsage;
import org.openstack.atlas.service.domain.usage.entities.LoadBalancerMergedHostUsage;
import org.openstack.atlas.usagerefactor.SnmpUsage;
import org.openstack.atlas.usagerefactor.UsageProcessor;

import java.util.*;

public class UsagePollerHelper{

    final static org.apache.commons.logging.Log LOG = LogFactory.getLog(UsageProcessor.class);

    public UsagePollerHelper() {}

    public void calculateUsage(SnmpUsage currentUsage, LoadBalancerHostUsage previousRecord,
                               LoadBalancerMergedHostUsage newMergedUsage) {
        long totIncomingTransfer = newMergedUsage.getIncomingTransfer();
        long totIncomingTransferSsl = newMergedUsage.getIncomingTransferSsl();
        long totOutgoingTransfer = newMergedUsage.getOutgoingTransfer();
        long totOutgoingTransferSsl = newMergedUsage.getOutgoingTransferSsl();
        //Handle normal virtual server resetting
        if (!isReset(currentUsage.getBytesIn(), previousRecord.getIncomingTransfer()) &&
            !isReset(currentUsage.getBytesOut(), previousRecord.getOutgoingTransfer())) {
            totIncomingTransfer += currentUsage.getBytesIn() - previousRecord.getIncomingTransfer();
            totOutgoingTransfer += currentUsage.getBytesOut() - previousRecord.getOutgoingTransfer();
        }
        //Handle SSL virtual server resetting
        if (!isReset(currentUsage.getBytesInSsl(), previousRecord.getIncomingTransferSsl()) &&
            !isReset(currentUsage.getBytesOutSsl(), previousRecord.getOutgoingTransferSsl())) {
            totIncomingTransferSsl += currentUsage.getBytesInSsl() - previousRecord.getIncomingTransferSsl();
            totOutgoingTransferSsl += currentUsage.getBytesOutSsl() - previousRecord.getOutgoingTransferSsl();
        }
        newMergedUsage.setIncomingTransfer(totIncomingTransfer);
        newMergedUsage.setIncomingTransferSsl(totIncomingTransferSsl);
        newMergedUsage.setOutgoingTransfer(totOutgoingTransfer);
        newMergedUsage.setOutgoingTransferSsl(totOutgoingTransferSsl);
        //Using concurrent connections regardless of reset since this is not a counter, only a snapshot
        long ccs = currentUsage.getConcurrentConnections() + newMergedUsage.getConcurrentConnections();
        long ccsSsl = currentUsage.getConcurrentConnectionsSsl() + newMergedUsage.getConcurrentConnectionsSsl();
        newMergedUsage.setConcurrentConnections(ccs);
        newMergedUsage.setConcurrentConnectionsSsl(ccsSsl);
    }

    public void calculateUsage(LoadBalancerHostUsage currentRecord, LoadBalancerHostUsage previousRecord,
                               LoadBalancerMergedHostUsage newMergedUsage) {
        long totIncomingTransfer = newMergedUsage.getIncomingTransfer();
        long totIncomingTransferSsl = newMergedUsage.getIncomingTransferSsl();
        long totOutgoingTransfer = newMergedUsage.getOutgoingTransfer();
        long totOutgoingTransferSsl = newMergedUsage.getOutgoingTransferSsl();
        //Handle normal virtual server resetting
        if (!isReset(currentRecord.getIncomingTransfer(), previousRecord.getIncomingTransfer()) &&
            !isReset(currentRecord.getOutgoingTransfer(), previousRecord.getOutgoingTransfer())) {
            totIncomingTransfer += currentRecord.getIncomingTransfer() - previousRecord.getIncomingTransfer();
            totOutgoingTransfer += currentRecord.getOutgoingTransfer() - previousRecord.getOutgoingTransfer();
        }
        //Handle SSL virtual server resetting
        if (!isReset(currentRecord.getIncomingTransferSsl(), previousRecord.getIncomingTransferSsl()) &&
            !isReset(currentRecord.getOutgoingTransferSsl(), previousRecord.getOutgoingTransferSsl())) {
            totIncomingTransferSsl += currentRecord.getIncomingTransferSsl() - previousRecord.getIncomingTransferSsl();
            totOutgoingTransferSsl += currentRecord.getOutgoingTransferSsl() - previousRecord.getOutgoingTransferSsl();
        }
        newMergedUsage.setIncomingTransfer(totIncomingTransfer);
        newMergedUsage.setIncomingTransferSsl(totIncomingTransferSsl);
        newMergedUsage.setOutgoingTransfer(totOutgoingTransfer);
        newMergedUsage.setOutgoingTransferSsl(totOutgoingTransferSsl);
        //Using concurrent connections regardless of reset since this is not a counter, only a snapshot
        long ccs = currentRecord.getConcurrentConnections() + newMergedUsage.getConcurrentConnections();
        long ccsSsl = currentRecord.getConcurrentConnectionsSsl() + newMergedUsage.getConcurrentConnectionsSsl();
        newMergedUsage.setConcurrentConnections(ccs);
        newMergedUsage.setConcurrentConnectionsSsl(ccsSsl);
    }

    public boolean isReset(long currentBandwidth, long previousBandwidth) {
        return currentBandwidth < previousBandwidth;
    }

    public UsageProcessorResult processCurrentUsage(Map<Integer, Map<Integer, List<LoadBalancerHostUsage>>> existingUsages,
                                                    Map<Integer, Map<Integer, SnmpUsage>> currentUsages,
                                                    Calendar pollTime){
        List<LoadBalancerMergedHostUsage> mergedUsages = new ArrayList<LoadBalancerMergedHostUsage>();
        List<LoadBalancerHostUsage> newLBHostUsages = new ArrayList<LoadBalancerHostUsage>();

        for (Integer loadbalancerId : currentUsages.keySet()) {
            if (!existingUsages.containsKey(loadbalancerId)) {
                //TODO:
                //There are no previous records for this load balancer.
                //Pull numVips and tags from DB and 0 everything else out for the merged record.
                //Store the counters as they are in the lb_host_usage table.
                continue;
            }
            LoadBalancerMergedHostUsage newMergedRecord = new LoadBalancerMergedHostUsage();
            boolean isFirstPass = true;
            for (Integer hostId : currentUsages.get(loadbalancerId).keySet()) {
                if (!existingUsages.get(loadbalancerId).containsKey(hostId)) {

                }
                List<LoadBalancerHostUsage> loadBalancerHostUsages = existingUsages.get(loadbalancerId).get(hostId);
                SnmpUsage currentUsage = currentUsages.get(loadbalancerId).get(hostId);
                if (loadBalancerHostUsages.size() == 0) {
                    //TODO:
                    //There are no previous records for this load balancer on this host.
                    //This condition shouldn't ever be met though.
                }
                LoadBalancerHostUsage existingUsage = loadBalancerHostUsages.get(loadBalancerHostUsages.size() - 1);
                if (isFirstPass) {
                    newMergedRecord = initializeMergedRecord(existingUsage);
                    newMergedRecord.setPollTime(pollTime);
                    newMergedRecord.setEventType(null);
                    isFirstPass = false;
                }
                if(!existingUsages.get(loadbalancerId).containsKey(hostId)) {
                    //TODO:
                    //No previous records exist for this load balancer on this host.
                    //What should be done? Nothing methinks
                }

                calculateUsage(currentUsage, existingUsage, newMergedRecord);
                newLBHostUsages.add(convertSnmpUsageToLBHostUsage(currentUsage, existingUsage, pollTime));
            }
            mergedUsages.add(newMergedRecord);
        }
        //TODO: Check for loadbalancers that exist in existingUsages and not in currentUsages

        return new UsageProcessorResult(mergedUsages, newLBHostUsages);
    }
    public List<LoadBalancerMergedHostUsage> processExistingEvents(Map<Integer, Map<Integer, List<LoadBalancerHostUsage>>> existingUsages) {
        List<LoadBalancerMergedHostUsage> newMergedEventRecords = new ArrayList<LoadBalancerMergedHostUsage>();

        for (Integer loadBalancerId : existingUsages.keySet()) {
            HashMap<String, LoadBalancerMergedHostUsage> mergedUsagesMap = new HashMap<String, LoadBalancerMergedHostUsage>();

            for (Integer hostId : existingUsages.get(loadBalancerId).keySet()) {
                List<LoadBalancerHostUsage> loadBalancerHostUsages = existingUsages.get(loadBalancerId).get(hostId);

                if (loadBalancerHostUsages.size() == 0) {
                    LOG.info("Received a list of size 0 for a load balancer id and host id combination.  This should not have happened.");
                    continue;
                }

                //If there is only one record. then it is most likely just the previous poll. Check event just in case.
                if (loadBalancerHostUsages.size() == 1) {
                    if (loadBalancerHostUsages.get(0).getEventType() != null) {
                        LOG.info("Event record encountered that did not have a previous record to compare with.");
                    }
                    continue;
                }

                //If for some reason there are more than 1 record and the last record is a null
                if (loadBalancerHostUsages.get(loadBalancerHostUsages.size() - 1).getEventType() == null) {
                    continue;
                }

                for(int i = 1; i < loadBalancerHostUsages.size(); i++) {
                    String timeKey = loadBalancerHostUsages.get(i).getPollTime().getTime().toString();
                    if (!mergedUsagesMap.containsKey(timeKey)) {
                        mergedUsagesMap.put(timeKey, initializeMergedRecord(loadBalancerHostUsages.get(i)));
                    }
                    LoadBalancerMergedHostUsage newMergedUsage = mergedUsagesMap.get(timeKey);
                    calculateUsage(loadBalancerHostUsages.get(i), loadBalancerHostUsages.get(i - 1), newMergedUsage);
                }
            }

            //Add all events into list that shall be returned
            for(String timeKey : mergedUsagesMap.keySet()) {
                newMergedEventRecords.add(mergedUsagesMap.get(timeKey));
            }
        }

        return newMergedEventRecords;
    }

    public LoadBalancerMergedHostUsage initializeMergedRecord(LoadBalancerHostUsage lbHostUsage) {
        LoadBalancerMergedHostUsage newLBMergedHostUsage = new LoadBalancerMergedHostUsage();
        newLBMergedHostUsage.setAccountId(lbHostUsage.getAccountId());
        newLBMergedHostUsage.setLoadbalancerId(lbHostUsage.getLoadbalancerId());
        newLBMergedHostUsage.setNumVips(lbHostUsage.getNumVips());
        newLBMergedHostUsage.setEventType(lbHostUsage.getEventType());
        Calendar pollTime = Calendar.getInstance();
        pollTime.setTime(lbHostUsage.getPollTime().getTime());
        newLBMergedHostUsage.setPollTime(pollTime);
        newLBMergedHostUsage.setTagsBitmask(lbHostUsage.getTagsBitmask());
        return newLBMergedHostUsage;
    }

    public LoadBalancerHostUsage convertSnmpUsageToLBHostUsage(SnmpUsage snmpUsage, LoadBalancerHostUsage previousUsage, Calendar pollTime) {
        LoadBalancerHostUsage newlbHostUsage = new LoadBalancerHostUsage();
        newlbHostUsage.setAccountId(previousUsage.getAccountId());
        newlbHostUsage.setLoadbalancerId(previousUsage.getLoadbalancerId());
        newlbHostUsage.setTagsBitmask(previousUsage.getTagsBitmask());
        newlbHostUsage.setNumVips(previousUsage.getNumVips());
        newlbHostUsage.setPollTime(pollTime);
        newlbHostUsage.setHostId(previousUsage.getHostId());
        if  (snmpUsage != null) {
            newlbHostUsage.setOutgoingTransfer(snmpUsage.getBytesOut());
            newlbHostUsage.setOutgoingTransferSsl(snmpUsage.getBytesOutSsl());
            newlbHostUsage.setIncomingTransfer(snmpUsage.getBytesIn());
            newlbHostUsage.setIncomingTransferSsl(snmpUsage.getBytesInSsl());
            newlbHostUsage.setConcurrentConnections(snmpUsage.getConcurrentConnections());
            newlbHostUsage.setConcurrentConnectionsSsl(snmpUsage.getConcurrentConnectionsSsl());
        }

        return newlbHostUsage;
    }
}