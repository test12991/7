/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2030 The XdagJ Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.xdag.net.message.consensus;

import io.xdag.core.MainBlock;
import io.xdag.net.message.Message;
import io.xdag.net.message.MessageCode;
import lombok.Getter;

@Getter
public class MainBlockMessage extends Message {

    private final MainBlock block;

    public MainBlockMessage(MainBlock block) {
        super(MessageCode.MAIN_BLOCK, null);

        this.block = block;

        this.body = block.toBytes();
    }

    public MainBlockMessage(byte[] body) {
        super(MessageCode.MAIN_BLOCK, null);

        this.block = MainBlock.fromBytes(body);

        this.body = body;
    }

    @Override
    public String toString() {
        return "BlockMessage [block=" + block + "]";
    }
}