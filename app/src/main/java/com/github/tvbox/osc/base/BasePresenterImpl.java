package com.github.tvbox.osc.base;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.github.tvbox.osc.base.impl.IPresenter;
import com.github.tvbox.osc.base.impl.IView;
import com.github.tvbox.osc.base.rxjava.RxExecutors;


public abstract class BasePresenterImpl<T extends IView> implements IPresenter {

    protected T mView;

    @SuppressWarnings(value = "unchecked")
    @Override
    public void attachView(@NonNull IView iView) {
        mView = (T) iView;
    }


    @CallSuper
    @Override
    public void detachView() {
        RxExecutors.setDefault(null);
    }

}
