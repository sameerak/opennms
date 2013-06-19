package org.opennms.features.rest.demo;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.opennms.netmgt.dao.NodeDao;
import org.opennms.netmgt.model.OnmsNode;

@Path("/nodes")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class NodeResource {

    private NodeDao nodeDao;
    @Context
    private SearchContext context;
    
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

    @GET
    @Path("/search")
    public List<OnmsNode> searchNodes(@QueryParam("_s") String value) {
        try {
            SearchCondition<OnmsNode> sc = context.getCondition(OnmsNode.class);
            // SearchCondition#isMet method can also be used to build a list of matching beans
        } catch (Exception e) {
            System.out.println(e.getMessage()); //for debugging purpose
        }
        
        List<OnmsNode> found = nodeDao.findAll();
        return found;
    }
    
}
