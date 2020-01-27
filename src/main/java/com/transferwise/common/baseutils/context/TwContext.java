package com.transferwise.common.baseutils.context;

import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class TwContext {
    private static final ThreadLocal<TwContext> contextTl = new ThreadLocal<>();

    public static final String GROUP_KEY = "TW_CONTEXT_GROUP";
    public static final String NAME_KEY = "TW_CONTEXT_NAME";
    public static final String GROUP_GENERIC = "GENERIC";

    public static TwContext current() {
        TwContext context = contextTl.get();
        return context;
    }

    public static TwContext subContext() {
        return new TwContext();
    }

    @Getter
    private TwContext parent;
    @Getter
    private Map<String, Object> attributes;

    public TwContext() {
        this(TwContext.current());
    }

    public TwContext(TwContext parent) {
        this.parent = parent;
        if (parent == null) {
            attributes = new HashMap<>();
            setGroup(GROUP_GENERIC);
        } else {
            attributes = new HashMap<>(parent.attributes);
        }
    }

    public TwContext asEntryPoint(@NonNull String group, @NonNull String name) {
        set(GROUP_KEY, group);
        set(NAME_KEY, name);
        return this;
    }

    public TwContext attach() {
        TwContext current = contextTl.get();
        contextTl.set(this);
        return current;
    }

    public void detach(TwContext previous) {
        contextTl.set(previous);
    }

    public <T> T get(String key) {
        return (T) attributes.get(key);
    }

    public TwContext set(String key, Object value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
        return this;
    }

    public void replaceValueDeep(String key, Object search, Object value) {
        TwContext context = this;
        int i = 0;
        while (context != null) {
            if (Objects.equals(context.get(key), value)) {
                context.set(key, value);
            }
            context = context.getParent();
            if (i++ > 1_000_000) {
                throw new IllegalStateException("Indefinite loop detected.");
            }
        }
    }

    public TwContext setName(String name) {
        set(NAME_KEY, name);
        return this;
    }

    public TwContext setGroup(String group) {
        set(GROUP_KEY, group);
        return this;
    }

    public String getName() {
        return get(NAME_KEY);
    }

    public String getGroup() {
        return get(GROUP_KEY);
    }

    public <T> T execute(Supplier<T> supplier) {
        TwContext previous = attach();
        try {
            return supplier.get();
        } finally {
            detach(previous);
        }
    }

    public void execute(Runnable runnable) {
        TwContext previous = attach();
        try {
            runnable.run();
        } finally {
            detach(previous);
        }
    }
}
