package com.github.tvbox.osc.util.js;

import android.content.Context;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.quickjs.JSArray;
import com.github.tvbox.quickjs.JSModule;
import com.github.tvbox.quickjs.JSObject;
import com.orhanobut.hawk.Hawk;
import com.script.ScriptException;
import com.script.SimpleBindings;
import com.script.javascript.RhinoScriptEngine;

import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import catvod.Catvod;
import catvod.GoSpider;
import io.zwz.analyze.AnalyzeRule;
import io.zwz.analyze.utils.GsonExtensionsKt;

public class SpiderJS extends Spider {

    private String key;
    private String js;
    private String ext;
    private JSObject jsObject = null;
    private int engine;
    private JSEngine.JSThread jsThread = null;
    private NativeFunction initFunc = null;
    private NativeFunction homeFunc = null;
    private NativeFunction homeVodFunc = null;
    private NativeFunction categoryFunc = null;
    private NativeFunction detailFunc = null;
    private NativeFunction playFunc = null;
    private NativeFunction searchFunc = null;
    private GoSpider goSpider = null;

    public SpiderJS(String key, String js, String ext) {
        this.key = key;
        this.js = js;
        this.ext = ext;
    }

    void checkLoaderJS() {

        if (jsThread == null) {
            jsThread = JSEngine.getInstance().getJSThread();
        }
        if (jsObject == null && jsThread != null) {
            try {
                jsThread.postVoid((ctx, globalThis) -> {
                    String moduleKey = "__" + UUID.randomUUID().toString().replace("-", "") + "__";
                    String jsContent = JSEngine.getInstance().loadModule(js);
                    try {
                        if (js.contains(".js?")) {
                            int spIdx = js.indexOf(".js?");
                            String[] query = js.substring(spIdx + 4).split("&|=");
                            js = js.substring(0, spIdx);
                            for (int i = 0; i < query.length; i += 2) {
                                String key = query[i];
                                String val = query[i + 1];
                                String sub = JSModule.convertModuleName(js, val);
                                String content = JSEngine.getInstance().loadModule(sub);
                                jsContent = jsContent.replace("__" + key.toUpperCase() + "__", content);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    engine = Hawk.get(HawkConfig.JS_ENGINE, 0);
                    if (engine == 2 || jsContent.contains("__GO_SPIDER__")) {
                        engine = 2;
                        try {
                            goSpider = Catvod.newGoSpider(jsContent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        goSpider.init(ext);
                        return null;
                    }
                    jsContent = jsContent.replace("__JS_SPIDER__", "globalThis." + moduleKey);
                    //use quickJs
                    if (engine == 1) {
                        ctx.evaluateModule(jsContent, js);
                        jsObject = (JSObject) ctx.getProperty(globalThis, moduleKey);
                        jsObject.getJSFunction("init").call(ext);
                        return null;
                    }
                    //use rhinoJs
                    SimpleBindings bindings = new SimpleBindings();
                    bindings.put("globalThis", new HashMap<String, Object>());
                    bindings.put("java", new AnalyzeRule().setContent(""));

                    RhinoScriptEngine engine = new RhinoScriptEngine();
                    try {
                        engine.eval(jsContent, bindings);
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }
                    if (bindings.get("globalThis") != null) {
                        HashMap<String, Object> globalFuncs = (HashMap<String, Object>) bindings.get("globalThis");
                        Object modules = globalFuncs.get(moduleKey);
                        if (modules instanceof NativeObject) {
                            NativeObject moduleFuncs = (NativeObject) modules;
                            if (moduleFuncs.get("init") instanceof NativeFunction) {
                                initFunc = (NativeFunction) moduleFuncs.get("init");
                            }
                            if (moduleFuncs.get("home") instanceof NativeFunction) {
                                homeFunc = (NativeFunction) moduleFuncs.get("home");
                            }
                            if (moduleFuncs.get("homeVod") instanceof NativeFunction) {
                                homeVodFunc = (NativeFunction) moduleFuncs.get("homeVod");
                            }
                            if (moduleFuncs.get("category") instanceof NativeFunction) {
                                categoryFunc = (NativeFunction) moduleFuncs.get("category");
                            }
                            if (moduleFuncs.get("detail") instanceof NativeFunction) {
                                detailFunc = (NativeFunction) moduleFuncs.get("detail");
                            }
                            if (moduleFuncs.get("play") instanceof NativeFunction) {
                                playFunc = (NativeFunction) moduleFuncs.get("play");
                            }
                            if (moduleFuncs.get("search") instanceof NativeFunction) {
                                searchFunc = (NativeFunction) moduleFuncs.get("search");
                            }
                        }
                    }

                    return null;
                });
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    static org.mozilla.javascript.Context RhinoContext() {
        org.mozilla.javascript.Context ctx = ContextFactory.getGlobal().enterContext();
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);
        return ctx;
    }

    String postFunc(String func, Object... args) {
        checkLoaderJS();
        if (jsObject != null) {
            try {
                return jsThread.post((ctx, globalThis) -> (String) jsObject.getJSFunction(func).call(args));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return "";
    }

    @Override
    public void init(Context context, String extend) {
        super.init(context, extend);
        checkLoaderJS();
        if (engine == 0 && initFunc != null) {
            Scriptable scope = initFunc.getPrototype();
            initFunc.call(RhinoContext(), scope, scope, new Object[]{extend});
        }
    }

    @Override
    public String homeContent(boolean filter) {
        if (engine == 1) {
            return postFunc("home", filter);
        } else if (engine == 2) {
            if (goSpider != null) {
                return goSpider.home(filter);
            }
            return "";
        }
        if (homeFunc != null) {
            Scriptable scope = homeFunc.getPrototype();
            return homeFunc.call(RhinoContext(), scope, scope, new Object[]{filter}).toString();
        }
        return "";
    }

    @Override
    public String homeVideoContent() {
        if (engine == 1) {
            return postFunc("homeVod");
        } else if (engine == 2) {
            if (goSpider != null) {
                return goSpider.homeVod();
            }
            return "";
        }
        if (homeVodFunc != null) {
            Scriptable scope = homeVodFunc.getPrototype();
            return homeVodFunc.call(RhinoContext(), scope, scope, new Object[]{}).toString();
        }
        return "";
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        if (engine == 0) {
            if (categoryFunc != null) {
                Scriptable scope = categoryFunc.getPrototype();
                return categoryFunc.call(RhinoContext(), scope, scope, new Object[]{tid, pg, filter, extend}).toString();
            }
            return "";
        } else if (engine == 2) {
            if (goSpider != null) {
                return goSpider.category(tid, pg, filter, GsonExtensionsKt.getGSON().toJson(extend));
            }
            return "";
        }
        try {
            JSObject obj = jsThread.post((ctx, globalThis) -> {
                JSObject o = ctx.createNewJSObject();
                if (extend != null) {
                    for (String s : extend.keySet()) {
                        o.setProperty(s, extend.get(s));
                    }
                }
                return o;
            });
            return postFunc("category", tid, pg, filter, obj);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return "";

    }

    @Override
    public String detailContent(List<String> ids) {
        if (engine == 1) {
            return postFunc("detail", ids.get(0));
        } else if (engine == 2) {
            if (goSpider != null) {
                return goSpider.detail(ids.get(0));
            }
            return "";
        }
        if (detailFunc != null) {
            Scriptable scope = detailFunc.getPrototype();
            return detailFunc.call(RhinoContext(), scope, scope, new Object[]{ids.get(0)}).toString();
        }
        return "";
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        if (engine == 0) {
            if (playFunc != null) {
                Scriptable scope = playFunc.getPrototype();
                String res = playFunc.call(RhinoContext(), scope, scope, new Object[]{flag, id, vipFlags}).toString();
                return res;
            }
            return "";
        } else if (engine == 2) {
            if (goSpider != null) {
                return goSpider.play(flag, id, GsonExtensionsKt.getGSON().toJson(vipFlags));
            }
            return "";
        }
        try {
            JSArray array = jsThread.post((ctx, globalThis) -> {
                JSArray arr = ctx.createNewJSArray();
                if (vipFlags != null) {
                    for (int i = 0; i < vipFlags.size(); i++) {
                        arr.set(vipFlags.get(i), i);
                    }
                }
                return arr;
            });
            return postFunc("play", flag, id, array);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return "";
    }

    @Override
    public String searchContent(String key, boolean quick) {
        if (engine == 1) {
            return postFunc("search", key, quick);
        } else if (engine == 2) {
            if (goSpider != null) {
                return goSpider.search(key, quick);
            }
            return "";
        }

        if (searchFunc != null) {
            Scriptable scope = searchFunc.getPrototype();
            return searchFunc.call(RhinoContext(), scope, scope, new Object[]{key, quick}).toString();
        }
        return "";
    }
}
