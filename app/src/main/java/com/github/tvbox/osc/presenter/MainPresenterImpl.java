package com.github.tvbox.osc.presenter;

import android.annotation.SuppressLint;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BasePresenterImpl;
import com.github.tvbox.osc.base.impl.IView;
import com.github.tvbox.osc.base.rxjava.RxExecutors;
import com.github.tvbox.osc.event.SpiderEvent;
import com.github.tvbox.osc.presenter.contract.MainContract;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class MainPresenterImpl extends BasePresenterImpl<MainContract.View> implements MainContract.Presenter {
    private boolean dataInitOk = false;
    private boolean jarInitOk = false;
    private CompositeDisposable disposables = new CompositeDisposable();


    public void loadSpiderConfig() {
        ApiConfig.get().
                loadSpiderConfig(mView.getUseCacheConfig()).
                subscribeOn(RxExecutors.getDefault()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(@NotNull Disposable d) {
                        disposables.add(d);
                    }

                    @Override
                    public void onNext(@NotNull Boolean aBoolean) {
                        EventBus.getDefault().post(new SpiderEvent(SpiderEvent.TYPE_DATA_INIT_OK));
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        EventBus.getDefault().post(new SpiderEvent(SpiderEvent.TYPE_DATA_INIT_OK));
                        mView.toast(e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void loadSpiderJson() {
        ApiConfig.get().
                loadSpiderJar(mView.getUseCacheConfig(), ApiConfig.get().getSpider()).
                subscribeOn(RxExecutors.getDefault()).
                observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(@NotNull Disposable d) {
                disposables.add(d);
            }

            @Override
            public void onNext(@NotNull Boolean aBoolean) {
                EventBus.getDefault().post(new SpiderEvent(SpiderEvent.TYPE_JAR_INIT_OK));
            }

            @Override
            public void onError(@NotNull Throwable e) {
                mView.toast(e.getMessage());
                EventBus.getDefault().post(new SpiderEvent(SpiderEvent.TYPE_JAR_INIT_OK));
            }

            @Override
            public void onComplete() {

            }
        });
    }

    @Override
    public void initSpider() {
        mView.showLoading();
        loadSpiderConfig();
    }

    @Subscribe
    public void onSpiderLoad(SpiderEvent event) {
        if (event.type == SpiderEvent.TYPE_DATA_INIT_OK) {
            dataInitOk = true;
            if (!jarInitOk) {
                loadSpiderJson();
            }
        }
        if (event.type == SpiderEvent.TYPE_JAR_INIT_OK) {
            jarInitOk = true;
            if (dataInitOk) {
                mView.getHomeSort();
            }
        }

    }

    @Override
    public void attachView(@NonNull @NotNull IView iView) {
        EventBus.getDefault().register(this);
        super.attachView(iView);

    }

    @Override
    public void detachView() {
        EventBus.getDefault().unregister(this);
        disposables.dispose();
        super.detachView();
    }
}
