package com.github.tvbox.osc.presenter.contract;

import androidx.annotation.StringRes;

import com.github.tvbox.osc.base.impl.IPresenter;
import com.github.tvbox.osc.base.impl.IView;
import com.github.tvbox.osc.viewmodel.SourceViewModel;

public interface MainContract {
    interface View extends IView {
        void setTvDate(String date);
        void updateTvName();
        boolean checkPermission(String permission);
        boolean getUseCacheConfig();
        void toast(String msg);
        void toast(@StringRes int resId);
        void showLoading();
        void showEmpty();
        void showSuccess();
        void getHomeSort();
    }

    interface Presenter extends IPresenter {
        void initSpider();
    }
}
