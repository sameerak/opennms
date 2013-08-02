package org.opennms.features.rest.demo;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.opennms.netmgt.dao.NotificationDao;
import org.opennms.netmgt.model.OnmsNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/notifications")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class NotificationResource {

    private NotificationDao notificationDao;
    private static Logger logger = LoggerFactory.getLogger(NodeResource.class);    

    @GET
    public List<OnmsNotification> getNotifications() {
        return notificationDao.findAll();
    }

    @GET
    @Path("{notificationId}")
    public OnmsNotification getNotificationById(@PathParam("notificationId") final Integer notificationId) {
        return notificationDao.get(notificationId);
    }
    
    /**
     * method to initialize local variable eventDao using blueprint
     * @param eventDao
     */
    public void setNotificationDao(NotificationDao outageDao) {
        this.notificationDao = outageDao;
    }
}
