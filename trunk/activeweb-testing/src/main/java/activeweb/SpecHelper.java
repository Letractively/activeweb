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
package activeweb;

import activeweb.freemarker.FreeMarkerTag;
import activeweb.freemarker.FreeMarkerTemplateManager;
import app.controllers.SimpleController;
import com.google.inject.Injector;
import javalite.test.jspec.JSpecSupport;
import javalite.test.jspec.TestException;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.mock.web.MockFilterConfig;

/**
 * This class is not used directly in applications.
 * 
 * @author Igor Polevoy
 */
public class SpecHelper extends JSpecSupport{

    private MockHttpSession session;

    protected HttpSession session(){
        return session;
    }

    @Before
    public void atStart() {
        session = new MockHttpSession();

        MatchedRoute route = new MatchedRoute(new SimpleController(), "index");
        ContextAccess.setTLs(null, new MockHttpServletResponse(), new MockFilterConfig(), new ControllerRegistry(new MockFilterConfig()), new AppContext());

        ContextAccess.setRoute(route);
        setTemplateLocation("src/main/webapp/WEB-INF/views");//default location of all views
    }

    @After
    public void afterEnd(){
        ContextAccess.clear();
    }

    /**
     * @param location this is a relative location starting from the module root, intended for testing. 
     */
    protected void setTemplateLocation(String location){        
        activeweb.Configuration.getTemplateManager().setTemplateLocation(location);
    }

    /**
     * Provides status code set on response by controller
     *
     * @return  status code set on response by controller
     */
    protected int statusCode(){
        return ContextAccess.getControllerResponse().getStatus();
    }

    /**
     * Provides content type set on response by controller
     *
     * @return  content type set on response by controller
     */
    protected String contentType(){
        return ContextAccess.getControllerResponse().getContentType();
    }

    /**
     * Provides content generated by controller after controller execution - if views were integrated.
     * 
     * @return content generated by controller/view
     */
    protected String responseContent(){
        try{
            return ((MockHttpServletResponse)ContextAccess.getHttpResponse()).getContentAsString();
        }
        catch(Exception e){
            throw new SpecException(e);
        }
    }

    /**
     * Provides
     *
     * @return
     */
    protected byte[] bytesContent(){
        try{
            return ((MockHttpServletResponse)ContextAccess.getHttpResponse()).getContentAsByteArray();
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Provides layout set after executing an action of a controller.
     *
     * @return  layout set after executing an action of a controller.
     */
    protected String layout(){
        ControllerResponse resp = ContextAccess.getControllerResponse();
        try{
            if(!(resp instanceof RenderTemplateResponse))
                throw new SpecException("failed to get layout, did you perform a render operation? I found a different " +
                        " response: " + resp.getClass());
            return ((RenderTemplateResponse)resp).getLayout();
        }catch(ClassCastException e){
            return null;
        }
    }

    /**
     * Provides values assigned by controller during execution. These values will
     * 
     * be forwarded to a view during normal processing.
     *
     * @return values assigned by controller during execution
     */
    protected Map assigns(){
        if(ContextAccess.getControllerResponse() == null){
            throw new TestException("There is no controller response, did you actually invoke a controller/action?");
        }
        return ContextAccess.getControllerResponse().values();
    }

    /**
     * Returns true after execution of an action that sent a redirect.
     * @return true after execution of an action that sent a redirect, false otherwise.
     */
    protected boolean redirected(){
        return ContextAccess.getControllerResponse() instanceof RedirectResponse;
    }

    /**
     * Returns a redirect value if one was produced by a controller or filter, null if not.
     *
     * @return a redirect value if one was produced by a controller or filter, null if not.
     */
    protected String redirectValue(){
        ControllerResponse resp = ContextAccess.getControllerResponse();
        if(resp != null && resp instanceof RedirectResponse){
            RedirectResponse redirectResponse = (RedirectResponse)resp;
            return redirectResponse.redirectValue();
        }
        return null;
    }

    /**
     * Returns all cookies from last response. Use in test validations.
     *
     * @return all cookies from last response.
     */
    protected Cookie[] getCookies(){
        if(ContextAccess.getHttpResponse() == null) throw new IllegalStateException("response does not exist");
        javax.servlet.http.Cookie[] servletCookies = ((MockHttpServletResponse)ContextAccess.getHttpResponse()).getCookies();
        List<Cookie> cookies = new ArrayList<Cookie>();
        for(javax.servlet.http.Cookie cookie: servletCookies){
            cookies.add(Cookie.fromServletCookie(cookie));
        }
        return cookies.toArray(new Cookie[0]);
    }

    /**
     * Returns a cookie from last response by name, <code>null</code> if not found.
     * @param name name of cookie.
     * @return a cookie from last response by name, <code>null</code> if not found.
     */
    protected Cookie cookie(String name){
        Cookie[] cookies = getCookies();
        for(Cookie cookie: cookies){
            if(cookie.getName().equals(name)){
                return cookie;
            }
        }
        return null;        
    }

    /**
     * Convenience method, returns cookie value.
     *
     * @param name name of cookie.
     * @return cookie value.
     */
    protected String cookieValue(String name){
        return cookie(name).getValue();
    }

    protected void setInjector(Injector injector){
        ContextAccess.getControllerRegistry().setInjector(injector);
    }

    /**
     * Registers a single custom tag. You can call this method as many times as necessary to register multiple tags in tests.
     * If you want to use all tags that you registered in <code>app.config.AppBootstrap</code> class, then you an
     * option of using <code>AppIntegrationSpec</code> as a super class.
     *
     * @param name tag name where name is a part of the tag on page like so: <code><@name...</code>.
     * @param tag instance of tag to register.
     */
    protected void registerTag(String name, FreeMarkerTag tag){
        ((FreeMarkerTemplateManager)Configuration.getTemplateManager()).registerTag(name, tag);
    }


    /**
     * Returns a named flash value assigned to session by controller.
     *
     * @param name name of flash value.
     * @return flash value assigned to session by controller.
     */
    protected String flash(String name){
        if(session().getAttribute("flasher") == null)
            return null;

        Map flasher = (Map) session().getAttribute("flasher");
        return flasher.get(name) == null? null :flasher.get(name).toString();
    }
}
