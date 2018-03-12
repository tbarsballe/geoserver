/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

/**
 * Informix implementation.
 *
 * @author Timothy De Bock
 */
public class InformixDialectImpl extends DefaultDialectImpl {

    @Override
    public String quote(String tableName) {
        return tableName;
    }


    /**
     * Override because in a view informix returns the value of the underlying column definition of the table.
     * Even when performing left join in the create view statement.
     *
     * @param nullable
     * @return
     */
    @Override
    public int isNullable(int nullable) {
        return -1;
    }
}
