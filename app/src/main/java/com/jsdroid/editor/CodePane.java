/*
 * Copyright 2018. who<980008027@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jsdroid.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;


/**
 * Created by Administrator on 2018/2/11.
 */

public class CodePane extends HorizontalScrollView {
    CodeText mCodeText;
    int mCodeTextMinWidth;

    public CodePane(Context context) {
        super(context);
        init();

    }

    public CodePane(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //设置背景颜色
        setBackgroundColor(0xffffffff);
        mCodeText = new CodeText(getContext());
        mCodeText.setScrollView(this);
        addView(mCodeText, params);
        setHorizontalScrollBarEnabled(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //计算CodeText宽高
        int codeWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        if (mCodeTextMinWidth != codeWidth) {
            mCodeTextMinWidth = codeWidth;
            mCodeText.setMinWidth(mCodeTextMinWidth);
            invalidate();
            return;
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        //滑动的时候，通知CodeText绘制高亮
        mCodeText.postInvalidate();
    }

    public CodeText getCodeText() {
        return mCodeText;
    }

}