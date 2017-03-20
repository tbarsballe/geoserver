package org.geoserver.restng;

import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.PageInfo;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by tbarsballe on 2017-03-21.
 */
public class RESTInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // http://host:port/appName
        //TODO: Port PathInfo to be more compatible with Spring MVC (this may require rewriting ftl templates)
        String baseURL = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
        String rootPath= request.getServletPath(); //= request.getRootRef().toString().substring(baseURL.length());
        String pagePath = request.getServletPath()+request.getPathInfo(); //= request.getResourceRef().toString().substring(baseURL.length());
        String basePath = request.getServletPath();
        //if ( request.getResourceRef().getBaseRef() != null ) {
        //    basePath = request.getResourceRef().getBaseRef().toString().substring(baseURL.length());
        //}

        //strip off the extension
        String extension = ResponseUtils.getExtension(pagePath);
        if ( extension != null ) {
            pagePath = pagePath.substring(0, pagePath.length() - extension.length() - 1);
        }

        //trim leading slash
        if ( pagePath.endsWith( "/" ) ) {
            pagePath = pagePath.substring(0, pagePath.length()-1);
        }
        //create a page info object and put it into a request attribute
        PageInfo pageInfo = new PageInfo();
        pageInfo.setBaseURL(baseURL);
        pageInfo.setRootPath(rootPath);
        pageInfo.setBasePath(basePath);
        pageInfo.setPagePath(pagePath);
        pageInfo.setExtension( extension );
        RequestContextHolder.getRequestAttributes().setAttribute( PageInfo.KEY, pageInfo, RequestAttributes.SCOPE_REQUEST );

        return true;
    }
}
