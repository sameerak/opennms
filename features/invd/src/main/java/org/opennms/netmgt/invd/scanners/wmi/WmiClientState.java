//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
package org.opennms.netmgt.invd.scanners.wmi;

import org.opennms.protocols.wmi.WmiManager;
import org.opennms.protocols.wmi.IWmiClient;
import org.opennms.protocols.wmi.WmiAgentConfig;
import org.opennms.protocols.wmi.WmiClient;
import org.opennms.protocols.wmi.WmiException;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.WmiPeerFactory;
import org.apache.log4j.Category;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.net.InetAddress;

public class WmiClientState {
    private WmiManager m_manager;
    private IWmiClient m_wmiClient;

    private WmiAgentConfig m_agentConfig;
    private String m_address;
    private HashMap<String, WmiAssetState> m_assetStates = new HashMap<String, WmiAssetState>();

    public WmiClientState(InetAddress address, Map parameters) {
        // TODO allow parameters to override agentConfig.
        m_address = address.getHostAddress();
        m_agentConfig = WmiPeerFactory.getInstance().getAgentConfig(address);
        m_manager = new WmiManager(m_address, m_agentConfig.getUsername(),
						m_agentConfig.getPassword(), m_agentConfig.getDomain());

        try {
            m_wmiClient = new WmiClient(m_address);
        } catch(WmiException e) {
            log().error("Failed to create WMI client: " + e.getMessage(), e);
        }
    }

    public void connect() {
        try {
            m_wmiClient.connect(m_agentConfig.getDomain(), m_agentConfig.getUsername(), m_agentConfig.getPassword());
        } catch(WmiException e) {
            log().error("Failed to connect to host: "+ e.getMessage(), e);
        }
    }

    public String getAddress() {
        return m_address;
    }

    public WmiManager getManager() {
        return m_manager;
    }

    public boolean assetIsAvailable(String assetName) {
        WmiAssetState assetState = m_assetStates.get(assetName);
        if (assetState == null) {
            return false; // If the group availability hasn't been set
            // yet, it's not available.
        }
        return assetState.isAvailable();
    }

    public void setAssetIsAvailable(String assetName, boolean available) {
        WmiAssetState groupState = m_assetStates.get(assetName);
        if (groupState == null) {
            groupState = new WmiAssetState(available);
        }
        groupState.setAvailable(available);
        m_assetStates.put(assetName, groupState);
    }

    public boolean shouldCheckAvailability(String assetName, int recheckInterval) {
        WmiAssetState assetState = m_assetStates.get(assetName);
        if (assetState == null) {
            // If the asset hasn't got a status yet, then it should be
            // checked regardless (and setGroupIsAvailable will
            // be called soon to create the status object)
            return true;
        }
        Date lastchecked = assetState.getLastChecked();
        Date now = new Date();
        return (now.getTime() - lastchecked.getTime() > recheckInterval);
    }
//
//    public void didCheckGroupAvailability(String groupName) {
//        WmiGroupState groupState = m_groupStates.get(groupName);
//        if (groupState == null) {
//            // Probably an error - log it as a warning, and give up
//            log().warn("didCheckGroupAvailability called on a group without state - this is odd");
//            return;
//        }
//        groupState.setLastChecked(new Date());
//    }

    public IWmiClient getWmiClient() {
        return m_wmiClient;
    }

    public void setWmiClient(IWmiClient wmiClient) {
        this.m_wmiClient = wmiClient;
    }

    private ThreadCategory log() {
		return ThreadCategory.getInstance(getClass());
	}
}