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
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;

/**
 * Created by Administrator on 2018/2/11.
 */

public class CodeText extends ColorsText {
    JsCodeParser codeParser;

    public CodeText(Context context) {
        super(context);
        init();
    }

    public CodeText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        codeParser = new JsCodeParser(this);
        // 动态解析js代码更新文字颜色
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Layout layout = getLayout();
                if(layout != null && before == 0 && s.subSequence(start, start + count).toString().equals("\n")){
                    int line = layout.getLineForOffset(start);
                    int startPos = layout.getLineStart(line);
                    String space = "";
                    for (int i = startPos; i < s.length(); i++) {
                        if (getText().charAt(i) == ' ') {
                            space += " ";
                        } else {
                            break;
                        }
                    }
                    String insert = space;

                    char beforeChar = 0, afterChar = 0;
                    if(start != 0) beforeChar = s.charAt(start-1);
                    if(start + count < s.length()) afterChar = s.charAt(start + count);
                    if (beforeChar == '{' || beforeChar == '[' || beforeChar == '(') {
                        insert += "    ";
                    }
                    int selectionPos = start + 1 + insert.length();
                    if ((beforeChar == '{' && afterChar == '}') || (beforeChar == '[' && afterChar == ']') || (beforeChar == '(' && afterChar == ')')) {
                        insert += "\n" + space;
                    }
                    getText().insert(start + 1, insert);
                    setSelection(selectionPos);
                    return;
                }
                codeParser.parse(start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                getParent().requestLayout();
            }
        });
    }

}