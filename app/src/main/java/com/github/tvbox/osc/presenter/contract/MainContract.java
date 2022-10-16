package com.github.tvbox.osc.presenter.contract;

import com.github.tvbox.osc.base.impl.IPresenter;
import com.github.tvbox.osc.base.impl.IView;

public interface MainContract {
    interface View extends IView {
        void toast(String msg);
    }
    interface Presenter extends IPresenter {
        void clearCache();
    }
}
