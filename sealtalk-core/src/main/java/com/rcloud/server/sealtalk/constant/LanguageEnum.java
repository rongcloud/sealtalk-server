package com.rcloud.server.sealtalk.constant;

public enum LanguageEnum {



    ZH("zh"),
    EN("en"),
    AR("ar"),
    ;

    private final String language;

    LanguageEnum(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }
}
