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

import activeweb.controller_filters.ControllerFilter;

/**
 * @author Igor Polevoy
 */
public abstract class AbstractControllerConfig {

    public class FilterBuilder {
        private ControllerFilter[] filters;
        private Class<? extends AppController>[] controllerClasses;

        protected FilterBuilder(ControllerFilter[] filters) {
            this.filters = filters;
        }

        /**
         * Provides a list of controllers to which filters are added.
         *
         * @param controllerClasses list of controller classes to which filters are added.
         * @return self, usually to run a method {@link #forActions(String...)}.
         */
        public FilterBuilder to(Class<? extends AppController>... controllerClasses) {
            this.controllerClasses = controllerClasses;
            for (Class<? extends AppController> controllerClass : controllerClasses) {
                ContextAccess.getControllerRegistry().getMetaData(controllerClass).addFilters(filters);
            }
            return this;
        }

        /**
         * Adds a list of actions for which filters are configured.
         *
         * @param actionNames list of action names for which filters are configured.
         */
        public void forActions(String... actionNames) {
            if (controllerClasses == null)
                throw new IllegalArgumentException("controller classes not provided. Please call 'to(controllers)' before 'forActions(actions)'");

            for (Class<? extends AppController> controllerClass : controllerClasses) {
                ContextAccess.getControllerRegistry().getMetaData(controllerClass).addFilters(filters, actionNames);
            }
        }
    }


    /**
     * Adds a set of filters to a set of controllers.
     * The filters are invoked in the order specified.
     *
     * @param filters filters to be added.
     * @return object with <code>to()</code> method which accepts a controller class. The return type is not important and not used by itself.
     */
    protected FilterBuilder add(ControllerFilter... filters) {
        return new FilterBuilder(filters);
    }

    /**
     * Adds filters to all controllers globally.
     * Example of usage:
     * <pre>
     * ...
     *   addGlobalFilters(new TimingFilter(), new DBConnectionFilter());
     * ...
     * </pre>
     *
     * @param filters filters to be added.
     */
    protected void addGlobalFilters(ControllerFilter... filters) {
        ContextAccess.getControllerRegistry().addGlobalFilters(filters);
    }

    protected abstract void init();
}
