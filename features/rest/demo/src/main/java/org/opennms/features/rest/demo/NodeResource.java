package org.opennms.features.rest.demo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.opennms.features.rest.demo.exception.NotFIQLOperatorException;
import org.opennms.features.rest.demo.util.QueryDecoder;
import org.opennms.netmgt.dao.NodeDao;
import org.opennms.netmgt.dao.CategoryDao;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsNodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/nodes")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class NodeResource extends QueryDecoder{

    private NodeDao nodeDao;
    private CategoryDao categoryDao;
    private static Logger logger = LoggerFactory.getLogger(NodeResource.class);
    
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
    public Response getNode(@PathParam("nodeId") final String nodeId) {
        OnmsNode result = nodeDao.get(nodeId);
        if (result == null) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("Please specify a valid node ID").build();
        }
        return Response.ok().entity(result).build();
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
        try{    //Added for verification purposes
            if (categories.isEmpty()){
                /*
                 * options to consider
                 * - 400 bad request will be returned with a list of available categories - implemented
                 * - 200 oK with a collection of all the nodes available
                 * - 400 bad request with a error message
                 */
                List<OnmsCategory> result = categoryDao.findAll();
                OnmsCategory[] resultArray = null;
                resultArray = result.toArray(new OnmsCategory[0]);
                return Response.status(Response.Status.BAD_REQUEST).entity(resultArray).build();
            }
            else {
                List<OnmsCategory> onmsCategories = new ArrayList<OnmsCategory>();
                for (String category : categories) {
                    onmsCategories.add(categoryDao.findByName(category));
                    if (onmsCategories.get(onmsCategories.size() - 1) == null){ // invalid category specified
                        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).
                                entity("Please specify a set of valid categories").build();
                    }
                }
                List<OnmsNode> result = nodeDao.findAllByCategoryList(onmsCategories);
                if (result.isEmpty()) {                                         //result set is empty
                    return Response.noContent().build();
                }
                OnmsNode[] resultArray = new OnmsNode[result.size()];
                return Response.ok().entity(result.toArray(resultArray)).build();
            }
        }catch(Exception e){    //Added for verification purposes
            logger.error(e.getMessage(), e);
            //TODO refine the http response body
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();       //in case of a unidentified error caused
        }
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
        //{category} == null case is handled by getNodesByCategories method
        //therefore no need to check it in this method
        try{    
            OnmsCategory onmsCategory = categoryDao.findByName(category);
            if (onmsCategory == null){                                      // invalid category specified
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("Please specify a valid category").build();
            }
            List<OnmsNode> result = nodeDao.findByCategory(onmsCategory);
            if (result.isEmpty()) {                                         //result set is empty
                return Response.noContent().build();
            }
            OnmsNode[] resultArray = new OnmsNode[result.size()];
            return Response.ok().entity(result.toArray(resultArray)).build();
        }catch(Exception e){  
            logger.error(e.getMessage(), e);  
            //TODO refine the http response body
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();   //in case of a unidentified error caused
        }
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
                logger.error(e.getMessage(), e);
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
    public Response searchNodes(@QueryParam("_s") String queryString) {
        try{
            Criteria crit = CreateCriteria(queryString);
            OnmsNodeList result = new OnmsNodeList(nodeDao.findMatching(crit));
            if (result.isEmpty()) {         //result set is empty
                return Response.noContent().build();
            }
            return Response.ok().entity(result).build();
        }
        catch(NotFIQLOperatorException e){    //in a case where user has specified an invalid FIQL operator
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();   
        }
        catch(ParseException e){    //in a case where user has provided data in wrong format
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();   
        }
        catch(Exception e){
            System.out.println(e.getMessage());    
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();   //in case of an unidentified error caused
        }
    }
    
    /**
     * implemented abstract method from QueryDecoder class
     * in order to create the appropriate criteria object
     * @throws Exception 
     * 
     */
    public Criteria CreateCriteria(String fiqlQuery) throws Exception{
        final CriteriaBuilder builder = new CriteriaBuilder(OnmsNode.class);
        builder.alias("snmpInterfaces", "snmpInterface", JoinType.LEFT_JOIN);
        builder.alias("ipInterfaces", "ipInterface", JoinType.LEFT_JOIN);
        builder.alias("categories", "category", JoinType.LEFT_JOIN);

        builder.orderBy("label").asc();
        
        final Criteria crit = builder.toCriteria();
        
        final List<Restriction> restrictions = new ArrayList<Restriction>(crit.getRestrictions());
        Restriction restriction = removeBrackets(fiqlQuery);
        restrictions.add(restriction);
        crit.setRestrictions(restrictions);
        
        return crit;
    }
    
    /**
     * implemented abstract method from QueryDecoder class
     * For the given property name respective comparable object is created
     * ex - createTime -> java.util.Date
     * 
     * TODO - extend to provide validations
     * 
     * @param propertyName
     * @param compareValue
     * @return
     * @throws ParseException 
     */
    protected Object getCompareObject(String propertyName, String compareValue) throws ParseException {
        if (propertyName.equals("createTime") || propertyName.equals("lastCapsdPoll")) {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                Date formattedDate = formatter.parse(compareValue);
                return formattedDate;
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new ParseException("Please specify dates in format \"yyyy-MM-dd'T'HH:mm:ss\"", 0);
            }
        }
        return compareValue;
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
//            restrictions.add(Restrictions.ne("type", "D"));
            List<OnmsCategory> onmsCategory = categoryDao.findAll();
            restrictions.add(Restrictions.eq("categories", onmsCategory.get(0)));
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
