package com.nfcspoofer;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

import java.lang.reflect.Method;

public class MainHook extends XposedModule {

    public MainHook(XposedInterface base, XposedModuleInterface.ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onSystemServerStarting() {
        log("NFC Spoofer: Hooking system_server");

        hookHasSystemFeature();
        hookNfcAdapter();
    }

    @XposedHooker
    static class HasSystemFeatureHooker implements Hooker {
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            String feature = (String) callback.getArgs()[0];
            if (feature != null && feature.contains("nfc")) {
                callback.returnAndSkip(true);
            }
        }
    }

    private void hookHasSystemFeature() {
        try {
            Method method = Class.forName("android.content.pm.PackageManager")
                .getMethod("hasSystemFeature", String.class);
            hook(method, HasSystemFeatureHooker.class);
            log("NFC Spoofer: Hooked PackageManager.hasSystemFeature");
        } catch (Throwable t) {
            log("NFC Spoofer: hasSystemFeature failed: " + t.getMessage());
        }

        try {
            Method method = Class.forName("com.android.server.pm.PackageManagerService")
                .getMethod("hasSystemFeature", String.class);
            hook(method, HasSystemFeatureHooker.class);
            log("NFC Spoofer: Hooked PMS internal hasSystemFeature");
        } catch (Throwable t) {
            log("NFC Spoofer: PMS internal hook skipped: " + t.getMessage());
        }
    }

    @XposedHooker
    static class IsEnabledHooker implements Hooker {
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            callback.returnAndSkip(true);
        }
    }

    @XposedHooker
    static class GetAdapterStateHooker implements Hooker {
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            callback.returnAndSkip(3);
        }
    }

    private void hookNfcAdapter() {
        try {
            Class<?> nfcAdapter = Class.forName("android.nfc.NfcAdapter");

            Method isEnabled = nfcAdapter.getMethod("isEnabled");
            hook(isEnabled, IsEnabledHooker.class);

            Method getState = nfcAdapter.getMethod("getAdapterState");
            hook(getState, GetAdapterStateHooker.class);

            log("NFC Spoofer: Hooked NfcAdapter framework methods");
        } catch (Throwable t) {
            log("NFC Spoofer: NfcAdapter hook failed: " + t.getMessage());
        }

        try {
            Class<?> nfcService = Class.forName("com.android.nfc.NfcService");
            Method getState = nfcService.getMethod("getState");
            hook(getState, GetAdapterStateHooker.class);
            log("NFC Spoofer: Hooked NfcService.getState");
        } catch (Throwable t) {
            log("NFC Spoofer: NfcService not present (expected): " + t.getMessage());
        }
    }
}
