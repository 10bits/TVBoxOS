package com.github.tvbox.osc.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.squareup.picasso.Transformation;

public class BlurTransformation implements Transformation {

    RenderScript rs;

    public BlurTransformation(Context context) {

        super();

        rs = RenderScript.create(context);

    }

    @Override

    public Bitmap transform(Bitmap bitmap) {

        Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Allocation input = Allocation.createFromBitmap(rs, blurredBitmap, Allocation.MipmapControl.MIPMAP_FULL, Allocation.USAGE_SHARED);

        Allocation output = Allocation.createTyped(rs, input.getType());

        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        script.setInput(input);

        //设置模糊半径

        script.setRadius(14);

        script.forEach(output);

        output.copyTo(blurredBitmap);

        bitmap.recycle();

        return blurredBitmap;

    }

    @Override

    public String key() {

        return "blur";

    }

}