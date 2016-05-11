package org.infinispan.ensemble.test;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class WebPage implements Serializable{

    private String key;
    private byte[] content;

    public void setKey(String key) {
        this.key = key;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebPage webPage = (WebPage) o;

        if (key != null ? !key.equals(webPage.key) : webPage.key != null) return false;
        return Arrays.equals(content, webPage.content);

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }
}
