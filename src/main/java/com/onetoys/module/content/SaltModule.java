package com.onetoys.module.content;

public class SaltModule {
    public static final String ID = "salt";
    private static final SaltModule INSTANCE = new SaltModule();

    public static SaltModule getInstance() {
        return INSTANCE;
    }

    public static boolean isSaltEnabled() {
        return ContentModule.isSaltEnabled();
    }

    public boolean isEnabled() {
        return isSaltEnabled();
    }
}
