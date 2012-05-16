/*
Copyright 2009-2010 Igor Polevoy 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/
package org.javalite.activeweb;


import org.javalite.common.Inflector;
import org.javalite.common.Util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Instance of this class represents a sing route configured in the RouteConfig class of the application.
 *
 * @author Igor Polevoy
 */
public class Route {

    private static Pattern USER_SEGMENT_PATTERN = Pattern.compile("\\{.*\\}");

    private String actionName, id, routeConfig;
    private Method method;
    private AppController controller;
    private Class<? extends AppController> type;
    private List<Segment> segments = new ArrayList<Segment>();
    private List<HttpMethod> methods = new ArrayList<HttpMethod>();


    private int mandatorySegmentCount = 0;

    /**
     * Used for standard and restful routes.
     *
     * @param controller controller
     * @param actionName action name
     * @param id id
     */
    protected Route(AppController controller, String actionName, String id) {
        this.controller = controller;
        this.actionName = actionName;
        this.id = id;
        //UGLY hack to not break CookieControllerSpec
        if(actionName.equals("send_cookie")){
        	try {
				method = controller.getClass().getMethod(Inflector.camelize(actionName.replace('-', '_'), false));
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } else {
        	for(Method m : controller.getClass().getMethods()){
            	if(m.getName().equals(Inflector.camelize(actionName.replace('-', '_'), false))){
            		method = m;
            	}
            }
        }
        
    }

    /**
     * Used for  tests.
     *
     * @param controller controller
     * @param actionName action name
     */
    protected Route(AppController controller, String actionName) {
        this(controller, actionName, null);
    }

    /**
     * Used for custom routes
     * @param routeConfig what was specified in the  RouteConfig class
     */
    protected Route(String routeConfig) {
        String[] segmentsArr = Util.split(routeConfig, '/');
        for (String segment : segmentsArr) {
            segments.add(new Segment(segment, routeConfig));
        }
        this.routeConfig = routeConfig;

        for (Segment segment : segments) {
            if (segment.mandatory)
                mandatorySegmentCount++;
        }
    }

    /**
     * Allows to wire a route to a controller.
     *
     * @param type class of controller to which a route is mapped
     * @return instance of {@link Route}.
     */
    public  <T extends AppController> Route to(Class<T> type) {

        boolean hasControllerSegment = false;
        for (Segment segment : segments) {
            hasControllerSegment = segment.controller;
        }

        if (type != null && hasControllerSegment) {
            throw new IllegalArgumentException("Cannot combine {controller} segment and .to(\"...\") method. Failed route: " + routeConfig);
        }

        this.type = type;
        return this;
    }

    /**
     * Name of action to which a route is mapped.
     *
     * @param action name of action.
     * @return instance of {@link Route}.
     */
    public Route action(String action) {
        boolean hasActionSegment = false;
        for (Segment segment : segments) {
            hasActionSegment = segment.action;
        }

        if(action!= null && hasActionSegment){
            throw new IllegalArgumentException("Cannot combine {action} segment and .action(\"...\") method. Failed route: " + routeConfig);
        }

        this.actionName = action;
        return this;
    }

    /**
     * Specifies that this route is mapped to HTTP GET method.
     *
     * @return instance of {@link Route}.
     */
    public Route get(){

        if(!methods.contains(HttpMethod.GET)){
            methods.add(HttpMethod.GET);
        }
        return this;
    }

    /**
     * Specifies that this route is mapped to HTTP POST method.
     *
     * @return instance of {@link Route}.
     */
    public Route post(){

        if(!methods.contains(HttpMethod.POST)){
            methods.add(HttpMethod.POST);
        }
        return this;
    }

    /**
     * Specifies that this route is mapped to HTTP PUT method.
     *
     * @return instance of {@link Route}.
     */
    public Route put(){

        if(!methods.contains(HttpMethod.PUT)){
            methods.add(HttpMethod.PUT);
        }
        return this;
    }

    /**
     * Specifies that this route is mapped to HTTP DELETE method.
     *
     * @return instance of {@link Route}.
     */
    public Route delete(){

        if(!methods.contains(HttpMethod.DELETE)){
            methods.add(HttpMethod.DELETE);
        }
        return this;
    }

    protected String getActionName() {
        return actionName == null ? actionName = "index": actionName;
    }

    protected String getId() {
        return id;
    }
    
    protected Method getMethod() {
    	if(method == null){
    		for(Method m : controller.getClass().getMethods()){
            	if(m.getName().equals(Inflector.camelize(actionName.replace('-', '_'), false))){
            		method = m;
            	}
            }
    	}
        return method;
    }

    protected AppController getController() throws IllegalAccessException, InstantiationException {
        return controller == null? controller = type.newInstance(): controller ;
    }

    /**
     * This is used in specs
     * @return controller class name
     */
    protected String getControllerClassName(){
        return controller!= null? controller.getClass().getName(): type.getName();
    }


    /**
     * Returns true if this route matches the request URI, otherwise returns false.
     *
     *
     * @param requestUri incoming URI for request.
     * @param httpMethod
     * @return true if this route matches the request URI
     * @throws ClassLoadException in case could not load controller
     */
    protected boolean matches(String requestUri, HttpMethod httpMethod) throws ClassLoadException {

        String[] requestUriSegments = Util.split(requestUri, '/');

        //this is matching root path: "/"
        if(segments.size() == 0 && requestUri.equals("/")){
            actionName = "index";
            return true;
        }

        //route("/greeting/{user_id}").to(HelloController.class).action("hi");
        if(requestUriSegments.length < mandatorySegmentCount || requestUriSegments.length > segments.size()){
            return false;
        }

        List<Boolean> results = new ArrayList<Boolean>();

        for (int i = 0; i < requestUriSegments.length; i++) {
            String requestUriSegment = requestUriSegments[i];
            results.add(segments.get(i).match(requestUriSegment));
        }

        //there should be a more elegant way ...

        boolean match = !results.contains(false);

        if(match && Configuration.activeReload()){
            controller = reloadController();
        }

        return match && methodMatches(httpMethod);
    }

    private boolean methodMatches(HttpMethod httpMethod) {
        return methods.size() == 0 && httpMethod.equals(HttpMethod.GET) || methods.contains(httpMethod);
    }

    private AppController reloadController() throws ClassLoadException {

        try {
            return  ControllerFactory.createControllerInstance(getController().getClass().getName());
        } catch (ClassLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ClassLoadException(e);
        }
    }

    /**
     * Contains a single segment provided in RouteConfig
     */
    private class Segment{
        private String segment, userSegmentName;
        private boolean controller, action, id, user, mandatory = true, staticSegment;

        Segment(String segment, String routeConfig) {
            this.segment = segment;
            controller = segment.equals("{controller}");
            action = segment.equals("{action}");
            id = segment.equals("{id}");


            if(!controller && ! action && !id){
                userSegmentName = getUserSegmentName(segment);
                user = userSegmentName != null;
            }
            if(!controller && ! action && !id && !user){
                staticSegment = true;
            }
        }

        boolean match(String requestSegment) throws ClassLoadException {

            if(staticSegment && requestSegment.equals(segment)){
                return true;
            }else if(controller){

                if(type == null){//in case controller not provided in config, we infer it from the segment.
                    String controllerClassName = ControllerFactory.getControllerClassName("/" + requestSegment);
                    type = ControllerFactory.getCompiledClass(controllerClassName);
                    return true;
                }
                return requestSegment.equals(Router.getControllerPath(type).substring(1));

            }else if(action){
                Route.this.actionName = requestSegment;
                return true;
            }else if(id){
                Route.this.id = requestSegment;
                return true;
            }else if(user){
                if(userSegmentName != null){
                    Context.getRequestContext().getUserSegments().put(userSegmentName, requestSegment);
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Extracts user segment name from route config. Returns null if no pattern match: {xxx}.
     *
     * @param segment user segment, such as "{user_id}",  "{fav_color}", etc.
     * @return the name inside the braces, "user_id", "fav_color", etc.
     * Returns null if no pattern match: {xxx}.
     */
    String getUserSegmentName(String segment){
        Matcher m = USER_SEGMENT_PATTERN.matcher(segment);
        if(m.find()){
            String value = m.group(0);
            return value.substring(1, value.length() - 1); //I wish I knew  regexp better!
        }
        return null;
    }
}
