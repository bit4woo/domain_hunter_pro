package title;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
// 来自sun.misc.CharacterEncoder

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

public abstract class CharacterEncoder {
    protected PrintStream pStream;

    public CharacterEncoder() {
    }

    protected abstract int bytesPerAtom();

    protected abstract int bytesPerLine();

    protected void encodeBufferPrefix(OutputStream var1) throws IOException {
        this.pStream = new PrintStream(var1);
    }

    protected void encodeBufferSuffix(OutputStream var1) throws IOException {
    }

    protected void encodeLinePrefix(OutputStream var1, int var2) throws IOException {
    }

    protected void encodeLineSuffix(OutputStream var1) throws IOException {
        this.pStream.println();
    }

    protected abstract void encodeAtom(OutputStream var1, byte[] var2, int var3, int var4) throws IOException;

    protected int readFully(InputStream var1, byte[] var2) throws IOException {
        for(int var3 = 0; var3 < var2.length; ++var3) {
            int var4 = var1.read();
            if (var4 == -1) {
                return var3;
            }

            var2[var3] = (byte)var4;
        }

        return var2.length;
    }

    public void encode(InputStream var1, OutputStream var2) throws IOException {
        byte[] var5 = new byte[this.bytesPerLine()];
        this.encodeBufferPrefix(var2);

        while(true) {
            int var4 = this.readFully(var1, var5);
            if (var4 == 0) {
                break;
            }

            this.encodeLinePrefix(var2, var4);

            for(int var3 = 0; var3 < var4; var3 += this.bytesPerAtom()) {
                if (var3 + this.bytesPerAtom() <= var4) {
                    this.encodeAtom(var2, var5, var3, this.bytesPerAtom());
                } else {
                    this.encodeAtom(var2, var5, var3, var4 - var3);
                }
            }

            if (var4 < this.bytesPerLine()) {
                break;
            }

            this.encodeLineSuffix(var2);
        }

        this.encodeBufferSuffix(var2);
    }

    public void encode(byte[] var1, OutputStream var2) throws IOException {
        ByteArrayInputStream var3 = new ByteArrayInputStream(var1);
        this.encode((InputStream)var3, var2);
    }

    public String encode(byte[] var1) {
        ByteArrayOutputStream var2 = new ByteArrayOutputStream();
        ByteArrayInputStream var3 = new ByteArrayInputStream(var1);
        String var4 = null;

        try {
            this.encode((InputStream)var3, var2);
            var4 = var2.toString("8859_1");
            return var4;
        } catch (Exception var6) {
            throw new Error("CharacterEncoder.encode internal error");
        }
    }

    private byte[] getBytes(ByteBuffer var1) {
        byte[] var2 = null;
        if (var1.hasArray()) {
            byte[] var3 = var1.array();
            if (var3.length == var1.capacity() && var3.length == var1.remaining()) {
                var2 = var3;
                var1.position(var1.limit());
            }
        }

        if (var2 == null) {
            var2 = new byte[var1.remaining()];
            var1.get(var2);
        }

        return var2;
    }

    public void encode(ByteBuffer var1, OutputStream var2) throws IOException {
        byte[] var3 = this.getBytes(var1);
        this.encode(var3, var2);
    }

    public String encode(ByteBuffer var1) {
        byte[] var2 = this.getBytes(var1);
        return this.encode(var2);
    }

    public void encodeBuffer(InputStream var1, OutputStream var2) throws IOException {
        byte[] var5 = new byte[this.bytesPerLine()];
        this.encodeBufferPrefix(var2);

        int var4;
        do {
            var4 = this.readFully(var1, var5);
            if (var4 == 0) {
                break;
            }

            this.encodeLinePrefix(var2, var4);

            for(int var3 = 0; var3 < var4; var3 += this.bytesPerAtom()) {
                if (var3 + this.bytesPerAtom() <= var4) {
                    this.encodeAtom(var2, var5, var3, this.bytesPerAtom());
                } else {
                    this.encodeAtom(var2, var5, var3, var4 - var3);
                }
            }

            this.encodeLineSuffix(var2);
        } while(var4 >= this.bytesPerLine());

        this.encodeBufferSuffix(var2);
    }

    public void encodeBuffer(byte[] var1, OutputStream var2) throws IOException {
        ByteArrayInputStream var3 = new ByteArrayInputStream(var1);
        this.encodeBuffer((InputStream)var3, var2);
    }

    public String encodeBuffer(byte[] var1) {
        ByteArrayOutputStream var2 = new ByteArrayOutputStream();
        ByteArrayInputStream var3 = new ByteArrayInputStream(var1);

        try {
            this.encodeBuffer((InputStream)var3, var2);
        } catch (Exception var5) {
            throw new Error("CharacterEncoder.encodeBuffer internal error");
        }

        return var2.toString();
    }

    public void encodeBuffer(ByteBuffer var1, OutputStream var2) throws IOException {
        byte[] var3 = this.getBytes(var1);
        this.encodeBuffer(var3, var2);
    }

    public String encodeBuffer(ByteBuffer var1) {
        byte[] var2 = this.getBytes(var1);
        return this.encodeBuffer(var2);
    }
}
