/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2011 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/


package org.opennms.dashboard.client.portlet;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * <p>Abstract SurveillanceSet class.</p>
 *
 * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 * @version $Id: $
 * @since 1.8.1
 */
public abstract class SurveillanceSet implements IsSerializable {

    /** Constant <code>DEFAULT</code> */
    public static final SurveillanceSet DEFAULT = new DefaultSurveillanceSet();

    /**
     * <p>isDefault</p>
     *
     * @return a boolean.
     */
    public boolean isDefault() { return false; }
    
    /**
     * <p>visit</p>
     *
     * @param v a {@link org.opennms.dashboard.client.Visitor} object.
     */
    public abstract void visit(Visitor v);
    
    public static class DefaultSurveillanceSet extends SurveillanceSet {
        
        public boolean isDefault() { return true; }
        
        public String toString() {
            return "All Surveillance Nodes";
        }
        
        public void visit(Visitor v) {
            v.visitAll();
        }
    }
    
}