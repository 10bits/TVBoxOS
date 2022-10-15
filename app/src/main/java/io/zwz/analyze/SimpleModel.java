package io.zwz.analyze;

import io.reactivex.Observable;
import io.zwz.analyze.help.AjaxWebView;
import io.zwz.analyze.help.BaseModelImpl;
import io.zwz.analyze.help.ContextHolder;
import io.zwz.analyze.help.OkHttpHelper;
import io.zwz.analyze.help.impl.IHttpGetApi;
import io.zwz.analyze.help.impl.IHttpPostApi;
import retrofit2.Response;

public class SimpleModel extends BaseModelImpl {
    public static Observable<Response<String>> getResponse(AnalyzeUrl analyzeUrl) {
        try {
            if (analyzeUrl.getUseWebView()) {
                final AjaxWebView.AjaxParams params = new AjaxWebView.AjaxParams(ContextHolder.getContext())
                        .requestMethod(analyzeUrl.getRequestMethod())
                        .postData(analyzeUrl.getPostData())
                        .headerMap(analyzeUrl.getHeaderMap());
                switch (analyzeUrl.getRequestMethod()) {
                    case GET:
                        params.url(analyzeUrl.getQueryUrl());
                        break;
                    case POST:
                    case DEFAULT:
                        params.url(analyzeUrl.getUrl());
                        break;
                }
                return ajaxWebView(params);
            }
            switch (analyzeUrl.getRequestMethod()) {
                case POST:
                    return OkHttpHelper.getInstance().createService(analyzeUrl.getHost(), IHttpPostApi.class)
                            .searchBook(analyzeUrl.getPath(),
                                    analyzeUrl.getQueryMap(),
                                    analyzeUrl.getHeaderMap());
                case GET:
                    return OkHttpHelper.getInstance().createService(analyzeUrl.getHost(), IHttpGetApi.class)
                            .searchBook(analyzeUrl.getPath(),
                                    analyzeUrl.getQueryMap(),
                                    analyzeUrl.getHeaderMap());
                default:
                    return OkHttpHelper.getInstance().createService(analyzeUrl.getHost(), IHttpGetApi.class)
                            .getWebContent(analyzeUrl.getPath(),
                                    analyzeUrl.getHeaderMap());
            }
        } catch (Exception e) {
            return Observable.error(e);
        }
    }
}
