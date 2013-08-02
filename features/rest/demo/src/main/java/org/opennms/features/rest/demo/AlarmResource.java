package org.opennms.features.rest.demo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.opennms.features.rest.demo.exception.NotFIQLOperatorException;
import org.opennms.features.rest.demo.util.QueryDecoder;
import org.opennms.netmgt.dao.AlarmDao;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsAlarmCollection;
import org.opennms.netmgt.model.OnmsSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateQueryException;

@Path("/alarms")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class AlarmResource {

    private AlarmDao alarmDao;
    private static Logger logger = LoggerFactory.getLogger(NodeResource.class);    

    @GET
    public List<OnmsAlarm> getAlarms() {
        return alarmDao.findAll();
    }

    @GET
    @Path("{alarmId}")
    public OnmsAlarm getAlarmById(@PathParam("alarmId") final Integer alarmId) {
        return alarmDao.get(alarmId);
    }

    @GET
    @Path("/search")
    public Response searchAlarms(@QueryParam("_s") String queryString) {
        try{
            QueryDecoder aqd = new AlarmQueryDecoder();
            Criteria crit = aqd.FIQLtoCriteria(queryString);
            OnmsAlarmCollection result = new OnmsAlarmCollection(alarmDao.findMatching(crit));
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
        catch(HibernateQueryException e){    //in a case where user has requested a non existing data type
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();   
        }
        catch(Exception e){
            logger.error(e.getMessage(), e);    
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();   //in case of an unidentified error caused
        }
    }

    /**
     * method to initialize local variable alarmDao using blueprint
     * @param alarmDao
     */
    public void setAlarmDao(AlarmDao alarmDao) {
        this.alarmDao = alarmDao;
    }
    
    /**
     * inner class to do the query decoding part of the search function 
     *
     */
    private class AlarmQueryDecoder extends QueryDecoder { //start of inner class
        
        /**
         * implemented abstract method from QueryDecoder class
         * in order to create the appropriate criteria object
         * 
         */
        protected Criteria CreateCriteria(){
            final CriteriaBuilder builder = new CriteriaBuilder(OnmsAlarm.class);
            
            builder.orderBy("lastEventTime").desc();
            builder.orderBy("id").desc();
            
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
            if (propertyName.equals("firstEventTime") || propertyName.equals("lastEventTime") || propertyName.equals("firstAutomationTime")
                    || propertyName.equals("suppressedUntil") || propertyName.equals("suppressedTime") || propertyName.equals("alarmAckTime")) {
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                try {
                    Date formattedDate = formatter.parse(compareValue);
                    return formattedDate;
                } catch (ParseException e) {
                    e.printStackTrace();
                    throw new ParseException("Please specify dates in format \"yyyy-MM-dd'T'HH:mm:ss\"", 0);
                }
            }
            else if (propertyName.equals("id") || propertyName.equals("ifIndex") || propertyName.equals("counter")) {
                return Integer.parseInt(compareValue);
            }
            else if (propertyName.equals("severity")) {
                return OnmsSeverity.get(Integer.parseInt(compareValue));
            }
            return compareValue;
        }
    }//end of inner class
    
}
