package org.openstack.atlas.usagerefactor;

import org.openstack.atlas.service.domain.entities.Usage;
import org.openstack.atlas.service.domain.usage.entities.LoadBalancerMergedHostUsage;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public interface UsageRollupProcessor {
    Map<Integer, List<LoadBalancerMergedHostUsage>> breakDownUsagesByLbId(List<LoadBalancerMergedHostUsage> LoadBalancerMergedHostUsages);

    /*
     *  @param hourToProcess
     *  Indicates the hour to process usage for.
     *
     *  Please note, the minutes and seconds are stripped out
     *  so that usages that are processed fall into the indicated hour. Example:
     *  '2013-12-28 13:23:59' corresponds to usage being processed between
     *  '2013-12-28 13:00:00' and '2013-12-28 14:00:00'
     */
    List<Usage> processRecords(List<LoadBalancerMergedHostUsage> LoadBalancerMergedHostUsages, Calendar hourToProcess);

    /*
     *  @param hourToProcess
     *  Indicates the hour to process usage for.
     *
     *  Please note, the minutes and seconds are stripped out
     *  so that usages that are processed fall into the indicated hour. Example:
     *  '2013-12-28 13:23:59' corresponds to usage being processed between
     *  '2013-12-28 13:00:00' and '2013-12-28 14:00:00'
     */
    List<Usage> processRecordsForLb(List<LoadBalancerMergedHostUsage> LBMergedHostUsageRecordsForLoadBalancer, Calendar hourToProcess);
}
