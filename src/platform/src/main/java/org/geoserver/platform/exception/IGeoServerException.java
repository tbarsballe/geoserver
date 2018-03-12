/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.exception;

/**
 * Interface class for exceptions whose messages can be localized.
 *
 * @author Justin Deoliveira, OpenGeo
 * @see GeoServerException
 * @see GeoServerRuntimException
 */
public interface IGeoServerException {

    /**
     * Id for the exception, used to locate localized message for the exception.
     */
    String getId();

    /**
     * Arguments to pass into the localized exception message
     */
    Object[] getArgs();
}
