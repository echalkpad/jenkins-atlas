package org.openstack.atlas.api.helpers;

import java.util.ArrayList;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.deser.CustomDeserializerFactory;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.openstack.atlas.api.helpers.JsonDeserializer.DateTimeDeserializer;
import org.openstack.atlas.api.helpers.JsonDeserializer.DeserializerProviderBuilder;
import org.openstack.atlas.api.helpers.JsonDeserializer.ObjectWrapperDeserializer;
import org.openstack.atlas.api.helpers.JsonDeserializer.PropertyListDeserializer;
import org.openstack.atlas.api.helpers.JsonSerializer.DateTimeSerializer;
import org.openstack.atlas.api.helpers.JsonSerializer.ObjectWrapperSerializer;
import org.openstack.atlas.api.helpers.JsonSerializer.PropertyCollectionSerializer;
import org.openstack.atlas.docs.loadbalancers.api.management.v1.Host;
import org.openstack.atlas.docs.loadbalancers.api.management.v1.RateLimit;
import org.openstack.atlas.docs.loadbalancers.api.v1.*;
import org.w3.atom.Link;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import org.openstack.atlas.util.debug.Debug;

/* import org.openstack.atlas.docs.loadbalancers.api.v1.*; STOP splat codeing this. Yes that means you Trevor */
/* import org.openstack.atlas.docs.loadbalancers.api.v1.*; STOP splat codeing this. Yes that means you Trevor */
public class JsonObjectMapper extends ObjectMapper {

    private static String initStackTrace;
    private static List<String> callInfo = new ArrayList<String>();

    public static String getInitStackTrace() {
        return initStackTrace;
    }

    public static List<String> getCallInfo() {
        return callInfo;
    }

    public static String getCallInfoString() {
        StringBuilder sb = new StringBuilder();
        for (String info : callInfo) {
            sb.append(info).append("\n");
        }
        return sb.toString();
    }

    public static void addCallInfo(String msg) {
        callInfo.add(msg);
    }

    public static void resetCallInfo() {
        callInfo = new ArrayList<String>();
    }

    public void init() {
        initStackTrace = Debug.getStackTrace();
        CustomSerializerFactory csf = new CustomSerializerFactory();
        CustomDeserializerFactory cdf = new CustomDeserializerFactory();
        SerializationConfig serConf = this.getSerializationConfig();
        DeserializationConfig deserConf = this.getDeserializationConfig();

        csf.addSpecificMapping(GregorianCalendar.class, new DateTimeSerializer(serConf, null));
        cdf.addSpecificMapping(Calendar.class, new DateTimeDeserializer(Calendar.class));


        Class[] serializerWrapperClasses = new Class[]{HealthMonitor.class,
            SessionPersistence.class, ConnectionLogging.class, ConnectionThrottle.class, Meta.class,
            Node.class, RateLimit.class, Errorpage.class, SslTermination.class, Link.class, AllowedDomain.class, ContentCaching.class};

        Class[] deserializerWrapperClasses = new Class[]{Node.class, HealthMonitor.class,
            SessionPersistence.class, ConnectionLogging.class, Meta.class,
            ConnectionThrottle.class, LoadBalancer.class, NetworkItem.class, RateLimit.class,
            Errorpage.class, SslTermination.class, Host.class, Link.class, AllowedDomain.class, ContentCaching.class};


        for (Class wrapperClass : serializerWrapperClasses) {
            csf.addSpecificMapping(wrapperClass, new ObjectWrapperSerializer(serConf, wrapperClass));
        }


        for (Class wrapperClass : deserializerWrapperClasses) {
            cdf.addSpecificMapping(wrapperClass, new ObjectWrapperDeserializer(wrapperClass));
        }

        cdf.addSpecificMapping(LoadBalancer.class, new ObjectWrapperDeserializer(LoadBalancer.class));
        cdf.addSpecificMapping(VirtualIps.class, new ObjectWrapperDeserializer(VirtualIps.class));
        // Define any collections utilizing the custom serializers above to
        // use the clean collections serializer, which will ensure proper JSON
        // formatting.

        // Load balancer is a bit of a special case since we want loadbalancer
        // wrapped, but none of the collections within loadbalancer.

        csf.addSpecificMapping(LoadBalancer.class, new ObjectWrapperSerializer(this.getSerializationConfig(), LoadBalancer.class));
        csf.addSpecificMapping(LoadBalancers.class, new PropertyCollectionSerializer(serConf, LoadBalancers.class, "getLoadBalancers", true));

        csf.addSpecificMapping(AccessList.class, new PropertyCollectionSerializer(serConf, AccessList.class, "getNetworkItems"));
        csf.addSpecificMapping(Nodes.class, new PropertyCollectionSerializer(serConf, Nodes.class, "getNodes"));
        csf.addSpecificMapping(Metadata.class, new PropertyCollectionSerializer(serConf, Metadata.class, "getMetas"));

        cdf.addSpecificMapping(Metadata.class, new PropertyListDeserializer(Metadata.class, Meta.class, "getMetas"));
        cdf.addSpecificMapping(AccessList.class, new PropertyListDeserializer(AccessList.class, NetworkItem.class, "getNetworkItems"));
        cdf.addSpecificMapping(VirtualIps.class, new PropertyListDeserializer(VirtualIps.class, VirtualIp.class, "getVirtualIps"));


        this.setSerializerFactory(csf);
        this.setDeserializerProvider(new DeserializerProviderBuilder(cdf));
        // Suppress null properties from being serialized.
        this.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false);
        serConf.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }
}
