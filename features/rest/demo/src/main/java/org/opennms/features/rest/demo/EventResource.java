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
import org.opennms.core.criteria.restrictions.Restriction;
import org.opennms.features.rest.demo.exception.NotFIQLOperatorException;
import org.opennms.features.rest.demo.util.QueryDecoder;
import org.opennms.netmgt.dao.EventDao;
import org.opennms.netmgt.dao.NodeDao;

import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsEventCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/events")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class EventResource {

    private NodeDao nodeDao;
    private EventDao eventDao;
    private static Logger logger = LoggerFactory.getLogger(NodeResource.class);    

    @GET
    public List<OnmsEvent> getEvents() {
        return eventDao.findAll();
    }

    @GET
    @Path("{eventId}")
    public OnmsEvent getEventById(@PathParam("eventId") final Integer eventId) {
        return eventDao.get(eventId);
    }

    @GET
    @Path("/search")
    public Response searchNodes(@QueryParam("_s") String queryString) {
        try{
            QueryDecoder eqd = new EventQueryDecoder();
            Criteria crit = eqd.FIQLtoCriteria(queryString);
            OnmsEventCollection result = new OnmsEventCollection(eventDao.findMatching(crit));
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
            logger.error(e.getMessage(), e);    
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();   //in case of an unidentified error caused
        }
    }

    /**
     * method to initialize local variable nodeDao using blueprint
     * @param nodeDao
     */
    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }

    /**
     * method to initialize local variable eventDao using blueprint
     * @param eventDao
     */
    public void setEventDao(EventDao eventDao) {
        this.eventDao = eventDao;
    }
    
    /**
     * inner class to do the query decoding part of the search function 
     *
     */
    private class EventQueryDecoder extends QueryDecoder { //start of inner class
        
        /**
         * implemented abstract method from QueryDecoder class
         * in order to create the appropriate criteria object
         * 
         */
        protected Criteria CreateCriteria(){
            final CriteriaBuilder builder = new CriteriaBuilder(OnmsEvent.class);
            
            builder.orderBy("eventTime").asc();
            
            final Criteria crit = builder.toCriteria();
            
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
            if (propertyName.equals("eventCreateTime") || propertyName.equals("eventTime") || propertyName.equals("eventAckTime")) {
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                try {
                    Date formattedDate = formatter.parse(compareValue);
                    return formattedDate;
                } catch (ParseException e) {
                    e.printStackTrace();
                    throw new ParseException("Please specify dates in format \"yyyy-MM-dd'T'HH:mm:ss\"", 0);
                }
            }
            else if (propertyName.equals("eventId") || propertyName.equals("eventSeverity")) {
                return Integer.parseInt(compareValue);
            }
            return compareValue;
        }
    }//end of inner class
    
}
