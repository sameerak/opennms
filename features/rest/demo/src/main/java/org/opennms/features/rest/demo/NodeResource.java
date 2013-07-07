package org.opennms.features.rest.demo;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.criteria.Order;
import org.opennms.core.criteria.Alias.JoinType;
import org.opennms.core.criteria.restrictions.Restriction;
import org.opennms.core.criteria.restrictions.Restrictions;
import org.opennms.features.rest.demo.util.QueryDecoder;
import org.opennms.netmgt.dao.NodeDao;
import org.opennms.netmgt.dao.CategoryDao;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsNodeList;

@Path("/nodes")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class NodeResource {

    private NodeDao nodeDao;
    private CategoryDao categoryDao;

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
        OnmsNode result = nodeDao.get(nodeId);
        return result;
    }

    /**
     * method to initialize local variable nodeDao using blueprint
     * @param nodeDao
     */
    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }
    
    /**
     * method to initialize local variable categoryDao using blueprint
     * @param categoryDao
     */
    public void setCategoryDao(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }
    
    /**
     * method to get nodes belonging to several specified categories
     * categories are specified as an array of strings
     * sample URL
     * "http://localhost:8980/opennms/rest2/nodes/categories?q=Servers&q=Routers&q=Switches&q=Production&q=Test&q=Development"
     * 
     * @param categories
     * @return
     */
    @GET
    @Path("/categories")
    public Response getNodesByCategories(@QueryParam("q") List<String> categories){
        if (categories.isEmpty()){
            //400 bad request
            //will be returned with
            //a list of available categories
            try{    //Added for verification purposes
                List<OnmsCategory> result = categoryDao.findAll();
                OnmsCategory[] resultArray = null;
                resultArray = result.toArray(new OnmsCategory[0]);
                return Response.status(Response.Status.BAD_REQUEST).entity(resultArray).build();
            }catch(Exception e){    //Added for verification purposes
                System.out.println(e.getMessage());
            }
        }
        else {
            try{    //Added for verification purposes
                List<OnmsCategory> onmsCategories = new ArrayList<OnmsCategory>();
                for (String category : categories) {
                    onmsCategories.add(categoryDao.findByName(category));
                }
                List<OnmsNode> result = nodeDao.findAllByCategoryList(onmsCategories);
                OnmsNode[] resultArray = new OnmsNode[result.size()];
                return Response.ok().entity(result.toArray(resultArray)).build();
            }catch(Exception e){    //Added for verification purposes
                System.out.println(e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * 
     * "http://localhost:8980/opennms/rest2/nodes/foreignSource/Servers"
     * 
     * @param category
     * @return
     */
    @GET
    @Path("/foreignSource/{foreignSource}")
    public List<OnmsNode> getNodesByForeignSource(@PathParam("foreignSource") String foreignSource){
        if (foreignSource == null){
            //400 bad request
            //OR
            //return all available categories
            System.out.println("NO category");
        }
        if (foreignSource != null){
            try{    //Added for verification purposes
                List<OnmsNode> result = nodeDao.findByForeignSource(foreignSource);
                return result;
            }catch(Exception e){    //Added for verification purposes
                System.out.println(e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * Method to retrieve all nodes belonging to a single category
     * sample URL
     * "http://localhost:8980/opennms/rest2/nodes/categories/Servers"
     * 
     * @param category
     * @return
     */
    @GET
    @Path("/categories/{category}")
    public Response getNodesByCategory(@PathParam("category") String category){
        if (category != null){
            try{    //Added for verification purposes
                OnmsCategory onmsCategory = categoryDao.findByName(category);
                List<OnmsNode> result = nodeDao.findByCategory(onmsCategory);
                OnmsNode[] resultArray = new OnmsNode[result.size()];
                //TODO add validation for 0 matches
                return Response.ok().entity(result.toArray(resultArray)).build();
            }catch(Exception e){    //Added for verification purposes
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    /**
     * quering node data using core.criteria
     * FIQL query is transmitted as a query parameter in the http request
     * 
     * example URLs - 
     * http://localhost:8980/opennms/rest2/nodes/search?_s=type==A
     * http://localhost:8980/opennms/rest2/nodes/search?_s=createTime==2013-01-01
     * http://localhost:8980/opennms/rest2/nodes/search?_s=createTime=gt=2013-06-14T20:41:45;(type==D,lastCapsdPoll=le=2013-12-30T00:00:00)
     * 
     * @param queryString
     * @return
     */
    @GET
    @Path("/search")
//    @Produces(MediaType.TEXT_PLAIN)
    public List<OnmsNode> searchNodes(@QueryParam("_s") String queryString) {
        //return queryString;
        //TODO
        //implement conversion from query string to core.criteria
        //to enable dynamic searching
        //Integrate searching functionality to the main URL 
        //("/nodes?_s=(age=lt=25,age=gt=35);city==London")
        try{    //Added for verification purposes
            Criteria crit = QueryDecoder.CreateCriteria(queryString);
            OnmsNodeList coll = new OnmsNodeList(nodeDao.findMatching(crit));
            return coll;
        }catch(Exception e){    //Added for verification purposes
            System.out.println(e.getMessage());
        }
        
        return null;
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
//            List<OnmsCategory> onmsCategory = categoryDao.findAll();
//            restrictions.add(Restrictions.eq("categories", onmsCategory));
            crit.setRestrictions(restrictions);
            
            OnmsNodeList coll = null;
            
        try{
            coll = new OnmsNodeList(nodeDao.findMatching(crit));
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        
        crit.setLimit(null);
        crit.setOffset(null);
        crit.setOrders(new ArrayList<Order>());

        coll.setTotalCount(nodeDao.countMatching(crit));

        return coll;
    }
}
