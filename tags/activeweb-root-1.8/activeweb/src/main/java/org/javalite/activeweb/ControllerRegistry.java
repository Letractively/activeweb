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

import org.javalite.activeweb.controller_filters.ControllerFilter;
import com.google.inject.Injector;

import javax.servlet.FilterConfig;
import java.util.*;

/**
 * Registration facility for {@link ControllerMetaData}.
 *
 * @author Igor Polevoy
 */
class ControllerRegistry {

    /**
     * key - controller class name, value ControllerMetaData.
     */
    private Map<String, ControllerMetaData> metaDataMap = new HashMap<String, ControllerMetaData>();
    private List<FilterList> globalFilterLists = new ArrayList<FilterList>();
    private Injector injector;

    // these are not full package names, just partial package names between "app.controllers"
    // and simple name of controller class
    private List<String> controllerPackages;

    private final Object token = new Object();

    private boolean filtersInjected = false;


    protected ControllerRegistry(FilterConfig config) {
        controllerPackages = ControllerPackageLocator.locateControllerPackages(config);
    }


    /**
     * Returns controller metadata for a class.
     *
     * @param controllerClass controller class.
     * @return controller metadata for a controller class.
     */
    protected ControllerMetaData getMetaData(Class<? extends AppController> controllerClass) {
        if (metaDataMap.get(controllerClass.getName()) == null) {
            metaDataMap.put(controllerClass.getName(), new ControllerMetaData());
        }
        return metaDataMap.get(controllerClass.getName());
    }

    protected void addGlobalFilters(ControllerFilter... filters) {
        globalFilterLists.add(new FilterList(Arrays.asList(filters), new ArrayList()));
    }

    protected void addGlobalFilters(List<ControllerFilter> filters, List<Class<? extends AppController>> excludeControllerClasses) {
        globalFilterLists.add(new FilterList(filters, excludeControllerClasses));
    }

    protected List<FilterList> getGlobalFilterLists() {
        return Collections.unmodifiableList(globalFilterLists);
    }

    protected void setInjector(Injector injector) {
        this.injector = injector;
    }

    protected Injector getInjector() {
        return injector;
    }


    protected void injectFilters() {

        if (!filtersInjected) {
            synchronized (token) {
                if (injector != null) {
                    //inject global filters:
                    for (FilterList filterList : globalFilterLists) {
                        List<ControllerFilter> filters = filterList.getFilters();
                        for (ControllerFilter controllerFilter : filters) {
                            injector.injectMembers(controllerFilter);
                        }
                    }
                    //inject specific controller filters:
                    for (String key : metaDataMap.keySet()) {
                        metaDataMap.get(key).injectFilters(injector);
                    }
                }
                filtersInjected = true;
            }
        }
    }

    protected List<String> getControllerPackages() {
        return controllerPackages;
    }

    // instance contains a list of filters and corresponding  list of controllers for which these filters
    // need to be excluded.
    class FilterList<T extends AppController>{
        private List<ControllerFilter> filters = new ArrayList<ControllerFilter>();
        private List<Class<T>> excludedControllers = new ArrayList<Class<T>>();

        FilterList(List<ControllerFilter> filters, List<Class<T>> excludedControllers) {
            this.filters = filters;
            this.excludedControllers = excludedControllers;
        }

        public List<ControllerFilter> getFilters() {
            return Collections.unmodifiableList(filters);
        }

        public boolean excludesController(AppController controller) {

            for (Class<T> clazz : excludedControllers) {
                //must use string here, because when controller re-compiles, class instance is different
                if(clazz.getName().equals(controller.getClass().getName()))
                    return true;
            }
            return false;
        }
    }
}
