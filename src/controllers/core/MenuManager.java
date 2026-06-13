package controllers.core;

import views.core.BaseView;

import java.util.Stack;

public class MenuManager {
    private BaseView currentView;
    private Stack<BaseView> viewHistory;

    public void changeView(BaseView currentView) {}

    public BaseView getCurrentView() {return this.currentView;}

}
