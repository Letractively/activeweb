package org.javalite.activeweb;

/**
 *  Instance of this class will contain routing information.
 *
 * @author Igor Polevoy: 1/8/13 4:21 PM
 */
public class Route {

    private AppController controller;
    private String actionName, id;

    public Route(AppController controller, String actionName) {
        this.controller = controller;
        this.actionName = actionName;
    }

    public Route(AppController controller, String actionName, String id) {
        this.controller = controller;
        this.actionName = actionName;
        this.id = id;
    }

    public Route(RouteBuilder builder) {
        controller = builder.getController();
        actionName = builder.getActionName();
        id = builder.getId();
    }

    public Route(AppController controller) {
        this.controller = controller;
    }

    public AppController getController() {
        return controller;
    }

    public String getActionName() {
        return actionName;
    }

    public String getId() {
        return id;
    }

    protected String getControllerPath(){
        return Router.getControllerPath(controller.getClass());
    }

    /**
     * This is used in specs
     *
     * @return controller class name
     */
    protected String getControllerClassName() {
        return controller.getClass().getName();//controller != null ? controller.getClass().getName() : type.getName();
    }
}
