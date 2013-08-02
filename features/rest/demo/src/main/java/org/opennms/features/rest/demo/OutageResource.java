package org.opennms.features.rest.demo;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.opennms.netmgt.dao.OutageDao;
import org.opennms.netmgt.model.OnmsOutage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/outages")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class OutageResource {
    
    private OutageDao outageDao;
    private static Logger logger = LoggerFactory.getLogger(NodeResource.class);    

    @GET
    public List<OnmsOutage> getOutages() {
        return outageDao.findAll();
    }

    @GET
    @Path("{outageId}")
    public OnmsOutage getOutageById(@PathParam("outageId") final Integer outageId) {
        return outageDao.get(outageId);
    }
    
    /**
     * method to initialize local variable eventDao using blueprint
     * @param eventDao
     */
    public void setOutageDao(OutageDao outageDao) {
        this.outageDao = outageDao;
    }
}
