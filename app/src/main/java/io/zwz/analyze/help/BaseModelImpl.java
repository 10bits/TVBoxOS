package io.zwz.analyze.help;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import retrofit2.Response;

public class BaseModelImpl {


    protected <T> T createService(String url, Class<T> tClass) {
        return OkHttpHelper.getInstance().createService(url, tClass);
    }

    protected <T> T createService(String url, String encode, Class<T> tClass) {
        return OkHttpHelper.getInstance().createService(url, encode, tClass);
    }


    protected Observable<String> ajax(AjaxWebView.AjaxParams params) {
        return Observable.create(emitter -> {
            new AjaxWebView().ajax(params, new AjaxCallback(emitter));
        });
    }

    protected static Observable<Response<String>> ajaxWebView(AjaxWebView.AjaxParams params) {
        return Observable.create(emitter -> {
            new AjaxWebView().ajax(params, new AjaxCallback2(emitter));
        });
    }

    protected Observable<String> sniff(AjaxWebView.AjaxParams params) {
        return Observable.create(emitter -> new AjaxWebView().sniff(params, new AjaxCallback(emitter)));
    }

    public static class AjaxCallback2 extends AjaxWebView.Callback {

        private final ObservableEmitter<Response<String>> emitter;

        private AjaxCallback2(ObservableEmitter<Response<String>> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onResult(String result) {
            if (!emitter.isDisposed()) {
                emitter.onNext(Response.success(result));
                emitter.onComplete();
            }
        }

        @Override
        public void onError(Throwable error) {
            if (!emitter.isDisposed()) {
                emitter.onError(error);
            }
        }
    }

    public static class AjaxCallback extends AjaxWebView.Callback {

        private final ObservableEmitter<String> emitter;

        private AjaxCallback(ObservableEmitter<String> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onResult(String result) {
            if (!emitter.isDisposed()) {
                emitter.onNext(result);
                emitter.onComplete();
            }
        }

        @Override
        public void onError(Throwable error) {
            if (!emitter.isDisposed()) {
                emitter.onError(error);
            }
        }
    }


}