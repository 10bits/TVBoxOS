package com.github.tvbox.osc.util;

import java.util.HashMap;

public class JsEngineHelp {
    public static final HashMap<Integer, String> engines;

    static {
        engines = new HashMap<>();
        engines.put(0, "RhinoJS");
        engines.put(1, "QuickJS");
        engines.put(2,"GoJS");
    }
}
