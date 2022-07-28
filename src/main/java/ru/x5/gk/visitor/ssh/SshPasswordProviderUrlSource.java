package ru.x5.gk.visitor.ssh;

public class SshPasswordProviderUrlSource {

    private final String gkPasswordProviderUrl = "http://mrmtsx-gk.x5.ru:9992/password?shop=";

    public String get() {
        return gkPasswordProviderUrl;
    }

}
