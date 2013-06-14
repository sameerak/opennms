package org.opennms.features.rest.demo;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.criteria.Order;
import org.opennms.core.criteria.Alias.JoinType;
import org.opennms.core.criteria.restrictions.Restriction;
import org.opennms.core.criteria.restrictions.Restrictions;
import org.opennms.netmgt.dao.NodeDao;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsNodeList;

@Path("/nodes")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class NodeResource {

    private NodeDao nodeDao;

    /**
     * get a list of all the nodes present in the system
     * @return List<OnmsNode>
     */
    @GET
    public List<OnmsNode> getNodes() {
        return nodeDao.findAll();
    }

    /**
     * get a specified node's details
     * @param nodeId
     * @return OnmsNode
     */
    @GET
    @Path("{nodeId}")
    public OnmsNode getNode(@PathParam("nodeId") final String nodeId) {
        return nodeDao.get(nodeId);
    }

    /**
     * method to initialize local variable nodeDao using blueprint
     * @param nodeDao
     */
    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }

    /**
     * method to test the criteria for node searching
     * based on the NodeRestService of old REST API
     * @return List<OnmsNode>
     */
    @GET
    @Path("/test")
    public List<OnmsNode> testSearch() {
        final CriteriaBuilder builder = new CriteriaBuilder(OnmsNode.class);
        builder.alias("snmpInterfaces", "snmpInterface", JoinType.LEFT_JOIN);
        builder.alias("ipInterfaces", "ipInterface", JoinType.LEFT_JOIN);
        builder.alias("categories", "category", JoinType.LEFT_JOIN);

        builder.orderBy("label").asc();
        
        final Criteria crit = builder.toCriteria();

        final List<Restriction> restrictions = new ArrayList<Restriction>(crit.getRestrictions());
            restrictions.add(Restrictions.ne("type", "D"));
            crit.setRestrictions(restrictions);
        
        final OnmsNodeList coll = new OnmsNodeList(nodeDao.findMatching(crit));
        
        crit.setLimit(null);
        crit.setOffset(null);
        crit.setOrders(new ArrayList<Order>());

        coll.setTotalCount(nodeDao.countMatching(crit));

        return coll;
    }
}
