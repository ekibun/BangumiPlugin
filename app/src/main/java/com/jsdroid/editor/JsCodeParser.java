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

import org.mozilla.javascript.Token;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2018/2/12.
 */

public class JsCodeParser implements Runnable {
    private static ExecutorService parserThreadPool = Executors.newSingleThreadExecutor();
    boolean running;
    CodeText codeText;
    //start:改变的位置
    int start;
    //before:删除的数量
    int before;
    //count:添加的数量
    int count;

    public JsCodeParser(CodeText codeText) {
        this.codeText = codeText;
    }

    public void run() {
        //颜色移动位置
        //start:改变的位置
        //before:删除的数量
        //count:添加的数量
        //如果添加的数量大于删除的数量，右移动;否则，左移动
        int[] codeColors = codeText.getCodeColors();
        if (count > before) {
            //添加
            //右移动[1,2,3,0,0] >> [0,0,1,2,3]
            int off = count - before;
            for (int i = codeColors.length - 1; i > start + off && i > 1; i--) {
                if (reparse) {
                    break;
                }
                codeColors[i] = codeColors[i - off];
            }
        } else {
            //删除
            //左移动 [0,0,1,2,3] >> [1,2,3,0,0]
            int off = before - count;
            for (int i = start; i + off < codeColors.length; i++) {
                if (reparse) {
                    break;
                }
                codeColors[i] = codeColors[i + off];
            }
        }
        if (running) {
            codeText.postInvalidate();
        }
        try {
            TokenStream ts = new TokenStream(null, codeText.getText()
                    .toString(), 0);
            while (running) {
                if (reparse) {
                    break;
                }
                try {
                    int token = ts.getToken();
                    if (token == Token.EOF) {
                        codeText.postInvalidate();
                        break;
                    }
                    int color = getColor(token);
                    for (int i = ts.getTokenBeg(); i <= ts.getTokenEnd(); i++) {
                        codeColors[i] = color;
                    }
                } catch (Exception e) {
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        if (reparse) {
            reparse = false;
            reparse();
        }
        running = false;
    }

    private boolean reparse;

    private void reparse() {
        parserThreadPool.execute(this);
    }

    public synchronized void parse(int start, int before,
                                   int count) {
        if (running) {
            reparse = true;
            return;
        }
        running = true;
        this.start = start;
        this.before = before;
        this.count = count;
        parserThreadPool.execute(this);
    }

    public static int getColor(int token) {
        switch (token) {
            case Token.ERROR:
                return  0xffff0000;
            case Token.FUNCTION:
            case Token.VAR:
            case Token.BREAK:
            case Token.CASE:
            case Token.CONTINUE:
            case Token.DEFAULT:
            case Token.DELPROP:
            case Token.DO:
            case Token.ELSE:
            case Token.FALSE:
            case Token.FOR:
            case Token.IF:
            case Token.IN:
            case Token.NEW:
            case Token.NULL:
            case Token.RETURN:
            case Token.SWITCH:
            case Token.THIS:
            case Token.TRUE:
            case Token.TYPEOF:
            case Token.VOID:
            case Token.WHILE:
            case Token.WITH:
            case Token.CATCH:
            case Token.FINALLY:
            case Token.INSTANCEOF:
            case Token.THROW:
            case Token.TRY:
                return 0xff0055ff;
            case Token.NUMBER://
                return 0xff09885a;
            case Token.STRING:
                return 0xffa31515;
            case Token.NAME:
                return 0xff4e1080;
            case Token.COMMENT:
                return 0xff008000;
        }
        return 0xff000000;
    }
}
