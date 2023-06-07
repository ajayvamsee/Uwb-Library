package com.example.uwb.tools;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 04:18
 */
public interface IBaseObservableView<ListenerType> extends IBaseView{

    void registerListener (ListenerType listener);
    void unregisterListener (ListenerType listener);

}
