package info.kgeorgiy.ja.fedoseev.walk;

import java.io.IOException;
import java.io.InputStream;

public class FNWHasher {
    static final int FNV_32_PRIME = 0x01000193;

    static int FNV1Hash(InputStream input) {
        int hval = 0x811c9dc5;
        try {
            int ch = input.read();
            while (ch != -1) {
                hval *= FNV_32_PRIME;
                hval ^= ch;
                ch = input.read();
            }
        } catch (IOException e) {
            hval = 0;
        }
        return hval;
    }
}
