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
package activeweb.freemarker;

import activeweb.RequestSpec;
import activeweb.ViewException;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static javalite.common.Collections.map;

/**
 * @author Igor Polevoy
 */
public class DebugTagSpec extends RequestSpec {

    FreeMarkerTemplateManager manager = new FreeMarkerTemplateManager();
    StringWriter sw = new StringWriter();

    @Before
    public void before() throws IOException, ServletException {
        super.before();
        manager.setTemplateLocation("src/test/views");
    }

    @Test
    public void shouldPrintDebugInformationForMap() {
        sw = new StringWriter();
        manager.merge(map("context_path", "/bookstore", "activeweb", map("controller", "simple", "restful", false)),
                "/debug/debug", sw);
        a(sw.toString()).shouldBeEqual("{restful=false, controller=simple}");
    }
}
