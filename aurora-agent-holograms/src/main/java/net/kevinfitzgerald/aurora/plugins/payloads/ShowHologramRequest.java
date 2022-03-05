package net.kevinfitzgerald.aurora.plugins.payloads;

import java.util.Arrays;

public class ShowHologramRequest {
    public String id = null;
    public String permission = null;
    public Integer point = null;
    public Object[] lines;

    public ShowHologramRequest() {

    }

    @Override
    public String toString() {
        return "ShowHologramRequest{ " +
            "id='" + id + '\'' +
            ", permission='" + permission + '\'' +
            ", point=" + point +
            ", lines=" + Arrays.toString(lines) +
            " }";
    }
}
